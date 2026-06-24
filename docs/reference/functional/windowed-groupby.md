# GroupBy window and aggregation support
--- 

### Tumbling GroupBy

See sample - [TumblingGroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/TumblingGroupBySample.java)

```java
public class TumblingGroupBySample {
    public record Trade(String symbol, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder
                .subscribe(Trade.class)
                .groupByTumbling(Trade::symbol, Trade::amountTraded, Aggregates.intSumFactory(), 250)
                .map(GroupBy::toMap)
                .console("Trade volume for last 250 millis:{} timeDelta:%dt")
                .build();

        Random rand = new Random();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(
                    () -> processor.onEvent(new Trade(symbols[rand.nextInt(symbols.length)], rand.nextInt(100))),
                    10, 10, TimeUnit.MILLISECONDS);
            Thread.sleep(4_000);
        }
    }
}
```

Running the example code above logs to console

```console
Trade volume for last 250 millis:{MSFT=364, GOOG=479, AMZN=243, TKM=219} timeDelta:256
Trade volume for last 250 millis:{MSFT=453, GOOG=426, AMZN=288, TKM=259} timeDelta:505
Trade volume for last 250 millis:{MSFT=341, GOOG=317, AMZN=136, TKM=351} timeDelta:755
Trade volume for last 250 millis:{MSFT=569, GOOG=273, AMZN=168, TKM=297} timeDelta:1005
Trade volume for last 250 millis:{MSFT=219, GOOG=436, AMZN=233, TKM=588} timeDelta:1255
Trade volume for last 250 millis:{MSFT=138, GOOG=353, AMZN=296, TKM=382} timeDelta:1505
Trade volume for last 250 millis:{MSFT=227, GOOG=629, AMZN=271, TKM=202} timeDelta:1755
Trade volume for last 250 millis:{MSFT=315, GOOG=370, AMZN=252, TKM=254} timeDelta:2005
Trade volume for last 250 millis:{MSFT=247, GOOG=418, AMZN=336, TKM=275} timeDelta:2254
Trade volume for last 250 millis:{MSFT=314, GOOG=300, AMZN=218, TKM=367} timeDelta:2506
Trade volume for last 250 millis:{MSFT=354, GOOG=132, AMZN=339, TKM=724} timeDelta:2755
Trade volume for last 250 millis:{MSFT=504, GOOG=55, AMZN=548, TKM=243} timeDelta:3006
Trade volume for last 250 millis:{MSFT=348, GOOG=249, AMZN=392, TKM=340} timeDelta:3255
Trade volume for last 250 millis:{MSFT=216, GOOG=276, AMZN=551, TKM=264} timeDelta:3505
Trade volume for last 250 millis:{MSFT=350, GOOG=348, AMZN=196, TKM=228} timeDelta:3756
Trade volume for last 250 millis:{MSFT=263, GOOG=197, AMZN=411, TKM=373} timeDelta:4005
```


### Sliding GroupBy

See sample - [SlidingGroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/SlidingGroupBySample.java)

```java
public class SlidingGroupBySample {
    public record Trade(String symbol, int amountTraded) {}

    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder
                .subscribe(Trade.class)
                .groupBySliding(Trade::symbol, Trade::amountTraded, Aggregates.intSumFactory(), 250, 4)
                .map(GroupBy::toMap)
                .console("Trade volume for last second:{} timeDelta:%dt")
                .build();

        Random rand = new Random();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(
                    () -> processor.onEvent(new Trade(symbols[rand.nextInt(symbols.length)], rand.nextInt(100))),
                    10, 10, TimeUnit.MILLISECONDS);
            Thread.sleep(4_000);
        }
    }
}
```

Running the example code above logs to console

