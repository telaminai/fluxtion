/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Serializable descriptor for a single node in the topologically sorted dependency graph.
 * Contains the node's identity, graph topology edges (as variable-name strings), and the
 * list of annotated methods discovered by reflection on the client side.
 * No live object references are held.
 */
public final class NodeDto implements Serializable {
    private static final long serialVersionUID = 3L;

    /** The generated variable name for this node (e.g. {@code "intHandler_0"}). */
    private final String variableName;

    /** Canonical class name of this node (e.g. {@code "com.example.MyHandler"}). */
    private final String canonicalClassName;

    /** Whether this node is declared as a public node in the event processor. */
    private final boolean isPublic;

    /** Whether this node is an auditor (implements {@code Auditor}). */
    private final boolean isAuditor;

    /**
     * Whether this node implements {@code EventHandlerNode<T>} directly (not via @OnEventHandler).
     * If true, {@link #eventHandlerNodeEventClass} and {@link #eventHandlerOnEventMethodDescriptor}
     * are populated.
     */
    private final boolean isEventHandlerNode;

    /**
     * Canonical class name of the event type {@code T} in {@code EventHandlerNode<T>},
     * or {@code null} if this node is not an {@code EventHandlerNode}.
     */
    private final String eventHandlerNodeEventClass;

    /**
     * {@link MethodDescriptor} for the {@code onEvent(T)} method of an {@code EventHandlerNode<T>}
     * node, or {@code null} if this node is not an {@code EventHandlerNode}.
     */
    private final MethodDescriptor eventHandlerOnEventMethodDescriptor;

    /**
     * Filter ID for {@code EventHandlerNode<T>} nodes (from {@code EventHandlerNode.filterId()} or
     * a filter-map override). {@code Integer.MIN_VALUE} means no integer filter.
     */
    private final int eventHandlerFilterId;

    /** Filter string for {@code EventHandlerNode<T>} nodes (from {@code EventHandlerNode.filterString()}). */
    private final String eventHandlerFilterString;

    /**
     * Whether this node is a {@code ParallelFunction} whose {@code parallelCandidate()} returns
     * {@code true} at construction time. Used to set {@code forkExecution} in
     * the callback method handle during DTO-based model generation.
     */
    private final boolean isParallelCandidate;

    /** Methods with Fluxtion annotations discovered on this node's class by reflection. */
    private final List<AnnotatedMethodDto> annotatedMethods;

    /** Variable names of nodes this node directly depends on (topological parents). */
    private final List<String> directParentVariableNames;

    /** Variable names of nodes that directly depend on this node (topological children). */
    private final List<String> directChildrenVariableNames;

    /**
     * Variable names of parent nodes that participate in event propagation toward this node
     * (i.e. parents whose event handling triggers this node).
     */
    private final List<String> directParentsListeningForEvent;

    /**
     * Variable names of child nodes that this node propagates events to
     * (i.e. children that will be triggered when this node fires).
     */
    private final List<String> directChildrenListeningForEvent;

    /**
     * For event-source nodes (EventHandlerNode or @OnEventHandler), the topologically ordered
     * variable names of all nodes reachable via event propagation from this node (including self).
     * This is the DTO equivalent of {@code TopologicallySortedDependencyGraph.getEventSortedDependents()}.
     * Empty for non-event-source nodes.
     */
    private final List<String> eventSortedDependentVariableNames;

    // ── Source-generation output fields (populated by FieldSerializer client-side) ──────

    /** Pre-computed constructor assignment string for this node (source-gen path). */
    private final String constructorString;

    /** Pre-computed bean property setter strings for this node (source-gen path). */
    private final List<String> beanPropertyStrings;

    /** Pre-computed public member assignment strings for this node (source-gen path). */
    private final List<String> publicMemberStrings;

    /** Pre-computed type declaration string for this node (source-gen path). */
    private final String typeDeclaration;

    /** Canonical names of import classes contributed by this node (source-gen path). */
    private final List<String> importClassStrings;

