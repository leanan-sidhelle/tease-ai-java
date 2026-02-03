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
package com.github.leanan.sidhelle.media.internal.core;

import com.github.leanan.sidhelle.media.api.core.MediaSourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.util.SourceTask;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avformat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe implementation of {@link MediaSourceState}.
 * 
 * @param <T> The type of the source handle (e.g., File, URL).
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaSourceState<T> implements MediaSourceState<T> {
    /** The actual source object (e.g. java.io.File). */
    private final T source;

    /** Native pointer to the format context for demuxing. */
    private final AVFormatContext formatCtx;

    /** Thread-safe list of streams discovered in this source. */
    private final List<StreamState> streams = new CopyOnWriteArrayList<>();

    /** The task responsible for reading packets from this source. */
    private final SourceTask demuxTask;

    /** Lifecycle flag. */
    private boolean closed = false;

    /**
     * Initializes the source state.
     * 
     * @param source the source handle.
     * @param formatCtx the {@link AVFormatContext}.
     * @param streams the {@link List}.
     * @param demuxTask the task to be executed by a worker.
     */
    public DefaultMediaSourceState(T source, AVFormatContext formatCtx, 
            List<StreamState> streams,
            SourceTask demuxTask) {
        this.source = source;
        this.formatCtx = formatCtx;
        this.streams.addAll(streams);
        this.demuxTask = demuxTask;
    }

    /** @return the source handle. */
    @Override public T getSource() { return source; }

    /** @return an unmodifiable view of the streams. */
    @Override public List<StreamState> getStreams() { return Collections.unmodifiableList(streams); }

    /** @return the demuxing callable. */
    @Override public SourceTask getDemuxTask() { return demuxTask; }

    /** @return the native format context. */
    @Override public synchronized AVFormatContext formatContext() { return formatCtx; }

    /**
     * Adds a new stream to this source.
     * @param stream the {@link StreamState} to add.
     */
    public void addStream(StreamState stream) { streams.add(stream); }

    /**
     * Closes the source and all associated streams.
     * 
     * @throws Exception if native cleanup fails.
     */
    @Override
    public synchronized void close() throws Exception {
        if (!closed) {
            /* Close child streams first to release codecs/graphs */
            for (StreamState stream : streams) {
                stream.close();
            }
            
            /* Use FFmpeg to close the input and free the format context */
            if (formatCtx != null) avformat.avformat_close_input(formatCtx);
            
            closed = true;
        }
    }
}
