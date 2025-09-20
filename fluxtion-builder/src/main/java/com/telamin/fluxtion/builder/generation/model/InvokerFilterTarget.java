/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import com.telamin.fluxtion.builder.filter.FilterDescription;

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
