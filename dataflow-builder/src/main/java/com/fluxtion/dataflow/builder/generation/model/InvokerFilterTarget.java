/*
 * SPDX-File Copyright: © 2019-2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */
package com.fluxtion.dataflow.builder.generation.model;

import com.fluxtion.dataflow.builder.filter.FilterDescription;

/**
 * A target for an invoker holding a call tree for a filtered event.
 *
 * @author Greg Higgins
 */
public class InvokerFilterTarget {
    public FilterDescription filterDescription;
    /**
     * The body of the method thst is the call tree for the filtered dispatch
     */
    public String methodBody;
    /**
     * Name of the method to invoke that holds the call tree for the filtered
     * processing.
     */
    public String methodName;
    /**
     *
     */
    public String eventClassName;
    /**
     * the name of the map holding the invokers for this Event class
     */
    public String intMapName;
    /**
     * the name of the map holding the invokers for this Event class
     */
    public String stringMapName;

    public String getMethodDispatch() {
        return eventClassName == null ? "" : methodName + "(typedEvent);\n";
    }

    public String toMethodString() {
        if (eventClassName == null) {
            return "";
        }
        return "\nprivate void " + methodName + "(" + eventClassName + " typedEvent){\n" +
                methodBody +
                "}\n";
    }
}
