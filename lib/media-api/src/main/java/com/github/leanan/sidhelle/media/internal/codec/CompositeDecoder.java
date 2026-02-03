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

import com.github.leanan.sidhelle.media.api.codec.Decoder;
import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import org.bytedeco.ffmpeg.global.avutil;

import java.util.HashMap;
import java.util.Map;

/**
 * A composite manager that dispatches decoding operations to specific
 * media-type implementations.
 * <p>
 * This class instantiates all required decoders once during construction
 * and maps them to their respective streams, improving performance and
 * extensibility.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class CompositeDecoder implements Decoder {

    /** Maps each individual stream to its specialized decoder instance. */
    private final Map<StreamState, Decoder> decoderMap = new HashMap<>();

    /**
     * Constructs the composite by scanning all sources and streams.
     *
     * @param state the {@link MediaState} containing the streams to decode.
     */
    public CompositeDecoder(MediaState state) {
        for (SourceState<?> source : state.getSources()) {
            for (StreamState stream : source.getStreams()) {
                Decoder decoder = createDecoder(stream.getMediaType());
                if (decoder != null) {
                    decoderMap.put(stream, decoder);
                }
            }
        }
    }

    /**
     * Factory method to instantiate specialized decoders based on media type.
     * 
     * @param mediaType the FFmpeg media type constant.
     * @return a specific {@link Decoder} implementation, or null if unsupported.
     */
    private Decoder createDecoder(int mediaType) {
        return switch (mediaType) {
            case avutil.AVMEDIA_TYPE_VIDEO -> new VideoDecoder();
            case avutil.AVMEDIA_TYPE_AUDIO -> new AudioDecoder();
            case avutil.AVMEDIA_TYPE_SUBTITLE -> new SubtitleDecoder();
            default -> null;
        };
    }
    
    /**
     * TODO:
     * @param state
     * @return 
     */
    public Decoder getDecoderForStream(StreamState state) {
        return decoderMap.get(state);
    }

    /**
     * Dispatches the decode call to the decoder assigned to the given stream.
     */
    @Override
    public void decode(StreamState state,boolean isDraining) throws Exception {
        Decoder decoder = getDecoderForStream(state);
        if (decoder != null) {
            decoder.decode(state,isDraining);
        }
    }

    @Override
    public void flush(StreamState state) throws Exception {
        Decoder decoder = decoderMap.get(state);
        if (decoder != null) {
            decoder.flush(state);
        }
    }

    @Override
    public void reset(StreamState state) {
        Decoder decoder = decoderMap.get(state);
        if (decoder != null) {
            decoder.reset(state);
        }
    }

    @Override
    public boolean isReady(StreamState state,boolean isDraining) {
        Decoder decoder = decoderMap.get(state);
        return decoder != null && decoder.isReady(state,isDraining);
    }

    /**
     * Closes all managed decoder instances.
     */
    @Override
    public void close() throws Exception {
        for (Decoder decoder : decoderMap.values()) {
            decoder.close();
        }
        decoderMap.clear();
    }
}
