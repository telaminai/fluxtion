# Trigger reference controls
---

## No propagate event handler
An event handler method can prevent its method triggering a notification by setting the propagate attribute to false 
on any event handler annotation, `@OnEventHandler(propagate = false)`

```java

```

Output
```console
MyNode::handleStringEvent received:test
Child:triggered

MyNode::handleIntEvent received:200
```

## No trigger reference
A child can isolate itself from a parent's event notification by marking the reference with a `@NoTriggerReference`
annotation. This will stop the onTrigger method from firing even when the parent has triggered.

```java
public class NoPropagateHandler {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode::handleStringEvent received:" + stringToProcess);
            return true;
        }

        @OnEventHandler(propagate = false)
        public boolean handleIntEvent(int intToProcess) {
            System.out.println("MyNode::handleIntEvent received:" + intToProcess);
            return true;
        }
    }

    public static class Child {
        private final MyNode myNode;

        public Child(MyNode myNode) {
            this.myNode = myNode;
        }

        @OnTrigger
        public boolean triggered(){
            System.out.println("Child:triggered");
            return true;
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new Child(new MyNode()))
                .build();

        processor.onEvent("test");
        System.out.println();
        processor.onEvent(200);
    }
}
```

Output
```console
MyNode::handleStringEvent received:test
Child:triggered

MyNode2::handleIntEvent received:200
```

## Override trigger reference
A child can force only a single parent to fire its trigger, all other parents will be treated as if they were annotated with 
`@NoTriggerReference` and removed from the event notification triggers for this class.

```java
public class SingleTriggerOverride {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode::handleStringEvent received:" + stringToProcess);
            return true;
        }
    }

    public static class MyNode2 {
        @OnEventHandler
        public boolean handleIntEvent(int intToProcess) {
            System.out.println("MyNode2::handleIntEvent received:" + intToProcess);
            return true;
        }
    }


    public static class Child {
        private final MyNode myNode;
        @TriggerEventOverride
        private final MyNode2 myNode2;

        public Child(MyNode myNode, MyNode2 myNode2) {
            this.myNode = myNode;
            this.myNode2 = myNode2;
        }


        @OnTrigger
        public boolean triggered() {
            System.out.println("Child:triggered");
            return true;
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new Child(new MyNode(), new MyNode2()))
                .build();

        processor.onEvent("test");
        System.out.println();
        processor.onEvent(200);
    }
}
```

Output
```console
MyNode::handleStringEvent received:test

MyNode2::handleIntEvent received:200
Child:triggered
```

## Non-dirty triggering
The condition that causes a trigger callback to fire can be inverted so that an indication of no change from the parent
will cause the trigger to fire.

```java
public static class MyNode {
    @OnEventHandler
    public boolean handleStringEvent(int intToProcess) {
        boolean propagate = intToProcess > 100;
        System.out.println("conditional propagate:" + propagate);
        return propagate;
    }
}


public static class Child {
    private final MyNode myNode;

    public Child(MyNode myNode) {
        this.myNode = myNode;
    }

    @OnTrigger
    public boolean triggered() {
        System.out.println("Child:triggered");
        return true;
    }
}

public static class NonDirtyChild {
    private final MyNode myNode;

    public NonDirtyChild(MyNode myNode) {
        this.myNode = myNode;
    }

    @OnTrigger(dirty = false)
    public boolean triggered() {
        System.out.println("NonDirtyChild:triggered");
        return true;
    }
}

public static void main(String[] args) {
    MyNode myNode = new MyNode();
    var processor = Fluxtion.interpret(new Child(myNode), new NonDirtyChild(myNode));
    processor.init();
    processor.onEvent("test");
    System.out.println();
    processor.onEvent(200);
    System.out.println();
    processor.onEvent(50);
}
```

Output
```console
conditional propagate:true
Child:triggered

conditional propagate:false
NonDirtyChild:triggered
```
## Push trigger
Invert the trigger order so the instance holding the reference receives the event notification before the reference target
and can push data into the target. Annotate the reference to be a push target with the `@PushReference` annotation.

The normal order is to trigger the target first, which can perform internal calculations if required. Then the instance
holding the reference is triggered so it can pull calculated data from the target reference.

```java
public class PushTrigger {
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
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode(new PushTarget()))
                .build();

        processor.onEvent("PUSH - test 1");
        System.out.println();
        processor.onEvent("ignore me - XXXXX");
        System.out.println();
        processor.onEvent("PUSH - test 2");
    }
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
