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

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public enum States {

    /**
     * Initialize state variables
     */
    INITIALIZE,

    /**
     * Stimulate the subject to a baseline arousal (i.e. achieve an erection).
     */
    WARMUP,
    
    /**
     * Use the configured stimulation pattern(s) to randomly stimulate the subject to the edge of orgasm and ruined orgasms.
     */
    TEASE,
    
    /**
     * Stimulate the subject to the edge of orgasm.
     */
    INDUCE_EDGE,
    
    /**
     * Stimulate the subject to a ruined orgasm.
     */
    INDUCE_RUIN,
    
    /**
     * Suspend stimulation and allow the subject to lose arousal.
     */
    COOLDOWN,
    
    /**
     * Configure the tease ending.
     */
    CONFIG_ENDING,
    
    /**
     * Stimulate the subject to a full orgasm.
     */
    ORGASM_ENDING1,
    
    /**
     * Continue to stimulate the subject until the orgasm is complete.
     */
    ORGASM_ENDING2,
    
    /**
     * Continue to stimulate the subject for post-orgasm torture (POT).
     */
    ENDING_POT,
    
    /**
     * Stimulate the subject to a ruined orgasm.
     */
    RUIN_ENDING,
    
    /**
     * Deny the subject any orgasm.
     */
    DENIED_ENDING,
    
    /**
     * Final state.
     */
    END;

}
