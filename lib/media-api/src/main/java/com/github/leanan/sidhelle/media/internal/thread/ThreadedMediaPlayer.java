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
package com.github.leanan.sidhelle.media.internal.thread;

import com.github.leanan.sidhelle.media.api.codec.Decoder;
import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.exception.FFmpegException;
import com.github.leanan.sidhelle.media.internal.codec.CompositeDecoder;
import com.github.leanan.sidhelle.media.util.MediaUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;

/**
 * A multi-threaded media player that decouples demuxing, decoding, and presentation.
 * <p>
 * This implementation tracks source completion internally, avoiding the need for 
 * state-specific EOF flags in the {@link SourceState} API. It ensures all data 
 * is fully drained from internal queues before terminating.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class ThreadedMediaPlayer implements Runnable, AutoCloseable {
    
    /** The structural state of the media session. */
    private final MediaState state;
    
    /** The manager for stream-specific decoding operations. */
    private final Decoder compositeDecoder;
    
    /** Pool for parallel decoding of multiple streams. */
    private final ExecutorService decodeExecutor;
    
    /** Single-threaded executor for sequential demuxing from sources. */
    private final ExecutorService demuxExecutor = Executors.newSingleThreadExecutor();
    
    /** 
     * Local tracking for sources that have reached End-Of-File (EOF).
     * Using ConcurrentHashMap.newKeySet() for thread-safe access across loops.
     */
    private final Set<SourceState<?>> finishedSources = ConcurrentHashMap.newKeySet();
    
    /** Global flag to signal all threads to stop processing. */
    private volatile boolean running = false;

    /**
     * Constructs a player with the provided media state and a default decoder.
     * 
     * @param state the {@link MediaState} containing initialized sources and streams.
     */
    public ThreadedMediaPlayer(MediaState state) {
        this(state, new CompositeDecoder(state));
    }
    
    /**
     * Constructs a player with a custom decoder and initializes thread pools.
     * 
     * @param state   the {@link MediaState} to manage.
     * @param decoder the {@link Decoder} implementation to use for packets.
     */
    public ThreadedMediaPlayer(MediaState state, Decoder decoder) {
        this.state = state;
        this.compositeDecoder = decoder;
        
        /* Initialize a fixed pool based on current stream count */
        int totalStreams = state.getSources().stream()
                                .mapToInt(s -> s.getStreams().size()).sum();
        this.decodeExecutor = Executors.newFixedThreadPool(totalStreams);
    }

    /**
     * Executes the player lifecycle, including background task submission 
     * and the master synchronization loop.
     */
    @Override
    public void run() {
        this.running = true;
        finishedSources.clear();
        state.getClock().resume();

        /* Submit background demuxing task */
        demuxExecutor.submit(this::demuxLoop);

        /* Submit background decoding tasks for each stream */
        for (SourceState<?> source : state.getSources()) {
            for (StreamState stream : source.getStreams()) {
                decodeExecutor.submit(() -> decodeLoop(source, stream));
            }
        }

        /* 
         * Presentation Loop: 
         * Continues until all sources are demuxed AND all data is presented. 
         */
        try {
            while (running) {
                MediaUtils.presentReadyFrames(state);
                
                if (isPlaybackComplete()) {
                    break;
                }
                
                Thread.sleep(1); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Log fatal presentation error
        } finally {
            shutdown();
        }
    }

    /**
     * Background loop for demuxing packets from sources.
     * <p>
     * Triggers a decoder flush when a source returns AVERROR_EOF.
     * </p>
     */
    private void demuxLoop() {
        while (running) {
            boolean produced = false;
            for (SourceState<?> source : state.getSources()) {
                if (finishedSources.contains(source)) continue;

                try {
                    MediaUtils.demux(source);
                    produced = true;
                } catch (FFmpegException e) {
                    if (e.getErrorCode() == AVERROR_EOF) {
                        finishedSources.add(source);
                        triggerFlush(source);
                    }
                } catch (Exception e) {
                    return; 
                }
            }
            if (!produced) yieldThread();
        }
    }

    /**
     * Background loop for decoding packets into frames for a specific stream.
     * 
     * @param source the parent source of the stream.
     * @param stream the stream state to be decoded.
     */
    private void decodeLoop(SourceState<?> source, StreamState stream) {
        while (running) {
            boolean isDraining = finishedSources.contains(source);
            
            if (compositeDecoder.isReady(stream, isDraining)) {
                try {
                    compositeDecoder.decode(stream, isDraining);
                } catch (Exception e) {
                    // Log stream decoding error
                }
            } else if (isDraining && stream.packetQueue().isEmpty()) {
                /* Packet queue is dry and source is finished; exit thread */
                break;
            } else {
                yieldThread();
            }
        }
    }

    /**
     * Forces the decoder to release any internal "delayed" frames 
     * back into the frame queue.
     * 
     * @param source the source whose decoders should be flushed.
     */
    private void triggerFlush(SourceState<?> source) {
        for (StreamState stream : source.getStreams()) {
            try {
                compositeDecoder.flush(stream);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Prevents busy-waiting by yielding the current thread.
     */
    private void yieldThread() {
        try { 
            Thread.sleep(1); 
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Terminates all threads and releases decoder resources.
     */
    private void shutdown() {
        running = false;
        demuxExecutor.shutdownNow();
        decodeExecutor.shutdownNow();
        try {
            compositeDecoder.close();
        } catch (Exception ignored) {}
    }

    @Override
    public void close() throws Exception {
        try (state) {
            shutdown();
        }
    }

    /**
     * Determines if the player has completely finished all tasks.
     * 
     * @return {@code true} only if all sources are finished AND all queues are empty.
     */
    private boolean isPlaybackComplete() {
        // 1. Check if all known sources have reported EOF
        boolean allSourcesDemuxed = finishedSources.size() >= state.getSources().size();
        
        // 2. Ensure all data has been presented
        return allSourcesDemuxed && allQueuesEmpty(state);
    }

    /**
     * Inspects every stream to ensure no packets or frames are pending.
     * 
     * @param state the state to check.
     * @return {@code true} if every queue in every stream is empty.
     */
    private boolean allQueuesEmpty(MediaState state) {
        return state.getSources().stream()
            .flatMap(s -> s.getStreams().stream())
            .allMatch(st -> st.packetQueue().isEmpty() && st.frameQueue().isEmpty());
    }
}
