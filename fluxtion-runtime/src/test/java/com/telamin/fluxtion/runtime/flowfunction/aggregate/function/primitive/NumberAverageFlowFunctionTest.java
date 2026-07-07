package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupByFlowFunctionWrapper;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import org.junit.Assert;
import org.junit.Test;

/** {@link NumberAverageFlowFunction}: exact double average over int/long/double input. */
public class NumberAverageFlowFunctionTest {

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
    public void averagesIntInputExactly() {
        NumberAverageFlowFunction avg = new NumberAverageFlowFunction();
        avg.aggregate(10);
        avg.aggregate(7);
        Assert.assertEquals(8.5, avg.get(), 1e-9); // (10+7)/2, not lossy integer 8
    }

    @Test
    public void combineAndDeductMaintainAverage() {
        NumberAverageFlowFunction a = new NumberAverageFlowFunction();
        a.aggregate(10);
        a.aggregate(20);
        NumberAverageFlowFunction b = new NumberAverageFlowFunction();
        b.aggregate(30);
        a.combine(b);
        Assert.assertEquals(20.0, a.get(), 1e-9); // (10+20+30)/3
        a.deduct(b);
        Assert.assertEquals(15.0, a.get(), 1e-9); // back to (10+20)/2
    }

    @Test
    public void intGroupByAveragesToDouble() {
        GroupByFlowFunctionWrapper<Ev, String, Number, Double, NumberAverageFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Number>) Ev::i, Aggregates.numberAverageFactory());
        w.aggregate(new Ev("A", 10, 0, 0));
        w.aggregate(new Ev("A", 7, 0, 0));
        w.aggregate(new Ev("B", 4, 0, 0));
        Assert.assertEquals(8.5, (Double) w.toMap().get("A"), 1e-9);
        Assert.assertEquals(4.0, (Double) w.toMap().get("B"), 1e-9);
    }

    @Test
    public void longGroupByAveragesToDouble() {
        GroupByFlowFunctionWrapper<Ev, String, Number, Double, NumberAverageFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Number>) Ev::l, Aggregates.numberAverageFactory());
        w.aggregate(new Ev("A", 0, 10, 0));
        w.aggregate(new Ev("A", 0, 5, 0));
        Assert.assertEquals(7.5, (Double) w.toMap().get("A"), 1e-9);
    }

    @Test
    public void doubleGroupByAveragesToDouble() {
        GroupByFlowFunctionWrapper<Ev, String, Number, Double, NumberAverageFlowFunction> w =
                new GroupByFlowFunctionWrapper<>(KEY, (SerializableFunction<Ev, Number>) Ev::d, Aggregates.numberAverageFactory());
        w.aggregate(new Ev("A", 0, 0, 1.5));
        w.aggregate(new Ev("A", 0, 0, 2.0));
        Assert.assertEquals(1.75, (Double) w.toMap().get("A"), 1e-9);
    }
}
