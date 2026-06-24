/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.annotations.AfterTrigger;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.flowfunction.ParallelFunction;
import com.telamin.fluxtion.runtime.meta.dto.AnnotatedMethodDto;
import com.telamin.fluxtion.runtime.meta.dto.AnnotationDescriptorDto;
import com.telamin.fluxtion.runtime.meta.dto.MethodDescriptor;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Greg Higgins
 */
public class CbMethodHandle implements SourceCbMethodHandle, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum CallBackType {TRIGGER, EVENT_HANDLER, EXPORT_FUNCTION;}


    /**
     * The callback method.
     */
    @Getter
    private transient Method method;
    /**
     * the instance the method will operate on.
     */
    @Getter
    private transient Object instance;
    /**
     * the variable name of the instance in the SEP.
     */
    @Getter
    private final String variableName;

    /**
     * the parameter type of the callback - can be null
     */
    @Getter
    private final String parameterClass;

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
    private final String returnType;
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

    /** Generic parameter type names (e.g. {@code java.util.List<String>}) — stored for serialization-safe source generation. */
    @Getter
    private final List<String> genericParameterTypeNames;

    /** Whether the method is a varargs method. */
    @Getter
    private final boolean varArgs;

    public CbMethodHandle(Method method, Object instance, String variableName) {
        this(method, instance, variableName, null, false, false);
    }

    /**
     * Private constructor used by {@link #fromAnnotatedMethod} — populates all fields from
     * pre-computed values without requiring live {@link Method} or object instance references.
     * The {@code method} and {@code instance} transient fields are left {@code null}; they are
     * resolved locally by {@code InMemoryEventProcessor} using a variable-name→instance map.
     */
    private CbMethodHandle(String variableName, String parameterClass,
                           boolean eventHandler, boolean exportedHandler,
                           boolean postEventHandler, boolean invertedDirtyHandler,
                           boolean forkExecution, boolean failBuildOnUnguardedTrigger,
                           boolean guardedParent, boolean noPropagateEventHandler,
                           String methodTarget, String methodName, int parameterCount,
                           String returnType, String methodString,
                           List<String> genericParameterTypeNames, boolean varArgs) {
        this.method = null;
        this.instance = null;
        this.variableName = variableName;
        this.parameterClass = parameterClass;
        this.eventHandler = eventHandler;
        this.exportedHandler = exportedHandler;
        this.postEventHandler = postEventHandler;
        this.invertedDirtyHandler = invertedDirtyHandler;
        this.forkExecution = forkExecution;
        this.failBuildOnUnguardedTrigger = failBuildOnUnguardedTrigger;
        this.guardedParent = guardedParent;
        this.noPropagateEventHandler = noPropagateEventHandler;
        this.methodTarget = methodTarget;
        this.methodName = methodName;
        this.parameterCount = parameterCount;
        this.returnType = returnType;
        this.methodString = methodString;
        this.genericParameterTypeNames = Collections.unmodifiableList(new ArrayList<>(genericParameterTypeNames));
        this.varArgs = varArgs;
    }

    /**
     * Factory that constructs a {@link CbMethodHandle} from a DTO {@link AnnotatedMethodDto}
     * without any live object references. Used in {@code generateMetaModelFromDto()} so that
     * lifecycle and event dispatch maps can be built purely from DTO data.
     *
     * @param annotatedMethod    the DTO method descriptor with annotation attributes
     * @param variableName       the node's variable name in the generated class
     * @param isEventHandler     {@code true} for @OnEventHandler / EventHandlerNode callbacks
     * @param isExportedHandler  {@code true} for exported-service dispatch callbacks
     * @param parameterClassName canonical class name of the first parameter, or {@code null}
     * @param nodeIsParallelCandidate whether the owning node is a parallel-candidate
     */
    public static CbMethodHandle fromAnnotatedMethod(AnnotatedMethodDto annotatedMethod,
                                                     String variableName,
                                                     boolean isEventHandler,
                                                     boolean isExportedHandler,
                                                     String parameterClassName,
                                                     boolean nodeIsParallelCandidate) {
        MethodDescriptor md = annotatedMethod.getMethod();
        AnnotationDescriptorDto onTriggerA = annotatedMethod.findAnnotation(OnTrigger.class.getCanonicalName());
        AnnotationDescriptorDto onParentUpdateA = annotatedMethod.findAnnotation(OnParentUpdate.class.getCanonicalName());
        AnnotationDescriptorDto onEventHandlerA = annotatedMethod.findAnnotation(OnEventHandler.class.getCanonicalName());

        boolean postEventHandler = annotatedMethod.hasAnnotation(AfterTrigger.class.getCanonicalName());
        boolean invertedDirtyHandler = onTriggerA != null
                && Boolean.FALSE.equals(onTriggerA.getAttribute("dirty", true));
        boolean parallelFromNode = nodeIsParallelCandidate;
        boolean parallelFromAnnot = onTriggerA != null
                && Boolean.TRUE.equals(onTriggerA.getAttribute("parallelExecution", false));
        boolean forkExecution = parallelFromNode || parallelFromAnnot;
        boolean failBuildOnUnguardedTrigger = onTriggerA != null
                && Boolean.TRUE.equals(onTriggerA.getAttribute("failBuildIfMissingBooleanReturn", false));
        boolean guardedParent = onParentUpdateA != null
                && Boolean.TRUE.equals(onParentUpdateA.getAttribute("guarded", false));
        boolean noPropagateEventHandler = (onEventHandlerA != null
                && Boolean.FALSE.equals(onEventHandlerA.getAttribute("propagate", true)))
                || annotatedMethod.hasAnnotation("com.telamin.fluxtion.runtime.annotations.NoPropagateFunction");

        // methodTarget: static methods use declaring-class simple name; instance methods use variableName
        String methodTarget;
        if (annotatedMethod.isStatic()) {
            String declClass = md.getDeclaringClassName();
            int lastDot = declClass.lastIndexOf('.');
            String simpleName = lastDot >= 0 ? declClass.substring(lastDot + 1) : declClass;
            methodTarget = simpleName.replace('$', '.');
        } else {
            methodTarget = variableName;
        }

        return new CbMethodHandle(
                variableName, parameterClassName, isEventHandler, isExportedHandler,
                postEventHandler, invertedDirtyHandler, forkExecution, failBuildOnUnguardedTrigger,
                guardedParent, noPropagateEventHandler,
                methodTarget, md.getMethodName(), md.getParameterTypeNames().size(),
                md.getReturnTypeName(), md.getGenericString(),
                md.getGenericParameterTypeNames(), md.isVarArgs());
    }

    public CbMethodHandle(Method method, Object instance, String variableName, Class<?> parameterClass, boolean eventHandler, boolean exportedHandler) {
        this.method = method;
        this.instance = instance;
        this.variableName = variableName;
        this.parameterClass = parameterClass == null ? null : parameterClass.getCanonicalName();
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
        this.returnType = method.getReturnType().getCanonicalName();
        this.methodString = method.toString();
        this.genericParameterTypeNames = Arrays.stream(method.getGenericParameterTypes())
                .map(Type::getTypeName)
                .collect(Collectors.toList());
        this.varArgs = method.isVarArgs();
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
        hash = 23 * hash + Objects.hashCode(this.methodString);
        hash = 23 * hash + Objects.hashCode(this.variableName);
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

    /** Sets the live Method. Used by {@code resolveInstances()} after DTO-based model generation. */
    public void setMethod(Method method) { this.method = method; }

    /** Sets the live instance. Used by {@code resolveInstances()} after DTO-based model generation. */
    public void setInstance(Object instance) { this.instance = instance; }
}
