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
package com.github.leanan.sidhelle.media.internal;

import com.github.leanan.sidhelle.media.api.MediaState;
import com.github.leanan.sidhelle.media.api.SourceSpec;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.*;
import java.io.File;
import java.net.URL;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the loading phase of the MediaState API.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaStateTest {

    /**
     * TODO:
     */
    private static String testFilePath;
    
    /**
     * TODO:
     */
    private MediaState mediaState;

    /**
     * TODO:
     */
    @BeforeEach
    public void setup() {
        // Resolve test file from src/test/resources
        URL resource = getClass().getResource("/test-pattern.mp4");
        assertNotNull(resource, "Test file 'test-pattern.mp4' must exist in resources.");
        testFilePath = new File(resource.getFile()).getAbsolutePath();
    }

    /**
     * TODO:
     */
    @Test
    @DisplayName("Verify source ordinal mapping and stream detection")
    public void testStateInitialization() {
        List<SourceSpec> inputs = List.of(SourceSpec.ofFile(testFilePath));
        mediaState = new DefaultMediaState(inputs);

        assertEquals(1, mediaState.getSourceCount());
        
        int streams = mediaState.getStreamCount(0);
        assertTrue(streams > 0);

        // Verify that video and audio decoders were initialized
        boolean videoFound = false;
        for (int i = 0; i < streams; i++) {
            var ctx = mediaState.codecContext(0, i);
            if (ctx != null && ctx.codec_type() == avutil.AVMEDIA_TYPE_VIDEO) {
                videoFound = true;
            }
        }
        assertTrue(videoFound, "Video stream should be detected and opened.");
    }

    /**
     * TODO:
     */
    @AfterEach
    public void tearDown() {
        if (mediaState != null) mediaState.close();
    }
}
