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

import org.bytedeco.ffmpeg.global.avcodec;
import java.util.concurrent.LinkedBlockingQueue;
import org.bytedeco.ffmpeg.avcodec.AVPacket;

/**
 * A thread-safe queue for AVPackets that prevents native memory leaks.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class BlockingPacketQueue extends LinkedBlockingQueue<AVPacket> {

    public BlockingPacketQueue(int capacity) {
        super(capacity);
    }

    /**
     * Offers a packet to the queue. 
     * If the queue is full, the native packet is automatically freed.
     */
    @Override
    public boolean offer(AVPacket pkt) {
        if (pkt == null || pkt.isNull()) return false;
        
        boolean accepted = super.offer(pkt);
        if (!accepted) {
            avcodec.av_packet_free(pkt);
        }
        return accepted;
    }

    /**
     * Frees all native packets currently residing in the queue.
     */
    @Override
    public void clear() {
        AVPacket pkt;
        while ((pkt = super.poll()) != null) {
            avcodec.av_packet_free(pkt);
        }
    }

    /**
     * Alias for clear() to be used during StreamState shutdown.
     */
    public void close() {
        this.clear();
    }
}
