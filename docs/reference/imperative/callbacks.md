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
    var processor = Fluxtion.interpret(new MyNode());
    processor.init();
    System.out.println();
    processor.onEvent("TEST");
    System.out.println();
    processor.onEvent(23);
}
```

Output
```console
MyNode::init

MyNode::handleStringEvent received:TEST
MyNode::afterTrigger
```

## Push trigger
Invert the trigger order so the instance holding the reference receives the event notification before the reference target 
and can push data into the target. Annotate the reference to be a push target with the `@PushReference` annotation.

The normal order is to trigger the target first, which can perform internal calculations if required. Then the instance 
holding the reference is triggered so it can pull calculated data from the target reference.

```java
public static class MyNode {
    @PushReference
    private final PushTarget pushTarget;

    public MyNode(PushTarget pushTarget) {
        this.pushTarget = pushTarget;
    }

    @OnEventHandler
    public boolean handleStringEvent(String stringToProcess) {
        System.out.println("MyNode::handleStringEvent " + stringToProcess);
        if (stringToProcess.startsWith("PUSH")) {
            pushTarget.myValue = stringToProcess;
            return true;
        }
        return false;
    }
}

public static class PushTarget {
    public String myValue;

    @OnTrigger
    public boolean onTrigger() {
        System.out.println("PushTarget::onTrigger -> myValue:'" + myValue + "'");
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new MyNode(new PushTarget()));
    processor.init();
    processor.onEvent("PUSH - test 1");
    System.out.println();
    processor.onEvent("ignore me - XXXXX");
    System.out.println();
    processor.onEvent("PUSH - test 2");
}
```

Output
```console
MyNode::handleStringEvent PUSH - test 1
PushTarget::onTrigger ->  myValue:'PUSH - test 1'

MyNode::handleStringEvent ignore me - XXXXX

MyNode::handleStringEvent PUSH - test 2
PushTarget::onTrigger ->  myValue:'PUSH - test 2'
```
