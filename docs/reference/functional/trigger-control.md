# Trigger control
---

Fluxtion offers a way to override the triggering of a flow node in the event processor. There are four trigger controls
available for client code to customise:

* **Flow.publishTrigger** - Notifies a child node when triggered, adds a notification to the normal publish
* **Flow.publishTriggerOverride** - Notifies a child node when triggered, removes all other publish notifications
* **Flow.updateTrigger** - Overrides when the flow node runs its functional operation
* **Flow.resetTrigger** - If the functional operation is stateful calls the reset function

In the trigger examples we are using the `DataFlow.subscribeToSignal` and `processor.publishSignal` to drive the trigger
controls on the flow node.

## PublishTrigger
In this example the publishTrigger control enables multiple publish calls for the flow node. Child notifications are in 
addition to the normal triggering operation of the flow node. The values in the parent node are unchanged when publishing.

`publishTrigger(DataFlow.subscribeToSignal("publishMe"))`

Child DataFlow nodes are notified when publishTrigger fires or the map function executes in a calculation cycle.

```java
public class TriggerPublishSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribeToNode(new SubscribeToNodeSample.MyComplexNode())
                .console("node triggered -> {}")
                .map(SubscribeToNodeSample.MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .publishTrigger(DataFlowBuilder.subscribeToSignal("publishMe"))
                .console("last 4 elements:{}")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");
        processor.onEvent("E");
        processor.onEvent("F");

        processor.publishSignal("publishMe");
        processor.publishSignal("publishMe");
        processor.publishSignal("publishMe");
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
last 4 elements:[C, D, E, F]
last 4 elements:[C, D, E, F]
last 4 elements:[C, D, E, F]

```

## PublishTriggerOverride
In this example the publishTrigger control overrides the normal triggering operation of the flow node. The child is notified
only when publishTriggerOverride fires, changes due to recalculation are swallowed and not published downstream.
The values in the parent node are unchanged when publishing.

`publishTriggerOverride(DataFlow.subscribeToSignal("publishMe"))`

Child DataFlow nodes are notified when publishTriggerOverride fires.

```java
public class TriggerPublishOverrideSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribeToNode(new SubscribeToNodeSample.MyComplexNode())
                .console("node triggered -> {}")
                .map(SubscribeToNodeSample.MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .publishTriggerOverride(DataFlowBuilder.subscribeToSignal("publishMe"))
                .console("last 4 elements:{}\n")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");

        processor.publishSignal("publishMe");
        processor.onEvent("E");
        processor.onEvent("F");
        processor.onEvent("G");
        processor.onEvent("H");

        processor.publishSignal("publishMe");
    }
}
```

Running the example code above logs to console

```console
node triggered -> SubscribeToNodeSample.MyComplexNode(in=A)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=B)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=C)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=D)
last 4 elements:[A, B, C, D]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=E)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=F)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=G)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=H)
last 4 elements:[E, F, G, H]
```

## UpdateTrigger
In this example the updateTrigger controls when the functional mapping operation of the flow node is invoked. The values 
are only aggregated when the update trigger is called. Notifications from the parent node are ignored and do not trigger
a mapping operation.

`updateTrigger(DataFlow.subscribeToSignal("updateMe"))`

A map operation only occurs when the update trigger fires. 

```java
public class TriggerUpdateSample {
    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new SubscribeToNodeSample.MyComplexNode())
                .console("node triggered -> {}")
                .map(SubscribeToNodeSample.MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .updateTrigger(DataFlowBuilder.subscribeToSignal("updateMe"))
                .console("last 4 elements:{}\n")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.publishSignal("updateMe");

        processor.onEvent("D");
        processor.onEvent("E");
        processor.onEvent("F");
        processor.publishSignal("updateMe");
    }
}
```

Running the example code above logs to console

```console
node triggered -> SubscribeToNodeSample.MyComplexNode(in=A)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=B)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=C)
last 4 elements:[C]

node triggered -> SubscribeToNodeSample.MyComplexNode(in=D)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=E)
node triggered -> SubscribeToNodeSample.MyComplexNode(in=F)
last 4 elements:[C, F]
```

### ResetTrigger
In this example the resetTrigger controls when the functional mapping operation of the flow node is reset. The aggregate
operation is stateful so all the values in the list are removed when then reset trigger fires. The reset operation causes 
trigger a notification to children of the flow node.

`resetTrigger(DataFlow.subscribeToSignal("resetMe"))`

The reset trigger notifies the stateful function to clear its state.

```java
public class TriggerResetSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribeToNode(new SubscribeToNodeSample.MyComplexNode())
                .console("node triggered -> {}")
                .map(SubscribeToNodeSample.MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .resetTrigger(DataFlowBuilder.subscribeToSignal("resetMe").console("\n--- resetTrigger ---"))
                .console("last 4 elements:{}")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");

        processor.publishSignal("resetMe");
        processor.onEvent("E");
        processor.onEvent("F");
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

--- resetTrigger ---
last 4 elements:[]
node triggered -> SubscribeToNodeSample.MyComplexNode(in=E)
last 4 elements:[E]
node triggered -> SubscribeToNodeSample.MyComplexNode(in=F)
last 4 elements:[E, F]
```

###Stateful function reset
Stateful functions can be reset by implementing the [Stateful]({{fluxtion_src_runtime}}/dataflow/Stateful.java) interface with a reset 
method. Configuring the resetTrigger will automatically route calls to the reset method of the stateful function.


```java
public class ResetFunctionSample {
    public static void main(String[] args) {
        DataFlow processor = DataFlowBuilder
                .subscribe(String.class)
                .map(new MyResetSum()::increment)
                .resetTrigger(DataFlowBuilder.subscribeToSignal("resetMe"))
                .console("count:{}")
                .build();

        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");

        processor.publishSignal("resetMe");
        processor.onEvent("E");
        processor.onEvent("F");
    }

    public static class MyResetSum implements Stateful<Integer> {
        public int count = 0;

        public int increment(Object o) {
            return ++count;
        }

        @Override
        public Integer reset() {
            System.out.println("--- RESET CALLED ---");
            count = 0;
            return count;
        }
    }
}
```

Running the example code above logs to console

```console
count:1
count:2
count:3
count:4
--- RESET CALLED ---
count:0
count:1
count:2
```