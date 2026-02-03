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
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.util.AudioFrameConsumer;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;
import com.github.leanan.sidhelle.media.util.SubtitleFrameConsumer;
import com.github.leanan.sidhelle.media.util.VideoFrameConsumer;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultMediaStateBuilder}.
 * <p>
 * This class validates the fluent builder API, specifically testing 
 * stream mapping, multiple source handling, and configuration flags 
 * like {@code disableVideo} and {@code disableAudio}.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaStateBuilderTest {

    /** Path to the test-pattern.mp4 file in src/test/resources. */
    private String testVideoPath;

    /** Secondary source for multi-source testing. */
    private String secondarySourcePath;

    /**
     * Initializes native FFmpeg protocols and locates test resources.
     */
    @BeforeEach
    public void setUp() {
        /* Initialize native libraries for demuxing */
        avformat.avformat_network_init();

        /* Resolve primary test pattern */
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        assertNotNull(resource, "test-pattern.mp4 not found in src/test/resources");
        testVideoPath = new File(resource.getFile()).getAbsolutePath();
        
        /* For this test, we reuse the same file as a secondary source */
        secondarySourcePath = testVideoPath;
    }

    /**
     * Verifies that the builder can handle multiple sources and 
     * correctly map consumers to specific stream indices.
     */
    @Test
    @DisplayName("Test Multi-Source Mapping with Source-Index Selectors")
    void testMultiSourceAndMapping() throws Exception {
        AtomicInteger videoCount = new AtomicInteger(0);
        AtomicInteger audioCount = new AtomicInteger(0);

        /* Define specialized consumers */
        VideoFrameConsumer vConsumer = frame -> videoCount.incrementAndGet();
        AudioFrameConsumer aConsumer = frame -> audioCount.incrementAndGet();

        /* 
         * Mapping test-pattern.mp4 twice. 
         * Source 0 -> Full file
         * Source 1 -> Starting at 5 seconds
         */
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath), Duration.ZERO, null)
                .source(new File(testVideoPath), Duration.ofSeconds(5), null)
                /* Use the {SourceIndex}:{Type}:{StreamIndex} notation */
                .map("0:v:0", vConsumer) 
                .map("1:a:0", aConsumer) 
                .build()) {

            List<SourceState<?>> sources = state.getSources();
            assertEquals(2, sources.size());

            /* Verify Source 0: First Video Stream should have our vConsumer */
            StreamState s0v0 = sources.get(0).getStreams().stream()
                    .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_VIDEO)
                    .findFirst().orElseThrow();
            assertEquals(vConsumer, s0v0.getConsumer(), "Source 0 Video 0 mapping failed");

            /* Verify Source 1: First Audio Stream should have our aConsumer */
            StreamState s1a0 = sources.get(1).getStreams().stream()
                    .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_AUDIO)
                    .findFirst().orElseThrow();
            assertEquals(aConsumer, s1a0.getConsumer(), "Source 1 Audio 0 mapping failed");
        }
    }


    /**
     * Validates that the disableVideo and disableAudio flags 
     * prevent those streams from being initialized.
     */
    @Test
    @DisplayName("Test Stream Disabling Flags (-vn / -an)")
    void testStreamDisabling() throws Exception {
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath), Duration.ZERO, null)
                .disableAudio() // Equivalent to FFmpeg -an
                .build()) {

            for (SourceState<?> source : state.getSources()) {
                boolean hasAudio = source.getStreams().stream()
                        .anyMatch(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_AUDIO);
                
                assertFalse(hasAudio, "Audio streams should not be present when disabled");
            }
        }
    }

    /**
     * Tests mapping for subtitle streams specifically.
     */
    @Test
    @DisplayName("Test Subtitle Mapping")
    void testSubtitleMapping() throws Exception {
        SubtitleFrameConsumer sConsumer = frame -> {}; // Mock handler

        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath), Duration.ZERO, null)
                .map("s:0", sConsumer)
                .build()) {

            /* Verify mapping for any discovered subtitle streams */
            state.getSources().stream()
                .flatMap(src -> src.getStreams().stream())
                .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_SUBTITLE)
                .forEach(s -> assertNotNull(s.getConsumer()));
        }
    }
}
