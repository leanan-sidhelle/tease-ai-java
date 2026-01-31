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
package com.github.leanan.sidhelle.media.api;

import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import java.util.concurrent.BlockingQueue;
import java.util.List;

/**
 * Central registry for native FFmpeg contexts and thread-safe data queues.
 * Manages the lifecycle of demuxers, decoders, and data buffers.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public interface MediaState extends AutoCloseable {
    /** @return The number of input sources registered. */
    int getSourceCount();

    /** @param source Ordinal index of the source. */
    SourceSpec getSourceSpec(int source);

    /** @param source Ordinal index of the source. */
    int getStreamCount(int source);

    /** @return The native Format Context for demuxing. */
    AVFormatContext formatContext(int source);

    /** @return The native Codec Context for decoding. */
    AVCodecContext codecContext(int source, int stream);

    /** @return Queue for encoded {@link AVPacket} data (Demuxer -> Decoder). */
    BlockingQueue<AVPacket> packetQueue(int source, int stream);

    /** @return Queue for raw {@link MediaFrame} data (Decoder -> Processor). */
    BlockingQueue<MediaFrame> frameQueue(int source, int stream);

    /** Frees all native resources associated with all sources. */
    @Override
    void close();
}
