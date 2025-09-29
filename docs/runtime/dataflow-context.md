# Using DataFlowContext at runtime
---

This guide shows how to interact with a running DataFlow graph using runtime services from fluxtion‑runtime, with live
code excerpts and the actual console output from the reference examples.

Source for all examples [context examples]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context)

What you can do at runtime:

- Provide and read contextual parameters for your graph
- Dispatch additional events from inside handlers
- Mark parts of the graph as dirty and drive recomputation

## Context parameters

Use DataFlowContext to pass configuration or dynamic values into the graph without changing the event stream.

Example: [ContextParamInput.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/ContextParamInput.java)

Key excerpts:

```java
public static class ContextParamReader {
    @Inject
    public DataFlowContext context;

    @Start
    public void start() {
        System.out.println("myContextParam1 -> " + context.getContextProperty("myContextParam1"));
        System.out.println("myContextParam2 -> " + context.getContextProperty("myContextParam2"));
        System.out.println();
    }
}
```

```java
public static void main(String[] args) {
    var processor = DataFlowBuilder
            .subscribeToNode(new ContextParamReader())
            .build();

    processor.addContextParameter("myContextParam1", "[param1: update 1]");
    processor.start();

    processor.addContextParameter("myContextParam1", "[param1: update 2]");
    processor.addContextParameter("myContextParam2", "[param2: update 1]");
    processor.start();
}
```

Console output when running the example:

```console
myContextParam1 -> [param1: update 1]
myContextParam2 -> null

myContextParam1 -> [param1: update 2]
myContextParam2 -> [param2: update 1]
```

What’s happening:

- The application adds context parameters before calling start().
- The node reads them during @Start and prints current values.
- Starting twice shows how updated context is visible on the next start.

## Emitting re-entrant events

Nodes can emit new events from inside handlers. Use EventDispatcher to process items as independent event cycles.

Example: [CallBackExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/CallBackExample.java)

Key excerpts:

```java
public static class MyCallbackNode {
    @Inject
    public EventDispatcher eventDispatcher;

    @OnEventHandler
    public boolean processString(String event) {
        System.out.println("MyCallbackNode::processString - " + event);
        for (String item : event.split(",")) {
            eventDispatcher.processAsNewEventCycle(Integer.parseInt(item));
        }
        return true;
    }

    @OnEventHandler
    public boolean processInteger(Integer event) {
        System.out.println("MyCallbackNode::processInteger - " + event);
        return false;
    }
}
```

```java

@Data
public static class IntegerHandler {
    private final MyCallbackNode myCallbackNode;

    @OnEventHandler
    public boolean processInteger(Integer event) {
        System.out.println("IntegerHandler::processInteger - " + event + "\n");
        return true;
    }

    @OnTrigger
    public boolean triggered() {
        System.out.println("IntegerHandler::triggered\n");
        return false;
    }
}
```

```java
public static void main(String[] args) {
    MyCallbackNode myCallbackNode = new MyCallbackNode();
    IntegerHandler intHandler = new IntegerHandler(myCallbackNode);

    var processor = DataFlowBuilder
            .subscribeToNode(intHandler)
            .build();

    processor.onEvent("20,45,89");
}
```

Console output when running the example:

```console
MyCallbackNode::processString - 20,45,89
MyCallbackNode::processInteger - 20
IntegerHandler::processInteger - 20

IntegerHandler::triggered

MyCallbackNode::processInteger - 45
IntegerHandler::processInteger - 45

IntegerHandler::triggered

MyCallbackNode::processInteger - 89
IntegerHandler::processInteger - 89

IntegerHandler::triggered
```

What’s happening:

- The String event is split into three integers.
- Each integer is dispatched as a new event cycle.
- For each integer cycle, the integer handlers run and then the node’s @OnTrigger callback fires.

## Controlling and monitoring dirty state

Explicitly mark data as dirty and trigger a compute pass so dependent nodes recompute even without new inputs.

Example: [DirtyStateMonitorExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/DirtyStateMonitorExample.java)

Key excerpts:

```java
public static class TriggeredChild implements NamedNode {
    @Inject
    public DirtyStateMonitor dirtyStateMonitor;
    private final FlowSupplier<Integer> intDataFlow;

    @OnTrigger
    public boolean triggeredChild() {
        System.out.println("TriggeredChild -> " + intDataFlow.get());
        return true;
    }

    public void printDirtyStat() {
        System.out.println("\nintDataFlow dirtyState:" + dirtyStateMonitor.isDirty(intDataFlow));
    }

    public void markDirty() {
        dirtyStateMonitor.markDirty(intDataFlow);
        System.out.println("\nmark dirty intDataFlow dirtyState:" + dirtyStateMonitor.isDirty(intDataFlow));
    }

    @Override
    public String getName() {
        return "triggeredChild";
    }
}
```

```java
public static void main(String[] args) throws NoSuchFieldException {
    var intFlow = DataFlowBuilder.subscribe(Integer.class).flowSupplier();

    var processor = DataFlowBuilder
            .subscribeToNode(new TriggeredChild(intFlow))
            .build();

    TriggeredChild triggeredChild = processor.getNodeById("triggeredChild");

    processor.onEvent(2);
    processor.onEvent(4);

    //NOTHING HAPPENS
    triggeredChild.printDirtyStat();
    processor.triggerCalculation();

    //MARK DIRTY
    triggeredChild.markDirty();
    processor.triggerCalculation();
}
```

Console output when running the example:

```console
TriggeredChild -> 2
TriggeredChild -> 4

intDataFlow dirtyState:false

mark dirty intDataFlow dirtyState:true
TriggeredChild -> 4
```

What’s happening:

- The child prints values on incoming integer events.
- A manual trigger with no dirty state yields no new output.
- After marking the flow dirty, triggerCalculation causes recomputation; the child runs again with the last value.