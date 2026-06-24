/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.lookup;

import com.telamin.fluxtion.runtime.node.InstanceSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.function.ToIntFunction;

/**
 * Lookup a int value on a function supplied at runtime using a String key. Compares the look up int against a value
 * supplied in the {@link #isEqual(int)} method
 *
 * <pre>
 *
 * var eventProcessor = Fluxtion.interpret(c -> {
 *     EventFlow.subscribe(MarketUpdate.class)
 *             .filterByProperty(
 *                     MarketUpdate::id,
 *                     LongLookupPredicate.buildPredicate("EURUSD", "marketRefData"))
 *             .console("Filtered :{}");
 * });
 *
 * eventProcessor.injectNamedInstance((ToIntFunction<String>)new MarketReferenceData()::toId, ToIntFunction.class, "marketRefData");
 * eventProcessor.init();
 * </pre>
 */
public class IntLookupPredicate {

    private final String lookupString;
    private final InstanceSupplier<ToIntFunction<String>> intLookupFunction;

    /**
     * Build a LongLookupPredicate, supplying the functionId to use at runtime
     * <p>
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString        the String to apply at runtime to lookup the int value
     * @param intLookupFunctionId The lookup function provided at runtime
     * @return
     */
    public static SerializableFunction<Integer, Boolean> buildPredicate(String lookupString, String intLookupFunctionId) {
        return new IntLookupPredicate(lookupString, intLookupFunctionId)::isEqual;
    }

    /**
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString      the String to apply at runtime to lookup the int value
     * @param intLookupFunction The lookup function provided at runtime ready for injection
     */
    public IntLookupPredicate(String lookupString, InstanceSupplier<ToIntFunction<String>> intLookupFunction) {
        this.lookupString = lookupString;
        this.intLookupFunction = intLookupFunction;
    }

    /**
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString        the String to apply at runtime to lookup the int value
     * @param intLookupFunctionId The name of the lookup function provided at runtime ready for injection
     */
    public IntLookupPredicate(String lookupString, String intLookupFunctionId) {
        this(lookupString, InstanceSupplier.build(ToIntFunction.class, intLookupFunctionId));
    }

    public boolean isEqual(int intToCompare) {
        return intToCompare == intLookupFunction.get().applyAsInt(lookupString);
    }

}
