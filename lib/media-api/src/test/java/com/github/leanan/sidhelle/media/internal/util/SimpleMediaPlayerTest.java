/**
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <https://unlicense.org>
 */
package com.github.leanan.sidhelle.media.internal.util;


import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.model.AudioFrame;
import com.github.leanan.sidhelle.media.api.model.SubtitleFrame;
import com.github.leanan.sidhelle.media.api.model.VideoFrame;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;
import org.bytedeco.ffmpeg.global.avformat;



import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration test suite for {@link SimpleMediaPlayer}.
 * <p>
 * This class validates the sequential playback lifecycle, ensuring that video, 
 * audio, and subtitle frames are correctly demuxed, decoded, and dispatched 
 * to consumers in a single-threaded execution context.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class SimpleMediaPlayerTest {

    /** 
     * The absolute file system path to the test media resource. 
     */
    private String testVideoPath;

    /** 
     * Thread-safe counter to track the total number of video frames 
     * received by the consumer. 
     */
    private final AtomicInteger videoFrameCount = new AtomicInteger(0);
        
    /** 
     * Thread-safe counter to track the total number of audio frames 
     * received by the consumer. 
     */
    private final AtomicInteger audioFrameCount = new AtomicInteger(0);

    /** 
     * Thread-safe counter to track the total number of subtitle frames 
     * received by the consumer. 
     */
    private final AtomicInteger subtitleFrameCount = new AtomicInteger(0);

    /**
     * Pre-test configuration.
     * <p>
     * Initializes native FFmpeg network components and resolves the 
     * "test-pattern.mp4" resource from the classpath.
     * </p>
     */
    @BeforeEach
    public void setUp() {
        /* Initialize FFmpeg network protocols */
        avformat.avformat_network_init();
        
        /* Locate the test resource containing V, A, and S streams */
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        if (resource == null) {
            throw new RuntimeException("Required test resource 'test-pattern.mp4' is missing.");
        }
        testVideoPath = new File(resource.getFile()).getAbsolutePath();
    }

    /**
     * Executes a full playback session and verifies frame delivery accuracy.
     * <p>
     * This test maps specific stream types to consumer methods and asserts 
     * that the final counts match expected values derived from ffprobe.
     * </p>
     * 
     * @throws Exception if media state initialization or playback fails.
     */
    @Test
    @DisplayName("Verify Sequential Playback: Video, Audio, and Subtitles")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testFullPlaybackWithVerification() throws Exception {
        /* Reset counters for a clean state */
        videoFrameCount.set(0);
        audioFrameCount.set(0);
        subtitleFrameCount.set(0);

        /* 1. Initialize MediaState with mapped consumers for verification */
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                /* Map consumers to increment counters on every presentation event */
                .map("v:0", this::consumeVideoFrame)
                .map("a:0", this::consumeAudioFrame)
                .map("s:0", this::consumeSubtitleFrame)
                .build()) {

            SimpleMediaPlayer player = new SimpleMediaPlayer(state);

            /* 2. Run the player lifecycle (blocks until internal completion) */
            player.run();

            /* 
             * 3. FINAL SYNCHRONIZATION DRAIN:
             * Forces the clock forward and triggers manual presentation cycles 
             * to ensure any remaining frames in the queues are processed 
             * before final assertion.
             */
            int retryCount = 0;
            while ((videoFrameCount.get() < 390 || audioFrameCount.get() < 610) && retryCount < 10) {
                /* Force the master clock to the end of time */
                state.getClock().seek(Long.MAX_VALUE);

                /* Manually trigger presentation of queued frames */
                com.github.leanan.sidhelle.media.util.MediaUtils.presentReadyFrames(state);

                /* Allow time for memory visibility across atomic updates */
                Thread.sleep(50);
                retryCount++;
            }

            /* 4. Assertion of frame counts based on known file metadata */
            assertEquals(390, videoFrameCount.get(), "Video frame count mismatch.");
            assertEquals(610, audioFrameCount.get(), "Audio frame count mismatch.");
            assertEquals(3, subtitleFrameCount.get(), "Subtitle frame count mismatch.");

            System.out.printf("Playback Verified - Video: %d, Audio: %d, Subs: %d%n", 
                videoFrameCount.get(), audioFrameCount.get(), subtitleFrameCount.get());
        }
    }
    
    /**
     * Callback method for video frame consumption.
     * 
     * @param frame the {@link VideoFrame} presented by the player.
     */
    protected void consumeVideoFrame(VideoFrame frame) {
        videoFrameCount.incrementAndGet();
    }
    
    /**
     * Callback method for audio frame consumption.
     * 
     * @param frame the {@link AudioFrame} presented by the player.
     */
    protected void consumeAudioFrame(AudioFrame frame) {
        audioFrameCount.incrementAndGet();
    }

    /**
     * Callback method for subtitle frame consumption.
     * 
     * @param frame the {@link SubtitleFrame} presented by the player.
     */
    protected void consumeSubtitleFrame(SubtitleFrame frame) {
        subtitleFrameCount.incrementAndGet();
    }
}
