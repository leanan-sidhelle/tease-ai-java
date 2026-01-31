/**
 * TODO:again because the spring devs fucking suck!!!
 */

package com.github.leanan.sidhelle.spring.util;

import java.util.Map;
import org.hamcrest.Matcher;
import org.springframework.messaging.Message;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public interface StateMachineTestPlanStepBuilder<S,E> {

    /**
     * Add a new step and return StateMachineTestPlanBuilder for chaining.
     * @return 
     */
    public StateMachineTestPlanBuilder<S,E> and();
    
    /**
     * Expect event not accepter happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectEventNotAccepted(int count);
    
    /**
     * Expect state machine extended state variables changing count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectExtendedStateChanged(int count);
   
    /**
     * Expect a state S.
     * @param state
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectState(S state);

    /**
     * Expect state changed happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateChanged(int count);
    
    /**
     * Expect state enter happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateEntered(int count);

    /**
     * Expect states entered in order given.
     * @param states
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateEntered(S... states);
    
    /**
     * Expect state exit happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateExited(int count);
    
    /**
     * Expect states exited in order given.
     * @param states
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateExited(S... states);
    
    /**
     * Expect state machine start happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateMachineStarted(int count);
    
    /**
     * Expect state machine stop happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStateMachineStopped(int count);
    
    /**
     * Expect a states S.
     * @param states
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectStates(S... states);
    
    /**
     * Expect transition happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectTransition(int count);
    
    /**
     * Expect transition end happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectTransitionEnded(int count);
    
    /**
     * Expect transition start happening count times.
     * @param count
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectTransitionStarted(int count);
    
    /**
     * Expect variable to exist in extended state variables.
     * @param key
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectVariable(Object key);
    
    /**
     * Expect variable to exist in extended state variables and match with the value.
     * @param key
     * @param value
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectVariable(Object key, Object value);
    
    /**
     * Expect variable map with hamcrest Matcher.
     * @param matcher
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> expectVariableWith(Matcher<Map<? extends Object,?>> matcher);
    
    /**
     * Send an event E.
     * @param event
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(E event);
    
    /**
     * Send an event E.
     * @param event
     * @param sendToAll
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(E event, boolean sendToAll);
    
    /**
     * Send an event E.
     * @param event
     * @param sendToAll
     * @param sendParallel
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(E event, boolean sendToAll, boolean sendParallel);
    
    /**
     * Send an event E into a state machine identified by machineId.
     * @param event
     * @param machineId
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(E event, Object machineId);
    
    /**
     * Send a message Message%ltE%gt.
     * @param event
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(Message<E> event);
    
    /**
     * Send a message Message%ltE%gt.
     * @param event
     * @param sendToAll
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(Message<E> event, boolean sendToAll);
    
    /**
     * TODO:
     * @param event
     * @param machineId
     * @return 
     */
    public StateMachineTestPlanStepBuilder<S,E> sendEvent(Message<E> event, Object machineId);
    
}
