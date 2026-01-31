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

import com.github.leanan.sidhelle.media.api.MediaFrame;
import com.github.leanan.sidhelle.media.api.MediaState;
import com.github.leanan.sidhelle.media.api.SourceSpec;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;

/**
 * Default implementation of {@link MediaState}.
 * Uses {@link ConcurrentHashMap} and {@link ReentrantLock} to ensure 
 * thread safety across demuxing and decoding threads.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaState implements MediaState {
    /** Protects initialization and teardown of native resources. */
    private final ReentrantLock lock = new ReentrantLock();

    /** List of source metadata in ordinal order. */
    private final List<SourceSpec> sourceSpecs = new CopyOnWriteArrayList<>();

    /** Map of Source Index to Format Context. */
    private final Map<Integer, AVFormatContext> formatContexts = new ConcurrentHashMap<>();

    /** Map of "source:stream" keys to Codec Contexts. */
    private final Map<String, AVCodecContext> codecContexts = new ConcurrentHashMap<>();

    /** Map of "source:stream" keys to encoded packet queues. */
    private final Map<String, BlockingQueue<AVPacket>> packetQueues = new ConcurrentHashMap<>();

    /** Map of "source:stream" keys to decoded frame queues. */
    private final Map<String, BlockingQueue<MediaFrame>> frameQueues = new ConcurrentHashMap<>();

    /**
     * Initializes all sources and opens necessary decoders.
     * @param inputs List of source specifications to load.
     */
    public DefaultMediaState(List<SourceSpec> inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            initSource(i, inputs.get(i));
        }
    }

    /**
     * Internal utility to allocate native contexts and initialize queues.
     */
    private void initSource(int sourceIndex, SourceSpec spec) {
        lock.lock();
        try {
            // 1. Allocate Format Context
            AVFormatContext fmt = avformat.avformat_alloc_context();
            if (avformat.avformat_open_input(fmt, spec.path(), null, null) < 0) {
                throw new RuntimeException("Could not open source: " + spec.path());
            }

            // 2. Scan for stream information (codecs, timebases, etc)
            if (avformat.avformat_find_stream_info(fmt, (PointerPointer)null) < 0) {
                throw new RuntimeException("Could not find stream info for: " + spec.path());
            }

            sourceSpecs.add(spec);
            formatContexts.put(sourceIndex, fmt);

            // 3. Initialize per-stream resources
            for (int i = 0; i < fmt.nb_streams(); i++) {
                String key = sourceIndex + ":" + i;
                
                // Allocate handoff queues
                packetQueues.put(key, new LinkedBlockingQueue<>(128));
                frameQueues.put(key, new LinkedBlockingQueue<>(16));

                // Initialize Decoder Context
                AVCodecParameters par = fmt.streams(i).codecpar();
                AVCodec codec = avcodec.avcodec_find_decoder(par.codec_id());
                if (codec != null) {
                    AVCodecContext ctx = avcodec.avcodec_alloc_context3(codec);
                    avcodec.avcodec_parameters_to_context(ctx, par);
                    
                    // Enable multi-threaded decoding within FFmpeg
                    ctx.thread_count(0); 
                    
                    if (avcodec.avcodec_open2(ctx, codec, (AVDictionary)null) >= 0) {
                        codecContexts.put(key, ctx);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override public int getSourceCount() { return sourceSpecs.size(); }
    @Override public SourceSpec getSourceSpec(int source) { return sourceSpecs.get(source); }
    @Override public int getStreamCount(int source) { return formatContexts.get(source).nb_streams(); }
    @Override public AVFormatContext formatContext(int source) { return formatContexts.get(source); }
    @Override public AVCodecContext codecContext(int source, int stream) { return codecContexts.get(source + ":" + stream); }
    @Override public BlockingQueue<AVPacket> packetQueue(int source, int stream) { return packetQueues.get(source + ":" + stream); }
    @Override public BlockingQueue<MediaFrame> frameQueue(int source, int stream) { return frameQueues.get(source + ":" + stream); }

    /**
     * Orderly release of all native FFmpeg structures to prevent memory leaks.
     */
    @Override
    public void close() {
        lock.lock();
        try {
            // Free all pending packets in queues
            packetQueues.values().forEach(q -> {
                AVPacket p; while ((p = q.poll()) != null) avcodec.av_packet_free(p);
            });
            // Close all frames in queues
            frameQueues.values().forEach(q -> {
                MediaFrame f; while ((f = q.poll()) != null) try {
                    f.close();
                    } catch (Exception ex) {
                        System.getLogger(DefaultMediaState.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
            });
            // Free decoding contexts
            codecContexts.values().forEach(avcodec::avcodec_free_context);
            // Close input format contexts
            formatContexts.values().forEach(avformat::avformat_close_input);
        } finally {
            lock.unlock();
        }
    }
}
