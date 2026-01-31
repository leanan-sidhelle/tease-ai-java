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

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;

/**
 * An audio-specific implementation of {@link MediaFrame}.
 * Holds both the native {@link AVFrame} and the interleaved PCM data 
 * ready for standard Java Audio APIs.
 * 
 * @param nativeFrame The underlying native FFmpeg frame.
 * @param audioData   Interleaved PCM samples (e.g., S16 or Float32).
 * @param pts         Presentation timestamp.
 * @param source      The ordinal index of the input source.
 * @param stream      The stream index within the source.
 */
public record AudioFrame(
    AVFrame nativeFrame,
    byte[] audioData,
    long pts,
    int source,
    int stream
) implements MediaFrame {

    /**
     * Factory method to construct an AudioFrame with pre-converted PCM data.
     * 
     * @param nativeFrame The raw decoded frame.
     * @param audioData   The interleaved PCM byte buffer.
     * @param source      The source ID.
     * @param stream      The stream ID.
     * @return A new AudioFrame instance.
     */
    public static AudioFrame of(AVFrame nativeFrame, byte[] audioData, int source, int stream) {
        return new AudioFrame(nativeFrame, audioData, nativeFrame.pts(), source, stream);
    }

    @Override public AVFrame getNativeFrame() { return nativeFrame; }
    @Override public long getPts() { return pts; }
    @Override public int getSourceIndex() { return source; }
    @Override public int getStreamIndex() { return stream; }

    /**
     * Releases the native FFmpeg audio frame.
     */
    @Override
    public void close() {
        if (nativeFrame != null && !nativeFrame.isNull()) {
            avutil.av_frame_unref(nativeFrame);
            avutil.av_frame_free(nativeFrame);
        }
    }
}
