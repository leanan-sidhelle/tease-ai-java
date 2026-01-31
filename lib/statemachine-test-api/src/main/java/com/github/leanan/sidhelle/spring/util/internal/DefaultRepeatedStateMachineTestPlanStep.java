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

import com.github.leanan.sidhelle.spring.util.RepeatedStateMachineTestPlanStep;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.support.LatchStateMachineListener;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultRepeatedStateMachineTestPlanStep<S,E> extends 
        DefaultStateMachineTestPlanStep<S,E> implements 
        RepeatedStateMachineTestPlanStep<S,E> {
    
    /**
     * The {@link Logger} for message logging.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultRepeatedStateMachineTestPlanStep.class);

    /**
     * TODO:
     */
    protected int cycles;

    /**
     * TODO:
     * @param stepLocation
     * @param defaultAwaitTime
     * @param sendEvent
     * @param sendMessage
     * @param sendEventMachineId
     * @param sendEventToAll
     * @param sendEventParallel
     * @param expectStates
     * @param expectStateChanged
     * @param expectStateEntered
     * @param expectStateExited
     * @param expectEventNotAccepted
     * @param expectTransition
     * @param expectTransitionStarted
     * @param expectTransitionEnded
     * @param expectStateMachineStarted
     * @param expectStateMachineStopped
     * @param expectVariableKeys
     * @param expectVariableMatchers
     * @param expectVariables
     * @param expectExtendedStateChanged
     * @param expectStatesEntrered
     * @param expectStatesExited 
     * @param cycles 
     */
    public DefaultRepeatedStateMachineTestPlanStep(
        String stepLocation,
        long defaultAwaitTime,
        List<E> sendEvent, 
        List<Message<E>> sendMessage, 
        Object sendEventMachineId,
        boolean sendEventToAll,
        boolean sendEventParallel,
        Collection<S> expectStates,
        Integer expectStateChanged,
        Integer expectStateEntered,
        Integer expectStateExited,
        Integer expectEventNotAccepted,
        Integer expectTransition,
        Integer expectTransitionStarted,
        Integer expectTransitionEnded,
        Integer expectStateMachineStarted,
        Integer expectStateMachineStopped,
        Collection<Object> expectVariableKeys,
        Collection<Matcher<Map<? extends Object, ?>>> expectVariableMatchers,
        Map<Object, Object> expectVariables,
        Integer expectExtendedStateChanged,
        Collection<S> expectStatesEntrered,
        Collection<S> expectStatesExited,
        int cycles) {
        super(stepLocation,defaultAwaitTime,sendEvent,sendMessage,sendEventMachineId,
              sendEventToAll,sendEventParallel,expectStates,expectStateChanged,
              expectStateEntered,expectStateExited,expectEventNotAccepted,
              expectTransition,expectTransitionStarted,expectTransitionEnded,
              expectStateMachineStarted,expectStateMachineStopped,
              expectVariableKeys,expectVariableMatchers,expectVariables,
              expectExtendedStateChanged,expectStatesEntrered,
              expectStatesExited);
        this.cycles = cycles;
    }
    
    @Override
    public boolean isComplete() {
        LOGGER.debug("Testing is complete.");
        return cycles <= 0;
    }
    
    @Override
    public void evaluate(
        Map<StateMachine<S, E>, LatchStateMachineListener<S, E>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws 
        AssertionFailedError, Exception  {
        while(cycles>0) {
            super.evaluate(latches, stateMachines);
            cycles = cycles - 1;
            LOGGER.debug("Decrementing cycle count to {}.",cycles);
        }
    }

}
