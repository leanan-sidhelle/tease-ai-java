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
import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import com.github.leanan.sidhelle.media.api.model.VideoFrame;
import com.github.leanan.sidhelle.media.util.BaseDecoder;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGRA;
import org.bytedeco.ffmpeg.swscale.SwsContext;

/**
 * Implementation of a video decoder that transforms native frames into Java-safe {@link VideoFrame} records.
 * <p>
 * This class handles the conversion from hardware-specific pixel formats (like YUV420P) 
 * to a standard ARGB {@link BufferedImage} using the {@code sws_scale} API.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class VideoDecoder extends BaseDecoder {
    /** Native context for software scaling and pixel format conversion. */
    private SwsContext swsCtx;

    /**
     * Converts a native video frame into a {@link VideoFrame} containing a {@link BufferedImage}.
     * 
     * @param frame the native {@link AVFrame} containing decoded video data.
     * @param state the {@link StreamState} providing source and stream indices.
     * @return a new {@link VideoFrame} record.
     */
    @Override
    protected MediaFrame convert(AVFrame frame, StreamState state) {
        int w = frame.width();
        int h = frame.height();

        /* 
         * 1. Initialize or update the scaler. 
         * We convert to AV_PIX_FMT_BGRA because it maps directly to BufferedImage.TYPE_INT_ARGB memory.
         */
        swsCtx = swscale.sws_getCachedContext(swsCtx, 
            w, h, frame.format(), 
            w, h, AV_PIX_FMT_BGRA, 
            swscale.SWS_BILINEAR, null, null, (DoublePointer)null);

        /* 2. Prepare the destination BufferedImage and its raw data array */
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        
        /* 
         * 3. Set up the destination pointers. 
         * We wrap the Java int[] in a native pointer so FFmpeg can write to it directly.
         */
        PointerPointer dstData = new PointerPointer(1).put(new IntPointer(data));
        IntPointer dstLinesize = new IntPointer(1).put(w * 4);
        
        /* 4. Execute the software scale/conversion */
        swscale.sws_scale(swsCtx, frame.data(), frame.linesize(), 0, h, dstData, dstLinesize);

        /* 5. Return the finalized VideoFrame record */
        return VideoFrame.of(frame.pts(),state.source(),state.stream(),image);
    }

    /**
     * Releases the native scaler context during decoder shutdown.
     */
    @Override
    public void close() {
        if (swsCtx != null) {
            swscale.sws_freeContext(swsCtx);
        }
    }
}

