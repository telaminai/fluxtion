/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A serializable description of a {@link Method}. Replaces direct {@code Method} references
 * wherever the method must survive serialization (e.g. exported-function maps).
 * The live {@code Method} can be re-resolved locally via {@link #resolve()}.
 */
public final class MethodDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String declaringClassName;
    private final String methodName;
    private final String returnTypeName;
    /** Canonical parameter type names (no generics — for class resolution). */
    private final List<String> parameterTypeNames;
    /** Generic parameter type names as returned by {@link Type#getTypeName()} — used for source generation. */
    private final List<String> genericParameterTypeNames;
    private final boolean varArgs;
    /** Full generic string representation of the method — used for audit descriptions. */
    private final String genericString;

    private MethodDescriptor(String declaringClassName, String methodName, String returnTypeName,
                             List<String> parameterTypeNames, List<String> genericParameterTypeNames,
                             boolean varArgs, String genericString) {
        this.declaringClassName = declaringClassName;
        this.methodName = methodName;
        this.returnTypeName = returnTypeName;
        this.parameterTypeNames = parameterTypeNames;
        this.genericParameterTypeNames = genericParameterTypeNames;
        this.varArgs = varArgs;
        this.genericString = genericString;
    }

    /**
     * Build a {@code MethodDescriptor} from a live {@link Method}.
     */
    public static MethodDescriptor from(Method method) {
        List<String> paramNames = Arrays.stream(method.getParameterTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.toList());
        List<String> genericParamNames = Arrays.stream(method.getGenericParameterTypes())
                .map(Type::getTypeName)
                .collect(Collectors.toList());
        return new MethodDescriptor(
                method.getDeclaringClass().getCanonicalName(),
                method.getName(),
                method.getReturnType().getCanonicalName(),
                paramNames,
                genericParamNames,
                method.isVarArgs(),
                method.toGenericString()
        );
    }

    /**
     * Re-resolve to a live {@link Method} using the current classloader.
     */
    public Method resolve() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> declaring = TypeNameResolver.resolveType(declaringClassName);
        Class<?>[] params = new Class<?>[parameterTypeNames.size()];
        for (int i = 0; i < parameterTypeNames.size(); i++) {
            params[i] = TypeNameResolver.resolveType(parameterTypeNames.get(i));
        }
        return declaring.getDeclaredMethod(methodName, params);
    }

    public String getDeclaringClassName() { return declaringClassName; }
    public String getMethodName() { return methodName; }
    public String getReturnTypeName() { return returnTypeName; }
    public List<String> getParameterTypeNames() { return parameterTypeNames; }
    public List<String> getGenericParameterTypeNames() { return genericParameterTypeNames; }
    public boolean isVarArgs() { return varArgs; }
    public String getGenericString() { return genericString; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodDescriptor)) return false;
        MethodDescriptor that = (MethodDescriptor) o;
        return Objects.equals(declaringClassName, that.declaringClassName)
                && Objects.equals(methodName, that.methodName)
                && Objects.equals(parameterTypeNames, that.parameterTypeNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClassName, methodName, parameterTypeNames);
    }

    @Override
    public String toString() {
        return genericString != null ? genericString
                : declaringClassName + "." + methodName + "(" + String.join(", ", parameterTypeNames) + ")";
    }
}
