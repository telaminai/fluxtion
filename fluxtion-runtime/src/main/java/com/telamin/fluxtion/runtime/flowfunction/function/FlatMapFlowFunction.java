/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.callback.Callback;
import com.telamin.fluxtion.runtime.callback.DirtyStateMonitor;
import com.telamin.fluxtion.runtime.context.buildtime.GeneratorNodeCollection;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.node.BaseNode;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.MethodReferenceInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * Flatmap stream node
 *
 * @param <T> Incoming type
 * @param <R> Output type
 * @param <S> Previous {@link FlowFunction} type
 */
public class FlatMapFlowFunction<T, R, S extends FlowFunction<T>> extends BaseNode implements TriggeredFlowFunction<R> {

    @NoTriggerReference
    private final S inputEventStream;
    @NoTriggerReference
    private final transient Object streamFunctionInstance;
    /** Set only on the closed-world path; kept so the node can report how it was built. */
    private transient MethodReferenceInfo methodReferenceInfo;
    private final SerializableFunction<T, Iterable<R>> iterableFunction;
    private transient R value;
    @Inject
    public Callback<R> callback;
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;
    @Getter
    @Setter
    private String flatMapCompleteSignal;

    public FlatMapFlowFunction(S inputEventStream, SerializableFunction<T, Iterable<R>> iterableFunction) {
        this.inputEventStream = inputEventStream;
        this.iterableFunction = iterableFunction;
        if (iterableFunction.captured().length > 0) {
            streamFunctionInstance = GeneratorNodeCollection.service().addOrReuse(iterableFunction.captured()[0]);
        } else {
            streamFunctionInstance = null;
        }
    }

    /**
     * The closed-world constructor: the generator resolved the method reference, so nothing here needs
     * to look it up.
     *
     * <p><b>This is what makes a flatMap graph work as a native image.</b> The constructor above calls
     * {@code captured()}, which calls {@code serialized()}, which does
     * {@code getDeclaredMethod("writeReplace")} — and GraalVM does not emit {@code writeReplace} for
     * lambda classes unless serialization is registered, so a generated processor containing a flatMap
     * could not be CONSTRUCTED under native-image. It failed at startup; the event path never ran. A
     * method reference does not avoid it: that is a lambda class too.
     *
     * <p>Every other flow node already had this treatment — {@code MapRef2ToIntFlowFunction} is emitted
     * with a {@code MethodReferenceInfo} and never reflects. flatMap did not participate, for two
     * reasons that both had to be fixed: it had no such constructor, and it {@code extends BaseNode}
     * rather than {@code AbstractFlowFunction}, so the extractor's gate rejected it before looking.
     *
     * <p>{@code streamFunctionInstance} is deliberately left null. The field is WRITE-ONLY — nothing
     * reads it — and its assignment above exists for the side effect of registering the captured
     * instance as a node. That is builder work, done while the graph is BUILT through the constructor
     * above; by the time generated source runs, the node is already registered and wired. Re-running it
     * is exactly what the closed-world path exists to avoid.
     */
    public FlatMapFlowFunction(S inputEventStream, SerializableFunction<T, Iterable<R>> iterableFunction,
                               MethodReferenceInfo methodReferenceInfo) {
        this.inputEventStream = inputEventStream;
        this.iterableFunction = iterableFunction;
        this.streamFunctionInstance = null;
        this.methodReferenceInfo = methodReferenceInfo;
    }

    @OnParentUpdate("inputEventStream")
    public void inputUpdatedAndFlatMap(S inputEventStream) {
        T input = inputEventStream.get();
        Iterable<R> iterable = iterableFunction.apply(input);
        callback.fireCallback(iterable.iterator());
        if (flatMapCompleteSignal != null) {
            getContext().getParentDataFlow().publishSignal(flatMapCompleteSignal, flatMapCompleteSignal);
        }
    }

    @OnTrigger
    public void callbackReceived() {
        value = callback.get();
    }

    @Override
    public void parallel() {

    }

    @Override
    public boolean parallelCandidate() {
        return false;
    }

    @Override
    public boolean hasChanged() {
        return dirtyStateMonitor.isDirty(this);
    }

    @Override
    public R get() {
        return value;
    }

    @Override
    public void setUpdateTriggerNode(Object updateTriggerNode) {

    }

    @Override
    public void setPublishTriggerNode(Object publishTriggerNode) {

    }

    @Override
    public void setResetTriggerNode(Object resetTriggerNode) {

    }

    @Override
    public void setPublishTriggerOverrideNode(Object publishTriggerOverrideNode) {
    }
}
