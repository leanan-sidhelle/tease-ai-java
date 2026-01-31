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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlan;
import com.github.leanan.sidhelle.spring.util.StateMachineTestPlanBuilder;
import static com.github.leanan.sidhelle.tease.edger.Events.*;
import static com.github.leanan.sidhelle.tease.edger.States.*;
import com.github.leanan.sidhelle.tease.edger.action.*;
import com.github.leanan.sidhelle.tease.edger.guard.*;
import com.github.leanan.sidhelle.util.AdvancedSpringJUnitConfig;
import com.github.leanan.sidhelle.util.AdvancedTestPropertySource;
import static com.github.leanan.sidhelle.util.TimeUtils.minutesToDuration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
@AdvancedSpringJUnitConfig(classes={
    //TODO:how to reduce this boiler plate and still not require @SpringBootApplication???
    EdgerStateMachineFactory.class,
    OnConfigEdgeCooldown.class,
    OnConfigEnding.class,
    OnConfigRuinCooldown.class,
    OnConfigTease.class,
    OnConfigWarmup.class,
    OnDeny.class,
    OnEndStimulation.class,
    OnInduceEdge.class,
    OnInduceOrgasm.class,
    OnInduceRuin.class,
    OnInitialize.class,
    OnStimulate.class,
    IsEndSession.class,
    TestingListener.class, //TODO:replace with test listener...
})
//TODO:WHY THE FUCK IS SpringExtension NOT RESPECTING THIS!!!
@TestPropertySource(properties = {
    "logging.level.root=INFO",
    "logging.level.com.github.leanan.sidhelle=DEBUG",
     //TODO:why aren't these being overriden at the method level???
    "tease.time.max=0.1", //TODO:why isn't this being overriden at the method level???
    "tease.time.min=0.08",
    "tease.ending.orgasm=0.333",
    "tease.ending.ruin=0.333",
    "edge.cooldown=0.01",
    "edge.min=0",
    "ruin.cooldown=0.02",
    "ruin.min=0",
    "ruin.max=0",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EdgerStateMachineFactoryTest {
    
    /**
     * The {@link Logger} for message logging.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(EdgerStateMachineFactoryTest.class);

    /**
     * REMOVE AFTER UNFUCKING SPRING!!!!
     */
    @BeforeAll
    public static void setupLogger() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger packageLogger = context.getLogger("com.github.leanan.sidhelle");
        packageLogger.setLevel(Level.DEBUG);
        
        //TODO:should fix the goddamn reactor.core fucking error instead...
        packageLogger = context.getLogger("reactor.core");
        packageLogger.setLevel(Level.OFF);
    }
        
    /**
     * Test terminal path #1 ending at DENIED_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param maxTime
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.time.max=0.1", //TODO: remove after @TestPropertySource is fucking working
    })
    public void testTerminalPath1(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${tease.time.max}") float maxTime) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> DENIED_ENDING -> END -> [*] transitions.");
        Duration totalDuration = minutesToDuration(maxTime);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath1FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat((int)totalDuration.dividedBy(Duration.ofMillis(100)))
//        .forNext(totalDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(DENIED_ENDING)
            //TODO:test that end stimulation action called?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    
    /**
     * Test terminal path #2 ending at DENIED_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param maxTime
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.time.max=0.1", //TODO: remove after @TestPropertySource is fucking working
    })
    @Disabled
    public void testTerminalPath2(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${tease.time.max}") float maxTime) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> DENIED_ENDING -> END -> [*] transitions.");
        Duration totalDuration = minutesToDuration(maxTime);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath2FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .forNext(totalDuration.minusMillis(300)) //TODO:do we need to backup 100ms to test the transition?
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(DENIED_ENDING)
            //TODO:test that end stimulation action called?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test terminal path #3 ending at DENIED_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param maxTime
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.time.max=0.1", //TODO: remove after @TestPropertySource is fucking working
        "edge.cooldown=0.2",
    })
    @Disabled
    public void testTerminalPath3(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${tease.time.max}") float maxTime) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> DENIED_ENDING -> END -> [*] transitions.");
        Duration totalDuration = minutesToDuration(maxTime);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath3FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(totalDuration.minusMillis(300)) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(DENIED_ENDING)
            //TODO:test that end stimulation action called?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test terminal path #4 ending at DENIED_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param maxTime
     * @throws java.lang.InterruptedException
     * @throws java.lang.Exception
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.time.max=0.1", //TODO: remove after @TestPropertySource is fucking working
        "ruin.cooldown=0.2",
    })
    @Disabled
    public void testTerminalPath4(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${tease.time.max}") float maxTime) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> DENIED_ENDING -> END -> [*] transitions.");
        Duration totalDuration = minutesToDuration(maxTime);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath4FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an ruin rather than just a ruin event...
            .sendEvent(RUIN)
            //TODO:test the onConfigRuinCooldown action called?
            .and()
        .forNext(totalDuration.minusMillis(300)) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(DENIED_ENDING)
            //TODO:test that end stimulation action called?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #1 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> ORGASM_ENDING1 -> ORGASM_ENDING2 -> ENDING_POT -> END -> [*]
     * @param dutFactory
     * @param edgeCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=1.0",
        "tease.ending.ruin=0.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "edge.cooldown=0.01",
        "edge.min=0",
    })
    @Disabled
    public void testStandardPath1(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${edge.cooldown}") float edgeCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> ORGASM_ENDING1 -> ORGASM_ENDING2 -> ENDING_POT -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(edgeCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath3FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(ORGASM_ENDING1)
            .and()
        .step()
            .sendEvent(ORGASM)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(ORGASM_ENDING2)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(ENDING_POT)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #2 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> RUIN_ENDING -> END -> [*]
     * @param dutFactory
     * @param edgeCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=0.0",
        "tease.ending.ruin=1.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "edge.cooldown=0.01",
        "edge.min=0",
    })
    @Disabled
    public void testStandardPath2(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${edge.cooldown}") float edgeCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> RUIN_ENDING -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(edgeCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath3FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(RUIN_ENDING)
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #3 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param edgeCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=0.0",
        "tease.ending.ruin=0.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "edge.cooldown=0.01",
        "edge.min=0",
    })
    @Disabled
    public void testStandardPath3(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${edge.cooldown}") float edgeCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_EDGE -> COOLDOWN -> CONFIG_ENDING -> DENIED_ENDING -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(edgeCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testTerminalPath3FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(DENIED_ENDING)
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #4 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> ORGASM_ENDING1 -> ORGASM_ENDING2 -> ENDING_POT -> END -> [*]
     * @param dutFactory
     * @param ruinCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=1.0",
        "tease.ending.ruin=0.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "ruin.cooldown=0.01",
        "ruin.min=0",
    })
    @Disabled
    public void testStandardPath4(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${ruin.cooldown}") float ruinCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> ORGASM_ENDING1 -> ORGASM_ENDING2 -> ENDING_POT -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(ruinCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testStandardlPath4FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(ORGASM_ENDING1)
            .and()
        .step()
            .sendEvent(ORGASM)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(ORGASM_ENDING2)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(ENDING_POT)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #5 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> RUIN_ENDING -> END -> [*]
     * @param dutFactory
     * @param ruinCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=0.0",
        "tease.ending.ruin=1.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "ruin.cooldown=0.01",
        "ruin.min=0",
    })
    @Disabled
    public void testStandardPath5(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${ruin.cooldown}") float ruinCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> RUIN_ENDING -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(ruinCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testStandardlPath4FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(RUIN_ENDING)
            .and()
        .step()
            .sendEvent(RUIN)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }
    
    /**
     * Test standard ending path #6 via CONFIG_ENDING.
     * [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> DENIED_ENDING -> END -> [*]
     * @param dutFactory
     * @param ruinCooldown
     * @throws java.lang.InterruptedException
     */
    @Test
    @AdvancedTestPropertySource(properties = {
        "tease.ending.orgasm=0.0",
        "tease.ending.ruin=0.0",
         //TODO: remove after @TestPropertySource is fucking working
        "tease.time.max=0.1",
        "ruin.cooldown=0.01",
        "ruin.min=0",
    })
    @Disabled
    public void testStandardPath6(
    @Autowired StateMachineFactory<States,Events> dutFactory,
    @Value("${ruin.cooldown}") float ruinCooldown) throws InterruptedException, Exception {
        LOGGER.info("Testing [*] -> INITIALIZE -> WARMUP -> TEASE -> INDUCE_RUIN -> COOLDOWN -> CONFIG_ENDING -> DENIED_ENDING -> END -> [*] transitions.");
        Duration cooldownDuration = minutesToDuration(ruinCooldown);
        StateMachine<States, Events> dut = dutFactory.getStateMachine("testStandardlPath4FiniteStateMachine");
        dut.start(); //TODO:update to use startReactively?
        StateMachineTestPlan<States,Events> plan = StateMachineTestPlanBuilder
        .<States,Events>builder()
        .stateMachine(dutFactory.getStateMachine())
        .defaultAwaitTime(0.1f)
        .step()
            .expectState(INITIALIZE)
            //TODO:test that onInitialize action called?
            .and()
        .repeat(2)
            .expectState(WARMUP)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .sendEvent(HARD)
            //TODO:test the onConfigTease action called???
            .and()
        .repeat(2)
            .expectState(TEASE)
            //TODO:test that stimulate action called???
            .and()
        .step() //TODO:update this to actually be inducing an edge rather than just an edge event...
            .sendEvent(EDGE)
            //TODO:test the onConfigEdgeCooldown action called?
            .and()
        .forNext(cooldownDuration) //TODO:do we need to backup 100ms to test the transition?
            .expectState(COOLDOWN)
            //TODO:test that stimulate action called???
            .and()
        .step()
            .expectState(CONFIG_ENDING)
            //TODO:test that onInduceOrgasm action called?
            .and()
        .repeat(2)
            .expectState(RUIN_ENDING)
            .and()
        .step()
            .sendEvent(RUIN)
            //TODO:is there an action to test?
            .and()
        .step()
            .expectState(END)
            .and()
        .build();
        plan.test();
        dut.stopReactively(); //TODO:update to use stopReactively?
    }        
}
