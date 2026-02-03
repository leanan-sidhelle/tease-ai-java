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
package com.github.leanan.sidhelle.media.util;

import com.github.leanan.sidhelle.media.api.codec.Decoder;
import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import static org.bytedeco.ffmpeg.global.avutil.*;

import com.github.leanan.sidhelle.media.api.core.StreamState;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

/**
 * An abstract base implementation of the {@link Decoder} interface.
 * <p>
 * This class provides a standard FFmpeg decoding loop using the decoupled 
 * {@code avcodec_send_packet} and {@code avcodec_receive_frame} API.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public abstract class BaseDecoder implements Decoder {

    @Override
    public void decode(StreamState state, boolean isDraining) throws Exception {
        /* 1. Ensure we have space in the frame queue and either a packet or a drain request */
        if (!isReady(state, isDraining)) return;

        /* 2. Attempt to pull a packet from the native queue */
        AVPacket pkt = state.packetQueue().poll();

        if (pkt != null) {
            try {
                /* Feed the native packet to the codec */
                int ret = avcodec.avcodec_send_packet(state.codecContext(), pkt);
                if (ret < 0) return;

                /* Standard frame retrieval loop */
                processFrames(state);
            } finally {
                /* Always free the native packet shell */
                avcodec.av_packet_free(pkt);
            }
        } else if (isDraining) {
            /* 
             * 3. THE FIX: No packet found, but we are in draining mode.
             * We MUST call processFrames to execute avcodec_receive_frame,
             * which recovers the final 15 'trapped' frames from the native buffer.
             */
            processFrames(state);
        }
    }


    /**
     * Internal loop that receives frames from the codec until no more are available.
     * 
     * @param state the {@link StreamState} used to store decoded output.
     * @throws Exception if frame processing fails.
     */
    protected void processFrames(StreamState state) throws Exception {
        AVFrame frame = avutil.av_frame_alloc();
        try {
            int ret = 0;
            while (ret >= 0) {
                ret = avcodec.avcodec_receive_frame(state.codecContext(), frame);
                
                /* AVERROR(EAGAIN) means more input is required to produce a frame */
                if (ret == avutil.AVERROR_EAGAIN() || ret == avutil.AVERROR_EOF) break;
                if (ret < 0) throw new RuntimeException("Decoding error code: " + ret);

                /* 
                 * TIMEBASE CORRECTION: 
                 * Rescale the native frame PTS into nanoseconds using the authoritative AVStream timebase. 
                 */
                long ptsInNanos = rescalePts(frame.best_effort_timestamp(), state);
                frame.pts(ptsInNanos);

                /* Convert the native AVFrame into a safe Java MediaFrame */
                MediaFrame converted = convert(frame, state);
                if (converted != null) {
                    state.frameQueue().offer(converted);
                }
                
                /* Reset frame shell for reuse in the next loop iteration */
                avutil.av_frame_unref(frame);
            }
        } finally {
            /* Clean up the native frame container */
            avutil.av_frame_free(frame);
        }
    }

    
    /**
     * Rescales a raw PTS value to nanoseconds and applies the stream's PTS offset.
     * 
     * @param pts   the raw timestamp from the native {@link AVFrame}.
     * @param state the {@link StreamState} context.
     * @return the presentation timestamp converted to nanoseconds + current offset.
     */
    protected long rescalePts(long pts, StreamState state) {
        if (pts == AV_NOPTS_VALUE) return 0;

        AVRational nanoBase = new AVRational().num(1).den(1000000000);
        AVStream nativeStream = state.getFormatContext().streams(state.stream());

        /* 1. Scale native ticks to nanoseconds */
        long baseNanos = avutil.av_rescale_q(pts, nativeStream.time_base(), nanoBase);

        /* 2. Apply the offset for seamless appending */
        return baseNanos + state.getPtsOffset();
    }


    @Override
    public void flush(StreamState state) throws Exception {
        /* Sending a NULL packet signals the End of Stream (EOS) to the decoder */
        avcodec.avcodec_send_packet(state.codecContext(), null);
        processFrames(state);
    }

    @Override
    public void reset(StreamState state) {
        avcodec.avcodec_flush_buffers(state.codecContext());
    }

    @Override
    public boolean isReady(StreamState state, boolean isDraining) {
        /* 1. CAPACITY CHECK: If the output queue is full, we must NOT decode.
         * This applies even during draining to prevent dropping frames. 
         */
        if (state.frameQueue().remainingCapacity() <= 0) {
            return false;
        }

        /* 2. DATA CHECK: We are ready if there is a packet OR we are draining. */
        return !state.packetQueue().isEmpty() || isDraining;
    }

    /**
     * Template method implemented by subclasses to perform type-specific conversion.
     * 
     * @param frame the native {@link AVFrame} with raw decoded data and corrected PTS.
     * @param state the {@link StreamState} context for the conversion.
     * @return a Java-safe {@link MediaFrame}.
     * @throws Exception if conversion fails.
     */
    protected abstract MediaFrame convert(AVFrame frame, StreamState state) throws Exception;

    @Override public void close() throws Exception { /* Default empty */ }
}



