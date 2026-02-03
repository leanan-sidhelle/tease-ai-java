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

import com.github.leanan.sidhelle.media.api.core.MediaSourceState;
import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.exception.FFmpegException;
import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import java.util.List;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_ENOMEM;
import org.bytedeco.javacpp.BytePointer;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class MediaUtils {

    /**
     * Performs a single demuxing operation on a source.
     * <p>
     * This method conforms to the {@link library.api.io.SourceTask} signature. 
     * It reads the next packet from the native format context and dispatches 
     * it to the corresponding {@link StreamState} packet queue.
     * </p>
     * 
     * @param state the {@link SourceState} to demux from.
     * @throws java.lang.Exception
     * @throws FFmpegException if a native error or EOF is encountered.
     * @throws IllegalArgumentException if the state is not a {@link MediaSourceState}.
     */
    public static void demux(SourceState<?> state) throws Exception {
        /* 1. Pre-demux check: Ensure all packet queues can accept new data */
        if (!hasCapacityForAllStreams(state)) {
            /* 
             * Throw EAGAIN to signal that the demuxer is throttled 
             * by downstream backpressure.
             */
            int code = AVERROR_EAGAIN();
            String msg = getErrorMessage(code);
            throw new FFmpegException(msg,code);
        }
        
        /* 2. Ensure we have access to the native AVFormatContext */
        if (!(state instanceof MediaSourceState<?> mediaSource)) {
            throw new IllegalArgumentException("DemuxUtils requires a MediaSourceState implementation.");
        }

        /* 3. Allocate a native packet for the read operation */
        // We allocate once per call. For higher performance, consider 
        // reusing a thread-local AVPacket if thread safety is strictly managed.
        AVPacket pkt = av_packet_alloc();
        if (pkt == null || pkt.isNull()) {
            int code = AVERROR_ENOMEM();
            String msg = getErrorMessage(code);
            throw new FFmpegException(msg,code);
        }

        try {
            /* 4. Read the next frame from the demuxer */
            int ret = av_read_frame(mediaSource.formatContext(), pkt);

            /* 5. Handle non-zero return codes (Signals/Errors) */
            if (ret < 0) {
                // We free the empty packet container before throwing
                av_packet_free(pkt);
                String msg = getErrorMessage(ret);
                throw new FFmpegException(msg,ret);
            }

            /* 6. Dispatch the packet to the correct stream queue */
            List<StreamState> streams = mediaSource.getStreams();
            int streamIndex = pkt.stream_index();

            if (streamIndex >= 0 && streamIndex < streams.size()) {
                StreamState stream = streams.get(streamIndex);
                
                // Offer to the queue. If full, the packet must be freed immediately
                // to prevent a native memory leak.
                if (!stream.packetQueue().offer(pkt)) {
                    av_packet_unref(pkt);
                    av_packet_free(pkt);
                }
            } else {
                /* Packet belongs to a stream we aren't tracking; free it */
                av_packet_unref(pkt);
                av_packet_free(pkt);
            }
            
        } catch (FFmpegException e) {
            /* Final safety check to ensure native memory is freed on Java exceptions */
            if (pkt != null && !pkt.isNull()) {
                av_packet_free(pkt);
            }
            throw e;
        }
    }
    
    /**
     * Checks if all stream queues within a source have capacity for at least one more packet.
     * <p>
     * This is used as a "gatekeeper" for the demuxing task to prevent 
     * head-of-line blocking or forced packet drops.
     * </p>
     * 
     * @param state the source to verify.
     * @return {@code true} if all queues have remaining capacity; {@code false} otherwise.
     */
    public static boolean hasCapacityForAllStreams(SourceState<?> state) {
        List<StreamState> streams = state.getStreams();
        for (StreamState stream : streams) {
            /* 
             * LinkedBlockingQueue.remainingCapacity() is a low-overhead 
             * thread-safe check. 
             */
            if (stream.packetQueue().remainingCapacity() <= 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Retrieves the error message for a given native return code.
     *
     * @param errorCode the negative error code returned by an FFmpeg function.
     * @return a descriptive error message (e.g., "End of file").
     */
    public static String getErrorMessage(int errorCode) {
        /* Allocate a 1024-byte buffer for the native error string */
        BytePointer buffer = new BytePointer(1024);
        
        /* Fill the buffer with the description of the error code */
        if(av_strerror(errorCode, buffer, buffer.capacity()) == 0) {
            return buffer.getString();
        }
        
        return "Unknown error code: " + errorCode;
    }
    
    /**
     * Polls all available stream queues and dispatches ready frames to their consumers.
     * 
     * @param state The current MediaState containing sources, streams, and the clock.
     * @throws java.lang.Exception
     */
    public static void presentReadyFrames(MediaState state) throws Exception {
        MediaClock clock = state.getClock();
        long currentMediaTime = clock.getMediaTime();

        for (SourceState<?> source : state.getSources()) {
            for (StreamState stream : source.getStreams()) {
                /* 1. Peek at the next frame without removing it yet */
                MediaFrame frame = stream.frameQueue().peek();

                /* 2. Check if the frame is ready for presentation */
                // We present if PTS <= current clock time
                while (frame != null && frame.getPts() <= currentMediaTime) {
                    /* 3. Pull the frame from the queue for real */
                    frame = stream.frameQueue().poll();
                    
                    if (frame != null) {
                        try {
                            /* 4. Submit to the stream's specific consumer */
                            // This triggers VideoDecoder's BufferedImage or AudioDecoder's byte[]
                            dispatch(stream, frame);
                        } finally {
                            /* 5. Clean break: Ensure frame resources are released */
                            frame.close();
                        }
                    }
                    
                    /* Peek at the next one to see if it's also ready (bursting) */
                    frame = stream.frameQueue().peek();
                }
            }
        }
    }

    /**
     * Internal helper to handle the generic type safety of the FrameConsumer.
     */
    @SuppressWarnings("unchecked")
    private static <T extends MediaFrame> void dispatch(StreamState stream, MediaFrame frame) {
        FrameConsumer<T> consumer = (FrameConsumer<T>) stream.getConsumer();
        if (consumer != null) {
            consumer.accept((T) frame);
        }
    }
    
    /**
     * Static utilities shouldn't ever be instantiated.
     */
    private MediaUtils() {}
}
