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
package com.github.leanan.sidhelle.media.internal.codec;

import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.model.AudioFrame;
import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import com.github.leanan.sidhelle.media.util.BaseDecoder;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.ffmpeg.global.swresample.swr_alloc_set_opts2;
import static org.bytedeco.ffmpeg.global.swresample.swr_convert;
import static org.bytedeco.ffmpeg.global.swresample.swr_free;
import static org.bytedeco.ffmpeg.global.swresample.swr_get_delay;
import static org.bytedeco.ffmpeg.global.swresample.swr_get_out_samples;
import static org.bytedeco.ffmpeg.global.swresample.swr_init;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

/**
 * Implementation of an audio decoder that transforms native samples into Java-safe {@link AudioFrame} records.
 * <p>
 * This class handles resampling and format conversion (e.g., Planar to Interleaved) 
 * to produce standard PCM {@code byte[]} data.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class AudioDecoder extends BaseDecoder {
    
    /** Native context for audio resampling and conversion. */
    private SwrContext swrCtx;

    /**
     * Converts a native audio frame into an {@link AudioFrame} containing a PCM byte array.
     * 
     * @param frame the native {@link AVFrame} containing decoded audio samples.
     * @param state the {@link StreamState} providing source and stream indices.
     * @return a new {@link AudioFrame} record.
     * @throws Exception if native resampling fails.
     */
    @Override
    protected MediaFrame convert(AVFrame frame, StreamState state) throws Exception {
        /* 
         * 1. Initialize or Reconfigure the Resampler.
         * In JavaCPP, we must handle the pointer-to-pointer requirement of swr_alloc_set_opts2.
         */
        if (swrCtx == null) {
            // We initialize the context pointer. 
            // swr_alloc_set_opts2 returns 0 on success and sets the pointer.
            swrCtx = new SwrContext(); 
            int ret = swr_alloc_set_opts2(
                swrCtx,                      // ps: Pointer to the context
                state.codecContext().ch_layout(), // out_ch_layout
                AV_SAMPLE_FMT_S16,           // out_sample_fmt
                frame.sample_rate(),         // out_sample_rate
                frame.ch_layout(),           // in_ch_layout
                frame.format(),              // in_sample_fmt
                frame.sample_rate(),         // in_sample_rate
                0,                           // log_offset
                null                         // log_ctx
            );

            if (ret < 0) {
                throw new RuntimeException("Could not initialize audio resampler: " + ret);
            }

            /* Initialize the context after parameters are set */
            swr_init(swrCtx);
        }

        /* 2. Calculate the required output buffer size */
        int channels = frame.ch_layout().nb_channels();
        int outSamples = swr_get_out_samples(swrCtx, frame.nb_samples());

        /* 2 bytes per sample (S16) * channels */
        byte[] pcm = new byte[outSamples * 2 * channels];

        /* 3. Execute the conversion into the Java byte array */
        PointerPointer dst = new PointerPointer(1).put(new BytePointer(pcm));
        swr_convert(swrCtx, dst, outSamples, frame.data(), frame.nb_samples());

        /* 4. Return the finalized AudioFrame record */
        return AudioFrame.of(
            frame.pts(),
            state.source(), // Ensure your StreamState now has this!
            state.stream(), // Ensure your StreamState now has this!
            pcm
        );
    }
    
    @Override
    public void flush(StreamState state) throws Exception {
        /* 1. Enter draining mode in the native codec */
        avcodec_send_packet(state.codecContext(), null);

        /* 2. Standard processFrames loop will now recover the missing 15 frames */
        processFrames(state);

        /* 3. Handle any samples left in the resampler (swrCtx) */
        if (swrCtx != null) {
            drainResampler(state);
        }
    }


    /**
     * Drains the libswresample internal buffer.
     */
    private void drainResampler(StreamState state) {
        // 1. Force a flush by providing a NULL input and a large enough output buffer
        // 4096 samples is usually enough to catch any trailing AAC fragments
        int outSamples = 4096; 
        int channels = state.codecContext().ch_layout().nb_channels();
        byte[] pcm = new byte[outSamples * 2 * channels];
        PointerPointer dst = new PointerPointer(1).put(new BytePointer(pcm));

        // Pass 0 for in_count and null for in_data to force the internal buffer out
        int ret = swr_convert(swrCtx, dst, outSamples, null, 0);

        if (ret > 0) {
            // We actually recovered data! Push it to the queue.
            byte[] actualPcm = java.util.Arrays.copyOf(pcm, ret * 2 * channels);
            state.frameQueue().offer(AudioFrame.of(
                -1, 
                state.source(),
                state.stream(),
                actualPcm
            ));
        }
    }


    /**
     * Releases the native resampling context during decoder shutdown.
     */
    @Override
    public void close() {
        if (swrCtx != null) {
            swr_free(swrCtx);
        }
    }
}
