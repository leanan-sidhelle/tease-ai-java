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
package com.github.leanan.sidhelle.media.api.codec;

import com.github.leanan.sidhelle.media.api.core.StreamState;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public interface Decoder extends AutoCloseable {

    /**
     * Consumes one or more packets from the StreamState's packet queue,
     * decodes them, and pushes resulting MediaFrames to the frame queue.
     * 
     * @param state The stream state containing the queues and codec context.
     * @param isDraining True if the source has reached EOF and we are flushing.
     * @throws Exception if a non-recoverable native error occurs.
     */
    public void decode(StreamState state, boolean isDraining) throws Exception;

    /**
     * Signals the end of the stream and drains all internally buffered frames.
     * Some codecs (like H.264 with B-frames) require "flushing" with a null packet 
     * to release the last few frames.
     * @param state
     * @throws java.lang.Exception
     */
    public void flush(StreamState state) throws Exception;

    /**
     * Clears internal native codec buffers to prepare for a new stream segment (e.g., after seeking).
     * 
     * @param state the {@link StreamState} to reset.
     */
    public void reset(StreamState state);
    
    /**
     * Determines if the decoder is ready to process data.
     * <p>
     * This method ensures backpressure safety by checking frame queue capacity 
     * while also ensuring the draining phase (EOF) is not skipped.
     * </p>
     * 
     * @param state        the stream state to check.
     * @param isDraining   whether the source has reached EOF and is in draining mode.
     * @return true if work should be performed; false otherwise.
     */
    public boolean isReady(StreamState state, boolean isDraining);

}
