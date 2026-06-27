/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Capability-consistency contract for the {@code deductSupported()} / {@code deduct} pair.
 *
 * <p>{@code deductSupported()} is a correctness boundary: it tells {@code BucketedSlidingWindow}
 * whether it may subtract an expiring bucket (O(Δ) deduct path) or must recompute from the live
 * buckets. It has <b>no default</b> — every aggregate must declare its capability explicitly — so an
 * aggregate is invertible ONLY if it returns {@code true} AND backs that with a real {@code deduct}.
 *
 * <p>This test enumerates <b>every</b> built-in {@link Aggregates} factory by reflection and asserts:
 * <ol>
 *   <li>each factory's {@code deductSupported()} matches the expected invertibility (a missing factory
 *       fails the test, forcing this contract to be updated when a new aggregate is added);</li>
 *   <li>any factory that claims {@code deductSupported()==true} actually implements {@code deduct}
 *       (a fresh+fresh {@code combine}/{@code deduct} round trip must not throw the default
 *       "Sliding not supported" contract error).</li>
 * </ol>
 * Plus the set/list collection aggregates (not exposed via {@code Aggregates}) are asserted
 * non-invertible.
 */
public class AggregateDeductCapabilityContractTest {

    /** Expected invertibility per {@code Aggregates} factory method name. */
    private static Map<String, Boolean> expectedInvertible() {
        Map<String, Boolean> m = new HashMap<>();
        // invertible (group aggregates): a true inverse exists
        m.put("countFactory", true);
        m.put("intSumFactory", true);
        m.put("longSumFactory", true);
        m.put("doubleSumFactory", true);
        m.put("intAverageFactory", true);
        m.put("longAverageFactory", true);
        m.put("doubleAverageFactory", true);
        m.put("numberAverageFactory", true);
        // non-invertible: identity (no meaningful inverse) and semilattice min/max
        m.put("identityFactory", false);
        m.put("intIdentityFactory", false);
        m.put("longIdentityFactory", false);
        m.put("doubleIdentityFactory", false);
        m.put("intMaxFactory", false);
        m.put("longMaxFactory", false);
        m.put("doubleMaxFactory", false);
        m.put("intMinFactory", false);
        m.put("longMinFactory", false);
        m.put("doubleMinFactory", false);
        return m;
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void everyAggregatesFactoryHasConsistentDeductCapability() throws Exception {
        Map<String, Boolean> expected = expectedInvertible();
        TreeSet<String> seen = new TreeSet<>();

        for (Method method : Aggregates.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0
                    || !SerializableSupplier.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            String name = method.getName();
            seen.add(name);

            SerializableSupplier<?> supplier = (SerializableSupplier<?>) method.invoke(null);
            AggregateFlowFunction instance = (AggregateFlowFunction) supplier.get();

            if (!expected.containsKey(name)) {
                fail("New Aggregates factory '" + name + "' is not covered by the deduct-capability "
                        + "contract — add its expected invertibility to this test.");
            }
            assertEquals("deductSupported() for Aggregates." + name + "()",
                    expected.get(name), instance.deductSupported());

            // An opt-in must be backed by a real deduct: fresh+fresh combine/deduct must not throw the
            // default "Sliding not supported" contract error.
            if (instance.deductSupported()) {
                AggregateFlowFunction a = (AggregateFlowFunction) supplier.get();
                AggregateFlowFunction b = (AggregateFlowFunction) supplier.get();
                try {
                    a.combine(b);
                    a.deduct(b);
                } catch (RuntimeException e) {
                    fail("Aggregates." + name + "() claims deductSupported()==true but combine/deduct "
                            + "is not implemented: " + e);
                }
            }
        }

        // Guard against the enumeration silently matching nothing.
        assertEquals("expected every documented factory to be exercised",
                new TreeSet<>(expected.keySet()), seen);
    }

    @Test
    public void collectionAggregatesAreNonInvertible() {
        assertFalse("toSet has no multiplicity — removeAll is not the inverse of addAll",
                new AggregateToSetFlowFunction<>().deductSupported());
        assertFalse("toList removeAll ignores multiplicity and maxElementCount ordering",
                new AggregateToListFlowFunction<>().deductSupported());
    }
}
