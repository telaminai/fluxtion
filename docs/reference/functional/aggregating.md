# Aggregating
---
Aggregating extends the concept of stateful map functions by adding behaviour when using functions in stateful operations
like windowing and grouping. An aggregate function has these behaviours:

- Stateful - defines the reset method
- aggregate - aggregate a value and calculate a result
- combine/deduct - combine or deduct another instance of this function, used when windowing
- deduct supported - can this instance deduct another instance of this function or is loop required to recalculate

Create an aggregate in a DataFlow with the call:

`DataFlow.aggregate(Supplier<AggregateFlowFunction> aggregateSupplier)` 

DataFlow.aggregate takes a Supplier of [AggregateFlowFunction]({{fluxtion_src_runtime}}/dataflow/aggregate/AggregateFlowFunction.java)'s not a 
single AggregateFlowFunction instance. When managing windowing and groupBy operations the event processor creates instances 
of AggregateFlowFunction to partition function state.

```java
public class AggregateSample {
    public record ResetList() {}

    public static void main(String[] args) {
        var resetSignal = DataFlowBuilder.subscribe(ResetList.class).console("\n--- RESET ---");

        DataFlow processor = DataFlowBuilder
                .subscribe(String.class)
                .aggregate(Collectors.listFactory(3))
                .resetTrigger(resetSignal)
                .console("ROLLING list: {}").build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");
        processor.onEvent("E");

        processor.onEvent(new ResetList());
        processor.onEvent("P");
        processor.onEvent("Q");
        processor.onEvent("R");

        processor.onEvent(new ResetList());
        processor.onEvent("XX");
        processor.onEvent("YY");
    }
}
```

Running the example code above logs to console

```console
ROLLING list: [A]
ROLLING list: [A, B]
ROLLING list: [A, B, C]
ROLLING list: [B, C, D]
ROLLING list: [C, D, E]

--- RESET ---
ROLLING list: []
ROLLING list: [P]
ROLLING list: [P, Q]
ROLLING list: [P, Q, R]

--- RESET ---
ROLLING list: []
ROLLING list: [XX]
ROLLING list: [XX, YY]
```

## Custom aggregate function
Users can create aggregate functions that plug into the reset trigger callbacks in a DataFlow. The steps to create a
user aggregate function:

- Extend [AggregateFlowFunction]({{fluxtion_src_runtime}}/dataflow/aggregate/AggregateFlowFunction.java), the type parameters define the input and output types of the function
- Implement the reset, get and aggregate methods
- Return null from the aggregate method to indicate no change to the aggregate output

The example below maintains a date range as a String and resets the range when reset trigger is fired. When the date range
is unaltered the aggregate operation returns a null and no notifications are triggered.

```java
public class CustomAggregateFunctionSample {

    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder.subscribe(LocalDate.class)
                .aggregate(DateRangeAggregate::new)
                .resetTrigger(DataFlowBuilder.subscribeToSignal("resetDateRange"))
                .console("UPDATED date range : '{}'")
                .build();

        processor.onEvent(LocalDate.of(2019, 8, 10));
        processor.onEvent(LocalDate.of(2009, 6, 14));
        processor.onEvent(LocalDate.of(2024, 4, 22));
        processor.onEvent(LocalDate.of(2021, 3, 30));

        //reset
        processor.publishSignal("resetDateRange");
        processor.onEvent(LocalDate.of(2019, 8, 10));
        processor.onEvent(LocalDate.of(2021, 3, 30));
    }
}

public class DateRangeAggregate 
        implements AggregateFlowFunction<LocalDate, String, DateRangeAggregate> {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private String message;
    private final transient DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String reset() {
        System.out.println("--- RESET ---");
        startDate = null;
        endDate = null;
        message = null;
        return get();
    }

    @Override
    public String get() {
        return message;
    }

    @Override
    public String aggregate(LocalDate input) {
        startDate = startDate == null ? input : startDate;
        endDate = endDate == null ? input : endDate;
        if (input.isBefore(startDate)) {
            startDate = input;
        } else if (input.isAfter(endDate)) {
            endDate = input;
        } else {
            //RETURN NULL -> NO CHANGE NOTIFICATIONS FIRED
            return null;
        }
        message = formatter.format(startDate) + " - " + formatter.format(endDate);
        return message;
    }
}
```

Running the example code above logs to console

```console
UPDATED date range : '2009-06-14 - 2019-08-10'
UPDATED date range : '2009-06-14 - 2024-04-22'
--- RESET ---
UPDATED date range : '2019-08-10 - 2021-03-30'
```