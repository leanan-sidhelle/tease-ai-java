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
package com.github.leanan.sidhelle.media.api.core;

import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import com.github.leanan.sidhelle.media.util.FrameConsumer;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import java.util.concurrent.BlockingQueue;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;

/**
 * Manages the state, native contexts, and data queues for a specific media stream.
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public interface StreamState extends AutoCloseable {
    
    /**
     * Returns the FFmpeg media type (e.g., AVMEDIA_TYPE_VIDEO).
     * 
     * @return the integer constant representing the media type.
     */
    public int getMediaType();

    /**
     * The input queue for encoded packets waiting to be decoded.
     * 
     * @return the {@link BlockingQueue} of {@link AVPacket} objects.
     */
    public BlockingQueue<AVPacket> packetQueue();

    /**
     * The output queue for processed and converted frames.
     * 
     * @return the {@link BlockingQueue} of {@link MediaFrame} objects.
     */
    public BlockingQueue<MediaFrame> frameQueue();
    
    /**
     * TODO:
     * @return 
     */
    public AVFormatContext getFormatContext();

    /**
     * Returns the native codec context for this stream.
     * 
     * @return the {@link AVCodecContext} used for decoding.
     */
    public AVCodecContext codecContext();

    /**
     * Returns the filter graph assigned to this stream.
     * 
     * @return the {@link AVFilterGraph}, or null if no filtering is applied.
     */
    public AVFilterGraph filterGraph();

    /**
     * Returns the 'buffersrc' context used to push frames into the graph.
     * 
     * @return the input {@link AVFilterContext}.
     */
    public AVFilterContext filterSource();

    /**
     * Returns the 'buffersink' context used to pull frames from the graph.
     * 
     * @return the output {@link AVFilterContext}.
     */
    public AVFilterContext filterSink();

    /**
     * Retrieves the current consumer assigned to process frames from this stream.
     * 
     * @return the active {@link FrameConsumer}, or null if unmapped.
     */
    public FrameConsumer<? extends MediaFrame> getConsumer();

    /**
     * Updates the consumer for this stream. This allows for remapping 
     * handlers during runtime (e.g., switching from a preview to a high-res saver).
     * 
     * @param consumer the new {@link FrameConsumer} to handle output frames.
     */
    public void setConsumer(FrameConsumer<? extends MediaFrame> consumer);
    
    /**
     * TODO:
     * @return 
     */
    public int source();
    
    /**
     * TODO:
     * @return 
     */
    public int stream();
    
    /**
     * Retrieves the current PTS offset in nanoseconds.
     * This value is added to raw timestamps during decoding.
     * 
     * @return the offset in nanoseconds.
     */
    long getPtsOffset();

    /**
     * Sets a new PTS offset to be applied to future frames.
     * 
     * @param offsetNanos the new offset in nanoseconds.
     */
    void setPtsOffset(long offsetNanos);

}
