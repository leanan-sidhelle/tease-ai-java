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

import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanBuilder;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanStepBuilder;
import com.github.leanan.sidhelle.util.StacktraceUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultStateMachineTestPlanStepBuilder<S,E> implements 
        StateMachineTestPlanStepBuilder<S,E> {

    /**
     * TODO:
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultStateMachineTestPlanStepBuilder.class);
    
    /**
     * TODO:
     */
    protected final List<E> sendEvent = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected final List<Message<E>> sendMessage = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected Object sendEventMachineId;
    
    /**
     * TODO:
     */
    protected boolean sendEventToAll = false;
    
    /**
     * TODO:
     */
    protected boolean sendEventParallel = false;
    
    /**
     * TODO:
     */
    protected final Collection<S> expectStates = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected final Collection<S> expectStatesEntrered = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected final Collection<S> expectStatesExited = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected Integer expectStateChanged;
    
    /**
     * TODO:
     */
    protected Integer expectStateEntered;
    
    /**
     * TODO:
     */
    protected Integer expectStateExited;
    
    /**
     * TODO:
     */
    protected Integer expectEventNotAccepted;
    
    /**
     * TODO:
     */
    protected Integer expectTransition;
    
    /**
     * TODO:
     */
    protected Integer expectTransitionStarted;
    
    /**
     * TODO:
     */
    protected Integer expectTransitionEnded;
    
    /**
     * TODO:
     */
    protected Integer expectStateMachineStarted;
    
    /**
     * TODO:
     */
    protected Integer expectStateMachineStopped;
    
    /**
     * TODO:
     */
    protected Integer expectExtendedStateChanged;
    
    /**
     * TODO:
     */
    protected final Collection<Object> expectVariableKeys = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected final Collection<Matcher<Map<? extends Object, ?>>> expectVariableMatchers = new ArrayList<>();
    
    /**
     * TODO:
     */
    protected final Map<Object, Object> expectVariables = new HashMap<>();
    
    /**
     * TODO:
     */
    protected final DefaultStateMachineTestPlanBuilder<S, E> builder;
    
    /**
     * TODO:
     */
    protected final long defaultAwaitTime;
    
    /**
     * TODO:
     */
    protected final String stepLocation;
        
    /**
     * TODO:
     * @param stepLocation
     * @param builder 
     * @param defaultAwaitTime 
     */
    public DefaultStateMachineTestPlanStepBuilder(
        String stepLocation,
        DefaultStateMachineTestPlanBuilder<S,E> builder,
        long defaultAwaitTime) {
        this.stepLocation = stepLocation;
        this.builder = builder;
        this.defaultAwaitTime = defaultAwaitTime;
    }

    
    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectState(S state) {
        LOGGER.debug("Expected {} at {}.",state,StacktraceUtils.format(StacktraceUtils.trace(2)));
        this.expectStates.add(state);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked") //TODO:how to avoid this???
    public StateMachineTestPlanStepBuilder<S, E> expectStates(S... states) {
        this.expectStates.addAll(Arrays.asList(states));
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(E event) {
        return sendEvent(event, false);
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(E event, boolean sendToAll) {
        sendEvent(event, sendToAll, false);
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(E event, boolean sendToAll, boolean sendParallel) {
        this.sendEvent.add(event);
        this.sendEventMachineId = null;
        this.sendEventToAll = sendToAll;
        this.sendEventParallel = sendParallel;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(E event, Object machineId) {
        this.sendEvent.add(event);
        this.sendEventMachineId = machineId;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(Message<E> event) {
        return sendEvent(event, false);
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(Message<E> event, boolean sendToAll) {
        this.sendMessage.add(event);
        this.sendEventMachineId = null;
        this.sendEventToAll = sendToAll;
        return this;
    }
    
    @Override
    public StateMachineTestPlanStepBuilder<S, E> sendEvent(Message<E> event, Object machineId) {
        this.sendMessage.add(event);
        this.sendEventMachineId = machineId;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectVariable(Object key) {
        this.expectVariableKeys.add(key);
        return this;
    }
    
    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectVariableWith(Matcher<Map<? extends Object, ?>> matcher) {
        this.expectVariableMatchers.add(matcher);
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectVariable(Object key, Object value) {
        this.expectVariables.put(key, value);
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectStateChanged(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectStateChanged = count;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked") //TODO:how to avoid this?
    public StateMachineTestPlanStepBuilder<S, E> expectStateEntered(S... states) {
        this.expectStatesEntrered.addAll(Arrays.asList(states));
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked") //TODO:how to avoid this?
    public StateMachineTestPlanStepBuilder<S, E> expectStateExited(S... states) {
        this.expectStatesExited.addAll(Arrays.asList(states));
        return this;
    }
    
    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectStateEntered(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectStateEntered = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectStateExited(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectStateExited = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectEventNotAccepted(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectEventNotAccepted = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectTransition(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectTransition = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectTransitionStarted(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectTransitionStarted = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectTransitionEnded(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectTransitionEnded = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectStateMachineStarted(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectStateMachineStarted = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectStateMachineStopped(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectStateMachineStopped = count;
        return this;
    }

    @Override
    public StateMachineTestPlanStepBuilder<S, E> expectExtendedStateChanged(int count) {
        if (count < 0) {
                throw new IllegalArgumentException("Expected count cannot be negative, was " + count);
        }
        this.expectExtendedStateChanged = count;
        return this;
    }

    @Override
    public StateMachineTestPlanBuilder<S, E> and() {
        builder.steps().add(new DefaultStateMachineTestPlanStep(
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
        expectStatesExited));
        return this.builder;
    }


}
