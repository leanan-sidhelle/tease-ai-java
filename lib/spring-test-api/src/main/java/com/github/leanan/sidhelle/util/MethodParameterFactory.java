/*
 * BECAUSE SPRING FUCKING SUCKS AND MADE THIS SHIT PACKAGE PRIVATE!!!!
 */
package com.github.leanan.sidhelle.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.Assert;

/**
 * Factory for creating {@link MethodParameter} instances from Java 8
 * {@link Parameter Parameters}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see ParameterAutowireUtils
 * @see MethodParameter
 * @see SynthesizingMethodParameter
 * @see #createMethodParameter(Parameter)
 * @see #createSynthesizingMethodParameter(Parameter)
 */
public abstract class MethodParameterFactory {

    /**
     * Static utilities class that shouldn't be instantiated.
     */
    private MethodParameterFactory() {}

    /**
     * Create a standard {@link MethodParameter} from the supplied {@link Parameter}.
     * <p>Supports parameters declared in methods and constructors.
     * @param parameter the parameter to create a {@code MethodParameter} for;
     * never {@code null}
     * @return a new {@code MethodParameter}
     * @see #createSynthesizingMethodParameter(Parameter)
     */
    public static MethodParameter createMethodParameter(Parameter parameter) {
            Assert.notNull(parameter, "Parameter must not be null");
            Executable executable = parameter.getDeclaringExecutable();
            if (executable instanceof Method) {
                    return new MethodParameter((Method) executable, getIndex(parameter));
            }
            // else
            return new MethodParameter((Constructor<?>) executable, getIndex(parameter));
    }

    /**
     * Create a {@link SynthesizingMethodParameter} from the supplied {@link Parameter}.
     * <p>Supports parameters declared in methods.
     * @param parameter the parameter to create a {@code SynthesizingMethodParameter}
     * for; never {@code null}
     * @return a new {@code SynthesizingMethodParameter}
     * @throws UnsupportedOperationException if the supplied parameter is declared
     * in a constructor
     * @see #createMethodParameter(Parameter)
     */
    public static SynthesizingMethodParameter createSynthesizingMethodParameter(Parameter parameter) {
            Assert.notNull(parameter, "Parameter must not be null");
            Executable executable = parameter.getDeclaringExecutable();
            if (executable instanceof Method) {
                    return new SynthesizingMethodParameter((Method) executable, getIndex(parameter));
            }
            // else
            throw new UnsupportedOperationException(
                    "Cannot create a SynthesizingMethodParameter for a constructor parameter: " + parameter);
    }

    private static int getIndex(Parameter parameter) {
            Assert.notNull(parameter, "Parameter must not be null");
            Executable executable = parameter.getDeclaringExecutable();
            Parameter[] parameters = executable.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] == parameter) {
                            return i;
                    }
            }
            throw new IllegalStateException(String.format("Failed to resolve index of parameter [%s] in executable [%s]",
                    parameter, executable.toGenericString()));
    }

}