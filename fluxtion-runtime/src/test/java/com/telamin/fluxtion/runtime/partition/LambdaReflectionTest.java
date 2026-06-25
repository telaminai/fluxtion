package com.telamin.fluxtion.runtime.partition;

import com.telamin.fluxtion.runtime.partition.LambdaReflection.MethodReferenceReflection;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Behavioural contract for {@link LambdaReflection.MethodReferenceReflection} — the
 * load-bearing introspection used by the builder to render method references as source
 * and by interpreted aggregation to resolve the implementation class.
 */
public class LambdaReflectionTest {

    static int doubleIt(int v) {
        return v * 2;
    }

    @Test
    public void unboundInstanceMethodReferenceResolvesContainingClass() {
        SerializableFunction<String, Integer> length = String::length;

        assertSame(String.class, length.getContainingClass());
        assertEquals("length", length.method().getName());
        assertEquals(0, length.captured().length);
        assertFalse(length.isDefaultConstructor());
        assertEquals(2, (int) length.apply("ab"));
    }

    @Test
    public void staticMethodReferenceResolvesDeclaringClass() {
        SerializableFunction<Integer, Integer> dbl = LambdaReflectionTest::doubleIt;

        assertSame(LambdaReflectionTest.class, dbl.getContainingClass());
        assertEquals("doubleIt", dbl.method().getName());
        assertEquals(6, (int) dbl.apply(3));
    }

    @Test
    public void constructorReferenceIsFlaggedAndResolves() {
        SerializableSupplier<ArrayList> newList = ArrayList::new;

        assertTrue(newList.isDefaultConstructor());
        assertSame(ArrayList.class, newList.getContainingClass());
        assertTrue(newList.get().isEmpty());
    }

    @Test
    public void boundInstanceReferenceCapturesReceiver() {
        String receiver = "hello";
        SerializableSupplier<Integer> length = receiver::length;

        assertSame(String.class, length.getContainingClass());
        assertEquals(1, length.captured().length);
        assertEquals("hello", length.captured()[0]);
        assertEquals(5, (int) length.get());
    }

    @Test
    public void inlineLambdaResolvesCapturingClass() {
        SerializableFunction<Integer, Integer> inc = v -> v + 1;

        // The implementation method of an inline lambda lives on the capturing class.
        assertSame(LambdaReflectionTest.class, inc.getContainingClass());
        assertEquals(2, (int) inc.apply(1));
    }

    @Test
    public void getContainingClassWithExplicitLoaderHonoursThatLoader() {
        SerializableFunction<String, Integer> length = String::length;

        Class<?> resolved = ((MethodReferenceReflection) length)
                .getContainingClass(String.class.getClassLoader());
        assertSame(String.class, resolved);
    }
}
