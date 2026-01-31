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

package com.github.leanan.sidhelle.tease.util;

import com.github.leanan.sidhelle.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.TimerTrigger;
import org.springframework.statemachine.trigger.Trigger;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class LoggingStateMachineListener<S,E> implements 
        StateMachineListener<S,E> {

    /**
     * The {@link Logger} for message logging.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(LoggingStateMachineListener.class);
    
    /**
     * TODO:
     */
    public LoggingStateMachineListener() {}

    @Override
    public void stateChanged(State<S, E> from, State<S, E> to) {
        LOGGER.debug("State changed from {} to {}.",
            StateMachineUtils.id(from),StateMachineUtils.id(to));
    }

    @Override
    public void stateEntered(State<S, E> state) {
        //TODO:
    }

    @Override
    public void stateExited(State<S, E> state) {
        //TODO:
    }

    @Override
    public void eventNotAccepted(Message<E> msg) {
        LOGGER.debug("Did not accept {}.",msg.getPayload());
    }

    @Override
    public void transition(Transition<S, E> t) {
        Trigger<S, E> trigger = t.getTrigger();
        if(trigger instanceof TimerTrigger timer) {
            LOGGER.debug("Transitioned from {} to {} after {} (count={}).",
            StateMachineUtils.id(t.getSource()),
            StateMachineUtils.id(t.getTarget()),
            timer.getPeriod(),timer.getCount());
        } else {
            LOGGER.debug("Transitioned from {} to {} on {}.",
            StateMachineUtils.id(t.getSource()),
            StateMachineUtils.id(t.getTarget()),
            StateMachineUtils.id(trigger));            
        }
    }

    @Override
    public void transitionStarted(Transition<S, E> trnstn) {
        //TODO:
    }

    @Override
    public void transitionEnded(Transition<S, E> trnstn) {
        //TODO:
    }

    @Override
    public void stateMachineStarted(StateMachine<S, E> sm) {
        LOGGER.debug("Started {}.",StateMachineUtils.id(sm));
    }

    @Override
    public void stateMachineStopped(StateMachine<S, E> sm) {
        LOGGER.debug("Stopped {}.",StateMachineUtils.id(sm));
    }

    @Override
    public void stateMachineError(StateMachine<S, E> sm, Exception e) {
        LOGGER.error("Error executing {}.",StateMachineUtils.id(sm));
        LoggingUtils.trace(LOGGER, e);
    }

    @Override
    public void extendedStateChanged(Object key, Object value) {
        LOGGER.debug("Changed value of {} to {}.",key,value);
    }

    @Override
    public void stateContext(StateContext<S, E> sc) {
        //TODO:
    }

}
