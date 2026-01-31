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

import com.github.leanan.sidhelle.spring.util.ForNextStateMachineTestPlanStepBuilder;
import com.github.leanan.sidhelle.spring.util.RepeatedStateMachineTestPlanStepBuilder;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlan;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanBuilder;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanStepBuilder;
import com.github.leanan.sidhelle.util.StacktraceUtils;
import java.time.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.statemachine.StateMachine;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultStateMachineTestPlanBuilder<S,E> implements 
        StateMachineTestPlanBuilder<S,E> {

    /**
     * TODO:
     */
    protected final Map<Object, StateMachine<S, E>> stateMachines = new HashMap<>();

    /**
     * TODO:
     */
    protected final List<DefaultStateMachineTestPlanStep<S, E>> steps = new ArrayList<>();

    /**
     * TODO:
     * @return 
     */
    protected List<DefaultStateMachineTestPlanStep<S,E>> steps() {
        return this.steps;
    }
    
    /**
     * TODO:
     */
    protected Long defaultAwaitTime;

    @Override
    public StateMachineTestPlanBuilder<S, E> stateMachine(StateMachine<S, E> stateMachine) {
            return stateMachine(stateMachine, stateMachine);
    }

    @Override
    public StateMachineTestPlanBuilder<S, E> stateMachine(StateMachine<S, E> stateMachine, Object machineId) {
            this.stateMachines.put(machineId, stateMachine);
            return this;
    }
    
    @Override
    public StateMachineTestPlanBuilder<S, E> defaultAwaitTime(int seconds) {
        return defaultAwaitTime((float)seconds);
    }

    @Override
    public StateMachineTestPlanBuilder<S,E> defaultAwaitTime(float seconds) {
        if (seconds < 0) {
                throw new IllegalArgumentException("Default await time cannot be negative, was " + seconds);
        }
        this.defaultAwaitTime = toMillis(seconds);
        return this;
    }
    
    @Override
    public StateMachineTestPlanStepBuilder<S,E> step() {
        String location = StacktraceUtils.format(StacktraceUtils.trace(2));
        return new DefaultStateMachineTestPlanStepBuilder(location,this,defaultAwaitTime);
    }
    
    @Override
    public RepeatedStateMachineTestPlanStepBuilder<S,E> repeat(int cycles) {
        String location = StacktraceUtils.format(StacktraceUtils.trace(3)); //TODO:WHY ARE THESE DIFFERENT????
        return new DefaultRepeatedStateMachineTestPlanStepBuilder(location,this,defaultAwaitTime,cycles);
    }
    
    @Override
    public StateMachineTestPlan<S, E> build() {
        return new DefaultStateMachineTestPlan<>(stateMachines, steps);
    }



    /**
     * TODO:
     * @param seconds
     * @return 
     */
    protected static Long toMillis(float seconds) {
        return (long)(seconds * 1000.0f);
    }

    @Override
    public ForNextStateMachineTestPlanStepBuilder<S, E> forNext(Duration duration) {
        String location = StacktraceUtils.format(StacktraceUtils.trace(3)); //TODO:WHY ARE THESE DIFFERENT????
        return new DefaultForNextStateMachineTestPlanStepBuilder(location,this,defaultAwaitTime,duration);
    }
    
}
