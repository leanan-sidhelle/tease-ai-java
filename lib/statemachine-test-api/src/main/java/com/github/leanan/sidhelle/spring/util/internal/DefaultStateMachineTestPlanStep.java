/**
 * TODO: because the spring devs suck...
 */
package com.github.leanan.sidhelle.spring.util.internal;

import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanStep;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.hamcrest.collection.IsMapContaining;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.test.support.LatchStateMachineListener;
import reactor.core.publisher.Mono;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class DefaultStateMachineTestPlanStep<S,E> implements
        StateMachineTestPlanStep<S,E> {
    
    /**
     * TODO:
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultStateMachineTestPlanStep.class);

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
    protected final Collection<S> expectStates = new HashSet<>();

    /**
     * TODO:
     */
    protected final Collection<S> expectStatesEntrered = new HashSet<>();

    /**
     * TODO:
     */
    protected final Collection<S> expectStatesExited = new HashSet<>();

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
    protected final Collection<Object> expectVariableKeys = new HashSet<>();

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
    protected final long defaultAwaitTime;
    private final String stepLocation;
    
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
     */
    public DefaultStateMachineTestPlanStep(
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
        Collection<S> expectStatesExited) {
        this.stepLocation = stepLocation;
        this.defaultAwaitTime = defaultAwaitTime;
        this.sendEvent.addAll(sendEvent);
        this.sendMessage.addAll(sendMessage);
        this.sendEventMachineId = sendEventMachineId;
        this.sendEventToAll = sendEventToAll;
        this.sendEventParallel = sendEventParallel;
        this.expectStates.addAll(expectStates);
        this.expectStateChanged = expectStateChanged;
        this.expectStateEntered = expectStateEntered;
        this.expectStateExited = expectStateExited;
        this.expectEventNotAccepted = expectEventNotAccepted;
        this.expectTransition = expectTransition;
        this.expectTransitionStarted = expectTransitionStarted;
        this.expectTransitionEnded = expectTransitionEnded;
        this.expectStateMachineStarted = expectStateMachineStarted;
        this.expectStateMachineStopped = expectStateMachineStopped;
        this.expectVariableKeys.addAll(expectVariableKeys);
        this.expectVariableMatchers.addAll(expectVariableMatchers);
        this.expectVariables.putAll(expectVariables);
        this.expectExtendedStateChanged = expectExtendedStateChanged;
        this.expectStatesEntrered.addAll(expectStatesEntrered);
        this.expectStatesExited.addAll(expectStatesExited);
    }
    
    @Override
    public void evaluate(
        Map<StateMachine<S, E>, LatchStateMachineListener<S, E>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws 
        AssertionFailedError, Exception {
        reset(latches);

        // need to call start here, ok to call from all steps
        for(StateMachine<S, E> stateMachine : stateMachines.values()) {
            stateMachine.startReactively().block();
        }

        if(expectStateMachineStarted != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat("StateMachineStarted Await not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateMachineStartedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat("StateMachineStarted count not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateMachineStarted().size(), is(expectStateMachineStarted));
            }
        }

        if(!sendEvent.isEmpty()) {
            ArrayList<StateMachine<S, E>> sendVia = new ArrayList<>();
            if(sendEventMachineId != null) {
                sendVia.add(stateMachines.get(sendEventMachineId));
            } else if(sendEventToAll) {
                sendVia.addAll(stateMachines.values());
            } else {
                sendVia.add(stateMachines.values().iterator().next());
            }
            assertThat("Error finding machine to send via", sendVia, not(empty()));
            if (!sendEventParallel) {
                for(StateMachine<S, E> machine : sendVia) {
                    for(E event : sendEvent) {
                        LOGGER.info("Sending test event " + event + " via machine " + machine);
                        machine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
                    }
                }
            } else {
                for(E event : sendEvent) {
                    sendEventParallel(sendVia, event);
                }
            }
        } else if (!sendMessage.isEmpty()) {
            ArrayList<StateMachine<S, E>> sendVia = new ArrayList<>();
            if(sendEventMachineId != null) {
                sendVia.add(stateMachines.get(sendEventMachineId));
            } else if(sendEventToAll) {
                sendVia.addAll(stateMachines.values());
            } else {
                sendVia.add(stateMachines.values().iterator().next());
            }
            assertThat("Error finding machine to send via", sendVia, not(empty()));
            for(StateMachine<S, E> machine : sendVia) {
                for (Message<E> event : sendMessage) {
                    LOGGER.info("Sending test event " + event + " via machine " + machine);
                    machine.sendEvent(Mono.just(event)).blockLast();
                }
            }
        }

        if(expectStateChanged != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat("StateChanged Await not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChangedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat("StateChanged count not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChanged().size(), is(expectStateChanged));
            }
        }

        if(expectStateEntered != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getStateEnteredLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateEntered().size(), is(expectStateEntered));
            }
        }

        if(expectStateExited != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getStateExitedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateExited().size(), is(expectStateExited));
            }
        }

        if(expectEventNotAccepted != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getEventNotAcceptedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getEventNotAccepted().size(),is(expectEventNotAccepted));
            }
        }

        if(expectTransition != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getTransitionLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransition().size(), is(expectTransition));
            }
        }

        if(expectTransitionStarted != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getTransitionStartedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransitionStarted().size(), is(expectTransitionStarted));
            }
        }

        if(expectTransitionEnded != null) {
            for (Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getTransitionEndedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransitionEnded().size(), is(expectTransitionEnded));
            }
        }

        if(expectStateMachineStopped != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getStateMachineStoppedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateMachineStopped().size(), is(expectStateMachineStopped));
            }
        }

        if(!expectStates.isEmpty()) {
            for(StateMachine<S, E> stateMachine : stateMachines.values()) {
                assertThat(stateMachine.getState(), notNullValue());
                Collection<Matcher<? super S>> itemMatchers = new ArrayList<>();
                for(S expectState : expectStates) {
                    itemMatchers.add(is(expectState));
                }
                assertThat(stateMachine.getState().getIds(), containsInAnyOrder(itemMatchers));
            }
        }

        if(!expectStatesEntrered.isEmpty()) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                    Collection<S> states = new ArrayList<>();
                    for (State<S, E> s : entry.getValue().getStateEntered()) {
                            states.add(s.getId());
                    }
                    assertThat(expectStatesEntrered, containsInAnyOrder(states.toArray()));
            }
        }

        if(!expectStatesExited.isEmpty()) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                Collection<S> states = new ArrayList<>();
                for (State<S, E> s : entry.getValue().getStateExited()) {
                    states.add(s.getId());
                }
                assertThat(expectStatesExited, containsInAnyOrder(states.toArray()));
            }
        }

        if(expectExtendedStateChanged != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches.entrySet()) {
                assertThat(entry.getValue().getExtendedStateChangedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getExtendedStateChanged().size(), is(expectExtendedStateChanged));
            }
        }

        if(!expectVariableKeys.isEmpty()) {
            for(StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine.getExtendedState().getVariables();
                for (Object key : expectVariableKeys) {
                    assertThat("Key [" + key + 
                        "] doesn't exist in extended state variables",
                        variables,IsMapContaining.hasKey(key));
                }
            }
        }

        if(!expectVariableMatchers.isEmpty()) {
            for (StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine
                        .getExtendedState().getVariables();
                for (Matcher<Map<? extends Object, ?>> matcher : expectVariableMatchers) {
                        assertThat(variables, matcher);
                }
            }
        }

        if(!expectVariables.isEmpty()) {
            for (StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine
                        .getExtendedState().getVariables();
                for(Entry<Object, Object> entry : expectVariables.entrySet()) {
                        assertThat("Entry with key=[" + entry.getKey() + 
                            "] value=[" + entry.getValue() + 
                            "] doesn't exist in extended state variables",
                            variables, IsMapContaining.hasEntry(
                                    entry.getKey(), entry.getValue()));
                }
            }
        }
    }
    
    @Override
    public String getLocation() {
        return stepLocation;
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectStarted(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateMachineStarted != null) {
            for(Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                //TODO:can any of this boiler plate be refactored for re-use?
                assertThat("StateMachineStarted Await not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateMachineStartedLatch().await(defaultAwaitTime, TimeUnit.MILLISECONDS), is(true));
                assertThat("StateMachineStarted count not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateMachineStarted().size(), is(expectStateMachineStarted));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void sendEventsOrMessages(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!sendEvent.isEmpty()) {
            ArrayList<StateMachine<S, E>> sendVia = new ArrayList<>();
            if(sendEventMachineId != null) {
                sendVia.add(stateMachines.get(sendEventMachineId));
            } else if(sendEventToAll) {
                sendVia.addAll(stateMachines.values());
            } else {
                sendVia.add(stateMachines.values().iterator().next());
            }
            assertThat("Error finding machine to send via", sendVia, not(empty()));
            if(!sendEventParallel) {
                for(StateMachine<S, E> machine : sendVia) {
                    for(E event : sendEvent) {
                        LOGGER.info("Sending test event " + event + " via machine " + machine);
                        machine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
                    }
                }
            } else {
                for(E event : sendEvent) {
                    sendEventParallel(sendVia, event);
                }
            }
        } else if(!sendMessage.isEmpty()) {
            ArrayList<StateMachine<S, E>> sendVia = new ArrayList<>();
            if(sendEventMachineId != null) {
                sendVia.add(stateMachines.get(sendEventMachineId));
            } else if(sendEventToAll) {
                sendVia.addAll(stateMachines.values());
            } else {
                sendVia.add(stateMachines.values().iterator().next());
            }
            assertThat("Error finding machine to send via", sendVia, not(empty()));
            for(StateMachine<S, E> machine : sendVia) {
                for(Message<E> event : sendMessage) {
                    LOGGER.info("Sending test event " + event + " via machine " + machine);
                    machine.sendEvent(Mono.just(event)).blockLast();
                }
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectState(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateChanged != null) {
            for(Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat("StateChanged Await not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChangedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat("StateChanged count not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChanged().size(), is(expectStateChanged));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectStateEntered(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateEntered != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getStateEnteredLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateEntered().size(), is(expectStateEntered));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectStateExited(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateExited != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getStateExitedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateExited().size(), is(expectStateExited));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectEventNotAccepted(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
            if(expectEventNotAccepted != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getEventNotAcceptedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getEventNotAccepted().size(), is(expectEventNotAccepted));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectTransition(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectTransition != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getTransitionLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransition().size(), is(expectTransition));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectTransitionStarted(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectTransitionStarted != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getTransitionStartedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransitionStarted().size(), is(expectTransitionStarted));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectTransitionEnded(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectTransitionEnded != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getTransitionEndedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getTransitionEnded().size(), is(expectTransitionEnded));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectStopped(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateMachineStopped != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getStateMachineStoppedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getStateMachineStopped().size(), is(expectStateMachineStopped));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluatedExpectStates(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectStates.isEmpty()) {
            for(StateMachine<S, E> stateMachine : stateMachines.values()) {
                assertThat(stateMachine.getState(), notNullValue());
                Collection<Matcher<? super S>> itemMatchers = new ArrayList<>();
                for(S expectState : expectStates) {
                        itemMatchers.add(is(expectState));
                }
                LOGGER.debug("Expects {} and is {}.",itemMatchers,stateMachine.getState().getIds());
                assertThat(stateMachine.getState().getIds(), containsInAnyOrder(itemMatchers));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluateExpectStatesEntered(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectStatesEntrered.isEmpty()) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                Collection<S> states = new ArrayList<>();
                for(State<S, E> s : entry.getValue().getStateEntered()) {
                    states.add(s.getId());
                }
                assertThat(expectStatesEntrered, containsInAnyOrder(states.toArray()));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluateExpectStatesExited(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectStatesExited.isEmpty()) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                Collection<S> states = new ArrayList<>();
                for(State<S, E> s : entry.getValue().getStateExited()) {
                    states.add(s.getId());
                }
                assertThat(expectStatesExited, containsInAnyOrder(states.toArray()));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     * @throws java.lang.InterruptedException
     */
    protected void evaluateExpectExtendedStateChanged(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectExtendedStateChanged != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat(entry.getValue().getExtendedStateChangedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat(entry.getValue().getExtendedStateChanged().size(), is(expectExtendedStateChanged));
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluateExpectVariableKeys(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectVariableKeys.isEmpty()) {
            for(StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine.getExtendedState().getVariables();
                for (Object key : expectVariableKeys) {
                    assertThat("Key [" + key + 
                        "] doesn't exist in extended state variables",
                        variables,IsMapContaining.hasKey(key));
                }
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluateExpectVariableMatchers(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectVariableMatchers.isEmpty()) {
            for (StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine.getExtendedState().getVariables();
                for(Matcher<Map<? extends Object, ?>> matcher : expectVariableMatchers) {
                    assertThat(variables, matcher);
                }
            }
        }
    }

    /**
     * TODO:
     * @param latches
     * @param stateMachines
     */
    protected void evaluateExpectVariables(
        Set<Map.Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) {
        if(!expectVariables.isEmpty()) {
            for (StateMachine<S, E> stateMachine : stateMachines.values()) {
                Map<Object, Object> variables = stateMachine.getExtendedState().getVariables();
                for(Entry<Object, Object> entry : expectVariables.entrySet()) {
                    assertThat("Entry with key=[" + entry.getKey() + 
                        "] value=[" + entry.getValue() + 
                        "] doesn't exist in extended state variables",
                        variables, IsMapContaining.hasEntry(
                                entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    /**
    * Send event parallel to all machines.
    *
    * @param machines the machines
    * @param event the event
    */
   private void sendEventParallel(final List<StateMachine<S, E>> machines, final E event) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ArrayList<Thread> joins = new ArrayList<>();
        int threadCount = machines.size();
        for(int i = 0; i < threadCount; ++i) {
            final StateMachine<S,E> machine = machines.get(i);
            Runnable runner = () -> {
                try {
                    latch.await();
                    machine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
                } catch (InterruptedException e) {}
            };
            Thread t = new Thread(runner, "EventSenderThread" + i);
            joins.add(t);
            t.start();
        }
        latch.countDown();
        for(Thread t : joins) {
            try {
                t.join();
            } catch (InterruptedException e) {}
        }
    }
   
    //TODO:how to combine this into a single generic method???
    public static Integer orZero(Integer number) {
        return (number!=null) ? number : 0;
    }
    
    public static Long orZero(Long number) {
        return (number!=null) ? number : 0L;
    }

    public static Double orZero(Double number) {
        return (number!=null) ? number : 0.0;
    }
    
    public static Float orZero(Float number) {
        return (number!=null) ? number : 0.0f;
    }

    public static Short orZero(Short number) {
        return (number!=null) ? number : (short)0;
    }
    
    public static Byte orZero(Byte number) {
        return (number!=null) ? number : (byte)0;
    }
    
    public static BigDecimal orZero(BigDecimal number) {
        return (number!=null) ? number : BigDecimal.ZERO;
    }
    
    public static BigInteger orZero(BigInteger number) {
        return (number!=null) ? number : BigInteger.ZERO;
    }

    protected void reset(Map<StateMachine<S, E>, LatchStateMachineListener<S, E>> latches) {
        for (LatchStateMachineListener<S, E> listener : latches.values()) {
            listener.reset(
                orZero(expectStateChanged),
                orZero(expectStateEntered),
                orZero(expectStateExited),
                orZero(expectEventNotAccepted),
                orZero(expectTransition),
                orZero(expectTransitionStarted),
                orZero(expectTransitionEnded),
                orZero(expectStateMachineStarted),
                orZero(expectStateMachineStopped),
                orZero(expectExtendedStateChanged));
        }
    }
    
    protected void evaluateExpectStateChanged(
        Set<Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>>> latches,
        Map<Object, StateMachine<S, E>> stateMachines) throws InterruptedException {
        if(expectStateChanged != null) {
            for(Entry<StateMachine<S, E>, LatchStateMachineListener<S, E>> entry : latches) {
                assertThat("StateChanged Await not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChangedLatch().await(defaultAwaitTime,MILLISECONDS), is(true));
                assertThat("StateChanged count not matched for machine " + entry.getKey(), entry.getValue()
                    .getStateChanged().size(), is(expectStateChanged));
            }
        }
    }
}