    private NodeDto(Builder b) {
        this.variableName = b.variableName;
        this.canonicalClassName = b.canonicalClassName;
        this.isPublic = b.isPublic;
        this.isAuditor = b.isAuditor;
        this.isEventHandlerNode = b.isEventHandlerNode;
        this.eventHandlerNodeEventClass = b.eventHandlerNodeEventClass;
        this.eventHandlerOnEventMethodDescriptor = b.eventHandlerOnEventMethodDescriptor;
        this.eventHandlerFilterId = b.eventHandlerFilterId;
        this.eventHandlerFilterString = b.eventHandlerFilterString;
        this.isParallelCandidate = b.isParallelCandidate;
        this.annotatedMethods = Collections.unmodifiableList(new ArrayList<>(b.annotatedMethods));
        this.directParentVariableNames = Collections.unmodifiableList(new ArrayList<>(b.directParentVariableNames));
        this.directChildrenVariableNames = Collections.unmodifiableList(new ArrayList<>(b.directChildrenVariableNames));
        this.directParentsListeningForEvent = Collections.unmodifiableList(new ArrayList<>(b.directParentsListeningForEvent));
        this.directChildrenListeningForEvent = Collections.unmodifiableList(new ArrayList<>(b.directChildrenListeningForEvent));
        this.eventSortedDependentVariableNames = Collections.unmodifiableList(new ArrayList<>(b.eventSortedDependentVariableNames));
        this.constructorString = b.constructorString;
        this.beanPropertyStrings = Collections.unmodifiableList(new ArrayList<>(b.beanPropertyStrings));
        this.publicMemberStrings = Collections.unmodifiableList(new ArrayList<>(b.publicMemberStrings));
        this.typeDeclaration = b.typeDeclaration;
        this.importClassStrings = Collections.unmodifiableList(new ArrayList<>(b.importClassStrings));
    }

    public String getVariableName() { return variableName; }
    public String getCanonicalClassName() { return canonicalClassName; }
    public boolean isPublic() { return isPublic; }
    public boolean isAuditor() { return isAuditor; }
    public boolean isEventHandlerNode() { return isEventHandlerNode; }
    public String getEventHandlerNodeEventClass() { return eventHandlerNodeEventClass; }
    public MethodDescriptor getEventHandlerOnEventMethodDescriptor() { return eventHandlerOnEventMethodDescriptor; }
    public int getEventHandlerFilterId() { return eventHandlerFilterId; }
    public String getEventHandlerFilterString() { return eventHandlerFilterString; }
    public boolean isParallelCandidate() { return isParallelCandidate; }
    public List<AnnotatedMethodDto> getAnnotatedMethods() { return annotatedMethods; }
    public List<String> getDirectParentVariableNames() { return directParentVariableNames; }
    public List<String> getDirectChildrenVariableNames() { return directChildrenVariableNames; }
    public List<String> getDirectParentsListeningForEvent() { return directParentsListeningForEvent; }
    public List<String> getDirectChildrenListeningForEvent() { return directChildrenListeningForEvent; }
    public List<String> getEventSortedDependentVariableNames() { return eventSortedDependentVariableNames; }
    public String getConstructorString() { return constructorString; }
    public List<String> getBeanPropertyStrings() { return beanPropertyStrings; }
    public List<String> getPublicMemberStrings() { return publicMemberStrings; }
    public String getTypeDeclaration() { return typeDeclaration; }
    public List<String> getImportClassStrings() { return importClassStrings; }

    /** Return the first annotated method carrying {@code annotationClassName}, or {@code null}. */
    public AnnotatedMethodDto findMethodWithAnnotation(String annotationClassName) {
        for (AnnotatedMethodDto m : annotatedMethods) {
            if (m.hasAnnotation(annotationClassName)) return m;
        }
        return null;
    }

