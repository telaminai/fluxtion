/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.partition;

import lombok.SneakyThrows;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.*;

/**
 * @author Greg Higgins
 */
public interface LambdaReflection {

    interface MethodReferenceReflection {

        //inspired by: http://benjiweber.co.uk/blog/2015/08/17/lambda-parameter-names-with-reflection/
        default SerializedLambda serialized() {
            try {
                Method replaceMethod = getClass().getDeclaredMethod("writeReplace");
                replaceMethod.setAccessible(true);
                return (SerializedLambda) replaceMethod.invoke(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        default Class<?> getContainingClass(ClassLoader loader) {
            try {
                String className = serialized().getImplClass().replaceAll("/", ".");
                return Class.forName(className, true, loader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        default Class<?> getContainingClass() {
            String className = serialized().getImplClass().replaceAll("/", ".");
            // Resolve via the lambda's own class loader first, so a method reference whose
            // implementation class lives in a non-default loader (for example compiled
            // inline .fsql records, or any plugin/child class loader) resolves correctly.
            // Fall back to the thread context loader, then the historic default (this
            // interface's defining loader) for full backward compatibility.
            try {
                return Class.forName(className, true, getClass().getClassLoader());
            } catch (ClassNotFoundException | LinkageError ownLoaderMiss) {
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
                if (contextLoader != null) {
                    try {
                        return Class.forName(className, true, contextLoader);
                    } catch (ClassNotFoundException | LinkageError ignored) {
                        // fall through to the historic default
                    }
                }
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        default Object[] captured() {
            final SerializedLambda serialized = serialized();
            Object[] args = new Object[serialized.getCapturedArgCount()];
            for (int i = 0; i < serialized.getCapturedArgCount(); i++) {
                args[i] = serialized.getCapturedArg(i);
            }
            return args;
        }

        default Method method(ClassLoader loader) {
            return resolveMethod(serialized(), getContainingClass(loader));
        }

        default boolean isDefaultConstructor() {
            return serialized().getImplMethodName().equalsIgnoreCase("<init>");
        }

        @SneakyThrows
        default Method method() {
            return resolveMethod(serialized(), getContainingClass());
        }

        /**
         * True when this instance is an actual serializable lambda / method reference
         * (it carries a synthetic {@code writeReplace}). A concrete class that merely
         * implements the interface is not introspectable via {@link #serialized()};
         * callers should guard with this rather than catching the resulting exception.
         */
        default boolean isMethodReference() {
            try {
                getClass().getDeclaredMethod("writeReplace");
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        /**
         * Resolves the implementation method, disambiguating overloads by the lambda's
         * implementation-method signature ({@link SerializedLambda#getImplMethodSignature()}).
         * Falls back to the first name match when no exact signature match exists (e.g.
         * bridge or synthetic methods), preserving the historic best-effort behaviour.
         */
        static Method resolveMethod(SerializedLambda lambda, Class<?> containingClass) {
            String name = lambda.getImplMethodName();
            String signature = lambda.getImplMethodSignature();
            Method nameMatch = null;
            for (Method candidate : containingClass.getDeclaredMethods()) {
                if (!candidate.getName().equals(name)) {
                    continue;
                }
                if (methodDescriptor(candidate).equals(signature)) {
                    return candidate;
                }
                if (nameMatch == null) {
                    nameMatch = candidate;
                }
            }
            if (nameMatch != null) {
                return nameMatch;
            }
            throw new UnableToGuessMethodException();
        }

        /** JVM method descriptor, e.g. {@code (I)I} for {@code int f(int)}. */
        static String methodDescriptor(Method method) {
            StringBuilder descriptor = new StringBuilder("(");
            for (Class<?> parameterType : method.getParameterTypes()) {
                descriptor.append(typeDescriptor(parameterType));
            }
            return descriptor.append(')').append(typeDescriptor(method.getReturnType())).toString();
        }

        /** JVM field/type descriptor for a single type. */
        static String typeDescriptor(Class<?> type) {
            if (type.isArray()) {
                return "[" + typeDescriptor(type.getComponentType());
            }
            if (!type.isPrimitive()) {
                return "L" + type.getName().replace('.', '/') + ";";
            }
            if (type == int.class) {
                return "I";
            }
            if (type == long.class) {
                return "J";
            }
            if (type == double.class) {
                return "D";
            }
            if (type == boolean.class) {
                return "Z";
            }
            if (type == float.class) {
                return "F";
            }
            if (type == char.class) {
                return "C";
            }
            if (type == byte.class) {
                return "B";
            }
            if (type == short.class) {
                return "S";
            }
            return "V"; // void
        }

        class UnableToGuessMethodException extends RuntimeException {
        }
    }

    interface SerializableRunnable extends Runnable, Serializable, MethodReferenceReflection {
    }

    interface SerializableSupplier<t> extends Supplier<t>, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntSupplier extends IntSupplier, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleSupplier extends DoubleSupplier, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongSupplier extends LongSupplier, Serializable, MethodReferenceReflection {
    }

    interface SerializableConsumer<t> extends Consumer<t>, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntConsumer extends IntConsumer, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleConsumer extends DoubleConsumer, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongConsumer extends LongConsumer, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiConsumer<t, u> extends BiConsumer<t, u>, Serializable, MethodReferenceReflection {
    }

    interface SerializableFunction<t, r> extends Function<t, r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntFunction<r> extends IntFunction<r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleFunction<r> extends DoubleFunction<r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongFunction<r> extends LongFunction<r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableToIntFunction<t> extends ToIntFunction<t>, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntUnaryOperator extends IntUnaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleUnaryOperator extends DoubleUnaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongUnaryOperator extends LongUnaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableToDoubleFunction<t> extends ToDoubleFunction<t>, Serializable, MethodReferenceReflection {
    }

    interface SerializableToLongFunction<t> extends ToLongFunction<t>, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleToIntFunction extends DoubleToIntFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongToIntFunction extends LongToIntFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntToDoubleFunction extends IntToDoubleFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableLongToDoubleFunction extends LongToDoubleFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableIntToLongFunction extends IntToLongFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableDoubleToLongFunction extends DoubleToLongFunction, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiFunction<f, t, r> extends BiFunction<f, t, r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiIntFunction extends IntBinaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiIntPredicate extends IntBinaryPredicate, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiDoublePredicate extends DoubleBinaryPredicate, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiLongPredicate extends LongBinaryPredicate, Serializable, MethodReferenceReflection {
    }


    interface SerializableBiDoubleFunction extends DoubleBinaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableBiLongFunction extends LongBinaryOperator, Serializable, MethodReferenceReflection {
    }

    interface SerializableTriFunction<f, t, u, r> extends TriFunction<f, t, u, r>, Serializable, MethodReferenceReflection {
    }

    interface SerializableQuadFunction<f, t, u, v, r> extends QuadFunction<f, t, u, v, r>, Serializable, MethodReferenceReflection {
    }

    @FunctionalInterface
    interface IntBinaryPredicate {
        boolean apply(int argument1, int argument2);
    }

    @FunctionalInterface
    interface DoubleBinaryPredicate {
        boolean apply(double argument1, double argument2);
    }

    @FunctionalInterface
    interface LongBinaryPredicate {
        boolean apply(long argument1, long argument2);
    }

    @FunctionalInterface
    interface TriFunction<F, T, U, R> {
        R apply(F f, T t, U u);
    }

    @FunctionalInterface
    interface QuadFunction<F, T, U, V, R> {
        R apply(F f, T t, U u, V v);
    }

    @FunctionalInterface
    interface SerializableTriConsumer<A, B, C> extends Serializable, MethodReferenceReflection {
        void accept(A a, B b, C c);
    }

    @FunctionalInterface
    interface SerializableQuadConsumer<A, B, C, D> extends Serializable, MethodReferenceReflection {
        void accept(A a, B b, C c, D d);
    }

    @FunctionalInterface
    interface SerializableQuinConsumer<A, B, C, D, E> extends Serializable, MethodReferenceReflection {
        void accept(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    interface SerializableSextConsumer<A, B, C, D, E, F> extends Serializable, MethodReferenceReflection {
        void accept(A a, B b, C c, D d, E e, F f);
    }

    @FunctionalInterface
    interface SerializableSeptConsumer<A, B, C, D, E, F, G> extends Serializable, MethodReferenceReflection {
        void accept(A a, B b, C c, D d, E e, F f, G g);
    }

    static <T> Method getMethod(SerializableConsumer<T> supplier) {
        return supplier.method();
    }

    static <T, R> Method getMethod(SerializableFunction<T, R> supplier) {
        return supplier.method();
    }

    static <T, I, R> Method getMethod(SerializableBiFunction<T, I, R> supplier) {
        return supplier.method();
    }

    @SneakyThrows
    @SuppressWarnings("all")
    public static <T, R> LambdaReflection.SerializableFunction<T, R> method2Function(Method keyMethod) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<?> clazz = keyMethod.getDeclaringClass();
        CallSite site = LambdaMetafactory.altMetafactory(lookup,
                "apply",
                MethodType.methodType(SerializableFunction.class),
                MethodType.methodType(Object.class, Object.class),
                lookup.findVirtual(clazz, keyMethod.getName(), MethodType.methodType(keyMethod.getReturnType())),
                MethodType.methodType(keyMethod.getReturnType(), clazz),
                LambdaMetafactory.FLAG_SERIALIZABLE);
        return (SerializableFunction<T, R>) site.getTarget().invokeExact();
    }
}
