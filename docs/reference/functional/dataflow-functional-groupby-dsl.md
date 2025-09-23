
# DataFlow functional GroupBy DSL
---

Fluxtion dsl offers many groupBy operations that partition based on a key function and then apply and aggregate operation
to the partition.

## GroupBy core functions

### GroupBy and aggregate

```java
public class GroupBySample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder.subscribe(Integer.class)
                .groupBy(i -> i % 2 == 0 ? "evens" : "odds", Aggregates.countFactory())
                .resetTrigger(resetSignal)
                .map(GroupBy::toMap)
                .console("ODD/EVEN map:{}")
                .build();

        processor.onEvent(1);
        processor.onEvent(2);

        processor.onEvent(new ResetList());
        processor.onEvent(5);
        processor.onEvent(7);

        processor.onEvent(new ResetList());
        processor.onEvent(2);
    }
}
```

Running the example code above logs to console

```console
ODD/EVEN map:{odds=1}
ODD/EVEN map:{odds=1, evens=1}

--- RESET ---
ODD/EVEN map:{}
ODD/EVEN map:{odds=1}
ODD/EVEN map:{odds=2}

--- RESET ---
ODD/EVEN map:{}
ODD/EVEN map:{evens=1}
```


###GroupBy to list

Collect items in group to a list with this call.

`groupByToList(i -> i % 2 == 0 ? "evens" : "odds")`

This is shorthand for:

`.groupBy(i -> i % 2 == 0 ? "evens" : "odds", Collectors.listFactory())`

```java
public class GroupByToListSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder.subscribe(Integer.class)
                .groupByToList(i -> i % 2 == 0 ? "evens" : "odds")
                .resetTrigger(resetSignal)
                .map(GroupBy::toMap)
                .console("ODD/EVEN map:{}")
                .build();

        processor.onEvent(1);
        processor.onEvent(2);
        processor.onEvent(5);
        processor.onEvent(7);
        processor.onEvent(2);
        processor.onEvent(new ResetList());
    }
}
```

Running the example code above logs to console

```console
ODD/EVEN map:{odds=[1]}
ODD/EVEN map:{odds=[1], evens=[2]}
ODD/EVEN map:{odds=[1, 5], evens=[2]}
ODD/EVEN map:{odds=[1, 5, 7], evens=[2]}
ODD/EVEN map:{odds=[1, 5, 7], evens=[2, 2]}

--- RESET ---
ODD/EVEN map:{}
```


###GroupBy to set

```java
public class GroupByToSetSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder.subscribe(Integer.class)
                .groupByToSet(i -> i % 2 == 0 ? "evens" : "odds")
                .resetTrigger(resetSignal)
                .map(GroupBy::toMap)
                .console("ODD/EVEN map:{}")
                .build();

        processor.onEvent(1);
        processor.onEvent(2);
        processor.onEvent(2);
        processor.onEvent(5);
        processor.onEvent(5);
        processor.onEvent(5);
        processor.onEvent(7);
        processor.onEvent(2);
        processor.onEvent(new ResetList());
    }
}
```

Running the example code above logs to console

```console
ODD/EVEN map:{odds=[1]}
ODD/EVEN map:{odds=[1], evens=[2]}
ODD/EVEN map:{odds=[1], evens=[2]}
ODD/EVEN map:{odds=[1, 5], evens=[2]}
ODD/EVEN map:{odds=[1, 5], evens=[2]}
ODD/EVEN map:{odds=[1, 5], evens=[2]}
ODD/EVEN map:{odds=[1, 5, 7], evens=[2]}
ODD/EVEN map:{odds=[1, 5, 7], evens=[2]}

--- RESET ---
ODD/EVEN map:{}
```

###GroupBy with compound key

