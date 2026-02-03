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

/**
 * A master clock used to synchronize media presentation.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class MediaClock {
    
    private long startSystemTime = 0; // System time when playback started/resumed
    private long baseMediaTime = 0;   // Accumulated media time before the last pause
    private boolean paused = true;

    /**
     * Resumes the clock from the current position.
     */
    public synchronized void resume() {
        if (paused) {
            startSystemTime = System.nanoTime();
            paused = false;
        }
    }

    /**
     * Pauses the clock, freezing the media time.
     */
    public synchronized void pause() {
        if (!paused) {
            baseMediaTime = getMediaTime();
            paused = true;
        }
    }

    /**
     * Returns the current logical media time in nanoseconds.
     * @return 
     */
    public synchronized long getMediaTime() {
        if (paused) return baseMediaTime;
        return baseMediaTime + (System.nanoTime() - startSystemTime);
    }

    /**
     * Seeks the clock to a specific timestamp.
     * @param nsecs
     */
    public synchronized void seek(long nsecs) {
        baseMediaTime = nsecs;
        startSystemTime = System.nanoTime();
    }
}
