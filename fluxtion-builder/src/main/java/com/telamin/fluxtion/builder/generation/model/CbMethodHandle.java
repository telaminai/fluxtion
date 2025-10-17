/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import com.telamin.fluxtion.runtime.annotations.AfterTrigger;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.flowfunction.ParallelFunction;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * @author Greg Higgins
 */
public class CbMethodHandle implements SourceCbMethodHandle {

    public enum CallBackType {TRIGGER, EVENT_HANDLER, EXPORT_FUNCTION;}


    /**
     * The callback method.
     */
    @Getter
    private final Method method;
    /**
     * the instance the method will operate on.
     */
    @Getter
    private final Object instance;
    /**
     * the variable name of the instance in the SEP.
     */
    @Getter
    private final String variableName;

    /**
     * the parameter type of the callback - can be null
     */
    @Getter
    private final Class<?> parameterClass;

    /**
     * indicates is an {@link OnEventHandler} method
     */
    @Getter
    private final boolean eventHandler;
    /**
     * Is a multi arg event handler
     */
    @Getter
    private final String methodTarget;
    @Getter
    private final String methodName;
    @Getter
    private final int parameterCount;
    @Getter
    private final Class<?> returnType;
    @Getter
    private final boolean exportedHandler;
    @Getter
    private final String methodString;
    @Getter
    private final boolean postEventHandler;

    @Getter
    private final boolean invertedDirtyHandler;

    @Getter
    private final boolean guardedParent;

    @Getter
    private final boolean noPropagateEventHandler;
    private final boolean failBuildOnUnguardedTrigger;
    @Getter
    private final boolean forkExecution;

    public CbMethodHandle(Method method, Object instance, String variableName) {
        this(method, instance, variableName, null, false, false);
    }

    public CbMethodHandle(Method method, Object instance, String variableName, Class<?> parameterClass, boolean eventHandler, boolean exportedHandler) {
        this.method = method;
        this.instance = instance;
        this.variableName = variableName;
        this.parameterClass = parameterClass;
        this.eventHandler = eventHandler;
        this.postEventHandler = method.getAnnotation(AfterTrigger.class) != null;
        OnTrigger onTriggerAnnotation = method.getAnnotation(OnTrigger.class);
        OnParentUpdate onParentUpdateAnnotation = method.getAnnotation(OnParentUpdate.class);
        OnEventHandler onEventHandlerAnnotation = method.getAnnotation(OnEventHandler.class);
        this.exportedHandler = exportedHandler;
        this.invertedDirtyHandler = onTriggerAnnotation != null && !onTriggerAnnotation.dirty();
        boolean parallel = (instance instanceof ParallelFunction) ? ((ParallelFunction) instance).parallelCandidate() : false;
        this.forkExecution = parallel || onTriggerAnnotation != null && onTriggerAnnotation.parallelExecution();
        this.failBuildOnUnguardedTrigger = onTriggerAnnotation != null && onTriggerAnnotation.failBuildIfMissingBooleanReturn();
        this.guardedParent = onParentUpdateAnnotation != null && onParentUpdateAnnotation.guarded();
        this.noPropagateEventHandler = onEventHandlerAnnotation != null && !onEventHandlerAnnotation.propagate();
        this.methodTarget = Modifier.isStatic(getMethod().getModifiers()) ? instance.getClass().getSimpleName() : variableName;
        this.methodName = method.getName();
        this.parameterCount = method.getParameterCount();
        this.returnType = method.getReturnType();
        this.methodString = method.toString();
    }

    @Override
    public String invokeLambdaString() {
        return getMethodTarget() + "::" + getMethodName();
    }

    @Override
    public String forkVariableName() {
        return "fork_" + getVariableName();
    }

    @Override
    public String toString() {
        return "CbMethodHandle{" +
                "method=" + method +
                ", instance=" + instance +
                ", variableName='" + variableName + '\'' +
                ", parameterClass=" + parameterClass +
                ", isEventHandler=" + eventHandler +
                ", isExportHandler=" + exportedHandler +
                ", isPostEventHandler=" + postEventHandler +
                ", isInvertedDirtyHandler=" + invertedDirtyHandler +
                ", isGuardedParent=" + guardedParent +
                ", isNoPropagateEventHandler=" + noPropagateEventHandler +
                ", failBuildOnUnguardedTrigger=" + failBuildOnUnguardedTrigger +
                ", forkExecution=" + forkExecution +
                '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.method);
        hash = 23 * hash + Objects.hashCode(this.instance);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CbMethodHandle other = (CbMethodHandle) obj;
        if (!Objects.equals(this.methodString, other.methodString)) {
            return false;
        }
        return Objects.equals(this.variableName, other.variableName);
    }

    public boolean failBuildOnUnguardedTrigger() {
        return failBuildOnUnguardedTrigger;
    }
}
