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
package com.github.leanan.sidhelle.media.internal.codec;

import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.model.VideoFrame;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VideoDecoder} using the test-pattern.mp4 resource.
 * <p>
 * This class validates the full decoding pipeline: from demuxing packets using 
 * {@link SourceTask}, through the {@link VideoDecoder} loop, to the production 
 * of {@link VideoFrame} records.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class VideoDecoderTest {

    /** Path to the test video file. */
    private String testVideoPath;
    
    /** The decoder instance under test. */
    private VideoDecoder videoDecoder;

    /**
     * Initializes the native FFmpeg environment and locates the test resource.
     */
    @BeforeEach
    public void setUp() {
        /* Initialize native libraries for demuxing and decoding */
        avformat.avformat_network_init();

        /* Resolve the test resource from the classpath */
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        assertNotNull(resource, "test-pattern.mp4 not found in src/test/resources");
        testVideoPath = new File(resource.getFile()).getAbsolutePath();

        videoDecoder = new VideoDecoder();
    }

    /**
     * Reclaims native resources allocated during the test.
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (videoDecoder != null) {
            videoDecoder.close();
        }
    }

    /**
     * Validates that the VideoDecoder can produce high-definition BufferedImages.
     * <p>
     * This test executes the {@link SourceTask} to feed the packet queue and 
     * verifies that the resulting {@link VideoFrame} matches the 1920x1080 
     * resolution specified in the test file's metadata.
     * </p>
     */
    @Test
    @DisplayName("Test Video Decoding and BufferedImage Conversion")
    void testVideoDecoding() throws Exception {
        /* Use try-with-resources to ensure MediaState (and its native contexts) are closed */
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                .build()) {

            /* Extract the primary source and its video stream */
            SourceState<?> source = state.getSources().get(0);
            StreamState videoStream = source.getStreams().stream()
                    .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_VIDEO)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Video stream not found"));

            int decodedFrameCount = 0;
            int packetReadLimit = 100; // Safety limit to avoid infinite loops on EOF

            /* 
             * Loop until we've verified several frames. 
             * Note: Due to B-frames (has_b_frames=2), we must demux several 
             * packets before the first frame is available.
             */
            while (decodedFrameCount < 5 && packetReadLimit-- > 0) {
                
                /* 1. Execute the demuxing task for this specific source */
                source.getDemuxTask().apply(source);

                /* 2. Process the packet queue through the decoder */
                videoDecoder.decode(videoStream,false);

                /* 3. Check for decoded frames in the stream's frame queue */
                VideoFrame frame = (VideoFrame) videoStream.frameQueue().poll();
                
                if (frame != null) {
                    decodedFrameCount++;
                    
                    /* Verify standard Java record properties */
                    assertNotNull(frame.image(), "VideoFrame must contain a BufferedImage");
                    assertEquals(1920, frame.image().getWidth(), "Width mismatch for test-pattern.mp4");
                    assertEquals(1080, frame.image().getHeight(), "Height mismatch for test-pattern.mp4");
                    
                    /* Clean Break Check: Ensure we have no native references lingering */
                    assertDoesNotThrow(() -> frame.close(), "Closing a VideoFrame should be a safe no-op");
                }
            }

            assertTrue(decodedFrameCount > 0, "No frames were decoded within the packet limit");
        }
    }

    /**
     * Verifies that the decoder can be reset without crashing native memory.
     */
    @Test
    @DisplayName("Test Decoder Reset Functionality")
    void testDecoderReset() throws Exception {
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                .build()) {

            StreamState videoStream = state.getSources().get(0).getStreams().get(0);
            
            /* Resetting is critical for seeking; it should flush internal codec buffers */
            assertDoesNotThrow(() -> videoDecoder.reset(videoStream), 
                "Decoder reset failed to flush native buffers");
        }
    }
}