```console
Trade volume for last second:{MSFT=1458, GOOG=1127, AMZN=789, TKM=1433} timeDelta:1005
Trade volume for last second:{MSFT=1402, GOOG=1025, AMZN=893, TKM=1518} timeDelta:1255
Trade volume for last second:{MSFT=1290, GOOG=1249, AMZN=910, TKM=1278} timeDelta:1505
Trade volume for last second:{MSFT=1125, GOOG=1587, AMZN=1009, TKM=1208} timeDelta:1755
Trade volume for last second:{MSFT=996, GOOG=1487, AMZN=1268, TKM=1353} timeDelta:2005
Trade volume for last second:{MSFT=1016, GOOG=1512, AMZN=1165, TKM=1398} timeDelta:2254
Trade volume for last second:{MSFT=982, GOOG=1711, AMZN=1170, TKM=1388} timeDelta:2504
Trade volume for last second:{MSFT=1188, GOOG=1588, AMZN=931, TKM=1468} timeDelta:2754
Trade volume for last second:{MSFT=1201, GOOG=1757, AMZN=1082, TKM=1210} timeDelta:3005
Trade volume for last second:{MSFT=1375, GOOG=1723, AMZN=1244, TKM=815} timeDelta:3255
Trade volume for last second:{MSFT=1684, GOOG=1507, AMZN=1285, TKM=736} timeDelta:3505
Trade volume for last second:{MSFT=1361, GOOG=1423, AMZN=1466, TKM=811} timeDelta:3754
Trade volume for last second:{MSFT=1384, GOOG=1344, AMZN=1153, TKM=865} timeDelta:4005
```


### Tumbling GroupBy with compound key

See sample - [TumblingGroupByCompoundKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/TumblingGroupByCompoundKeySample.java)

```java
public class TumblingGroupByCompoundKeySample {
    public record Trade(String symbol, String client, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};
    private static String[] clients = new String[]{"client_A", "client_B", "client_D", "client_E"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder
                .subscribe(Trade.class)
                .groupByTumbling(
                        GroupByKey.build(Trade::client, Trade::symbol),
                        Trade::amountTraded,
                        Aggregates.intSumFactory(),
                        250)
                .map(TumblingGroupByCompoundKeySample::formatGroupBy)
                .console("Trade volume tumbling per 250 millis by client and symbol timeDelta:%dt:\n{}----------------------\n")
                .build();
        
        Random rand = new Random();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(
                    () -> processor.onEvent(new Trade(symbols[rand.nextInt(symbols.length)], clients[rand.nextInt(clients.length)], rand.nextInt(100))),
                    10, 10, TimeUnit.MILLISECONDS);
            Thread.sleep(4_000);
        }
    }

    private static <T> String formatGroupBy(GroupBy<GroupByKey<T>, Integer> groupBy) {
        Map<GroupByKey<T>, Integer> groupByMap = groupBy.toMap();
        StringBuilder stringBuilder = new StringBuilder();
        groupByMap.forEach((k, v) -> stringBuilder.append(k.getKey() + ": " + v + "\n"));
        return stringBuilder.toString();
    }
}
```

Running the example code above logs to console

```console
Trade volume tumbling per 250 millis by client and symbol timeDelta:258:
client_E_TKM_: 123
client_D_GOOG_: 106
client_E_AMZN_: 63
client_B_AMZN_: 83
client_D_AMZN_: 156
client_A_GOOG_: 2
client_B_GOOG_: 13
client_A_TKM_: 197
client_E_MSFT_: 95
client_B_MSFT_: 199
client_D_MSFT_: 7
client_A_MSFT_: 116
----------------------

Trade volume tumbling per 250 millis by client and symbol timeDelta:506:
client_B_TKM_: 73
client_E_AMZN_: 78
client_D_AMZN_: 60
client_E_TKM_: 85
client_A_AMZN_: 40
client_B_AMZN_: 104
client_D_TKM_: 103
client_A_GOOG_: 29
client_B_GOOG_: 42
client_E_MSFT_: 0
client_D_MSFT_: 193
client_B_MSFT_: 68
client_A_MSFT_: 60
----------------------

Trade volume tumbling per 250 millis by client and symbol timeDelta:754:
client_B_TKM_: 14
client_E_AMZN_: 73
client_D_AMZN_: 91
client_A_TKM_: 33
client_E_GOOG_: 56
client_E_TKM_: 194
client_D_GOOG_: 51
client_A_AMZN_: 148
client_B_AMZN_: 92
client_B_GOOG_: 143
client_E_MSFT_: 133
client_B_MSFT_: 45
client_D_MSFT_: 181
client_A_MSFT_: 65
----------------------
```