    /** Return all annotated methods carrying {@code annotationClassName}. */
    public List<AnnotatedMethodDto> findMethodsWithAnnotation(String annotationClassName) {
        List<AnnotatedMethodDto> result = new ArrayList<>();
        for (AnnotatedMethodDto m : annotatedMethods) {
            if (m.hasAnnotation(annotationClassName)) result.add(m);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeDto)) return false;
        NodeDto that = (NodeDto) o;
        return isPublic == that.isPublic
                && isAuditor == that.isAuditor
                && isEventHandlerNode == that.isEventHandlerNode
                && isParallelCandidate == that.isParallelCandidate
                && Objects.equals(variableName, that.variableName)
                && Objects.equals(canonicalClassName, that.canonicalClassName)
                && Objects.equals(eventHandlerNodeEventClass, that.eventHandlerNodeEventClass)
                && Objects.equals(annotatedMethods, that.annotatedMethods)
                && Objects.equals(directParentVariableNames, that.directParentVariableNames)
                && Objects.equals(directChildrenVariableNames, that.directChildrenVariableNames)
                && Objects.equals(directParentsListeningForEvent, that.directParentsListeningForEvent)
                && Objects.equals(directChildrenListeningForEvent, that.directChildrenListeningForEvent)
                && Objects.equals(eventSortedDependentVariableNames, that.eventSortedDependentVariableNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, canonicalClassName);
    }

    @Override
    public String toString() {
        return variableName + ":" + canonicalClassName + (isPublic ? "[public]" : "") + (isEventHandlerNode ? "[eventHandler]" : "");
    }

    public static Builder builder(String variableName, String canonicalClassName) {
        return new Builder(variableName, canonicalClassName);
    }

    public static final class Builder {
        private final String variableName;
        private final String canonicalClassName;
        private boolean isPublic;
        private boolean isAuditor;
        private boolean isEventHandlerNode;
        private String eventHandlerNodeEventClass;
        private MethodDescriptor eventHandlerOnEventMethodDescriptor;
        private int eventHandlerFilterId = Integer.MIN_VALUE;
        private String eventHandlerFilterString;
        private boolean isParallelCandidate;
        private List<AnnotatedMethodDto> annotatedMethods = new ArrayList<>();
        private List<String> directParentVariableNames = new ArrayList<>();
        private List<String> directChildrenVariableNames = new ArrayList<>();
        private List<String> directParentsListeningForEvent = new ArrayList<>();
        private List<String> directChildrenListeningForEvent = new ArrayList<>();
        private List<String> eventSortedDependentVariableNames = new ArrayList<>();
        private String constructorString;
        private List<String> beanPropertyStrings = new ArrayList<>();
        private List<String> publicMemberStrings = new ArrayList<>();
        private String typeDeclaration;
        private List<String> importClassStrings = new ArrayList<>();

        private Builder(String variableName, String canonicalClassName) {
            this.variableName = Objects.requireNonNull(variableName);
            this.canonicalClassName = Objects.requireNonNull(canonicalClassName);
        }

        public Builder isPublic(boolean isPublic) { this.isPublic = isPublic; return this; }
        public Builder isAuditor(boolean isAuditor) { this.isAuditor = isAuditor; return this; }
        public Builder isEventHandlerNode(boolean v) { this.isEventHandlerNode = v; return this; }
        public Builder eventHandlerNodeEventClass(String v) { this.eventHandlerNodeEventClass = v; return this; }
        public Builder eventHandlerOnEventMethodDescriptor(MethodDescriptor v) { this.eventHandlerOnEventMethodDescriptor = v; return this; }
        public Builder eventHandlerFilterId(int v) { this.eventHandlerFilterId = v; return this; }
        public Builder eventHandlerFilterString(String v) { this.eventHandlerFilterString = v; return this; }
        public Builder isParallelCandidate(boolean v) { this.isParallelCandidate = v; return this; }
        public Builder annotatedMethods(List<AnnotatedMethodDto> m) { this.annotatedMethods = m; return this; }
        public Builder directParentVariableNames(List<String> p) { this.directParentVariableNames = p; return this; }
        public Builder directChildrenVariableNames(List<String> c) { this.directChildrenVariableNames = c; return this; }
        public Builder directParentsListeningForEvent(List<String> p) { this.directParentsListeningForEvent = p; return this; }
        public Builder directChildrenListeningForEvent(List<String> c) { this.directChildrenListeningForEvent = c; return this; }
        public Builder eventSortedDependentVariableNames(List<String> v) { this.eventSortedDependentVariableNames = v; return this; }
        public Builder constructorString(String v) { this.constructorString = v; return this; }
        public Builder beanPropertyStrings(List<String> v) { this.beanPropertyStrings = v; return this; }
        public Builder publicMemberStrings(List<String> v) { this.publicMemberStrings = v; return this; }
        public Builder typeDeclaration(String v) { this.typeDeclaration = v; return this; }
        public Builder importClassStrings(List<String> v) { this.importClassStrings = v; return this; }

        public NodeDto build() { return new NodeDto(this); }
    }
}
