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
package com.github.leanan.sidhelle.media.internal.thread;

import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.model.AudioFrame;
import com.github.leanan.sidhelle.media.api.model.SubtitleFrame;
import com.github.leanan.sidhelle.media.api.model.VideoFrame;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;

/**
 * Integration test suite for the {@link ThreadedMediaPlayer}.
 * <p>
 * This class verifies the accuracy and thread-safety of the asynchronous 
 * media pipeline. It ensures that video, audio, and subtitle streams 
 * are processed in parallel without data loss or race conditions.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class ThreadedMediaPlayerTest {

    /** 
     * The absolute file system path to the test media resource. 
     */
    private String testVideoPath;

    /** 
     * Thread-safe counter for total video frames delivered. 
     */
    private final AtomicInteger videoFrameCount = new AtomicInteger(0);
        
    /** 
     * Thread-safe counter for total audio frames delivered. 
     */
    private final AtomicInteger audioFrameCount = new AtomicInteger(0);

    /** 
     * Thread-safe counter for total subtitle frames delivered. 
     */
    private final AtomicInteger subtitleFrameCount = new AtomicInteger(0);

    /**
     * Environment setup executed before each test.
     * <p>
     * Initializes the native FFmpeg network layer and resolves the 
     * "test-pattern.mp4" file from the local classpath.
     * </p>
     */
    @BeforeEach
    public void setUp() {
        /* Initialize native protocols for stream handling */
        avformat_network_init();
        
        /* Resolve the media file containing V, A, and S streams */
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        if (resource == null) {
            throw new RuntimeException("Media resource 'test-pattern.mp4' not found.");
        }
        testVideoPath = new File(resource.getFile()).getAbsolutePath();
    }

    /**
     * Validates full asynchronous playback and frame delivery accuracy.
     * <p>
     * This test executes the threaded player loop and verifies that every 
     * frame from the multiple background decoding threads is correctly 
     * routed to the consumer.
     * </p>
     * 
     * @throws Exception if media initialization or execution fails.
     */
    @Test
    @DisplayName("Verify Threaded Playback: Parallel Video, Audio, and Subtitles")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testThreadedFullPlayback() throws Exception {
        /* Reset counters for a clean test execution */
        videoFrameCount.set(0);
        audioFrameCount.set(0);
        subtitleFrameCount.set(0);

        /* 1. Initialize MediaState with mapped consumers for parallel tracking */
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                /* 
                 * Map stream selectors to consumer methods. The ThreadedMediaPlayer 
                 * will dispatch these from its presentation thread.
                 */
                .map("v:0", this::consumeVideoFrame)
                .map("a:0", this::consumeAudioFrame)
                .map("s:0", this::consumeSubtitleFrame)
                .build()) {

            /* 2. Initialize and run the player */
            ThreadedMediaPlayer player = new ThreadedMediaPlayer(state);
            
            /* 
             * This call blocks the test thread until the player's internal 
             * 'isPlaybackComplete()' logic determines all queues are exhausted.
             */
            player.run();

            /* 
             * 3. Assertions:
             * Expected values correspond to the ffprobe metadata of 'test-pattern.mp4'.
             */
            assertEquals(390, videoFrameCount.get(), "Threaded video frame count mismatch.");
            assertEquals(610, audioFrameCount.get(), "Threaded audio frame count mismatch.");
            assertEquals(3, subtitleFrameCount.get(), "Threaded subtitle frame count mismatch.");

            System.out.printf("Threaded Verification Complete - V: %d, A: %d, S: %d%n", 
                videoFrameCount.get(), audioFrameCount.get(), subtitleFrameCount.get());
        }
    }
    
    /**
     * Increments the video frame counter upon delivery.
     * 
     * @param frame the {@link VideoFrame} processed by the decoder.
     */
    protected void consumeVideoFrame(VideoFrame frame) {
        videoFrameCount.incrementAndGet();
    }
    
    /**
     * Increments the audio frame counter upon delivery.
     * 
     * @param frame the {@link AudioFrame} processed by the decoder.
     */
    protected void consumeAudioFrame(AudioFrame frame) {
        audioFrameCount.incrementAndGet();
    }

    /**
     * Increments the subtitle frame counter upon delivery.
     * 
     * @param frame the {@link SubtitleFrame} processed by the decoder.
     */
    protected void consumeSubtitleFrame(SubtitleFrame frame) {
        subtitleFrameCount.incrementAndGet();
    }
}
