# DataFlow quick reference
---

DSL is used to create a data flow that can be mapped, filter, windowed, grouped etc. A data flow is created with a
subscription and then can be manipulated with functional operations. Describes the api a developer must be familiar with to use DataFlow

| Use                        | DSL                                                                                                                                                                                                                       | Description                                                           |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| DataFlow from event stream | `DataFlowBuilder.subscribe(Class<T> eventClass)`                                                                                                                                                                                 | Subscribe to event of type T, creates a data flow of T                |
| DataFlow from a node       | `DataFlowBuilder.subscribeToNode(T sourceNode)`                                                                                                                                                                                  | Create a data flow of T. Triggers when T triggers                     |
| Map                        | `[DataFlow].map(Function<T, R> mapFunction)`                                                                                                                                                                                  | Maps T to R when triggered                                            |
| Filter                     | `[DataFlow].filter(Function<T, Boolean> filterFunction)`                                                                                                                                                                      | Filters T when triggered                                              |
| Tumbling window            | `[DataFlow].tumblingAggregate(` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`Supplier<AggregateFlowFunction> aggregateFunction, ` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`int bucketSizeMillis)`                                                     | Aggregates T with aggregate function <br/>in a tumbling window        |
| Sliding window             | `[DataFlow].slidingAggregate(` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`Supplier<AggregateFlowFunction> aggregateFunction, ` <br/>&nbsp;&nbsp;&nbsp;&nbsp;`int bucketSizeMillis, ` <br/>&nbsp;&nbsp;&nbsp;&nbsp;`int bucketsPerWindow)` | Aggregates T with aggregate function <br/>in a sliding window         |
| Group by                   | `[DataFlow].groupBy(` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`Function<T, K1> keyFunction, ` <br/>&nbsp;&nbsp;&nbsp;&nbsp;`Supplier<F> aggregateFunctionSupplier`                                                                      | Groups T with key function applies an aggregate function to each item |
| Joining                    | `JoinFlowBuilder.innerJoin(` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`GroupByFlow<K1, V1> leftGroupBy, ` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`GroupByFlow<K2, V2> rightGroupBy)`                                                          | Joins two group by data flows on their keys                           |



# Agent integration quick reference
Annotations that mark methods as receiving callbacks from the hosting DataFlow. 

## Event handling

Mark methods as callbacks that will be invoked on a calculation cycle. An event listener callback is triggered
when external events are posted to the processor. A trigger callback method is called when its parent has triggered due
to an incoming event. Boolean return type from trigger or event handler method indicates a change notification should be
propagated.

| Use                       | Annotation                         | DSL Equivalent                                                                                                                 | Description                                                                                                 |
|---------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Event listener            | `@OnEventHandler`                  | `DataFlowBuilder.subscribe(Class<T> eventClass)`                                                                                      | Marks method as a subscriber callback<br/> to event stream of type T                                        |
| Trigger                   | `@OnTrigger`                       | `[DataFlow].map.(Function<T, R> mapFunction)`                                                                                      | Marks method as callback calc method<br/>in a process cycle<br/>                                            |
| Identify trigger source   | `@OnParentUpdate`                  |                                                                                                                                | Marks method as callback method <br/>identifying changed parent. <br/>Called before trigger method          |
| No trigger Event listener | `@OnEventHandler(propagate=false)` |                                                                                                                                | Marks method as a subscriber callback<br/>No triggering of child callbacks                                  |
| Data only parent          | `@NoTriggerReference`              |                                                                                                                                | Mark a parent reference as data only.<br/>Parent changes are non-triggering for this                        |
| Push data to child        | `@PushReference`                   | `[DataFlow].push.(Consumer<T, R> mapFunction)`                                                                                     | Marks a parent reference as a push target<br/> This pushes data to parent. <br/>Parent triggers after child |
| Filter events             | `@OnEvent(filterId)`               | `DataFlowBuilder.subscribe(` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`Class<T> classSubscription, ` <br/> &nbsp;&nbsp;&nbsp;&nbsp;`int filter)` | Marks method as a subscriber callback<br/> to a filtered event stream of type T                             |

## Service export

Mark an interface as exported and the event processor will implement the interface and route any calls to the instance.
An interface method behaves as an event listener call back method that is annotated with `@OnEventHandler`.

| Use                   | Annotation                        | Description                                                          |
|-----------------------|-----------------------------------|----------------------------------------------------------------------|
| Export an interface   | `@ExportService`                  | All interface methods are event handlers triggering a process cycle  |
| No trigger one method | `@NoPropagateFunction`            | Mark a method as non-triggering an event process cycle on invocation |
| Data only interface   | `@ExportService(propagate=false)` | Mark a whole interface as non-triggering                             |


## Lifecycle

Mark methods to receive lifecycle callbacks that are invoked on the event processor. None of the lifecycle calls are
automatic it is the client code that is responsible for calling lifecycle methods on the event processor.

| Phase      | Annotation    | Description                                                                            |
|------------|---------------|----------------------------------------------------------------------------------------|
| Initialise | `@Initialise` | Called by client code once on an event processor. Must be called before start          |
| Start      | `@Start`      | Called by client code 0 to many time. Must be called after start                       |
| Stop       | `@Stop`       | Called by client code 0 to many time. Must be called after start                       |
| TearDown   | `@TearDown`   | Called by client code 0 or once on an event processor before the processor is disposed |


# Functional operations
The functional DSL supports a rich set of operations. Where appropriate functional operations support:

- Stateless functions
- Stateful functions
- Primitive specialisation
- Method references
- Inline lambdas 

## Map
A map operation takes the input from a parent function and then applies a function to the input. If the return of the
output is null then the event notification no longer propagates down that path.

