
# DataFlow functional GroupBy DSL
---

Fluxtion dsl offers many groupBy operations that partition based on a key function and then apply and aggregate operation
to the partition.

## GroupBy core functions

### GroupBy and aggregate

See sample - [GroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupBySample.java)

```java
public class GroupBySample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder
                .subscribe(ResetList.class)
                .console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder
                .subscribe(Integer.class)
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


### GroupBy to list

Collect items in group to a list with this call.

`groupByToList(i -> i % 2 == 0 ? "evens" : "odds")`

This is shorthand for:

`.groupBy(i -> i % 2 == 0 ? "evens" : "odds", Collectors.listFactory())`

See sample - [GroupByToListSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByToListSample.java)

```java
public class GroupByToListSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder
                .subscribe(ResetList.class)
                .console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder
                .subscribe(Integer.class)
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


### GroupBy to set

See sample - [GroupByToSetSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByToSetSample.java)

```java
public class GroupByToSetSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.
                subscribe(ResetList.class)
                .console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder
                .subscribe(Integer.class)
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

### GroupBy with compound key

See sample - [GroupByFieldsSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByFieldsSample.java)

```java
public class GroupByFieldsSample {

    public record Pupil(int year, String sex, String name) {}

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribe(Pupil.class)
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
        DataFlow processor = DataFlowBuilder
                .groupByToList(Pupil::year)
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

### Dataflow shortcut groupBy methods

The DataFlow class offers a set of shortcut methods for groupBy functions that do not require the 
subscription method to be declared as it is called implicitly. Some examples below

| shortcut method                                       | Full method                                                                     |
|-------------------------------------------------------|---------------------------------------------------------------------------------|
| `DataFlow.groupByFields(Function<T, ?>... accessors)` | `DataFlow.subscribe(Class<T> clazz).groupByFields(Function<T, ?>... accessors)` |
| `DataFlow.groupByToList(Function<T, ?>... accessors)` | `DataFlow.subscribe(Class<T> clazz).groupByToList(Function<T, ?>... accessors)` |
| `DataFlow.groupByToSet(Function<T, ?>... accessors)`  | `DataFlow.subscribe(Class<T> clazz).groupByToSet(Function<T, ?>... accessors)`  |
