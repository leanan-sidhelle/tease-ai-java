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

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for the {@link AudioFrame} record.
 * Verifies that the record successfully holds interleaved PCM bytes 
 * and maintains the native lifecycle of the {@link AVFrame}.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
class AudioFrameTest {

    /**
     * Verifies record properties and ensures close() releases the native struct.
     */
    @Test
    @DisplayName("Verify AudioFrame Record with Interleaved PCM Data")
    void testAudioFrameLifecycle() {
        // 1. Setup native frame (Independent of decoder)
        AVFrame nativeFrame = avutil.av_frame_alloc();
        assertNotNull(nativeFrame);
        long testPts = 55555L;
        nativeFrame.pts(testPts);

        // 2. Setup mock interleaved PCM data (e.g., 1024 bytes of silence)
        byte[] mockPcmData = new byte[1024];

        // 3. Instantiate record
        int sourceId = 0;
        int streamId = 1;
        AudioFrame audioFrame = AudioFrame.of(nativeFrame, mockPcmData, sourceId, streamId);

        // 4. Validate Property Mapping
        assertArrayEquals(mockPcmData, audioFrame.audioData(), "PCM data buffer mismatch.");
        assertEquals(testPts, audioFrame.pts(), "PTS mismatch.");
        assertEquals(sourceId, audioFrame.getSourceIndex());

        // 5. Verify Native Lifecycle
        audioFrame.close();
        assertTrue(nativeFrame.isNull(), "Native AVFrame was not freed correctly.");
    }
}
