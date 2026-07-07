/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.partition;

import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilities used by source generation to classify Java functional values and,
 * where safe, recreate non-capturing inline lambdas in generated AOT source.
 *
 * <p>This class intentionally lives in {@code fluxtion-runtime}: generated
 * processors must not depend on builder or generator artefacts at runtime.</p>
 */
public final class LambdaAotSupport {

    private LambdaAotSupport() {
    }

    public enum LambdaKind {
        METHOD_REFERENCE,
        CONSTRUCTOR_REFERENCE,
        BOUND_INSTANCE_REFERENCE,
        NON_CAPTURING_INLINE_LAMBDA,
        CAPTURING_INLINE_LAMBDA,
        CONCRETE_FUNCTION_OBJECT,
        UNKNOWN
    }

    public static final class LambdaAnalysis {
        private final LambdaKind kind;
        private final SerializedLambda serializedLambda;
        private final Class<?> implementationClass;
        private final Method implementationMethod;
        private final RuntimeException failure;

        private LambdaAnalysis(
                LambdaKind kind,
                SerializedLambda serializedLambda,
                Class<?> implementationClass,
                Method implementationMethod,
                RuntimeException failure) {
            this.kind = kind;
            this.serializedLambda = serializedLambda;
            this.implementationClass = implementationClass;
            this.implementationMethod = implementationMethod;
            this.failure = failure;
        }

        public LambdaKind getKind() {
            return kind;
        }

        public SerializedLambda getSerializedLambda() {
            return serializedLambda;
        }

        public Class<?> getImplementationClass() {
            return implementationClass;
        }

        public Method getImplementationMethod() {
            return implementationMethod;
        }

        public RuntimeException getFailure() {
            return failure;
        }

        public int getCapturedArgCount() {
            return serializedLambda == null ? 0 : serializedLambda.getCapturedArgCount();
        }

        public String getImplementationMethodName() {
            return serializedLambda == null ? null : serializedLambda.getImplMethodName();
        }

        public String getImplementationMethodSignature() {
            return serializedLambda == null ? null : serializedLambda.getImplMethodSignature();
        }

        public boolean isInlineLambda() {
            return kind == LambdaKind.NON_CAPTURING_INLINE_LAMBDA
                    || kind == LambdaKind.CAPTURING_INLINE_LAMBDA;
        }
    }

    public static LambdaAnalysis analyse(Object function) {
        if (!(function instanceof LambdaReflection.MethodReferenceReflection)) {
            return new LambdaAnalysis(LambdaKind.CONCRETE_FUNCTION_OBJECT, null, null, null, null);
        }

        LambdaReflection.MethodReferenceReflection reflection = (LambdaReflection.MethodReferenceReflection) function;
        if (!reflection.isMethodReference()) {
            return new LambdaAnalysis(LambdaKind.CONCRETE_FUNCTION_OBJECT, null, null, null, null);
        }

        try {
            SerializedLambda serializedLambda = reflection.serialized();
            Class<?> implementationClass = reflection.getContainingClass();
            Method implementationMethod = null;
            try {
                implementationMethod = LambdaReflection.MethodReferenceReflection.resolveMethod(
                        serializedLambda,
                        implementationClass);
            } catch (RuntimeException ignored) {
                // Constructor references and unusual compiler forms may not have
                // a declared method to resolve. Classification can continue from
                // SerializedLambda metadata.
            }
            LambdaKind kind = classify(serializedLambda, implementationMethod);
            return new LambdaAnalysis(kind, serializedLambda, implementationClass, implementationMethod, null);
        } catch (RuntimeException e) {
            return new LambdaAnalysis(LambdaKind.UNKNOWN, null, null, null, e);
        }
    }

