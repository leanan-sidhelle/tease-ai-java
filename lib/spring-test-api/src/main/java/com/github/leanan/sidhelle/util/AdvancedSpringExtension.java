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
package com.github.leanan.sidhelle.util;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Properties;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

/**
 * TODO:
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class AdvancedSpringExtension extends org.springframework.test.context.junit.jupiter.SpringExtension {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Method testMethod = context.getRequiredTestMethod();
        getTestContextManager(context).beforeTestMethod(testInstance, testMethod);
        //TODO:if this shit actually goes here, then call super.beforeEach instead???
        AdvancedTestPropertySource propConfig = testMethod.getAnnotation(AdvancedTestPropertySource.class);
        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) getApplicationContext(context);
        ConfigurableEnvironment env = configurableContext.getEnvironment();
        if(propConfig!=null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if(propConfig.properties().length>0) {
                for(String property : propConfig.properties()) {
                    pw.println(property);
                }
            }
            Properties props = new Properties();
            StringReader sr = new StringReader(sw.toString());
            props.load(sr);
            String source = String.format("%sPropertySource",testMethod.getName());
            PropertiesPropertySource pps = new PropertiesPropertySource(source, props);
            env.getPropertySources().addFirst(pps); 
        }
    }
    
    @Override
    public boolean supportsParameter(ParameterContext paramContext,ExtensionContext extContext) {
        Parameter parameter = paramContext.getParameter();
        Executable executable = parameter.getDeclaringExecutable();
        return (AnnotatedElementUtils.hasAnnotation(executable, Autowired.class)) || 
                ParameterAutowireUtils.isAutowirable(parameter);
    }
    
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        //TODO:do we need to change any of this?  If not they we will not need to override...
        Parameter parameter = parameterContext.getParameter();
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ApplicationContext applicationContext = getApplicationContext(extensionContext);
        return ParameterAutowireUtils.resolveDependency(parameter, testClass, applicationContext);
    }
    
    /**
     * BECAUSE SPRING SUCKS AND MADE THIS SHIT PRIVATE!!!
     * @param context
     * @return 
     */
    protected static TestContextManager getTestContextManager(ExtensionContext context) {
            Assert.notNull(context, "ExtensionContext must not be null");
            Class<?> testClass = context.getRequiredTestClass();
            Store store = getStore(context);
            return store.getOrComputeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
    }

    /**
     * BECAUSE SPRING SUCKS AND MADE THIS SHIT PRIVATE!!!
     * @param context
     * @return 
     */
    protected static Store getStore(ExtensionContext context) {
            return context.getRoot().getStore(NAMESPACE);
    }
    
    /**
     * BECAUSE SPRING SUCKS AND MADE THIS SHIT PRIVATE!!!
     */
    private static final Namespace NAMESPACE = Namespace.create(SpringExtension.class);
}
