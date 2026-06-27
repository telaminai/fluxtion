/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive.IntSumFlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.KeyValue;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * P1 producer: {@link GroupByFlowFunctionWrapper#aggregate} emits a single-key {@link GroupByDelta}
 * (ADD for a new key, UPDATE for an existing one); {@code reset()} emits a clear; {@code combine}/
 * {@code deduct} (windowed, multi-key — P3) signal {@code RECOMPUTE_REQUIRED}. Also checks the
 * {@link GroupByDeltaFlowFunctions} flat-map helpers behind {@code changedKeyValues()} / {@code changes()}.
 */
public class GroupByFlowFunctionWrapperDeltaTest {

    static final class Ev {
        final String k;
        final int v;

        Ev(String k, int v) {
            this.k = k;
            this.v = v;
        }

        String k() {
            return k;
        }

        int v() {
            return v;
        }
    }

    private static GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> newSumGroup() {
        SerializableFunction<Ev, String> key = Ev::k;
        SerializableFunction<Ev, Integer> value = Ev::v;
        SerializableSupplier<IntSumFlowFunction> agg = IntSumFlowFunction::new;
        return new GroupByFlowFunctionWrapper<>(key, value, agg);
    }

    @Test
    public void aggregateEmitsSingleKeyAddThenUpdate() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> g = newSumGroup();

        g.aggregate(new Ev("A", 5));
        GroupByDelta<String, Integer> d1 = g.delta();
        assertEquals(DeltaMode.INCREMENTAL, d1.mode());
        assertEquals(1, d1.entries().size());
        assertEquals(new Change<>("A", 5, ChangeOp.ADD), d1.entries().get(0));

        g.aggregate(new Ev("A", 3)); // sum -> 8, existing key
        GroupByDelta<String, Integer> d2 = g.delta();
        assertEquals(DeltaMode.INCREMENTAL, d2.mode());
        assertEquals(new Change<>("A", 8, ChangeOp.UPDATE), d2.entries().get(0));

        g.aggregate(new Ev("B", 2)); // new key
        assertEquals(new Change<>("B", 2, ChangeOp.ADD), g.delta().entries().get(0));
    }

    @Test
    public void resetEmitsClearThenApplyAndNextKeyIsAddAgain() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> g = newSumGroup();
        g.aggregate(new Ev("A", 5));
        g.reset();

        GroupByDelta<String, Integer> d = g.delta();
        assertEquals(DeltaMode.CLEAR_THEN_APPLY, d.mode());
        assertEquals(0, d.entries().size());

        g.aggregate(new Ev("A", 1)); // cleared, so A is new again
        assertEquals(new Change<>("A", 1, ChangeOp.ADD), g.delta().entries().get(0));
    }

    @Test
    public void combineSignalsRecomputeRequired() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> a = newSumGroup();
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> b = newSumGroup();
        a.aggregate(new Ev("A", 5));
        b.aggregate(new Ev("A", 9));

        a.combine(b);
        assertEquals(DeltaMode.RECOMPUTE_REQUIRED, a.delta().mode());
    }

    @Test
    public void changedKeyValuesHelperEmitsAddUpdateOnly() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntSumFlowFunction> g = newSumGroup();
        g.aggregate(new Ev("A", 5));

        @SuppressWarnings("rawtypes")
        List<KeyValue> kvs = GroupByDeltaFlowFunctions.changedKeyValues(g);
        assertEquals(1, kvs.size());
        assertEquals("A", kvs.get(0).getKey());
        assertEquals(Integer.valueOf(5), kvs.get(0).getValue());

        @SuppressWarnings("rawtypes")
        List<Change> changes = GroupByDeltaFlowFunctions.changes(g);
        assertEquals(1, changes.size());
        assertEquals(new Change<>("A", 5, ChangeOp.ADD), changes.get(0));
    }
}
