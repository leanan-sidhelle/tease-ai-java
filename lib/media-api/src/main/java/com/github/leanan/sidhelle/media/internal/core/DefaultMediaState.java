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
package com.github.leanan.sidhelle.media.internal.core;


import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.util.MediaClock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for managing multiple media sources.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultMediaState implements MediaState {
    /** 
     * Thread-safe list of active sources. 
     * Uses CopyOnWriteArrayList for thread-safe iteration during loop execution. 
     */
    private final List<SourceState<?>> sources = new CopyOnWriteArrayList<>();

    /** Lifecycle flag. */
    private boolean closed = false;

    /**
     * TODO:
     */
    private final MediaClock clock = new MediaClock();
    
    /**
     * TODO:
     * @param sources 
     */
    public DefaultMediaState(List<SourceState<?>> sources) {
        this.sources.addAll(sources);
    }

    /** @return an unmodifiable list of current sources. */
    @Override
    public List<SourceState<?>> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /**
     * Recursively closes all sources and clears the registry.
     * 
     * @throws Exception if any source fails to close correctly.
     */
    @Override
    public synchronized void close() throws Exception {
        if (!closed) {
            /* Iterate through all sources and trigger their cleanup */
            for (SourceState<?> source : sources) {
                source.close();
            }
            sources.clear();
            closed = true;
        }
    }

    @Override
    public MediaClock getClock() {
        return clock;
    }
}
