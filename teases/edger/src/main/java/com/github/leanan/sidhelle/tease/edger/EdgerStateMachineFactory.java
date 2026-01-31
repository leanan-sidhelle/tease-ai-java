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
package com.github.leanan.sidhelle.tease.edger;

import static com.github.leanan.sidhelle.tease.edger.Events.*;
import static com.github.leanan.sidhelle.tease.edger.States.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
@Configuration
@EnableStateMachineFactory
public class EdgerStateMachineFactory extends 
        StateMachineConfigurerAdapter<States,Events> {

    /**
     * TODO:
     */
    public static final String MAX_TIME_GUARD = 
    "#{ #extendedState['endSession'] <= T(java.lang.System).currentTimeMillis() }";
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onInitialize;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onConfigWarmup;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onConfigTease;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onEndStimulation;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onDeny;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onStimulate;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onInduceEdge;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onInduceRuin;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onConfigEdgeCooldown;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onConfigRuinCooldown;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onConfigEnding;
    
    /**
     * TODO:
     */
    @Autowired
    private Action<States,Events> onInduceOrgasm;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isEndSession;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isInduceEdge;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isInduceRuin;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isComplete;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isCycleCompleteAndHard;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isCycleCompleteAndSoft;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isOrgasmEnding;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isRuinEnding;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isDeniedEnding;
    
    /**
     * TODO:
     */
    @Autowired
    private Guard<States,Events> isCycleComplete;
    
    /**
     * TODO:
     */
    @Autowired(required=false)
    private StateMachineListener edgerListener;
        
    @Override
    public void configure(StateMachineConfigurationConfigurer<States,Events> config) throws Exception {
        if(edgerListener!=null) {
            config
                .withConfiguration()
                .listener(edgerListener);
        }
    }
    
    /**
     * TODO:
     * @param listenerClass
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException 
     */
    protected StateMachineListener newListener(String listenerClass) throws 
        ClassNotFoundException, NoSuchMethodException, InstantiationException, 
        InvocationTargetException, IllegalArgumentException, 
        IllegalAccessException {
        Class type = Class.forName(listenerClass);
        if(StateMachineListener.class.isAssignableFrom(type)) {
            Constructor constructor = type.getConstructor();
            return StateMachineListener.class.cast(constructor.newInstance());
        } else {
            String msg = String.format("%s is not a StateMachineListener.",
                    listenerClass);
            throw new ClassNotFoundException(msg);
        }
    }

    @Override
    public void configure(StateMachineStateConfigurer<States,Events> states) throws Exception {
        states
            .withStates()
            .initial(INITIALIZE,onInitialize)
            .end(END)
            .states(EnumSet.allOf(States.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States,Events> transitions) throws Exception {
        transitions
            //[*] --> INITIALIZE : / onInitialize() (Handled when INITIALIZE state is defined)
            // INITIALIZE --> WARMUP : after 100ms / onConfigWarmup()
            .withExternal()
                .source(INITIALIZE).target(WARMUP)
                .timerOnce(100)
                .action(onConfigWarmup)
            .and()
            // WARMUP --> DENIED_ENDING : every 100ms isEndSession() / onDeny()
            .withExternal()
                .source(WARMUP).target(DENIED_ENDING)
                .timer(100).guard(isEndSession)
                .action(onDeny)
            .and()
            // WARMUP --> TEASE : HARD / onConfigTease()
            .withExternal()
                .source(WARMUP).target(TEASE)
                .event(HARD)
                .action(onConfigTease)
            .and()
            // WARMUP --> WARMUP : every 100ms / onStimulate()
            .withInternal()
                .source(WARMUP)
                .timer(100)
                .action(onStimulate)
            .and()
            // TEASE --> DENIED_ENDING : every 100ms isEndSession() / onDeny()
            .withExternal()
                .source(TEASE).target(DENIED_ENDING)
                .timer(100).guard(isEndSession)
                .action(onDeny)
            .and()
            // TEASE --> INDUCE_EDGE : every 100ms isInduceEdge() / onInduceEdge()
            .withExternal()
                .source(TEASE).target(INDUCE_EDGE)
                .timer(100).guard(isInduceEdge)
                .action(onInduceEdge)
            .and()
            // TEASE --> INDUCE_RUIN : every 100ms isInduceRuin() / onInduceRuin()
            .withExternal()
                .source(TEASE).target(INDUCE_RUIN)
                .timer(100).guard(isInduceRuin)
                .action(onInduceRuin)
            .and()
            // TEASE --> TEASE : every 100ms / onStimulate()
            .withInternal()
                .source(TEASE)
                .timer(100)
                .action(onStimulate)
            .and()
            // INDUCE_EDGE --> COOLDOWN : after 100ms / onConfigEdgeCooldown()
            .withExternal()
                .source(INDUCE_EDGE).target(COOLDOWN)
                .timerOnce(100)
                .action(onConfigEdgeCooldown)
            .and()
            // INDUCE_RUIN --> COOLDOWN : after 100ms / onConfigRuinCooldown()
            .withExternal()
                .source(INDUCE_RUIN).target(COOLDOWN)
                .timerOnce(100)
                .action(onConfigRuinCooldown)
            .and()
            // COOLDOWN --> TEASE : every 100ms isCycleComplete() && isHard() / onConfigTease()
            .withExternal()
                .source(COOLDOWN).target(TEASE)
                .timer(100).guard(isCycleCompleteAndHard)
                .action(onConfigTease)
            .and()
            // COOLDOWN --> WARMUP : every 100ms isCycleComplete() && isSoft() / onConfigWarmup()
            .withExternal()
                .source(COOLDOWN).target(WARMUP)
                .timer(100).guard(isCycleCompleteAndSoft)
                .action(onConfigWarmup)
            .and()
            // COOLDOWN --> DENIED_ENDING : every 100ms isEndSession() / onDeny()
            .withExternal()
                .source(COOLDOWN).target(DENIED_ENDING)
                .timer(100).guard(isEndSession)
                .action(onDeny)
            .and()
            // COOLDOWN --> CONFIG_ENDING : every 100ms isComplete() / onConfigEnding()
            .withExternal()
                .source(COOLDOWN).target(CONFIG_ENDING)
                .timer(100).guard(isComplete)
                .action(onConfigEnding)
            .and()
            // CONFIG_ENDING --> ORGASM_ENDING1 : after 100ms isOrgasmEnding() / onInduceOrgasm()
            .withExternal()
                .source(CONFIG_ENDING).target(ORGASM_ENDING1)
                .timerOnce(100).guard(isOrgasmEnding)
                .action(onInduceOrgasm)
            .and()
            // CONFIG_ENDING --> RUIN_ENDING : after 100ms isRuinEnding() / onInduceRuin()
            .withExternal()
                .source(CONFIG_ENDING).target(RUIN_ENDING)
                .timerOnce(100).guard(isRuinEnding)
                .action(onInduceRuin)
            .and()
            // CONFIG_ENDING --> DENIED_ENDING : after 100ms isDeniedEnding() / onDeny()
            .withExternal()
                .source(CONFIG_ENDING).target(DENIED_ENDING)
                .timerOnce(100).guard(isDeniedEnding)
                .action(onDeny)
            .and()
            // ORGASM_ENDING1 --> ORGASM_ENDING2 : ORGASM
            .withExternal()
                .source(ORGASM_ENDING1).target(ORGASM_ENDING2)
                .event(ORGASM)
            .and()
            // ORGASM_ENDING2 --> ENDING_POT : after 5s 'TODO: how to make this an event based trigger?
            .withExternal()
                .source(ORGASM_ENDING2).target(ENDING_POT)
                .timer(5000)
            .and()
            // ENDING_POT --> END : every 100ms isCycleComplete() / onEndStimulation()
            .withExternal()
                .source(ENDING_POT).target(END)
                .timer(100).guard(isCycleComplete)
                .action(onEndStimulation)
            .and()
            // RUIN_ENDING --> END : RUIN / onEndStimulation()
            .withExternal()
                .source(RUIN_ENDING).target(END)
                .event(RUIN)
                .action(onEndStimulation)
            .and()
            // DENIED_ENDING --> END : / onEndStimulation()
            .withExternal()
                .source(DENIED_ENDING).target(END)
                .timerOnce(100)
                .action(onEndStimulation);
    }
}
