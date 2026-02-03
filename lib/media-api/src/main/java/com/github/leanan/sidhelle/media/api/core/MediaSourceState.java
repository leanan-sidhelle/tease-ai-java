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
package com.github.leanan.sidhelle.media.api.core;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;

/**
 * A specialized extension of {@link SourceState} for media sources processed 
 * via the FFmpeg {@code avformat} library.
 * <p>
 * This interface provides access to the native {@link AVFormatContext}, which is 
 * essential for retrieving global container-level metadata, stream configurations, 
 * and duration information.
 * </p>
 * 
 * @param <T> The type of the underlying source handle (e.g., {@link java.io.File}, 
 *            {@link java.net.URL}, or {@link java.io.InputStream}).
 * 
 * @author L. Sidhelle &lt;leanan-sidhelle@yahoo.com&gt;
 */
public interface MediaSourceState<T> extends SourceState<T> {

    /**
     * Retrieves the native FFmpeg format context for this source.
     * <p>
     * The {@link AVFormatContext} acts as the primary handle for demuxing operations. 
     * Implementations must ensure this context is properly initialized before 
     * use and is released only when the source is closed.
     * </p>
     * 
     * @return the {@link AVFormatContext} pointer used by the native demuxer.
     */
    public AVFormatContext formatContext();
    
}
