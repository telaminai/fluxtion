
# Connecting DataFlow and nodes
---
An event processor supports bi-directional linking between flows and normal java classes, also known as nodes,  in the
event processor.

{: .info }
Connecting DataFlow and nodes is a powerful mechanism for joining functional and imperative programming in a streaming environment
{: .fs-4 }

Supported bindings:

* Node to data flow. The node is the start of a data flow
* Data flow to node. The node has runtime access to pull current value of a data flow
* Data flow Push to node. Data is pushed from the data flow to the node
* Data flow to event processor. Data flow pushes re-entrant events to parent event processor, triggers new calculation cycle

## Node to DataFlow
A Dataflow can be created by subscribing to a node that has been imperatively added to the event processor. When the node
triggers in a calculation cycle the DataFlow will be triggered. Create a DataFlow from a node with:

`DataFlow.subscribeToNode(new MyComplexNode())`

If the node referred to in the DataFlow.subscribeToNode method call is not in the event processor it will be bound
automatically.

The example below creates an instance of MyComplexNode as the head of a DataFlow. When a String event is received the
DataFlow path is executed. In this case we are aggregating into a list that has the four most recent elements


```java
public class SubscribeToNodeSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder.subscribeToNode(new MyComplexNode())
                .console("node triggered -> {}")
                .map(MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .console("last 4 elements:{}\n")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");
        processor.onEvent("E");
        processor.onEvent("F");
    }

    @Getter
    @ToString
    public static class MyComplexNode {
        private String in;

        @OnEventHandler
        public boolean stringUpdate(String in) {
            this.in = in;
            return true;
        }
    }
}
```

Running the example code above logs to console
```console
node triggered -> SubscribeToNodeSample.MyComplexNode(in=A)
last 4 elements:[A]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=B)
last 4 elements:[A, B]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=C)
last 4 elements:[A, B, C]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=D)
last 4 elements:[A, B, C, D]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=E)
last 4 elements:[B, C, D, E]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=F)
last 4 elements:[C, D, E, F]
```


## DataFlow to node
A data flow can be consumed by a normal java class within the event processor. The data flow runtime class is

[FlowSupplier]({{fluxtion_src_runtime}}/dataflow/FlowSupplier.java)

FlowSupplier is a normal java Supplier the current value can be accessed by calling get(). When the data flow triggers
the OnTrigger callback method in the child class will be called.

When building the processor, the FlowSupplier is accessed with:

`[DataFlow].flowSupplier()`

This example binds a data flow of String's to a java record that has an onTrigger method annotated with `@OnTrigger`

```java
public static void main(String[] args) {
    FlowSupplier<String> stringFlow = DataFlowBuilder.subscribe(String.class).flowSupplier();
    DataFlow processor = DataFlowBuilder
            .subscribeToNode(new MyFlowHolder(stringFlow))
            .build();

    processor.onEvent("test");
}

public record MyFlowHolder(FlowSupplier<String> flowSupplier) {
    @OnTrigger
    public boolean onTrigger() {
        //FlowSupplier is used at runtime to access the current value of the data flow
        System.out.println("triggered by data flow -> " + flowSupplier.get().toUpperCase());
        return true;
    }
}
```

Running the example code above logs to console
```console
triggered by data flow -> TEST
```

## Push to node
A data flow can push a value to any normal java class

```java
public static void main(String[] args) {
    DataFlow processor = DataFlowBuilder.subscribe(String.class)
            .push(new MyPushTarget()::updated)
            .build();

    processor.onEvent("AAA");
    processor.onEvent("BBB");
}

public static class MyPushTarget {
    public void updated(String in) {
        System.out.println("received push: " + in);
    }
}
```

Running the example code above logs to console
```console
received push: AAA
received push: BBB
```

## Re-entrant events

Events can be added for processing from inside the graph for processing in the next available cycle. Internal events
are added to LIFO queue for processing in the correct order. The EventProcessor instance maintains the LIFO queue, any
new input events are queued if there is processing currently acting. Support for internal event publishing is built
into the streaming api.

Maps an int signal to a String and republishes to the graph
```java
public static void main(String[] args) {
    DataFlowBuilder.subscribeToIntSignal("myIntSignal")
            .mapToObj(d -> "intValue:" + d)
            .console("republish re-entrant [{}]")
            .processAsNewGraphEvent();

    var processor = DataFlowBuilder.subscribe(String.class)
            .console("received [{}]")
            .build();

    processor.publishSignal("myIntSignal", 256);
}
```

Output
```console
republish re-entrant [intValue:256]
received [intValue:256]
```