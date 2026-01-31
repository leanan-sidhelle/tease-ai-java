/**
 * TODO:because the spring devs suck...
 */
package com.github.leanan.sidhelle.spring.util;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public interface StateMachineTestPlan<S,E> {

    /**
     * Run test plan.
     *
     * @throws Exception the exception
     */
    public void test() throws Exception;
}