### Sliding GroupBy with compound key

See sample - [SlidingGroupByCompoundKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/SlidingGroupByCompoundKeySample.java)

```java
public class SlidingGroupByCompoundKeySample {
    public record Trade(String symbol, String client, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};
    private static String[] clients = new String[]{"client_A", "client_B", "client_D", "client_E"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder
                .subscribe(Trade.class)
                .groupBySliding(
                        GroupByKey.build(Trade::client, Trade::symbol),
                        Trade::amountTraded,
                        Aggregates.intSumFactory(),
                        250, 4)
                .map(SlidingGroupByCompoundKeySample::formatGroupBy)
                .console("Trade volume for last second by client and symbol timeDelta:%dt:\n{}----------------------\n")
                .build();

        Random rand = new Random();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(
                    () -> processor.onEvent(new Trade(symbols[rand.nextInt(symbols.length)], clients[rand.nextInt(clients.length)], rand.nextInt(100))),
                    10, 10, TimeUnit.MILLISECONDS);
            Thread.sleep(4_000);
        }
    }

    private static <T> String formatGroupBy(GroupBy<GroupByKey<T>, Integer> groupBy) {
        Map<GroupByKey<T>, Integer> groupByMap = groupBy.toMap();
        StringBuilder stringBuilder = new StringBuilder();
        groupByMap.forEach((k, v) -> stringBuilder.append(k.getKey() + ": " + v + "\n"));
        return stringBuilder.toString();
    }
}
```

Running the example code above logs to console

```console
Trade volume for last second by client and symbol timeDelta:1008:
client_B_TKM_: 184
client_E_AMZN_: 254
client_A_TKM_: 577
client_B_MSFT_: 432
client_A_GOOG_: 174
client_A_MSFT_: 111
client_B_GOOG_: 134
client_E_GOOG_: 392
client_D_GOOG_: 170
client_E_TKM_: 499
client_E_MSFT_: 526
client_D_MSFT_: 538
client_A_AMZN_: 179
client_B_AMZN_: 213
client_D_AMZN_: 274
client_D_TKM_: 329
----------------------

Trade volume for last second by client and symbol timeDelta:1256:
client_B_TKM_: 198
client_E_AMZN_: 123
client_A_TKM_: 544
client_B_MSFT_: 340
client_A_GOOG_: 174
client_A_MSFT_: 211
client_B_GOOG_: 96
client_E_GOOG_: 271
client_D_GOOG_: 164
client_E_TKM_: 531
client_E_MSFT_: 486
client_D_MSFT_: 477
client_A_AMZN_: 179
client_B_AMZN_: 478
client_D_AMZN_: 222
client_D_TKM_: 333
----------------------

Trade volume for last second by client and symbol timeDelta:1505:
client_B_TKM_: 259
client_E_AMZN_: 123
client_A_TKM_: 544
client_B_MSFT_: 238
client_A_GOOG_: 178
client_A_MSFT_: 267
client_B_GOOG_: 88
client_E_GOOG_: 280
client_D_GOOG_: 65
client_E_TKM_: 317
client_E_MSFT_: 576
client_D_MSFT_: 361
client_A_AMZN_: 215
client_B_AMZN_: 461
client_D_AMZN_: 197
client_D_TKM_: 305
----------------------
```
