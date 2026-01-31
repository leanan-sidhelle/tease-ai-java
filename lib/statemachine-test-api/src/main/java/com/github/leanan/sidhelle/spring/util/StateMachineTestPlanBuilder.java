/*
 * TODO: because the spring devs suck big donkey dicks...
*/
package com.github.leanan.sidhelle.spring.util;

import com.github.leanan.sidhelle.spring.util.internal.DefaultStateMachineTestPlanBuilder;
import java.time.Duration;
import org.springframework.statemachine.StateMachine;

/**
 * A builder for {@link StateMachineTestPlan}.
 *
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public interface StateMachineTestPlanBuilder<S, E> {

    /**
     * Gets a new instance of this builder.
     *
     * @param <S> the type of state
     * @param <E> the type of event
     * @return the state machine test plan builder
     */
    public static <S, E> StateMachineTestPlanBuilder<S, E> builder() {
            return new DefaultStateMachineTestPlanBuilder<>();
    }
    
    /**
     * Associate a state machine with this builder.
     *
     * @param stateMachine the state machine
     * @return the state machine test plan builder
     */
    public StateMachineTestPlanBuilder<S, E> stateMachine(StateMachine<S, E> stateMachine);

    /**
     * Associate a state machine with this builder.
     *
     * @param stateMachine the state machine
     * @param machineId the machine id to use for sending
     * @return the state machine test plan builder
     */
    public StateMachineTestPlanBuilder<S, E> stateMachine(StateMachine<S, E> stateMachine, Object machineId);

    /**
     * Sets default await time. This is in seconds how long a latch
     * will be waited for listening various callbacks.
     *
     * @param seconds the default await time in seconds
     * @return the state machine test plan builder
     */
    public StateMachineTestPlanBuilder<S, E> defaultAwaitTime(int seconds);

    /**
     * Sets default await time. This is in seconds how long a latch
     * will be waited for listening various callbacks.
     *
     * @param seconds the default await time in seconds
     * @return the state machine test plan builder
     */
    public StateMachineTestPlanBuilder<S, E> defaultAwaitTime(float seconds);

    /**
     * Gets a new step builder.
     *
     * @return the state machine test plan step builder
     */
    public StateMachineTestPlanStepBuilder<S,E> step();

    /**
     * Gets a new step builder.
     *
     * @param repetitions
     * @return the state machine test plan step builder
     */
    public StateMachineTestPlanStepBuilder<S,E> repeat(int repetitions);

    /**
     * TODO:
     * @param duration
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> forNext(Duration duration);
    
    /**
     * Builds the state machine test plan.
     *
     * @return the state machine test plan
     */
    public StateMachineTestPlan<S, E> build();

}