```java
var stringFlow = DataFlow.subscribe(String.class);

stringFlow.map(String::toLowerCase);
stringFlow.mapToInt(s -> s.length()/2);
```


- Stateless functions
- Stateful functions
- Primitive specialisation
- Method references
- Inline lambdas - **interpreted mode only support, AOT mode will not serialise the inline lambda**

## Filter
A filter predicate can be applied to a node to control event propagation, true continues the propagation and false swallows
the notification. If the predicate returns true then the input to the predicate is passed to the next operation in the
event processor.


```java
DataFlow.subscribe(String.class)
    .filter(Objects::nonNull)
    .mapToInt(s -> s.length()/2);

```

**Filter supports**

- Stateless functions
- Stateful functions
- Primitive specialisation
- Method references
- Inline lambdas - **interpreted mode only support, AOT mode will not serialise the inline lambda**

## Map with bi function
Takes two flow inputs and applies a bi function to the inputs. Applied once both functions have updated.

## Peek
View the state of a node, invoked when the parent triggers.

## Sink
Publishes the output of the function to a named sink end point. Client code can register as a named sink end point with
the running event processor.

## Id
A node can be given an id that makes it discoverable using EventProcessor.getNodeById.

## Aggregate
Aggregates the output of a node using a user supplied stateful function.

## Aggregate with sliding window
Aggregates the output of a node using a user supplied stateful function, in a sliding window.

## Aggregate with tumbling window
Aggregates the output of a node using a user supplied stateful function, in a tumbling window.

## Default value
Set the initial value of a node without needing an input event to create a value.

## Flat map
Flat map operations on a collection from a parent node.

## Group by
Group by operations.

## Group by with sliding window
Group by operations, in a sliding window.

## Group by with tumbling window
Group by operations, in a tumbling window.

## Lookup
Apply a lookup function to a value as a map operation.

## Merge
Merge multiple streams of the same type into a single output.

## Map and merge
Merge multiple streams of different types into a single output, applying a mapping operation to combine the different types

## Console
Specialisation of peek that logs to console

## Push
Pushes the output of a node to user class, joins functional to imperative flow

## Trigger overrides
External flows can override that standard triggering method to force publication/calculation/downstream notifications.

## Reentrant events
The output of an operation can be published to the event processor as a new event. Will be processed after the current
cycle finishes.

# Examples
The source project for the examples can be
found [here]({{fluxtion_example_src}}/runtime-execution/src/main/java/com/fluxtion/example/reference/execution)

## Bind functions to events

To bind functions to a flow of events a flow must be created with a subscription method in DataFlow.

`DataFlow.subscribe([event class])`

A lambda or a method reference can be bound as the next item in the function flow. 


```java
public static String toUpper(String incoming){
    return incoming.toUpperCase();
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(cfg -> {
        DataFlow.subscribe(String.class)
            .console("input: '{}'")
            .map(FunctionalStatic::toUpper)
            .console("transformed: '{}'");
    });

    processor.init();
    processor.onEvent("hello world");
}

```

Output

```console
input: 'hello world'
transformed: 'HELLO WORLD'

```

## Bind instance functions

Instance functions can be bound into the event processor using method references


```java
public static class PrefixString{
    private final String prefix;

    public PrefixString(String prefix) {
        this.prefix = prefix;
    }

    public String addPrefix(String input){
        return prefix + input;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(cfg -> {
        DataFlow.subscribe(String.class)
            .console("input: '{}'")
            .map(new PrefixString("XXXX")::addPrefix)
            .console("transformed: '{}'");
    });

    processor.init();
    processor.onEvent("hello world");
}

```

Output

```console
input: 'hello world'
transformed: 'XXXXhello world'

```

## Combining imperative and functional binding

Both imperative and functional binding can be used in the same build consumer. All the user classes and functions will
be added to the model for generation.


```java
public static String toUpper(String incoming){
    return incoming.toUpperCase();
}

public static class MyNode {
    @OnEventHandler
    public boolean handleStringEvent(String stringToProcess) {
        System.out.println("IMPERATIVE received:" + stringToProcess);
        return true;
    }
}

public static void main(String[] args) {
   var processor = Fluxtion.interpret(cfg -> {
        DataFlow.subscribe(String.class)
            .console("FUNCTIONAL input: '{}'")
            .map(CombineFunctionalAndImperative::toUpper)
            .console("FUNCTIONAL transformed: '{}'");

        cfg.addNode(new MyNode());
    });

    processor.init();
    processor.onEvent("hello world");
}

```

Output

```console
FUNCTIONAL input: 'hello world'
FUNCTIONAL transformed: 'HELLO WORLD'
IMPERATIVE received:hello world

```

## Re-entrant events

Events can be added for processing from inside the graph for processing in the next available cycle. Internal events
are added to LIFO queue for processing in the correct order. The EventProcessor instance maintains the LIFO queue, any
new input events are queued if there is processing currently acting. Support for internal event publishing is built
into the streaming api.

Maps an int signal to a String and republishes to the graph

```java
public static class MyNode {
    @OnEventHandler
    public boolean handleStringEvent(String stringToProcess) {
        System.out.println("received [" + stringToProcess +"]");
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(cfg -> {
        DataFlow.subscribeToIntSignal("myIntSignal")
            .mapToObj(d -> "intValue:" + d)
            .console("republish re-entrant [{}]")
            .processAsNewGraphEvent();
        cfg.addNode(new MyNode());
    });

    processor.init();
    processor.publishSignal("myIntSignal", 256);
}

```

Output

```console
republish re-entrant [intValue:256]
received [intValue:256]

```