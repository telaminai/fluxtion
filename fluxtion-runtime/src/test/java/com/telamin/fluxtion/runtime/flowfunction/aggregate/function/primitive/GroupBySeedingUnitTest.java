package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import org.junit.Assert;
import org.junit.Test;

/** Direct GroupByFlowFunctionWrapper seeding probe — no graph, no AOT. */
public class GroupBySeedingUnitTest {

    public static class Ev {
        final String k;
        final int i;
        final long l;
        final double d;
        Ev(String k, int i, long l, double d) { this.k = k; this.i = i; this.l = l; this.d = d; }
        public String k() { return k; }
        public int i() { return i; }
        public long l() { return l; }
        public double d() { return d; }
    }

    private static final SerializableFunction<Ev, String> KEY = Ev::k;

    @Test
    public void intMin() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntMinFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Integer>) Ev::i, Aggregates.intMinFactory());
        w.aggregate(new Ev("A", 10, 0, 0));
        w.aggregate(new Ev("A", 7, 0, 0));
        w.aggregate(new Ev("A", 12, 0, 0));
        Assert.assertEquals(Integer.valueOf(7), w.toMap().get("A"));
    }

    @Test
    public void longMin() {
        GroupByFlowFunctionWrapper<Ev, String, Long, Long, LongMinFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Long>) Ev::l, Aggregates.longMinFactory());
        w.aggregate(new Ev("A", 0, 10, 0));
        w.aggregate(new Ev("A", 0, 7, 0));
        Assert.assertEquals(Long.valueOf(7), w.toMap().get("A"));
    }

    @Test
    public void doubleMin() {
        GroupByFlowFunctionWrapper<Ev, String, Double, Double, DoubleMinFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Double>) Ev::d, Aggregates.doubleMinFactory());
        w.aggregate(new Ev("A", 0, 0, 10.0));
        w.aggregate(new Ev("A", 0, 0, 7.0));
        Assert.assertEquals(Double.valueOf(7.0), w.toMap().get("A"));
    }

    // MAX over all-negative data: a leaked-0 seed (0 > any input) would return 0.

    @Test
    public void intMax() {
        GroupByFlowFunctionWrapper<Ev, String, Integer, Integer, IntMaxFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Integer>) Ev::i, Aggregates.intMaxFactory());
        w.aggregate(new Ev("A", -10, 0, 0));
        w.aggregate(new Ev("A", -7, 0, 0));
        w.aggregate(new Ev("A", -12, 0, 0));
        Assert.assertEquals(Integer.valueOf(-7), w.toMap().get("A"));
    }

    @Test
    public void longMax() {
        GroupByFlowFunctionWrapper<Ev, String, Long, Long, LongMaxFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Long>) Ev::l, Aggregates.longMaxFactory());
        w.aggregate(new Ev("A", 0, -10, 0));
        w.aggregate(new Ev("A", 0, -7, 0));
        Assert.assertEquals(Long.valueOf(-7), w.toMap().get("A"));
    }

    @Test
    public void doubleMax() {
        GroupByFlowFunctionWrapper<Ev, String, Double, Double, DoubleMaxFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Double>) Ev::d, Aggregates.doubleMaxFactory());
        w.aggregate(new Ev("A", 0, 0, -10.0));
        w.aggregate(new Ev("A", 0, 0, -7.0));
        Assert.assertEquals(Double.valueOf(-7.0), w.toMap().get("A"));
    }
}