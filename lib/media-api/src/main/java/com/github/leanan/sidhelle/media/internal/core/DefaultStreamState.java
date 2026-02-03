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

import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import com.github.leanan.sidhelle.media.internal.util.DefaultFrameConsumerValidator;
import com.github.leanan.sidhelle.media.util.BlockingPacketQueue;
import com.github.leanan.sidhelle.media.util.FrameConsumer;
import com.github.leanan.sidhelle.media.util.FrameConsumerValidator;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avfilter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;

/**
 * Thread-safe default implementation of {@link StreamState}.
 * <p>
 * This class protects native FFmpeg contexts using synchronization to ensure 
 * that concurrent access from multiple threads (e.g., a decoder thread and 
 * a filter thread) does not cause memory corruption.
 * </p>
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultStreamState implements StreamState {
    
    /**
     * TODO:
     */
    private final AVFormatContext fmt_ctx;
    
    /** The FFmpeg media type (e.g., AVMEDIA_TYPE_VIDEO). */
    private final int mediaType;

    /** Thread-safe queue for encoded packets entering the stream. */
    private final BlockingQueue<AVPacket> packets;
    
    /** Thread-safe queue for processed frames exiting the stream. */
    private final BlockingQueue<MediaFrame> frames;

    /** Native pointer to the codec context. */
    private final AVCodecContext codecCtx;

    /** Native pointer to the filter graph. */
    private final AVFilterGraph filterGraph;

    /** Input endpoint for the filter graph. */
    private final AVFilterContext srcCtx;

    /** Output endpoint for the filter graph. */
    private final AVFilterContext sinkCtx;

    /** Atomic-style flag to prevent double-freeing native memory. */
    private boolean closed = false;

    /**
     * TODO:
     */
    private final int source;
    
    /**
     * TODO:
     */
    private final int stream;
    
    /**
     * TODO:
     */
    private final FrameConsumerValidator validator; // Injected validator
    
    /**
     * TODO:
     */
    private volatile FrameConsumer<? extends MediaFrame> consumer;
    
    /**
     * TODO:
     */
    private static final int MAX_PACKETS = 16;
    
    /**
     * TODO:
     */
    private static final int MAX_FRAMES = 32;
    
    /**
     * Constructs a new stream state with the required native contexts.
     * 
     * @param source
     * @param stream
     * @param fmt_ctx
     * @param mediaType the type of media (Video, Audio, etc.).
     * @param codecCtx the {@link AVCodecContext} for decoding.
     * @param filterGraph the {@link AVFilterGraph} for conversion.
     * @param src the input {@link AVFilterContext}.
     * @param sink the output {@link AVFilterContext}.
     */
    public DefaultStreamState(int source,int stream,AVFormatContext fmt_ctx,
            int mediaType, AVCodecContext codecCtx, 
            AVFilterGraph filterGraph, AVFilterContext src, 
            AVFilterContext sink) {
        this(source,stream,fmt_ctx,mediaType,codecCtx,filterGraph,src,sink,new DefaultFrameConsumerValidator());
    }
    
    /**
     * Constructs a new stream state with the required native contexts.
     * 
     * @param source
     * @param stream
     * @param fmt_ctx
     * @param mediaType the type of media (Video, Audio, etc.).
     * @param codecCtx the {@link AVCodecContext} for decoding.
     * @param filterGraph the {@link AVFilterGraph} for conversion.
     * @param src the input {@link AVFilterContext}.
     * @param sink the output {@link AVFilterContext}.
     * @param validator
     */
    public DefaultStreamState(int source,int stream,AVFormatContext fmt_ctx,
            int mediaType, AVCodecContext codecCtx, 
            AVFilterGraph filterGraph, AVFilterContext src, 
            AVFilterContext sink,
            FrameConsumerValidator validator) {
        this.source = source;
        this.stream = stream;
        this.fmt_ctx = fmt_ctx;
        this.packets = new BlockingPacketQueue(MAX_PACKETS);
        this.frames = new LinkedBlockingQueue(MAX_FRAMES);
        this.mediaType = mediaType;
        this.codecCtx = codecCtx;
        this.filterGraph = filterGraph;
        this.srcCtx = src;
        this.sinkCtx = sink;
        this.validator = validator;
    }

    /** @return the media type constant. */
    @Override public int getMediaType() { return mediaType; }

    /** @return the packet queue. */
    @Override public BlockingQueue<AVPacket> packetQueue() { return packets; }

    /** @return the frame queue. */
    @Override public BlockingQueue<MediaFrame> frameQueue() { return frames; }
    
    /** @return the codec context pointer. */
    @Override public synchronized AVCodecContext codecContext() { return codecCtx; }

    /** @return the filter graph pointer. */
    @Override public synchronized AVFilterGraph filterGraph() { return filterGraph; }

    /** @return the buffer source context. */
    @Override public synchronized AVFilterContext filterSource() { return srcCtx; }

    /** @return the buffer sink context. */
    @Override public synchronized AVFilterContext filterSink() { return sinkCtx; }

    /** @return frame consumer. */
    @Override public synchronized FrameConsumer<? extends MediaFrame> getConsumer() { return consumer; }

    @Override
    public void setConsumer(FrameConsumer<? extends MediaFrame> consumer) {
        /* Delegate the logic to the strategy */
        validator.validate(this.mediaType, consumer);
        this.consumer = consumer;
    }
    
    /**
     * Safely releases native FFmpeg resources.
     * <p>
     * This method uses {@link avcodec#avcodec_free_context} and 
     * {@link avfilter#avfilter_graph_free} to ensure native memory is 
     * reclaimed by the system.
     * </p>
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            /* 1. CRITICAL: Drain and free native packets in the queue */
            if (packets instanceof BlockingPacketQueue bpq) {
                bpq.close(); 
            } else {
                // Fallback if the type was changed
                AVPacket pkt;
                while ((pkt = packets.poll()) != null) {
                    avcodec.av_packet_free(pkt);
                }
            }

            /* 2. Clear Java-managed frames (GC handles the rest) */
            frames.clear();

            /* 3. Free the decoder context */
            if (codecCtx != null && !codecCtx.isNull()) {
                avcodec.avcodec_free_context(codecCtx);
            }

            /* 4. Free the filter graph (This also frees srcCtx and sinkCtx) */
            if (filterGraph != null && !filterGraph.isNull()) {
                avfilter.avfilter_graph_free(filterGraph);
            }

            closed = true;
        }
    }

    @Override
    public int source() {
        return source;
    }

    @Override
    public int stream() {
        return stream;
    }

    @Override
    public AVFormatContext getFormatContext() {
        return fmt_ctx;
    }
    
    /** 
     * Offset added to the base PTS during decoding to support 
     * seamless source transitions. 
     */
    private volatile long ptsOffset = 0L;

    @Override
    public long getPtsOffset() {
        return ptsOffset;
    }

    @Override
    public void setPtsOffset(long offsetNanos) {
        this.ptsOffset = offsetNanos;
    }
    
}

