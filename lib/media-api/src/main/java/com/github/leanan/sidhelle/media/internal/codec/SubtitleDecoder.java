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
import com.github.leanan.sidhelle.media.api.model.SubtitleFrame;
import com.github.leanan.sidhelle.media.util.BaseDecoder;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avcodec.AVSubtitle;
import org.bytedeco.ffmpeg.avcodec.AVSubtitleRect;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import static org.bytedeco.ffmpeg.global.avcodec.SUBTITLE_ASS;
import static org.bytedeco.ffmpeg.global.avcodec.SUBTITLE_BITMAP;
import static org.bytedeco.ffmpeg.global.avcodec.SUBTITLE_TEXT;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_decode_subtitle2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_flush_buffers;
import static org.bytedeco.ffmpeg.global.avcodec.avsubtitle_free;

/**
 * Implementation of a subtitle decoder that produces {@link SubtitleFrame} records.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class SubtitleDecoder extends BaseDecoder {

    /**
     * Overridden to handle the unique AVSubtitle decoding pattern.
     * 
     * @param state the {@link StreamState} context.
     * @param isDraining
     * @throws Exception if native decoding fails.
     */
    @Override
    public void decode(StreamState state,boolean isDraining) throws Exception {
        if (!isReady(state,isDraining)) return;

        AVPacket pkt = state.packetQueue().poll();
        if (pkt == null) return;

        AVSubtitle sub = new AVSubtitle();
        int[] gotSub = {0};

        try {
            /* Subtitles use a synchronous decode call in most FFmpeg versions */
            int ret = avcodec_decode_subtitle2(state.codecContext(), sub, gotSub, pkt);
            
            if (ret >= 0 && gotSub[0] != 0) {
                List<String> lines = new ArrayList<>();
                List<BufferedImage> bitmaps = new ArrayList<>();

                /* Iterate through subtitle rectangles (can be text or bitmap) */
                for (int i = 0; i < sub.num_rects(); i++) {
                    AVSubtitleRect rect = sub.rects(i);
                    switch (rect.type()) {
                        case SUBTITLE_TEXT -> lines.add(rect.text().getString());
                        case SUBTITLE_ASS -> lines.add(rect.ass().getString());
                        case SUBTITLE_BITMAP -> {
                        }
                        default -> {
                        }
                    }
                    /* Logic similar to VideoDecoder would go here to scale rect to BufferedImage */
                    // bitmaps.add(convertToImage(rect));
                                    }

                state.frameQueue().offer(SubtitleFrame.of(
                        pkt.pts(),
                        state.source(),
                        state.stream(),
                        lines,
                        bitmaps));
            }
        } finally {
            /* Free the internal native memory of the subtitle struct */
            avsubtitle_free(sub);
            /* Free the packet from the BlockingPacketQueue */
            av_packet_free(pkt);
        }
    }
    
    @Override
    public void reset(StreamState state) {
        /* 
         * avcodec_flush_buffers is the safe, native way to clear internal 
         * codec state for all media types, including subtitles.
         */
        if (state.codecContext() != null && !state.codecContext().isNull()) {
            avcodec_flush_buffers(state.codecContext());
        }
    }

    /**
     * Subtitles do not use the standard AVFrame receive loop.
     */
    @Override
    protected MediaFrame convert(AVFrame frame, StreamState state) {
        return null; // Not used for subtitles
    }

}
