# GroupBy map/reduce
--- 
Fluxtion offers extended methods for manipulating a GroupBy instance with map and reduce style semantics. Both keys 
and values can be mapped and reduced.

### Mapping keys
Keys of GroupBy can be mapped with

`mapKeys(Function<KEY_OLD, KEY_NEW> keyMappingFunction)`


See sample - [GroupByMapKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByMapKeySample.java)

```java
public class GroupByMapKeySample {
    public record Pupil(int year, String sex, String name) {}

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribe(Pupil.class)
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


### Mapping values
Values of GroupBy can be mapped with

`mapValues(Function<VALUE_OLD, VALUE_NEW> valueMappingFunction)`


See sample - [GroupByMapValuesSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByMapValuesSample.java)

```java
public class GroupByMapValuesSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder
                .subscribe(Integer.class)
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

### Reducing values
All the values of GroupBy can be reduced to a single value

`reduceValues(Supplier<AggregateFlowFunction> aggregateFactory)`

All the values are passed to the aggregate function and the single scalar output is published for downstream nodes to
consume.

See sample - [GroupByReduceSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByReduceSample.java)

```java
public class GroupByReduceSample {
    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribe(Integer.class)
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
