/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.annotations.builder.SepNode;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.TriggeredFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.AbstractFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import com.telamin.fluxtion.runtime.time.Clock;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event-time tumbling group-by: like {@link GroupByTumblingWindow} but the window boundary follows
 * <b>event-time</b> (the graph {@link Clock}'s {@link Clock#getEventTime() getEventTime()}, sourced from each
 * input {@code Event}) rather than wallclock. Crucially the roll is <b>driven by this window's own data
 * input</b> — the boundary is checked in {@link #inputUpdated(FlowFunction)} when a real data event arrives,
 * not by a global processing-time trigger. That is the load-bearing difference: a global event-time trigger
 * (a {@code FixedRateTrigger} reading event-time) fires on <em>every</em> event entering the processor, so it
 * would be advanced by framework events (which implement {@code Event}) and by <em>other</em> streams' events
 * — corrupting a per-stream event-time window. Reading event-time only on this window's input advances it on
 * the right stream alone and structurally ignores everything else.
 *
 * <p>The first window edge aligns to the first event seen; when an input's event-time has advanced a whole
 * {@code windowSizeMillis} past the current edge, the completed window is published (one bucket per crossing)
 * and a fresh window begins, into which the triggering event is then aggregated.
 *
 * <p><b>Scope (allowed-lateness = 0):</b> a <b>late</b> event — one whose event-time falls before the current
 * window edge, i.e. its bucket has already closed — is <b>dropped</b> (not aggregated into the current window,
 * which would silently corrupt an unrelated bucket). Watermarks + a configurable allowed-lateness horizon +
 * retract/re-emit of a closed bucket + late-data routing are a later slice (fsql-late-data.md).
 *
 * @param <T> input type
 * @param <K> key type from input T
 * @param <V> value type extracted from T, input to the aggregate
 * @param <R> aggregate output type
 * @param <S> the {@link FlowFunction} input type
 * @param <F> the aggregate function type
 */
public class GroupByTumblingEventTimeWindow<T, K, V, R, S extends FlowFunction<T>, F extends AggregateFlowFunction<V, R, F>>
        extends AbstractFlowFunction<T, GroupBy<K, R>, S>
        implements TriggeredFlowFunction<GroupBy<K, R>> {

    @SepNode
    @NoTriggerReference
    public GroupByFlowFunctionWrapper<T, K, V, R, F> groupByWindowedCollection;

    /** The graph singleton clock, injected — read for event-time only when this window's input updates. */
    @Inject
    @NoTriggerReference
    public Clock clock;

    private final int windowSizeMillis;

    // LinkedHashMap: first-key-seen order → deterministic multi-key emit, identical interpreted/AOT (see
    // GroupByHashMap / GroupByTumblingWindow).
    private transient final Map<K, R> mapOfValues = new LinkedHashMap<>();
    private transient final MyGroupBy results = new MyGroupBy();
    private transient long windowStart;
    private transient boolean primed;

    public GroupByTumblingEventTimeWindow(
            S inputEventStream,
            SerializableSupplier<F> windowFunctionSupplier,
            SerializableFunction<T, K> keyFunction,
            SerializableFunction<T, V> valueFunction,
            int windowSizeMillis) {
        this(inputEventStream, windowSizeMillis);
        this.groupByWindowedCollection = new GroupByFlowFunctionWrapper<>(keyFunction, valueFunction, windowFunctionSupplier);
    }

    public GroupByTumblingEventTimeWindow(S inputEventStream, int windowSizeMillis) {
        super(inputEventStream, null);
        this.windowSizeMillis = windowSizeMillis;
    }

    @Override
    public GroupBy<K, R> get() {
        return results;
    }

    protected void cacheWindowValue() {
        mapOfValues.clear();
        mapOfValues.putAll(groupByWindowedCollection.toMap());
    }

    protected void aggregateInputValue(S inputEventStream) {
        groupByWindowedCollection.aggregate(inputEventStream.get());
    }

    @OnParentUpdate
    public void inputUpdated(S inputEventStream) {
        long eventTime = clock.getEventTime();
        boolean rolled = false;
        if (!primed) {
            // Align the first edge to the global event-time grid (floor to a window multiple), not to the first
            // event's exact timestamp — so buckets are deterministic [0,size),[size,2*size)... regardless of
            // which event arrives first, matching standard event-time tumbling.
            windowStart = (eventTime / windowSizeMillis) * windowSizeMillis;
            primed = true;
        } else if (eventTime < windowStart) {
            // Late event (allowed-lateness = 0): its bucket already closed. DROP it — do NOT aggregate into the
            // current bucket (that silently corrupts an unrelated window) and do NOT propagate. Allowed-lateness
            // + retract/re-emit is a later slice (fsql-late-data.md).
            publishOverrideTriggered = false;
            inputStreamTriggered_1 = false;
            inputStreamTriggered = false;
            return;
        } else if (eventTime - windowStart >= windowSizeMillis) {
            // A whole bucket (or more) has elapsed in event-time: cache + publish the completed window,
            // reset the collection, and advance the edge by however many buckets were crossed.
            cacheWindowValue();
            groupByWindowedCollection.reset();
            windowStart += windowSizeMillis * ((eventTime - windowStart) / windowSizeMillis);
            rolled = true;
        }
        aggregateInputValue(inputEventStream);
        // Publish only on a roll (the just-completed bucket); otherwise accumulate silently — exactly the
        // processing-time tumbling semantics, but triggered by event-time crossing rather than a clock tick.
        publishOverrideTriggered = rolled && !overridePublishTrigger && !overrideUpdateTrigger;
        inputStreamTriggered_1 = rolled;
        inputStreamTriggered = rolled;
    }

    @OnParentUpdate("updateTriggerNode")
    public void updateTriggerNodeUpdated(Object triggerNode) {
        super.updateTriggerNodeUpdated(triggerNode);
        cacheWindowValue();
    }

    @Override
    protected void resetOperation() {
        mapOfValues.clear();
        groupByWindowedCollection.reset();
        primed = false;
    }

    @Override
    public boolean isStatefulFunction() {
        return true;
    }

    @OnTrigger
    public boolean triggered() {
        return fireEventUpdateNotification();
    }

    private class MyGroupBy implements GroupBy<K, R> {

        @Override
        public Map<K, R> toMap() {
            return mapOfValues;
        }

        @Override
        public Collection<R> values() {
            return mapOfValues.values();
        }

        @Override
        public R lastValue() {
            return groupByWindowedCollection.lastValue();
        }

        @Override
        public KeyValue<K, R> lastKeyValue() {
            return groupByWindowedCollection.lastKeyValue();
        }
    }
}
