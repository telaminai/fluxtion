package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AverageFlowFunctionTest {

    @Test
    public void intAverageDeductToEmptyWindowReturnsZero() {
        IntAverageFlowFunction total = new IntAverageFlowFunction();
        total.aggregateInt(100);

        IntAverageFlowFunction expired = new IntAverageFlowFunction();
        expired.aggregateInt(100);

        total.deduct(expired);

        assertEquals(0, total.getAsInt());
    }

    @Test
    public void longAverageDeductToEmptyWindowReturnsZero() {
        LongAverageFlowFunction total = new LongAverageFlowFunction();
        total.aggregateLong(100L);

        LongAverageFlowFunction expired = new LongAverageFlowFunction();
        expired.aggregateLong(100L);

        total.deduct(expired);

        assertEquals(0L, total.getAsLong());
    }

    @Test
    public void doubleAverageDeductToEmptyWindowReturnsZero() {
        DoubleAverageFlowFunction total = new DoubleAverageFlowFunction();
        total.aggregateDouble(100.0);

        DoubleAverageFlowFunction expired = new DoubleAverageFlowFunction();
        expired.aggregateDouble(100.0);

        total.deduct(expired);

        assertEquals(0.0, total.getAsDouble(), 0.0);
    }

    @Test
    public void intAverageDeductPartialWindowKeepsAverage() {
        IntAverageFlowFunction total = new IntAverageFlowFunction();
        total.aggregateInt(100);
        total.aggregateInt(300);

        IntAverageFlowFunction expired = new IntAverageFlowFunction();
        expired.aggregateInt(100);

        total.deduct(expired);

        assertEquals(300, total.getAsInt());
    }
}
