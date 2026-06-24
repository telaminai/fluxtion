/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;


/**
 * A target for an invoker holding a call tree for a filtered event.
 *
 * @author Greg Higgins
 */
public class InvokerFilterTarget implements java.io.Serializable {
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
