/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.lang.reflect.Array;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves canonical Java type names stored in DTOs back to {@link Class} instances.
 */
public final class TypeNameResolver {

    private TypeNameResolver() {
    }

    public static Class<?> resolveType(String className) throws ClassNotFoundException {
        if (className == null || className.isEmpty()) {
            throw new ClassNotFoundException(String.valueOf(className));
        }

        int arrayDepth = 0;
        while (className.endsWith("[]")) {
            arrayDepth++;
            className = className.substring(0, className.length() - 2);
        }

        Class<?> componentType = resolveNonArrayType(className);
        if (arrayDepth == 0) {
            return componentType;
        }

        int[] dimensions = new int[arrayDepth];
        return Array.newInstance(componentType, dimensions).getClass();
    }

    private static Class<?> resolveNonArrayType(String className) throws ClassNotFoundException {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
        }

        ClassNotFoundException lastException = null;
        for (ClassLoader classLoader : candidateClassLoaders()) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                lastException = e;
            }

            String attempt = className;
            int lastDot = attempt.lastIndexOf('.');
            while (lastDot > 0) {
                attempt = attempt.substring(0, lastDot) + '$' + attempt.substring(lastDot + 1);
                try {
                    return Class.forName(attempt, false, classLoader);
                } catch (ClassNotFoundException e) {
                    lastException = e;
                }
                lastDot = attempt.lastIndexOf('.');
            }
        }

        throw lastException != null ? lastException : new ClassNotFoundException(className);
    }

    private static Set<ClassLoader> candidateClassLoaders() {
        Set<ClassLoader> classLoaders = new LinkedHashSet<>();
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        if (threadContextClassLoader != null) {
            classLoaders.add(threadContextClassLoader);
        }
        ClassLoader typeNameResolverClassLoader = TypeNameResolver.class.getClassLoader();
        if (typeNameResolverClassLoader != null) {
            classLoaders.add(typeNameResolverClassLoader);
        }
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader != null) {
            classLoaders.add(systemClassLoader);
        }
        return classLoaders;
    }
}