```java
public class GroupByFieldsSample {

    public record Pupil(int year, String sex, String name) {}

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder.subscribe(Pupil.class)
                .groupByFieldsAggregate(Aggregates.countFactory(), Pupil::year, Pupil::sex)
                .map(GroupByFieldsSample::formatGroupBy)
                .console("Pupil count by year/sex \n----\n{}----\n")
                .build();

        processor.onEvent(new Pupil(2015, "Female", "Bob"));
        processor.onEvent(new Pupil(2013, "Male", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Male", "Channing"));
        processor.onEvent(new Pupil(2013, "Female", "Chelsea"));
        processor.onEvent(new Pupil(2013, "Female", "Tamsin"));
        processor.onEvent(new Pupil(2013, "Female", "Ayola"));
        processor.onEvent(new Pupil(2015, "Female", "Sunita"));
    }

    private static String formatGroupBy(GroupBy<GroupByKey<Pupil>, Integer> groupBy) {
        Map<GroupByKey<Pupil>, Integer> groupByMap = groupBy.toMap();
        StringBuilder stringBuilder = new StringBuilder();
        groupByMap.forEach((k, v) -> stringBuilder.append(k.getKey() + ": " + v + "\n"));
        return stringBuilder.toString();
    }
}
```

Running the example code above logs to console

```console
Pupil count by year/sex
----
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 1
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 2
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 2
2013_Female_: 1
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 2
2013_Female_: 2
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 2
2013_Female_: 3
2015_Female_: 1
----

Pupil count by year/sex
----
2013_Male_: 2
2013_Female_: 3
2015_Female_: 2
----
```

###Delete elements
Elements can be deleted from a groupBy data structure either by key or by value. When deleting bt value a stateful predicate
function is used that can be dynamically updated by the client code. Unlike filtering the groupBy data structure is 
mutated and elements are removed. 

In this example we are grouping pupils by graduation year, a delete by value predicate function removes students if 
there gradutaion year is too old. The predicate is subscribing to live data, so when it updates the elements in the 
collection are removed.

```java
public class GroupByDeleteSample {

    public record Pupil(long pupilId, int year, String name) {}

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder.groupByToList(Pupil::year)
                .deleteByValue(new DeleteFilter()::leftSchool)
                .map(GroupBy::toMap)
                .console()
                .build();

        processor.onEvent(new Pupil(1, 2025, "A"));
        processor.onEvent(new Pupil(2, 2025, "B"));
        processor.onEvent(new Pupil(3, 2022, "A_2022"));
        processor.onEvent(new Pupil(1, 2021, "A_2021"));

        //graduate
        System.out.println("\ngraduate 2021");
        processor.onEvent(2022);

        System.out.println("\ngraduate 2022");
        processor.onEvent(2022);

        System.out.println("\ngraduate 2023");
        processor.onEvent(2023);
    }

    public static class DeleteFilter {

        private int currentGraduationYear = Integer.MIN_VALUE;

        @OnEventHandler
        public boolean currentGraduationYear(int currentGraduationYear) {
            this.currentGraduationYear = currentGraduationYear;
            return true;
        }

        public boolean leftSchool(List<Pupil> pupil) {
            return !pupil.isEmpty() && pupil.getFirst().year() < this.currentGraduationYear;
        }
    }
}
```

Running the example code above logs to console

```console

{2025=[Pupil[pupilId=1, year=2025, name=A]]}
{2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}
{2022=[Pupil[pupilId=3, year=2022, name=A_2022]], 2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}
{2021=[Pupil[pupilId=1, year=2021, name=A_2021]], 2022=[Pupil[pupilId=3, year=2022, name=A_2022]], 2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}

graduate 2021
{2022=[Pupil[pupilId=3, year=2022, name=A_2022]], 2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}

graduate 2022
{2022=[Pupil[pupilId=3, year=2022, name=A_2022]], 2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}

graduate 2023
{2025=[Pupil[pupilId=1, year=2025, name=A], Pupil[pupilId=2, year=2025, name=B]]}
```

###Dataflow shortcut groupBy methods

The DataFlow class offers a set of shortcut methods for groupBy functions that do not require the 
subscription method to be declared as it is called implicitly. Some examples below

| shortcut method                                       | Full method                                                                     |
|-------------------------------------------------------|---------------------------------------------------------------------------------|
| `DataFlow.groupByFields(Function<T, ?>... accessors)` | `DataFlow.subscribe(Class<T> clazz).groupByFields(Function<T, ?>... accessors)` |
| `DataFlow.groupByToList(Function<T, ?>... accessors)` | `DataFlow.subscribe(Class<T> clazz).groupByToList(Function<T, ?>... accessors)` |
| `DataFlow.groupByToSet(Function<T, ?>... accessors)`  | `DataFlow.subscribe(Class<T> clazz).groupByToSet(Function<T, ?>... accessors)`  |


## Windowed GroupBy
--- 

###Tumbling GroupBy

