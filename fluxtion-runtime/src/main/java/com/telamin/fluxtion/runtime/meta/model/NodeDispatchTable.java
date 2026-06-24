/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.context.DataFlowContext;

import java.util.Map;

/**
 * An NodeDispatchTable has a dispatch table but no members to dispatch to, use the {@link #setField(String, Object)} to
 * assign a single members to the dispatch table. To set all the objects in the dispatch table use {@link #assignMembers(Map)}
 */
public interface NodeDispatchTable {

    /**
     * Assigns all members to the dispatch the table and initialises the event processor
     *
     * @param memberMap the object map to bind to dispatch
     */
    void assignMembers(Map<String, Object> memberMap);

    /**
     * Assigns all members to the dispatch the table and initialises the event processor
     *
     * @param memberMap  the object map to bind to dispatch
     * @param contextMap a context map that is fed into the {@link DataFlowContext}
     */
    void assignMembers(Map<String, Object> memberMap, Map<Object, Object> contextMap);

    /**
     * Set an individual field in the dispatch table. Does not initialise the event processor, that must be done with
     * a call to assignMembers
     *
     * @param fieldName the name of the instance in the dispatch table
     * @param field     the object to bind to the dispatch table
     * @param <T>       The type of the object to bind
     */
    <T> void setField(String fieldName, T field);
}
