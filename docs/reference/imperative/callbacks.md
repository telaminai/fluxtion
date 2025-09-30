# Callbacks
---

## After event callback
Register for a post event method callback with the `@AfterEvent` annotation. The callback will be executed whenever
any event is sent to the event processor. Unlike the `@AfterTrigger` which is only called if the containing instance has
been triggered.

```java
public class AfterEventCallback {
    public static class MyNode {
        @Initialise
        public void init(){
            System.out.println("MyNode::init");
        }

        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode::handleStringEvent received:" + stringToProcess);
            return true;
        }

        @AfterEvent
        public void afterEvent(){
            System.out.println("MyNode::afterEvent");
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        System.out.println();
        processor.onEvent("TEST");
        System.out.println();
        processor.onEvent(23);
    }
}
```

Output
```console
MyNode::init
MyNode::afterEvent

MyNode::handleStringEvent received:TEST
MyNode::afterEvent

MyNode::afterEvent
```

## After trigger callback
Register for a post trigger method callback with the `@AfterTrigger` annotation. The callback will only be executed if 
this class has been triggered on tby an incoming event. Unlike the `@AfterEvent` which is always called on any event.

```java
public class AfterTriggerCallback {
    public static class MyNode {
        @Initialise
        public void init(){
            System.out.println("MyNode::init");
        }

        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode::handleStringEvent received:" + stringToProcess);
            return true;
        }

        @AfterTrigger
        public void afterTrigger(){
            System.out.println("MyNode::afterTrigger");
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        System.out.println();
        processor.onEvent("TEST");
        System.out.println();
        processor.onEvent(23);
    }
}
```

Output
```console
MyNode::init

MyNode::handleStringEvent received:TEST
MyNode::afterTrigger
```