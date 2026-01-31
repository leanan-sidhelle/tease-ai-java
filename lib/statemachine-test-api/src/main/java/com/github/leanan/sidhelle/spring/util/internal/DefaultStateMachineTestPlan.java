/**
 * TODO: because the spring devs suck...
 */
package com.github.leanan.sidhelle.spring.util.internal;

import com.github.leanan.sidhelle.spring.util.StateMachineTestPlan;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanStep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.support.LatchStateMachineListener;
import org.springframework.util.StringUtils;


/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultStateMachineTestPlan<S,E> implements StateMachineTestPlan<S,E> {
    
    /**
     * The {@link Logger} for message logging.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultStateMachineTestPlan.class);
    
    /**
     * TODO:
     */
    private final Map<Object, StateMachine<S, E>> stateMachines;
    
    /**
     * TODO:
     */
    private final List<DefaultStateMachineTestPlanStep<S, E>> steps;
    
    /**
     * Instantiates a new state machine test plan.
     *
     * @param stateMachines the state machines
     * @param steps the steps
     */
    public DefaultStateMachineTestPlan(Map<Object, 
        StateMachine<S, E>> stateMachines, 
        List<DefaultStateMachineTestPlanStep<S, E>> steps) {
        this.stateMachines = stateMachines;
        this.steps = steps;
    }

    @Override
    public void test() throws Exception {
        //create latches to control stepping
        Map<StateMachine<S, E>, LatchStateMachineListener<S, E>> listeners = 
                new HashMap<>();
        for(StateMachine<S, E> stateMachine : stateMachines.values()) {
            LatchStateMachineListener<S, E> listener = 
                    new LatchStateMachineListener<>();
            listeners.put(stateMachine, listener);
            stateMachine.addStateListener(listener);
        }
        
        //execute the test plan steps
        LOGGER.info("Running test plan for machines " + 
            StringUtils.collectionToCommaDelimitedString(stateMachines.values()));
        int stepCounter = 0;
        for(StateMachineTestPlanStep<S, E> step : steps) {
            String location = step.getLocation();
            switch(step) {
                case DefaultRepeatedStateMachineTestPlanStep repeated -> {
                    int cycleCounter = 0;
                    LOGGER.debug("Running {} for {} cycles.",location,repeated.cycles);
                    do {
                        LOGGER.info("Running test plan step {} cycle {}. {}",stepCounter++,cycleCounter++,location);
                        repeated.evaluate(listeners,stateMachines);
                    } while(!repeated.isComplete());                    
                }
                case DefaultForNextStateMachineTestPlanStep repeated -> {
                    int cycleCounter = 0;
                    LOGGER.debug("Running {} for {}.",location,repeated.duration);
                    do {
                        LOGGER.info("Running test plan step {} cycle {}. {}",stepCounter++,cycleCounter++,location);
                        repeated.evaluate(listeners,stateMachines);
                    } while(!repeated.isComplete());
                }
                default -> {
                    LOGGER.info("Running test plan step {}. {}",stepCounter++,location);
                    step.evaluate(listeners,stateMachines);
                }
            }
        }
    }

}