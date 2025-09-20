/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction.lookup;

import com.telamin.fluxtion.runtime.node.InstanceSupplier;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.function.ToLongFunction;

/**
 * Lookup a long value on a function supplied at runtime using a String key. Compares the look up long against a value
 * supplied in the {@link #isEqual(long)} method
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
 * eventProcessor.injectNamedInstance((ToLongFunction<String>)new MarketReferenceData()::toId, ToLongFunction.class, "marketRefData");
 * eventProcessor.init();
 * </pre>
 */
public class LongLookupPredicate {

    private final String lookupString;
    private final InstanceSupplier<ToLongFunction<String>> longLookupFunction;

    /**
     * Build a LongLookupPredicate, supplying the functionId to use at runtime
     * <p>
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString         the String to apply at runtime to lookup the long value
     * @param longLookupFunctionId The lookup function provided at runtime
     * @return
     */
    public static SerializableFunction<Long, Boolean> buildPredicate(String lookupString, String longLookupFunctionId) {
        return new LongLookupPredicate(lookupString, longLookupFunctionId)::isEqual;
    }

    /**
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString       the String to apply at runtime to lookup the long value
     * @param longLookupFunction The lookup function provided at runtime ready for injection
     */
    public LongLookupPredicate(String lookupString, InstanceSupplier<ToLongFunction<String>> longLookupFunction) {
        this.lookupString = lookupString;
        this.longLookupFunction = longLookupFunction;
    }

    /**
     * See {@link InstanceSupplier} for injecting runtime instance of the lookup function
     *
     * @param lookupString         the String to apply at runtime to lookup the long value
     * @param longLookupFunctionId The name of the lookup function provided at runtime ready for injection
     */
    public LongLookupPredicate(String lookupString, String longLookupFunctionId) {
        this(lookupString, InstanceSupplier.build(ToLongFunction.class, longLookupFunctionId));
    }

    public boolean isEqual(long longToCompare) {
        return longToCompare == longLookupFunction.get().applyAsLong(lookupString);
    }

}
