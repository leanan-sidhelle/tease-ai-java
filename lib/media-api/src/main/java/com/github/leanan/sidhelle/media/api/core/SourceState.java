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

import com.github.leanan.sidhelle.media.util.SourceTask;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represents a single source of media data (e.g., a file or network stream).
 * 
 * @param <T> The type of the underlying source (e.g., {@link java.io.File}).
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public interface SourceState<T> extends AutoCloseable {

    /**
     * Retrieves the underlying source object.
     * 
     * @return the strongly-typed source (e.g., a File or URL).
     */
    public T getSource();

    /**
     * Retrieves the list of individual streams (Video, Audio, Data) 
     * found within this source.
     * 
     * @return an immutable {@link List} of {@link StreamState} objects.
     */
    public List<StreamState> getStreams();

    /**
     * Provides a non-blocking work unit that performs a single demuxing 
     * or parsing operation (e.g., reading one {@code AVPacket}).
     * 
     * @return a {@link Callable} returning 0 on success, negative on error/EOF.
     */
    public SourceTask getDemuxTask();

}
