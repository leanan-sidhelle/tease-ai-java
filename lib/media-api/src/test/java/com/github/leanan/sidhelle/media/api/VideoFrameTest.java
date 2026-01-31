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
package com.github.leanan.sidhelle.media.api;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;
import java.awt.image.BufferedImage;


/**
 * Unit test for the {@link VideoFrame} record.
 * 
 * This test is decoupled from MediaState and ImageUtils. It verifies that the record
 * correctly maintains its properties and, most importantly, handles the native
 * cleanup of the {@link AVFrame} struct upon closure.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
class VideoFrameTest {

    /**
     * Verifies that VideoFrame correctly stores its parameters and 
     * manages the native lifecycle of the allocated AVFrame.
     */
    @Test
    @DisplayName("Verify VideoFrame Record Integrity and Lifecycle")
    void testVideoFrameLifecycle() {
        // 1. Manually allocate a native AVFrame (Independent of MediaState)
        AVFrame nativeFrame = avutil.av_frame_alloc();
        assertNotNull(nativeFrame, "FFmpeg failed to allocate a native AVFrame struct.");
        
        // Setup some dummy metadata on the native struct
        long testPts = 12345L;
        nativeFrame.pts(testPts);

        // 2. Create a dummy BufferedImage (Independent of ImageUtils)
        int width = 640;
        int height = 480;
        BufferedImage dummyImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // 3. Instantiate the VideoFrame record
        int sourceId = 1;
        int streamId = 0;
        VideoFrame videoFrame = VideoFrame.of(nativeFrame, sourceId, streamId, dummyImage);

        // 4. Validate Property Mapping
        assertEquals(dummyImage, videoFrame.image(), "The BufferedImage was not stored correctly.");
        assertEquals(testPts, videoFrame.pts(), "The PTS was not mapped correctly from the native frame.");
        assertEquals(sourceId, videoFrame.getSourceIndex(), "The source index was not mapped correctly.");
        assertEquals(streamId, videoFrame.getStreamIndex(), "The stream index was not mapped correctly.");
        assertEquals(nativeFrame, videoFrame.getNativeFrame(), "The native frame reference was not maintained.");

        // 5. Verify Native Lifecycle Management
        // Closing the VideoFrame must trigger the release of the native AVFrame.
        videoFrame.close();
        
        // In the Bytedeco/FFmpeg API, after av_frame_free, the pointer's address 
        // will report as being null/invalid.
        assertTrue(nativeFrame.isNull(), "The native AVFrame was not freed correctly during VideoFrame.close().");
    }
}

