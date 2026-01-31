/**
 * TODO:because the spring devs suck...
 */
package com.github.leanan.sidhelle.spring.util;

import java.util.Map;
import org.opentest4j.AssertionFailedError;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.support.LatchStateMachineListener;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public interface StateMachineTestPlanStep<S,E> {
        
    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws AssertionFailedError - assertion failure during evaluation
     * @throws Exception - non-assertion exception encountered during 
     * evaluation
     */
    public void evaluate(
        Map<StateMachine<S, E>, LatchStateMachineListener<S, E>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws 
            AssertionFailedError,Exception;

    /**
     * TODO:
     * @return 
     */
    public String getLocation();
    
}