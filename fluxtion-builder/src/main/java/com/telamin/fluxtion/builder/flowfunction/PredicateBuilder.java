/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.flowfunction;


import com.telamin.fluxtion.runtime.flowfunction.helpers.Predicates;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Predicates.AllUpdatedPredicate;

public class PredicateBuilder {

    /**
     * Fires a notification if all objects have fired a trigger at least once.
     *
     * @param obj the nodes to monitor
     * @return A node that triggers when all inputs have triggered at least once
     */
    public static Object allChanged(Object... obj) {
        return new AllUpdatedPredicate(StreamHelper.getSourcesAsList(obj));
    }

    /**
     * Fires a notification if all objects have fired a trigger at least once. Monitor of inputs van be reset, after a
     * reset only the
     *
     * @param resetKey the trigger node that is monitored to reset the state
     * @param obj      the nodes to monitor
     * @return A node that triggers when all inputs have triggered at least once
     */
    public static Object allChangedWithReset(Object resetKey, Object... obj) {
        return new AllUpdatedPredicate(StreamHelper.getSourcesAsList(obj), StreamHelper.getSource(resetKey));
    }

    /**
     * Fires a notification if any objects have fired a trigger in this event cycle
     *
     * @param obj the nodes to monitor
     * @return A node that triggers when all inputs have triggered at least once
     */
    public static Object anyTriggered(Object... obj) {
        return new Predicates.AnyUpdatedPredicate(StreamHelper.getSourcesAsList(obj));
    }
}
