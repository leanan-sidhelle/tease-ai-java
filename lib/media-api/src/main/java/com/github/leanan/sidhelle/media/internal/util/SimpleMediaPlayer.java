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

import com.github.leanan.sidhelle.media.api.codec.Decoder;
import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.exception.FFmpegException;
import com.github.leanan.sidhelle.media.internal.codec.CompositeDecoder;
import com.github.leanan.sidhelle.media.util.MediaUtils;
import org.bytedeco.ffmpeg.global.avutil;

import java.util.HashSet;
import java.util.Set;

/**
 * A single-threaded media player that manages the end-to-end lifecycle 
 * of a media session.
 * <p>
 * This implementation handles synchronized playback by coordinating 
 * demuxing, decoding, and presentation. It specifically manages the 
 * draining phase to recover buffered frames from native decoders once 
 * the end of a source is reached.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class SimpleMediaPlayer implements Runnable, AutoCloseable {

    /** The structural state of the media session, containing sources and clock. */
    private final MediaState state;

    /** The decoder manager responsible for dispatching work to specialized decoders. */
    private final Decoder compositeDecoder;

    /** Tracks which sources have reached End-Of-File (EOF) and are in draining mode. */
    private final Set<SourceState<?>> finishedSources = new HashSet<>();

    /** Flag indicating if the playback loop is currently active. */
    private volatile boolean running = false;

    /** Flag used to signal a request for an immediate hard stop. */
    private volatile boolean stopRequested = false;

    /**
     * Constructs a new player for the given media state.
     * 
     * @param state the {@link MediaState} containing initialized sources and streams.
     */
    public SimpleMediaPlayer(MediaState state) {
        this.state = state;
        /* Initialize the composite decoder to map all streams to their implementations */
        this.compositeDecoder = new CompositeDecoder(state);
    }

    /**
     * Executes the main playback lifecycle loop.
     * <p>
     * The loop continuously performs:
     * <ol>
     *     <li><b>Demuxing:</b> Reading packets into queues until EOF is reached.</li>
     *     <li><b>Decoding:</b> Converting packets to frames, including draining native buffers at EOF.</li>
     *     <li><b>Presentation:</b> Syncing frames to the master clock for display.</li>
     * </ol>
     * </p>
     */
    @Override
    public void run() {
        this.running = true;
        this.stopRequested = false;
        finishedSources.clear();
        
        /* Ensure the master clock is ticking before entering the loop */
        state.getClock().resume();

        try {
            while (running && !stopRequested) {
                boolean workDone = false;

                /* 1. DEMUX: Process all configured input sources */
                for (SourceState<?> source : state.getSources()) {
                    if (finishedSources.contains(source)) {
                        continue;
                    }

                    try {
                        MediaUtils.demux(source);
                        workDone = true;
                    } catch (FFmpegException e) {
                        /* Handle end-of-file for a specific source */
                        if (e.getErrorCode() == avutil.AVERROR_EOF) {
                            /* Signal decoders for this source to flush internal buffers */
                            flushSource(source);
                            finishedSources.add(source);
                        } else if (e.getErrorCode() != avutil.AVERROR_EAGAIN()) {
                            throw e; 
                        }
                    }
                }

                /* 2. DECODE: Dispatch work to decoders, respecting the draining state */
                for (SourceState<?> source : state.getSources()) {
                    boolean isDraining = finishedSources.contains(source);
                    
                    for (StreamState stream : source.getStreams()) {
                        /* 
                         * Check if the decoder is ready (has space and has data/draining).
                         * We pass isDraining to ensure the final flush is not skipped.
                         */
                        if (compositeDecoder.isReady(stream, isDraining)) {
                            compositeDecoder.decode(stream, isDraining);
                            workDone = true;
                        }
                    }
                }

                /* 3. PRESENT: Sync frames to the current clock time */
                MediaUtils.presentReadyFrames(state);

                /* 4. EXIT CHECK: Stop only when all data is physically presented */
                if (isPlaybackComplete()) {
                    break;
                }

                /* 5. CPU THROTTLE: Yield if no activity occurred this cycle */
                if (!workDone) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error in SimpleMediaPlayer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Determines if playback is finished for all sources and queues.
     * 
     * @return {@code true} if every frame has been presented; {@code false} otherwise.
     */
    private boolean isPlaybackComplete() {
        if (finishedSources.size() < state.getSources().size()) {
            return false;
        }
        return allQueuesEmpty(state);
    }

    /**
     * Checks if every packet and frame queue in the state is empty.
     * 
     * @param state the state to inspect.
     * @return {@code true} if no data is in flight.
     */
    private boolean allQueuesEmpty(MediaState state) {
        for (SourceState<?> source : state.getSources()) {
            for (StreamState stream : source.getStreams()) {
                if (!stream.packetQueue().isEmpty() || !stream.frameQueue().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Triggers the flushing of all decoders associated with a specific source.
     * 
     * @param source the {@link SourceState} that has reached end-of-file.
     * @throws Exception if native flushing fails.
     */
    private void flushSource(SourceState<?> source) throws Exception {
        for (StreamState stream : source.getStreams()) {
            compositeDecoder.flush(stream);
        }
    }

    /**
     * Pauses the playback.
     */
    public void pause() {
        this.running = false;
        state.getClock().pause();
    }

    /**
     * Resumes the playback session.
     */
    public void resume() {
        state.getClock().resume();
        this.running = true;
    }

    /**
     * Requests a complete stop of the playback session.
     */
    public void stop() {
        this.stopRequested = true;
        this.running = false;
        state.getClock().pause();
    }

    /**
     * Internal helper to clean up native decoder resources.
     */
    private void shutdown() {
        try {
            compositeDecoder.close();
        } catch (Exception ignored) {}
    }

    /**
     * Releases all native and Java resources associated with the player.
     * 
     * @throws Exception if closing the state or decoders fails.
     */
    @Override
    public void close() throws Exception {
        try (state) {
            stop();
        }
    }
}



