/*
 * Copyright (c) 2025 gregory higgins.
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program.  If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

package com.telamin.fluxtion.builder.stream;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest;
import com.telamin.fluxtion.builder.test.util.SepTestConfig;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
import org.junit.Assert;
import org.junit.Test;

public class SlidingWindowTest extends MultipleSepTargetInProcessTest {

    public SlidingWindowTest(SepTestConfig testConfig) {
        super(testConfig);
    }


    @Test
    public void testSizedSlidingWindow() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .slidingAggregateByCount(Aggregates.intSumFactory(), 5)
                .id("sum")
        );

        int[] expected = new int[]{0, 0, 0, 0, 10, 15, 20, 25, 30, 35};

        for (int i = 0; i < 10; i++) {
            onEvent(i);
            if (i < 4) {
                Assert.assertNull(getStreamed("sum", Integer.class));
            } else {
                Assert.assertEquals(expected[i], getStreamed("sum", Integer.class).intValue());
            }
        }
    }

    @Test
    public void testSizedSlidingIntWindow() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .mapToInt(Integer::intValue)
                .slidingAggregateByCount(Aggregates.intSumFactory(), 5)
                .id("sum")
        );

        int[] expected = new int[]{0, 0, 0, 0, 10, 15, 20, 25, 30, 35};

        for (int i = 0; i < 10; i++) {
            onEvent(i);
            Assert.assertEquals(expected[i], getStreamed("sum", Integer.class).intValue());
        }
    }

    @Test
    public void testSizedSlidingDoubleWindow() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .mapToDouble(Integer::doubleValue)
                .slidingAggregateByCount(Aggregates.doubleSumFactory(), 5)
                .id("sum")
        );

        double[] expected = new double[]{0, 0, 0, 0, 10, 15, 20, 25, 30, 35};

        for (int i = 0; i < 10; i++) {
            onEvent(i);
            Assert.assertEquals(expected[i], getStreamed("sum", Double.class).doubleValue(), 0.00001);
        }
    }

    @Test
    public void testSizedSlidingLongWindow() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .mapToLong(Integer::longValue)
                .slidingAggregateByCount(Aggregates.longSumFactory(), 5)
                .id("sum")
        );
        long[] expected = new long[]{0, 0, 0, 0, 10, 15, 20, 25, 30, 35};

        for (int i = 0; i < 10; i++) {
            onEvent(i);
            Assert.assertEquals(expected[i], getStreamed("sum", Long.class).longValue());
        }
    }

    @Test
    public void slidingByTimeTest() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .slidingAggregate(Aggregates.intSumFactory(), 100, 2)
                .id("sum"));

        startTime(0);

        setTime(50);
        onEvent(30);
        setTime(80);
        onEvent(50);
        Assert.assertNull(getStreamed("sum", Integer.class));

        setTime(150);
        onEvent(60);
        Assert.assertNull(getStreamed("sum", Integer.class));


        setTime(230);
        onEvent(70);
        Assert.assertEquals(140, getStreamed("sum", Integer.class).intValue());

        setTime(350);
        onEvent(90);
        Assert.assertEquals(130, getStreamed("sum", Integer.class).intValue());

        tick(550);
        Assert.assertEquals(90, getStreamed("sum", Integer.class).intValue());

        onEvent(888888888);
        Assert.assertEquals(90, getStreamed("sum", Integer.class).intValue());

        setTime(910);
        onEvent(125);
        Assert.assertEquals(0, getStreamed("sum", Integer.class).intValue());


        setTime(1155);
        onEvent(500);
        Assert.assertEquals(125, getStreamed("sum", Integer.class).intValue());

        tick(1199);
        Assert.assertEquals(125, getStreamed("sum", Integer.class).intValue());

        tick(1200);
        Assert.assertEquals(500, getStreamed("sum", Integer.class).intValue());
    }

    @Test
    public void testIntSlidingTimeWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .slidingAggregate(Aggregates.intAverageFactory(), 100, 2)
                .id("avg"));

        startTime(0);
        tick(50);
        onEvent(10);

        tick(80);
        onEvent(20);
        Assert.assertNull(getStreamed("avg", Integer.class));

        tick(120);
        onEvent(30);
        Assert.assertNull(getStreamed("avg", Integer.class));

        tick(230);
        onEvent(40);
        Assert.assertEquals(20.0, getStreamed("avg", Integer.class), 0.001);

        tick(350);
        Assert.assertEquals(35.0, getStreamed("avg", Integer.class), 0.001);

        onEvent(50);
        Assert.assertEquals(35.0, getStreamed("avg", Integer.class), 0.001);

        tick(375);
        onEvent(60);
        Assert.assertEquals(35.0, getStreamed("avg", Integer.class), 0.001);

        tick(400);
        Assert.assertEquals(50.0, getStreamed("avg", Integer.class), 0.001);
    }

    @Test
    public void testIntSlidingCountWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Integer.class)
                .slidingAggregateByCount(Aggregates.intAverageFactory(), 3)
                .id("avg"));

        onEvent(10);
        Assert.assertNull(getStreamed("avg", Integer.class));

        onEvent(20);
        Assert.assertNull(getStreamed("avg", Integer.class));

        onEvent(30);
        Assert.assertEquals(20.0, getStreamed("avg", Integer.class), 0.001);

        onEvent(40);
        Assert.assertEquals(30.0, getStreamed("avg", Integer.class), 0.001);

        onEvent(50);
        Assert.assertEquals(40.0, getStreamed("avg", Integer.class), 0.001);
    }

    @Test
    public void testDoubleSlidingTimeWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Double.class)
                .slidingAggregate(Aggregates.doubleAverageFactory(), 100, 2)
                .id("avg"));

        startTime(0);
        tick(50);
        onEvent(10d);

        tick(80);
        onEvent(20d);
        Assert.assertNull(getStreamed("avg", Double.class));

        tick(150);
        onEvent(30d);
        Assert.assertNull(getStreamed("avg", Double.class));

        tick(230);
        onEvent(40d);
        Assert.assertEquals(20.0, getStreamed("avg", Double.class), 0.001);

        tick(350);
        Assert.assertEquals(35.0, getStreamed("avg", Double.class), 0.001);

        onEvent(50d);
        Assert.assertEquals(35.0, getStreamed("avg", Double.class), 0.001);

        tick(375);
        onEvent(60d);
        Assert.assertEquals(35.0, getStreamed("avg", Double.class), 0.001);

        tick(400);
        Assert.assertEquals(50.0, getStreamed("avg", Double.class), 0.001);
    }

    @Test
    public void testDoubleSlidingCountWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Double.class)
                .slidingAggregateByCount(Aggregates.doubleAverageFactory(), 3)
                .id("avg"));

        onEvent(10d);
        Assert.assertNull(getStreamed("avg", Double.class));

        onEvent(20d);
        Assert.assertNull(getStreamed("avg", Double.class));

        onEvent(30d);
        Assert.assertEquals(20.0, getStreamed("avg", Double.class), 0.001);

        onEvent(40d);
        Assert.assertEquals(30.0, getStreamed("avg", Double.class), 0.001);

        onEvent(50d);
        Assert.assertEquals(40.0, getStreamed("avg", Double.class), 0.001);
    }

    @Test
    public void testLongeSlidingTimeWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Long.class)
                .slidingAggregate(Aggregates.longAverageFactory(), 100, 2)
                .id("avg"));

        startTime(0);
        tick(50);
        onEvent(10L);

        tick(80);
        onEvent(20L);
        Assert.assertNull(getStreamed("avg", Long.class));

        tick(150);
        onEvent(30L);
        Assert.assertNull(getStreamed("avg", Long.class));

        tick(230);
        onEvent(40L);
        Assert.assertEquals(20.0, getStreamed("avg", Long.class), 0.001);

        tick(350);
        Assert.assertEquals(35.0, getStreamed("avg", Long.class), 0.001);

        onEvent(50L);
        Assert.assertEquals(35.0, getStreamed("avg", Long.class), 0.001);

        tick(375);
        onEvent(60L);
        Assert.assertEquals(35.0, getStreamed("avg", Long.class), 0.001);

        tick(400);
        Assert.assertEquals(50.0, getStreamed("avg", Long.class), 0.001);
    }

    @Test
    public void testLongSlidingCountWindowAverage() {
        sep(c -> DataFlowBuilder
                .subscribe(Long.class)
                .slidingAggregateByCount(Aggregates.longAverageFactory(), 3)
                .id("avg"));

        onEvent(10L);
        Assert.assertNull(getStreamed("avg", Long.class));

        onEvent(20L);
        Assert.assertNull(getStreamed("avg", Long.class));

        onEvent(30L);
        Assert.assertEquals(20.0, getStreamed("avg", Long.class), 0.001);

        onEvent(40L);
        Assert.assertEquals(30.0, getStreamed("avg", Long.class), 0.001);

        onEvent(50L);
        Assert.assertEquals(40.0, getStreamed("avg", Long.class), 0.001);
    }
}
