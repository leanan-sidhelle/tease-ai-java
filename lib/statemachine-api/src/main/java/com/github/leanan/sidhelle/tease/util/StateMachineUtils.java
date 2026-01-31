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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.trigger.Trigger;
import reactor.core.publisher.Mono;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class StateMachineUtils {

    /**
     * The {@link Logger} for message logging.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(StateMachineUtils.class);
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param sm 
     */
    public static <S,E> void start(StateMachine<S,E> sm) {
        LOGGER.debug("Starting {}.",id(sm));
        sm.startReactively().subscribe(s  -> {
            LOGGER.info("Successfully started {}.",id(sm));
        }, e -> {
            LOGGER.error("Error starting {}.",id(sm));
            LoggingUtils.trace(LOGGER,e);
        });
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param sm 
     */
    public static <S,E> void stop(StateMachine<S,E> sm) {
        LOGGER.debug("Stopping {}.",id(sm));
        sm.stopReactively().subscribe(s  -> {
            LOGGER.info("Successfully stopped {}.",id(sm));
        }, e -> {
            LOGGER.error("Error stopping {}.",id(sm));
            LoggingUtils.trace(LOGGER,e);
        });
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param sm 
     * @param event 
     */
    public static <S,E> void send(StateMachine<S,E> sm,E event) {
        LOGGER.debug("Sending {} to {}.",event,id(sm));
        Message<E> msg = MessageBuilder.withPayload(event).build();
        sm.sendEvent(Mono.just(msg)).subscribe(s  -> {
            LOGGER.info("Successfully sent {} to {}.",event,id(sm));
        }, e -> {
            LOGGER.error("Error sending {} to {}.",event,id(sm));
            LoggingUtils.trace(LOGGER,e);
        });
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param sm
     * @return 
     */
    public static <S,E> String id(StateMachine<S,E> sm) {
        return (sm!=null) ? sm.getId() : null;
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param state
     * @return 
     */
    public static <S,E> S id(State<S,E> state) {
        return (state!=null) ? state.getId() : null;
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param trigger
     * @return 
     */
    public static <S,E> E id(Trigger<S,E> trigger) {
        return (trigger!=null) ? trigger.getEvent() : null;
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param sc
     * @param key
     * @param value 
     */
    public static <S,E> void set(StateContext<S,E> sc,Object key,Object value) {
        sc.getExtendedState().getVariables().put(key, value);
    }
    
    /**
     * TODO:
     * @param <S>
     * @param <E>
     * @param <T>
     * @param sc
     * @param key
     * @param type
     * @return 
     */
    public static <S,E,T> T get(StateContext<S,E> sc,Object key,Class<T> type) {
        return sc.getExtendedState().get(key, type);
    }
    
    /**
     * Static utilities library shouldn't ever be instantiated.
     */
    private StateMachineUtils() {}
    
}