    public static <F extends Serializable> F recreateNonCapturingLambda(
            Class<F> functionalInterface,
            SerializedLambda serializedLambda) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = LambdaAotSupport.class.getClassLoader();
        }
        Class<?> implementationClass = loadClass(serializedLambda.getImplClass(), classLoader);
        return recreateNonCapturingLambda(
                functionalInterface,
                implementationClass,
                serializedLambda.getImplMethodKind(),
                serializedLambda.getImplMethodName(),
                serializedLambda.getImplMethodSignature(),
                serializedLambda.getFunctionalInterfaceMethodName(),
                serializedLambda.getFunctionalInterfaceMethodSignature(),
                serializedLambda.getInstantiatedMethodType(),
                serializedLambda.getCapturedArgCount());
    }

    public static <F extends Serializable> F recreateNonCapturingLambda(
            Class<F> functionalInterface,
            Class<?> implementationClass,
            int implementationMethodKind,
            String implementationMethodName,
            String implementationMethodDescriptor,
            String functionalInterfaceMethodName,
            String functionalInterfaceMethodDescriptor,
            String instantiatedMethodDescriptor) {
        return recreateNonCapturingLambda(
                functionalInterface,
                implementationClass,
                implementationMethodKind,
                implementationMethodName,
                implementationMethodDescriptor,
                functionalInterfaceMethodName,
                functionalInterfaceMethodDescriptor,
                instantiatedMethodDescriptor,
                0);
    }

    private static <F extends Serializable> F recreateNonCapturingLambda(
            Class<F> functionalInterface,
            Class<?> implementationClass,
            int implementationMethodKind,
            String implementationMethodName,
            String implementationMethodDescriptor,
            String functionalInterfaceMethodName,
            String functionalInterfaceMethodDescriptor,
            String instantiatedMethodDescriptor,
            int capturedArgCount) {
        if (capturedArgCount != 0) {
            throw new IllegalArgumentException(
                    "Cannot recreate capturing lambda as an AOT non-capturing lambda: capturedArgCount="
                            + capturedArgCount);
        }
        if (implementationMethodKind != MethodHandleInfo.REF_invokeStatic) {
            throw new IllegalArgumentException(
                    "Only static non-capturing lambda implementation methods are supported, implMethodKind="
                            + implementationMethodKind);
        }
        if (!functionalInterface.isInterface()) {
            throw new IllegalArgumentException("Functional target must be an interface: " + functionalInterface);
        }

        try {
            ClassLoader implementationLoader = implementationClass.getClassLoader();
            MethodType implementationMethodType = MethodType.fromMethodDescriptorString(
                    implementationMethodDescriptor,
                    implementationLoader);
            MethodType samMethodType = MethodType.fromMethodDescriptorString(
                    functionalInterfaceMethodDescriptor,
                    functionalInterface.getClassLoader());
            MethodType instantiatedMethodType = MethodType.fromMethodDescriptorString(
                    instantiatedMethodDescriptor,
                    implementationLoader);

            MethodHandles.Lookup lookup = privateLookupFor(implementationClass);
            MethodHandle implementationMethod = lookup.findStatic(
                    implementationClass,
                    implementationMethodName,
                    implementationMethodType);

            CallSite callSite = LambdaMetafactory.altMetafactory(
                    lookup,
                    functionalInterfaceMethodName,
                    MethodType.methodType(functionalInterface),
                    samMethodType,
                    implementationMethod,
                    instantiatedMethodType,
                    LambdaMetafactory.FLAG_SERIALIZABLE);

            return functionalInterface.cast(callSite.getTarget().invoke());
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to recreate non-capturing lambda "
                            + implementationClass.getName()
                            + "::"
                            + implementationMethodName
                            + implementationMethodDescriptor,
                    e);
        }
    }

    private static LambdaKind classify(SerializedLambda serializedLambda, Method implementationMethod) {
        if ("<init>".equals(serializedLambda.getImplMethodName())) {
            return LambdaKind.CONSTRUCTOR_REFERENCE;
        }

        boolean syntheticLambda = serializedLambda.getImplMethodName().startsWith("lambda$")
                || (implementationMethod != null && implementationMethod.isSynthetic());
        if (syntheticLambda) {
            return serializedLambda.getCapturedArgCount() == 0
                    ? LambdaKind.NON_CAPTURING_INLINE_LAMBDA
                    : LambdaKind.CAPTURING_INLINE_LAMBDA;
        }

        if (serializedLambda.getCapturedArgCount() > 0) {
            return LambdaKind.BOUND_INSTANCE_REFERENCE;
        }

        return LambdaKind.METHOD_REFERENCE;
    }

    private static MethodHandles.Lookup privateLookupFor(Class<?> implementationClass) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Method privateLookupIn = MethodHandles.class.getMethod(
                    "privateLookupIn",
                    Class.class,
                    MethodHandles.Lookup.class);
            return (MethodHandles.Lookup) privateLookupIn.invoke(null, implementationClass, lookup);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(
                    "AOT recreation of private synthetic lambda methods requires MethodHandles.privateLookupIn "
                            + "(Java 9+ runtime)",
                    e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            throw new IllegalStateException(
                    "Cannot obtain private method lookup for lambda implementation class: "
                            + implementationClass.getName(),
                    targetException);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot access MethodHandles.privateLookupIn for lambda implementation class: "
                            + implementationClass.getName(),
                    e);
        }
    }

    private static Class<?> loadClass(String internalName, ClassLoader preferredLoader) {
        String className = internalName.replace('/', '.');
        try {
            return Class.forName(className, true, preferredLoader);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Cannot load lambda implementation class: " + className, ex);
            }
        }
    }
}
