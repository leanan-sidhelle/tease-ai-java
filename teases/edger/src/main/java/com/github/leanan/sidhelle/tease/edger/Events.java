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

package com.github.leanan.sidhelle.tease.edger;

import com.github.leanan.sidhelle.tease.event.ArousalEvent;
import com.github.leanan.sidhelle.tease.event.EdgeEvent;
import com.github.leanan.sidhelle.tease.event.HardEvent;
import com.github.leanan.sidhelle.tease.event.OrgasmEvent;
import com.github.leanan.sidhelle.tease.event.RuinEvent;
import com.github.leanan.sidhelle.tease.event.SoftEvent;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public enum Events {

    /**
     * TODO:
     */
    SOFT,
    
    /**
     * TODO:
     */
    HARD,
    
    /**
     * TODO:
     */
    EDGE,
    
    /**
     * TODO:
     */
    ORGASM,
    
    /**
     * TODO:
     */
    RUIN;

    /**
     * TODO:
     * @param event
     * @return 
     */
    public static Events toEnum(ArousalEvent event) throws Exception {
        if(event instanceof SoftEvent) {
            return SOFT;
        } else if(event instanceof HardEvent) {
            return HARD;
        } else if(event instanceof EdgeEvent) {
            return EDGE;
        } else if(event instanceof OrgasmEvent) {
            return ORGASM;
        } else if(event instanceof RuinEvent) {
            return RUIN;
        } else {
            throw new Exception("Unhandled event type.");
        }
    }
}
