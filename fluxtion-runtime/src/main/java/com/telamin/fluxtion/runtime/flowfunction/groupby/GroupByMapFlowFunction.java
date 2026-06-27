/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.annotations.builder.SepNode;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.KeyValue;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GroupByMapFlowFunction {

    private final SerializableFunction mapFunction;
    private final SerializableBiFunction mapFrom2MapsBiFunction;
    private final transient GroupByHashMap outputCollection = new GroupByHashMap();
    private final transient GroupByView wrappedCollection = new GroupByView();
    @SepNode
    public Object defaultValue;

    public <T, R> GroupByMapFlowFunction(SerializableFunction<T, R> mapFunction) {
        this(mapFunction, null);
    }

    public <A, B, R> GroupByMapFlowFunction(SerializableBiFunction<A, B, R> mapFrom2MapsBiFunction) {
        this(null, mapFrom2MapsBiFunction);
    }

    public <K, V, A, O> GroupByMapFlowFunction(SerializableFunction<A, K> mapFunction, SerializableBiFunction<V, A, O> mapFrom2MapsBiFunction) {
        this.mapFunction = mapFunction;
        this.mapFrom2MapsBiFunction = mapFrom2MapsBiFunction;
    }

    public <K, V, A, O> GroupByMapFlowFunction(SerializableFunction<A, K> mapFunction, SerializableBiFunction<V, A, O> mapFrom2MapsBiFunction, Object defaultValue) {
        this.mapFunction = mapFunction;
        this.mapFrom2MapsBiFunction = mapFrom2MapsBiFunction;
        this.defaultValue = defaultValue;
    }

    //required for serialised version
    public <K, V> GroupBy<K, V> mapValues(Object inputMap) {
        return mapValues((GroupBy) inputMap);
    }

    public <K, V> GroupBy<K, V> mapForEachValues(Object inputMap) {
        return mapForEachValues((GroupBy) inputMap);
    }

    public <K, V> GroupBy<K, V> mapKeyedValue(Object inputMap, Object secondArgument) {
        return mapKeyedValue((GroupBy) inputMap, secondArgument);
    }

    public <K, R> GroupBy<K, R> mapValueWithKeyValue(Object inputMap, KeyValue secondArgument) {
        return mapValueWithKeyValue((GroupBy) inputMap, secondArgument);
    }

    public <K, V> GroupBy<K, V> biMapValuesWithParamMap(Object firstArgGroupBy, Object secondArgGroupBY) {
        return biMapValuesWithParamMap((GroupBy) firstArgGroupBy, (GroupBy) secondArgGroupBY);
    }

    public <K, V> GroupBy<K, V> mapKeys(Object inputMap) {
        return mapKeys((GroupBy) inputMap);
    }

    public <K, V> GroupBy<K, V> mapEntry(Object inputMap) {
        return mapEntry((GroupBy) inputMap);
    }

    public <K, V> GroupBy<K, V> mapForEachValues(GroupBy inputMap) {
        //make this recursive on value type == GroupBy?
        //TODO FIX
        throw new UnsupportedOperationException("not implemented");
//        return mapValues(inputMap);
    }

    /**
     * Delta-aware (P2b of the GroupBy delta IVM scope, {@code docs/design/groupby-delta-ivm.md}).
     * {@code mapValues} is a 1:1 key-preserving transform, so each input {@link Change} maps to one
     * output change with the same key and op and the transformed value — the output
     * {@link GroupByDelta} mirrors the input's shape. {@link DeltaMode#RECOMPUTE_REQUIRED} upstream
     * (e.g. a join or windowed combine) falls back to a full rescan and propagates
     * {@code RECOMPUTE_REQUIRED}; {@code toMap()} is always the full mapped state.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <K, V> GroupBy<K, V> mapValues(GroupBy inputMap) {
        GroupByDelta delta = inputMap.delta();
        Map outMap = outputCollection.toMap();

        if (delta.mode() == DeltaMode.RECOMPUTE_REQUIRED) {
            outputCollection.reset();
            inputMap.toMap().forEach((k, v) -> outMap.put(k, mapFunction.apply(v)));
            outputCollection.setDelta(GroupByDelta.recomputeRequired());
            return outputCollection;
        }

        boolean clear = delta.mode() == DeltaMode.CLEAR_THEN_APPLY;
        if (clear) {
            outputCollection.reset();
        }
        List outChanges = new ArrayList();
        List<Change> entries = delta.entries();
        for (int i = 0; i < entries.size(); i++) {
            Change c = entries.get(i);
            if (c.op() == ChangeOp.DELETE) {
                Object previous = outMap.remove(c.key());
                outChanges.add(Change.delete(c.key(), previous));
            } else {
                Object mapped = mapFunction.apply(c.value());
                outMap.put(c.key(), mapped);
                outChanges.add(new Change(c.key(), mapped, c.op()));
            }
        }
        outputCollection.setDelta(clear
                ? GroupByDelta.clearThenApply(outChanges)
                : GroupByDelta.incremental(outChanges));
        return outputCollection;
    }

    /**
     * Not delta-aware: a key remap may be non-injective (two input keys → one output key), and an
     * incremental DELETE cannot know whether another input key still occupies the output slot —
     * correct incremental maintenance is a groupBy-shaped (ref-counted) problem, deferred. So
     * {@code mapKeys} stays a full recompute and propagates {@link DeltaMode#RECOMPUTE_REQUIRED}
     * downstream (via {@code reset()}), which is always correct, just not O(Δ).
     */
    public <K, V> GroupBy<K, V> mapKeys(GroupBy inputMap) {
        outputCollection.reset();
        inputMap.toMap().entrySet().forEach(e -> {
            Entry entry = (Entry) e;
            outputCollection.toMap().put(mapFunction.apply(entry.getKey()), entry.getValue());
        });
        return outputCollection;
    }

    public <K, V> GroupBy<K, V> mapEntry(GroupBy inputMap) {
        outputCollection.reset();
        inputMap.toMap().entrySet().forEach(e -> {
            Entry entry = (Entry) mapFunction.apply(e);
            outputCollection.toMap().put(entry.getKey(), entry.getValue());
        });
        return outputCollection;
    }

    public <K, G extends GroupBy, R> GroupBy<K, R> mapKeyedValue(G inputMap, Object argumentProvider) {
        wrappedCollection.reset();
        Object key = mapFunction.apply(argumentProvider);
        Object item = inputMap.toMap().get(key);
        if (item != null) {
            KeyValue kv = new KeyValue(key, mapFrom2MapsBiFunction.apply(item, argumentProvider));
            outputCollection.fromMap(inputMap.toMap());
            outputCollection.add(kv);
            wrappedCollection.setGroupBy(outputCollection);
            wrappedCollection.setKeyValue(kv);
        }
        return wrappedCollection;
    }

    public <K, G extends GroupBy, R> GroupBy<K, R> mapValueWithKeyValue(G inputMap, KeyValue argumentProvider) {
        wrappedCollection.reset();
        Object key = argumentProvider.getKey();
        Object item = inputMap.toMap().get(key);
        if (item != null) {
            KeyValue kv = new KeyValue(key, mapFrom2MapsBiFunction.apply(item, argumentProvider.getValue()));
            outputCollection.fromMap(inputMap.toMap());
            outputCollection.add(kv);
            wrappedCollection.setGroupBy(outputCollection);
            wrappedCollection.setKeyValue(kv);
        }
        return wrappedCollection;
    }

    public <K, G extends GroupBy, H extends GroupBy, R> GroupBy<K, R> biMapValuesWithParamMap(G firstArgGroupBy, H secondArgGroupBY) {
        outputCollection.reset();
        Map arg2Map = (secondArgGroupBY == null && defaultValue != null) ? Collections.emptyMap() : secondArgGroupBY.toMap();
        firstArgGroupBy.toMap().forEach((key, arg1) -> {
            Object arg2 = arg2Map.getOrDefault(key, defaultValue);
            if (arg2 != null) {
                outputCollection.toMap().put(key, mapFrom2MapsBiFunction.apply(arg1, arg2));
            }
        });
        return outputCollection;
    }
}
