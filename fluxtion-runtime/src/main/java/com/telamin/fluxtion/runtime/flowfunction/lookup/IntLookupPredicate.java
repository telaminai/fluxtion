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
