# Basics
---

## Handle event input 
Sends an incoming even to the EventProcessor to trigger a new stream calculation. Any method annotated with 
`@OnEvent` receives the event from the event processor

See sample - [WrapNode.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/bindnode/subscribe/WrapNode.java)

```java
public class WrapNode {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("received:" + stringToProcess);
            return true;
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        processor.onEvent("TEST");
    }
}
```

Output
```console
received:TEST
```

## Handle multiple event types
An event handler class can handle multiple event types. Add as many handler methods as required and annotate each method
with an `@OnEvent` annotation.

See sample - [MultipleEventTypes.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/bindnode/subscribe/MultipleEventTypes.java)

```java
public class MultipleEventTypes {

    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("String received:" + stringToProcess);
            return true;
        }

        @OnEventHandler
        public boolean handleIntEvent(int intToProcess) {
            System.out.println("Int received:" + intToProcess);
            return true;
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        processor.onEvent("TEST");
        processor.onEvent(16);
    }
}
```

Output
```console
String received:TEST
Int received:16
```

## Handling unknown event types
An unknown event handler can be registered at runtime with the event processor, to catch any event types that are not handled
by the processor. Register the unKnownEventHandler with:

`[processor].setUnKnownEventHandler(Consumer<T> consumer)`

See sample - [UnknownEventHandling.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/bindnode/subscribe/UnknownEventHandling.java)

```java
public class UnknownEventHandling {

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        //set an unknown event handler
        processor.setUnKnownEventHandler(e -> System.out.println("Unregistered event type -> " + e.getClass().getName()));
        processor.onEvent("TEST");

        //handled by unKnownEventHandler
        processor.onEvent(Collections.emptyList());
    }

    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("received:" + stringToProcess);
            return true;
        }
    }
}
```

Output
```console
received:TEST
Unregistered event type -> java.util.Collections$EmptyList
```
