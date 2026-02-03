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

import com.github.leanan.sidhelle.media.api.core.MediaSourceState;
import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.internal.core.DefaultMediaSourceState;
import com.github.leanan.sidhelle.media.internal.core.DefaultMediaState;
import com.github.leanan.sidhelle.media.internal.core.DefaultStreamState;
import com.github.leanan.sidhelle.media.util.AudioFrameConsumer;
import com.github.leanan.sidhelle.media.util.FrameConsumer;
import com.github.leanan.sidhelle.media.util.FrameConsumerValidator;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;
import com.github.leanan.sidhelle.media.util.MediaUtils;
import com.github.leanan.sidhelle.media.util.SubtitleFrameConsumer;
import com.github.leanan.sidhelle.media.util.VideoFrameConsumer;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;

/**
 * Default implementation of {@link MediaStateBuilder}.
 * <p>
 * This class orchestrates the native FFmpeg initialization sequence. It opens 
 * format contexts, iterates through streams, opens decoders, and wires them 
 * to the provided {@link FrameConsumer}s via a {@link ConsumerValidator}.
 * </p>
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaStateBuilder implements MediaStateBuilder {

    @Override
    public MediaStateBuilder source(File file) throws IOException {
        return source(file,null,null);
    }

    @Override
    public MediaStateBuilder source(URL url) throws IOException {
        return source(url,null,null);
    }

    @Override
    public MediaStateBuilder source(InputStream stream) throws IOException {
        return source(stream,null);
    }

    /** Internal configuration storage for sources. */
    private record SourceConfig(Object handle, Duration start, Duration limit) {}

    /**
     * TODO:
     */
    private final List<SourceConfig> pendingSources = new ArrayList<>();
    
    /**
     * TODO:
     */
    private final Map<String, FrameConsumer<?>> consumerMap = new HashMap<>();
    
    /**
     * TODO:
     */
    private final FrameConsumerValidator validator = new DefaultFrameConsumerValidator();
    
    /**
     * TODO:
     */
    private boolean videoDisabled = false;
    
    /**
     * TODO:
     */
    private boolean audioDisabled = false;

    @Override
    public MediaState build() {
        List<SourceState<?>> initializedSources = new ArrayList<>();
        int srcCount = 0;

        for (SourceConfig config : pendingSources) {
            try {
                AVFormatContext fmtCtx = avformat.avformat_alloc_context();
                String path = resolvePath(config.handle());

                if (avformat_open_input(fmtCtx, path, null, null) < 0) {
                    throw new RuntimeException("Failed to open source: " + path);
                }

                if (avformat_find_stream_info(fmtCtx, (AVDictionary)null) < 0) {
                    throw new RuntimeException("Failed to find stream info for: " + path);
                }

                List<StreamState> streams = new ArrayList<>();

                // TRACKERS: Count occurrences of each type for selectors like "a:0"
                int videoIdx = 0;
                int audioIdx = 0;
                int subtitleIdx = 0;

                for (int i = 0; i < fmtCtx.nb_streams(); i++) {
                    AVStream avStream = fmtCtx.streams(i);
                    int type = avStream.codecpar().codec_type();

                    // 1. RESPECT DISABLE FLAGS: Skip initialization if disabled
                    if (type == avutil.AVMEDIA_TYPE_VIDEO && videoDisabled) continue;
                    if (type == avutil.AVMEDIA_TYPE_AUDIO && audioDisabled) continue;

                    // 2. Initialize Stream
                    StreamState streamState = createStream(fmtCtx,avStream,srcCount,i);
                    if (streamState != null) {

                        // 3. CALCULATE TYPE-SPECIFIC INDEX:
                        int typeSpecificIndex = switch (type) {
                            case avutil.AVMEDIA_TYPE_VIDEO -> videoIdx++;
                            case avutil.AVMEDIA_TYPE_AUDIO -> audioIdx++;
                            case avutil.AVMEDIA_TYPE_SUBTITLE -> subtitleIdx++;
                            default -> 0;
                        };

                        // 4. APPLY MAPPING: Use the per-type index for matching selectors
                        applyMapping(streamState, srcCount, typeSpecificIndex);
                        streams.add(streamState);
                    }
                }

                MediaSourceState source = new DefaultMediaSourceState(
                        config.handle(), fmtCtx, streams, MediaUtils::demux);
                initializedSources.add(source);
                srcCount++;

            } catch (RuntimeException e) {
                throw new RuntimeException("Critical failure during MediaState build", e);
            }
        }
        return new DefaultMediaState(initializedSources);
    }


    /**
     * Internal helper to initialize a native decoder and wrap it in a 
     * DefaultStreamState.
     * @param fmt_ctx
     * @param avStream
     * @param source
     * @param stream
     * @return 
     */
    protected StreamState createStream(AVFormatContext fmt_ctx,
            AVStream avStream,int source,int stream) {
        AVCodec codec = avcodec.avcodec_find_decoder(avStream.codecpar().codec_id());
        if (codec == null) return null;

        AVCodecContext codecCtx = avcodec.avcodec_alloc_context3(codec);
        avcodec.avcodec_parameters_to_context(codecCtx, avStream.codecpar());
        
        if(avcodec_open2(codecCtx, codec,(AVDictionary)null) < 0) {
            avcodec_free_context(codecCtx);
            return null;
        }

        /* We inject our Strategy-based validator here */
        return new DefaultStreamState(source,stream,fmt_ctx,
                avStream.codecpar().codec_type(),codecCtx, null,null,null,
                validator);
    }

    /**
     * Resolves the string path for FFmpeg based on the source handle type.
     */
    private String resolvePath(Object handle) {
        if (handle instanceof File f) return f.getAbsolutePath();
        if (handle instanceof URL u) return u.toString();
        if (handle instanceof InputStream) return "pipe:0"; // Placeholder for AVIO
        return handle.toString();
    }

    /**
     * Matches the discovered stream index against user-provided selectors 
     * and assigns the consumer to the StreamState.
     * 
     * Internal helper to match selectors like "0:v:0" or "v:0".
     * 
     * @param state       The stream being initialized.
     * @param sourceIndex The index of the source in the pending list.
     * @param streamIndex The index of the stream within that source.
     */
    private void applyMapping(StreamState state, int sourceIndex, int streamIndex) {
        String typeChar = switch (state.getMediaType()) {
            case avutil.AVMEDIA_TYPE_VIDEO -> "v";
            case avutil.AVMEDIA_TYPE_AUDIO -> "a";
            case avutil.AVMEDIA_TYPE_SUBTITLE -> "s";
            default -> "d";
        };

        /* Construct the specific selectors to check against the map */
        String fullSelector = sourceIndex + ":" + typeChar + ":" + streamIndex; // e.g. "0:v:0"
        String shortSelector = typeChar + ":" + streamIndex;                   // e.g. "v:0"

        /* Check for the most specific mapping first */
        FrameConsumer<?> consumer = (FrameConsumer<?>) consumerMap.getOrDefault(
                fullSelector, consumerMap.get(shortSelector));

        if (consumer != null) {
            state.setConsumer(consumer);
        }
    }

    @Override public MediaStateBuilder source(File f, Duration s, Duration l) { pendingSources.add(new SourceConfig(f, s, l)); return this; }
    @Override public MediaStateBuilder source(URL u, Duration s, Duration l) { pendingSources.add(new SourceConfig(u, s, l)); return this; }
    @Override public MediaStateBuilder source(InputStream i, Duration l) { pendingSources.add(new SourceConfig(i, null, l)); return this; }
    @Override public MediaStateBuilder disableVideo() { this.videoDisabled = true; return this; }
    @Override public MediaStateBuilder disableAudio() { this.audioDisabled = true; return this; }
    
    @Override public MediaStateBuilder map(String s, VideoFrameConsumer c) { consumerMap.put(s, c); return this; }
    @Override public MediaStateBuilder map(String s, AudioFrameConsumer c) { consumerMap.put(s, c); return this; }
    @Override public MediaStateBuilder map(String s, SubtitleFrameConsumer c) { consumerMap.put(s, c); return this; }
}
