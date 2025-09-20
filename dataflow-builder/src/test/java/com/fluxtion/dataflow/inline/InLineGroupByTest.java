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

package com.fluxtion.dataflow.inline;

import com.fluxtion.dataflow.builder.DataFlowBuilder;
import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupBy;
import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupByKey;
import lombok.SneakyThrows;
import lombok.Value;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;

public class InLineGroupByTest {

    @SneakyThrows
    @Test
    public void groupByCompoundKeyIdentityTest() {
        Map<GroupByKey<Data>, Data> expected = new HashMap<>();
        DataFlow processor = DataFlowBuilder
                .groupByFields(Data::getName, Data::getValue)
                .map(GroupBy::toMap)
                .id("results")
                .build();

        Data data_A_25 = new Data("A", 25);
        Data data_A_50 = new Data("A", 50);
        processor.onEvent(data_A_25);
        processor.onEvent(data_A_50);

        GroupByKey<Data> key = new GroupByKey<>(Data::getName, Data::getValue);

        expected.put(key.toKey(data_A_25), new Data("A", 25));
        expected.put(key.toKey(data_A_50), new Data("A", 50));
        Map<GroupByKey<Data>, Data> actual = processor.getStreamed("results");
        MatcherAssert.assertThat(actual, is(expected));

        Data data_A_10 = new Data("A", 10);
        Data data_B_11 = new Data("B", 11);
        processor.onEvent(data_A_10);
        processor.onEvent(data_B_11);

        expected.put(key.toKey(data_A_10), new Data("A", 10));
        expected.put(key.toKey(data_B_11), new Data("B", 11));
        expected.put(key.toKey(data_A_25), new Data("A", 25));
        expected.put(key.toKey(data_A_50), new Data("A", 50));

        MatcherAssert.assertThat(actual, is(expected));
    }

    @Value
    public static class Data {
        String name;
        int value;
    }
}
