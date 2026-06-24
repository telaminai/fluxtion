# Connecting DataFlow and nodes
---

An event processor supports bi-directional linking between flows and normal java classes, also known as nodes,  in the
event processor.

!!! info "Bridging functional and imperartive models"
    Connecting DataFlow and nodes is a powerful mechanism for joining functional and imperative programming in a streaming environment

Supported bindings:

* Node to data flow. The node is the start of a data flow
* Data flow to node. The node has runtime access to pull current value of a data flow
* Data flow Push to node. Data is pushed from the data flow to the node
* Data flow to event processor. Data flow pushes re-entrant events to parent event processor, triggers new calculation cycle

## Node to DataFlow
A Dataflow can be created by subscribing to a node that has been imperatively added to the event processor. When the node
triggers in a calculation cycle the DataFlow will be triggered. Create a DataFlow from a node with:

See sample - [SubscribeToNodeSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/SubscribeToNodeSample.java)

`DataFlow.subscribeToNode(new MyComplexNode())`

If the node referred to in the DataFlow.subscribeToNode method call is not in the event processor it will be bound
automatically.

The example below creates an instance of MyComplexNode as the head of a DataFlow. When a String event is received the
DataFlow path is executed. In this case we are aggregating into a list that has the four most recent elements


```java
public class SubscribeToNodeSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribeToNode(new MyComplexNode())
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
A data flow can be consumed by a normal java class within the event processor. The data flow runtime class [FlowSupplier]({{fluxtion_src_runtime}}/dataflow/FlowSupplier.java)

See sample - [FlowSupplierAsMemberVariableSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/FlowSupplierAsMemberVariableSample.java)

FlowSupplier is a normal java Supplier the current value can be accessed by calling get(). When the data flow triggers
the OnTrigger callback method in the child class will be called.

When building the processor, the FlowSupplier is accessed with: `[DataFlow].flowSupplier()`


This example binds a data flow of String's to a java record that has an onTrigger method annotated with `@OnTrigger`

```java
public static void main(String[] args) {
    FlowSupplier<String> stringFlow = DataFlowBuilder
            .subscribe(String.class)
            .flowSupplier();
    
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

See sample - [PushSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/PushSample.java)

```java
public static void main(String[] args) {
    DataFlow processor = DataFlowBuilder
            .subscribe(String.class)
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

## Map from node property

A data flow can retrieve a value from any normal java class and map it to the data flow

See sample - [MapFromNodePropertySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/MapNodeSupplierSample.java)

```java
public class MapNodeSupplierSample {
    public static void main(String[] args) {
        MyPushTarget myPushTarget = new MyPushTarget();
        DataFlow processor = DataFlowBuilder
                .subscribe(String.class)
                .push(myPushTarget::updated)
                .mapFromSupplier(myPushTarget::received)
                .console("Received - [{}]")
                .build();

        processor.onEvent("AAA");
        processor.onEvent("BBB");
    }

    public static class MyPushTarget {
        private String store = " ";
        public void updated(String in) {
            store += "'" + in + "' ";
        }

        public String received() {
            return store;
        }
    }
}
```

Running the example code above logs to console
```console
Received - [ 'AAA' ]
Received - [ 'AAA' 'BBB' ]
```

## Wrapping user functions

A data flow can wrap a user defined function and use it in the data flow. The function can be stateful or stateless, both
binaary and unary functions are supported.

See sample - [WrapFunctionsSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/WrapFunctionsSample.java)

```java
public class WrapFunctionsSample {

    public static void main(String[] args) {
        //STATEFUL FUNCTIONS
        MyFunctions myFunctions = new MyFunctions();

        var stringFlow = DataFlowBuilder
                .subscribe(String.class)
                .console("input: '{}'");

        var charCount = stringFlow
                .map(myFunctions::totalCharCount)
                .console("charCountAggregate: {}");

        var upperCharCount = stringFlow
                .map(myFunctions::totalUpperCaseCharCount)
                .console("upperCharCountAggregate: {}");

        DataFlowBuilder
                .mapBiFunction(
                        new MyFunctions.SimpleMath()::updatePercentage, 
                        upperCharCount, 
                        charCount)
                .console("percentage chars upperCase all words:{}");

        //STATELESS FUNCTION
        DataFlow processor = DataFlowBuilder
                .mapBiFunction(MyFunctions::wordUpperCasePercentage,
                        stringFlow.map(MyFunctions::upperCaseCharCount).console("charCourWord:{}"),
                        stringFlow.map(MyFunctions::charCount).console("upperCharCountWord:{}"))
                .console("percentage chars upperCase this word:{}\n")
                .build();

        processor.onEvent("test ME");
        processor.onEvent("and AGAIN");
        processor.onEvent("ALL CAPS");
    }
}
```

Running the example code above logs to console
```console
input: 'test ME'
charCountAggregate: 6
upperCharCountAggregate: 2
percentage chars upperCase all words:0.3333333333333333
charCourWord:2
upperCharCountWord:6
percentage chars upperCase this word:0.3333333333333333

input: 'and AGAIN'
charCountAggregate: 14
upperCharCountAggregate: 7
percentage chars upperCase all words:0.45
charCourWord:5
upperCharCountWord:8
percentage chars upperCase this word:0.625

input: 'ALL CAPS'
charCountAggregate: 21
upperCharCountAggregate: 14
percentage chars upperCase all words:0.5609756097560976
charCourWord:7
upperCharCountWord:7
percentage chars upperCase this word:1.0
```