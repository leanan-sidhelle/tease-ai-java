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
import java.awt.image.BufferedImage;

/**
 * A video-specific implementation of {@link MediaFrame}.
 * Maintains a reference to the native {@link AVFrame} until processed, 
 * and holds the converted {@link BufferedImage}.
 * 
 * @param nativeFrame The underlying native FFmpeg frame.
 * @param image       The Java2D representation of the frame.
 * @param pts         Presentation timestamp in stream timebase.
 * @param source      The ordinal index of the input source.
 * @param stream      The index of the stream within the source.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public record VideoFrame(
    AVFrame nativeFrame,
    BufferedImage image,
    long pts,
    int source,
    int stream
) implements MediaFrame {

    /**
     * Constructs a VideoFrame by converting the native frame using {@link ImageUtils}.
     * 
     * @param nativeFrame The raw decoded frame.
     * @param source      The source ID.
     * @param stream      The stream ID.
     * @return A new VideoFrame containing the converted image.
     */
    public static VideoFrame of(AVFrame nativeFrame, int source, int stream,BufferedImage img) {
        return new VideoFrame(nativeFrame, img, nativeFrame.pts(), source, stream);
    }

    @Override public AVFrame getNativeFrame() { return nativeFrame; }
    @Override public long getPts() { return pts; }
    @Override public int getSourceIndex() { return source; }
    @Override public int getStreamIndex() { return stream; }

    /**
     * Correctly unreferences and frees the native AVFrame.
     * Must be called by the consumer thread once processing is finished.
     */
    @Override
    public void close() {
        if (nativeFrame != null && !nativeFrame.isNull()) {
            // Unref decrements the internal reference count of the data buffers.
            avutil.av_frame_unref(nativeFrame);
            // Free releases the actual struct pointer.
            avutil.av_frame_free(nativeFrame);
        }
    }
}
