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

package com.github.leanan.sidhelle.spring.util.internal;

import com.github.leanan.sidhelle.spring.util.RepeatedStateMachineTestPlanStepBuilder;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanBuilder;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultRepeatedStateMachineTestPlanStepBuilder<S,E> extends
        DefaultStateMachineTestPlanStepBuilder<S,E> implements 
        RepeatedStateMachineTestPlanStepBuilder<S,E> {

    /**
     * TODO
     */
    private final int cycles;

    /**
     * TODO:
     * @param stepLocation
     * @param builder
     * @param cycles 
     * @param defaultAwaitTime 
     */
    public DefaultRepeatedStateMachineTestPlanStepBuilder(
        String stepLocation,
        DefaultStateMachineTestPlanBuilder<S, E> builder,
        long defaultAwaitTime,
        int cycles) {
        super(stepLocation,builder,defaultAwaitTime);
        this.cycles = cycles;
    }

    @Override
    public StateMachineTestPlanBuilder<S, E> and() {
        builder.steps().add(new DefaultRepeatedStateMachineTestPlanStep(
        stepLocation,
        defaultAwaitTime,
        sendEvent, 
        sendMessage, 
        sendEventMachineId,
        sendEventToAll,
        sendEventParallel,
        expectStates,
        expectStateChanged,
        expectStateEntered,
        expectStateExited,
        expectEventNotAccepted,
        expectTransition,
        expectTransitionStarted,
        expectTransitionEnded,
        expectStateMachineStarted,
        expectStateMachineStopped,
        expectVariableKeys,
        expectVariableMatchers,
        expectVariables,
        expectExtendedStateChanged,
        expectStatesEntrered,
        expectStatesExited,
        cycles));
        return this.builder;
    }
}
