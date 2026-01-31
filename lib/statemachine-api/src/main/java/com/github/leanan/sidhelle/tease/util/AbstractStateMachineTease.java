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
import com.github.leanan.sidhelle.tease.Tease;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 * @param <S>
 * @param <E>
 */
public class AbstractStateMachineTease<S,E> implements Runnable,Tease {

    /**
     * The {@link Logger} for message logging.
     */
    static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractStateMachineTease.class);
    
    /**
     * The {@link StateMachine} for starting, stopping, and signaling events.
     */
    protected final StateMachine<S, E> sm;

    /**
     * The {@link Lock} for controlling access to mutually exclusive code.
     */
    private final Lock lock;
    
    /**
     * The {@link Thread} for running the tease.
     */
    private final Thread thread;
    
    /**
     * TODO:
     * @param sm 
     */
    public AbstractStateMachineTease(StateMachine<S,E> sm) {
        this.lock = new ReentrantLock();
        this.sm = sm;
        this.thread = new Thread(this);
    }

    /**
     * TODO:
     * @return 
     */
    public boolean isRunning() {
        try {
            lock.lock();
            return sm.getState()!=null && !sm.isComplete();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isComplete() {
        try {
            lock.lock();
            return sm.isComplete();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {
            initialize();
            StateMachineUtils.start(sm); //TODO:does this need to be inside a lock?
            while(!thread.isInterrupted() && !isComplete()) {
                //TODO:is there a better way?
                sleep();
            }
        } catch(InterruptedException e) {
            //ignore, expected during shutdown
        } catch(Exception e) {
            LOGGER.error("Error running.");
            LoggingUtils.trace(LOGGER,e);
        }
        cleanup();
    }

    @Override
    public void start() {
        try {
            lock.lock();
            thread.start();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        thread.interrupt();
    }
    
    /**
     * TODO:
     * @throws java.lang.InterruptedException
     */
    protected void sleep() throws InterruptedException {
        Thread.sleep(100);
    }
 
    /**
     * TODO:
     */
    protected void initialize() {
        //stub for children
    }
    
    /**
     * TODO:
     */
    protected void cleanup() {
        //stub for children
    }
}