```java
public class TumblingGroupBySample {
    public record Trade(String symbol, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder.subscribe(Trade.class)
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


###Sliding GroupBy

```java
public class SlidingGroupBySample {
    public record Trade(String symbol, int amountTraded) {}

    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder.subscribe(Trade.class)
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


###Tumbling GroupBy with compound key

```java
public class TumblingGroupByCompoundKeySample {
    public record Trade(String symbol, String client, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};
    private static String[] clients = new String[]{"client_A", "client_B", "client_D", "client_E"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder.subscribe(Trade.class)
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


###Sliding GroupBy with compound key

```java
public class SlidingGroupByCompoundKeySample {
    public record Trade(String symbol, String client, int amountTraded) {}
    private static String[] symbols = new String[]{"GOOG", "AMZN", "MSFT", "TKM"};
    private static String[] clients = new String[]{"client_A", "client_B", "client_D", "client_E"};

    public static void main(String[] args) throws InterruptedException {
        DataFlow processor = DataFlowBuilder.subscribe(Trade.class)
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

## GroupBy functional support
--- 
Fluxtion offers extended methods for manipulating a GroupBy instance of DataFlow node

###Mapping keys
Keys of GroupBy can be mapped with

`mapKeys(Function<KEY_OLD, KEY_NEW> keyMappingFunction)`


```java
public class GroupByMapKeySample {
    public record Pupil(int year, String sex, String name) {}

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder.subscribe(Pupil.class)
                .groupByFieldsAggregate(Aggregates.countFactory(), Pupil::year, Pupil::sex)
                .mapKeys(GroupByKey::getKey)//MAPS KEYS
                .map(GroupBy::toMap)
                .console("{}")
                .build();

        processor.onEvent(new Pupil(2015, "Female", "Bob"));
        processor.onEvent(new Pupil(2013, "Male", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Male", "Channing"));
        processor.onEvent(new Pupil(2013, "Female", "Chelsea"));
        processor.onEvent(new Pupil(2013, "Female", "Tamsin"));
        processor.onEvent(new Pupil(2013, "Female", "Ayola"));
        processor.onEvent(new Pupil(2015, "Female", "Sunita"));
    }
}
```

Running the example code above logs to console

```console
{2015_Female_=1}
{2013_Male_=1, 2015_Female_=1}
{2013_Male_=2, 2015_Female_=1}
{2013_Male_=2, 2013_Female_=1, 2015_Female_=1}
{2013_Male_=2, 2013_Female_=2, 2015_Female_=1}
{2013_Male_=2, 2013_Female_=3, 2015_Female_=1}
{2013_Male_=2, 2013_Female_=3, 2015_Female_=2}
```


###Mapping values
Values of GroupBy can be mapped with

`mapValues(Function<VALUE_OLD, VALUE_NEW> valueMappingFunction)`


```java
public class GroupByMapValuesSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder.subscribe(Integer.class)
                .groupByToSet(i -> i % 2 == 0 ? "evens" : "odds")
                .resetTrigger(resetSignal)
                .mapValues(GroupByMapValuesSample::toRange)//MAPS VALUES
                .map(GroupBy::toMap)
                .console("ODD/EVEN map:{}")
                .build();

        processor.init();
        processor.onEvent(1);
        processor.onEvent(2);
        processor.onEvent(2);
        processor.onEvent(5);
        processor.onEvent(5);
        processor.onEvent(5);
        processor.onEvent(7);
        processor.onEvent(2);
        processor.onEvent(new ResetList());
    }

    private static String toRange(Set<Integer> integers) {
        int max = integers.stream().max(Integer::compareTo).get();
        int min = integers.stream().min(Integer::compareTo).get();
        return "range [" + min + "," + max + "]";
    }
}
```

Running the example code above logs to console

```console
ODD/EVEN map:{odds=range [1,1]}
ODD/EVEN map:{odds=range [1,1], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,1], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,5], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,5], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,5], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,7], evens=range [2,2]}
ODD/EVEN map:{odds=range [1,7], evens=range [2,2]}

--- RESET ---
ODD/EVEN map:{}
```

###Reducing values
All the values of GroupBy can be reduced to a single value

`reduceValues(Supplier<AggregateFlowFunction> aggregateFactory)`

All the values are passed to the aggregate function and the single scalar output is published for downstream nodes to
consume.

```java
public class GroupByReduceSample {
    public static void main(String[] args) {
        var processor = DataFlowBuilder.subscribe(Integer.class)
                .groupBy(i -> i % 2 == 0 ? "evens" : "odds", Aggregates.intSumFactory())
                .console("ODD/EVEN sum:{}")
                .reduceValues(Aggregates.intSumFactory())
                .console("REDUCED sum:{}\n")
                .build();

        processor.onEvent(1);
        processor.onEvent(2);
        processor.onEvent(5);
        processor.onEvent(7);
        processor.onEvent(2);
    }
}
```

Running the example code above logs to console

```console
ODD/EVEN sum:GroupByFlowFunctionWrapper{mapOfValues={odds=1}}
REDUCED sum:1

ODD/EVEN sum:GroupByFlowFunctionWrapper{mapOfValues={odds=1, evens=2}}
REDUCED sum:3

ODD/EVEN sum:GroupByFlowFunctionWrapper{mapOfValues={odds=6, evens=2}}
REDUCED sum:8

ODD/EVEN sum:GroupByFlowFunctionWrapper{mapOfValues={odds=13, evens=2}}
REDUCED sum:15

ODD/EVEN sum:GroupByFlowFunctionWrapper{mapOfValues={odds=13, evens=4}}
REDUCED sum:17
```


## Joining
--- 

Fluxtion supports join operations for groupBy data flow nodes.

###Inner join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.innerJoin(schools, pupils)`

The value type of the joined GroupBy is a Tuple, the first value is the left join and the second value is the right join.
The utility static method in [Tuples]({{fluxtion_src_runtime}}/dataflow/helpers/Tuples.java)

`Tuples.mapTuple`

Is used to map the School, Pupil Tuple into a pretty print String.

```java
public class GroupByJoinSample {
    public record Pupil(int year, String school, String name) {}
    public record School(String name) {}

    public static void main(String[] args) {
        var pupils = DataFlowBuilder.subscribe(Pupil.class).groupByToList(Pupil::school);
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);

        DataFlow processor = JoinFlowBuilder.innerJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));
        processor.onEvent(new Pupil(2013, "RGS", "Chelsea"));
        processor.onEvent(new Pupil(2013, "Belles", "Tamsin"));
        processor.onEvent(new Pupil(2013, "Belles", "Ayola"));
        processor.onEvent(new Pupil(2015, "Belles", "Sunita"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[Bob]}
{RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola,Sunita], RGS=pupils[Bob,Ashkay,Chelsea]}
```

###Left outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.leftJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

```java
public class GroupByLeftOuterJoinSample {
    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        var pupils = DataFlowBuilder.subscribe(Pupil.class)
                .groupByToList(Pupil::school)
                .defaultValue(GroupBy.emptyCollection());

        DataFlow processor = JoinFlowBuilder.leftJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByLeftOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));
        processor.onEvent(new Pupil(2015, "Belles", "Sunita"));

        System.out.println("left outer join\n");
        //left outer
        processor.onEvent(new School("Framling"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[]}
{Belles=pupils[], RGS=pupils[]}
{Belles=pupils[], RGS=pupils[Bob]}
{Belles=pupils[], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing,Sunita], RGS=pupils[Bob,Ashkay]}
left outer join

{Belles=pupils[Channing,Sunita], RGS=pupils[Bob,Ashkay], Framling=pupils[]}
```

###right outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.rightJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

```java
public class GroupByRightOuterJoinSample {
    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        var pupils = DataFlowBuilder.subscribe(Pupil.class).groupByToList(Pupil::school);

        DataFlow processor = JoinFlowBuilder.rightJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByRightOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));

        System.out.println("right outer join\n");
        //left outer
        processor.onEvent(new Pupil(2015, "Framling", "Sunita"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[Bob]}
{RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
right outer join

{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
```


###Full outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.outerJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

```java
public class GroupByFullOuterJoinSample {

    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        var pupils = DataFlowBuilder.subscribe(Pupil.class).groupByToList(Pupil::school);

        DataFlow processor = JoinFlowBuilder.outerJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByFullOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));

        System.out.println("full outer join\n");
        //full outer
        processor.onEvent(new Pupil(2015, "Framling", "Sunita"));
        processor.onEvent(new School("St trinians"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
07-May-24 21:31:33 [main] INFO GenerationContext - classloader:jdk.internal.loader.ClassLoaders$AppClassLoader@4e0e2f2a
{Belles=pupils[], RGS=pupils[Bob]}
{Belles=pupils[], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
full outer join

{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
{Belles=pupils[Channing], St trinians=pupils[], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
```

###Multi join or Co-group

Multi leg joins are supported with no limitation on the number of joins, The [MultiJoinBuilder]({{fluxtion_src_compiler}}/builder/dataflow/MultiJoinBuilder.java)
is used to construct a multi leg join with a builder style pattern

`MultiJoinBuilder.builder(Class<K> keyClass, Supplier<T> target`

Legs are joined on a common key class results are sent to target class. Each join is added from a flow and pushed into
the target class by specifying the consumer method on the target instance.

`[multijoinbuilder].addJoin(GroupByFlowBuilder<K2, B> flow, BiConsumer<T, B> setter)`

An optional join can be specified. The optional will be null in the target instance until a key match is found

`[multijoinbuilder].addOptionalJoin(GroupByFlowBuilder<K2, B> flow, BiConsumer<T, B> setter)`

The GroupBy data flow is created by calling

`[multijoinbuilder].dataFlow()`

The example joins four groupBy data flows for a person, using the String name as a key. When a matching join is found
individual item are set on MergedData instance. Dependents are an optional requirement for the join, so is not required
to publish a MergedData record to the flow. 

The MergedData instance is added to the GroupBy data flow keyed by name. The multi join data flow can be operated on 
as any normal flow, in this case we are mapping the value with a 
pretty printing function.

```java
public class MultiJoinSample {

    public static void main(String[] args) {
        var ageDataFlow = DataFlowBuilder.groupBy(Age::getName);
        var genderDataFlow = DataFlowBuilder.groupBy(Gender::getName);
        var nationalityDataFlow = DataFlowBuilder.groupBy(Nationality::getName);
        var dependentDataFlow = DataFlowBuilder.groupByToList(Dependent::getGuardianName);

        DataFlow processor = MultiJoinBuilder.builder(String.class, MergedData::new)
                .addJoin(ageDataFlow, MergedData::setAge)
                .addJoin(genderDataFlow, MergedData::setGender)
                .addJoin(nationalityDataFlow, MergedData::setNationality)
                .addOptionalJoin(dependentDataFlow, MergedData::setDependent)
                .dataFlow()
                .mapValues(MergedData::formattedString)
                .map(GroupBy::toMap)
                .console("multi join result : {}")
                .build();


        processor.onEvent(new Age("greg", 47));
        processor.onEvent(new Gender("greg", "male"));
        processor.onEvent(new Nationality("greg", "UK"));
        //update
        processor.onEvent(new Age("greg", 55));
        //new record
        processor.onEvent(new Age("tim", 47));
        processor.onEvent(new Gender("tim", "male"));
        processor.onEvent(new Nationality("tim", "UK"));

        processor.onEvent(new Dependent("greg", "ajay"));
        processor.onEvent(new Dependent("greg", "sammy"));

    }

    @Data
    public static class MergedData {
        private Age age;
        private Gender gender;
        private Nationality nationality;
        private List<Dependent> dependent;

        public String formattedString() {
            String dependentString = " no dependents";
            if (dependent != null) {
                dependentString = dependent.stream()
                        .map(Dependent::getDependentName)
                        .collect(Collectors.joining(", ", " guardian for: [", "]"));
            }
            return age.getAge() + " " + gender.getSex() + " " + nationality.getCountry() + dependentString;
        }
    }

    @Value
    public static class Age {
        String name;
        int age;
    }

    @Value
    public static class Gender {
        String name;
        String sex;
    }


    @Value
    public static class Nationality {
        String name;
        String country;
    }

    @Value
    public static class Dependent {
        String guardianName;
        String dependentName;
    }
}
```

Running the example code above logs to console

```console
multi join result : {greg=47 male UK no dependents}
multi join result : {greg=55 male UK no dependents}
multi join result : {tim=47 male UK no dependents, greg=55 male UK no dependents}
multi join result : {tim=47 male UK no dependents, greg=55 male UK guardian for: [ajay]}
multi join result : {tim=47 male UK no dependents, greg=55 male UK guardian for: [ajay, sammy]}
```
