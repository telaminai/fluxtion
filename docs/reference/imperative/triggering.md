# Triggering
---

## Triggering children
Event notification is propagated to child instances of event handlers. The notification is sent to any method that is
annotated with an `@OnTrigger` annotation. Trigger propagation is in topological order.

```java
public class TriggerChildren {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("received:" + stringToProcess);
            return true;
        }
    }

    public static class MyNode2 {
        @OnEventHandler
        public boolean handleStringEvent(int intToProcess) {
            System.out.println("received:" + intToProcess);
            return true;
        }
    }

    public static class Child {
        private final MyNode myNode;
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
received:test
Child:triggered

received:200
Child:triggered
```

## Conditional triggering children
Event notification is propagated to child instances of event handlers if the event handler method returns a true value. 
A false return value will cause the event processor to swallow the triggering notification.

```java
public class ConditionalTriggerChildren {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("received:" + stringToProcess);
            return true;
        }
    }

    public static class MyNode2 {
        @OnEventHandler
        public boolean handleStringEvent(int intToProcess) {
            boolean propagate = intToProcess > 100;
            System.out.println("conditional propagate:" + propagate);
            return propagate;
        }
    }

    public static class Child{
        private final MyNode myNode;
        private final MyNode2 myNode2;

        public Child(MyNode myNode, MyNode2 myNode2) {
            this.myNode = myNode;
            this.myNode2 = myNode2;
        }

        @OnTrigger
        public boolean triggered(){
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
        System.out.println();
        processor.onEvent(50);
    }
}
```

Output
```console
received:test
Child:triggered

conditional propagate:true
Child:triggered

conditional propagate:false
```

## Identify triggering parent
It is possible to identify the parent that has triggered a change by adding an `@OnParentUpdate` annotation to a child 
instance. The method must accept a single parameter of the type of the parent to observe. The OnParent callback gives
granular detail of which parent has changed, whereas OnTrigger callbacks signify that at least one parent is triggering.

The OnParent callbacks are guaranteed to be received before the OnTrigger callback.

```java
public class IdentifyTriggerParent {
    public static class MyNode {
        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode event received:" + stringToProcess);
            return true;
        }
    }

    public static class MyNode2 {
        @OnEventHandler
        public boolean handleIntEvent(int intToProcess) {
            boolean propagate = intToProcess > 100;
            System.out.println("MyNode2 conditional propagate:" + propagate);
            return propagate;
        }

        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println("MyNode2 event received:" + stringToProcess);
            return true;
        }
    }

    public static class Child {
        private final MyNode myNode;
        private final MyNode2 myNode2;

        public Child(MyNode myNode, MyNode2 myNode2) {
            this.myNode = myNode;
            this.myNode2 = myNode2;
        }

        @OnParentUpdate
        public void node1Updated(MyNode myNode1) {
            System.out.println("1 - myNode updated");
        }

        @OnParentUpdate
        public void node2Updated(MyNode2 myNode2) {
            System.out.println("2 - myNode2 updated");
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
        System.out.println();
        processor.onEvent(50);
    }
}
```

Output
```console
MyNode2 event received:test
2 - myNode2 updated
MyNode event received:test
1 - myNode updated
Child:triggered

MyNode2 conditional propagate:true
2 - myNode2 updated
Child:triggered

MyNode2 conditional propagate:false
```

## Identifying parent by name
When a child has multiple parents of the same type then name resolution can be used to identify the parent that has 
triggered the update. Add the variable name to the `@OnParentyUpdate` annotation to enforce name and type resolution.
The OnParent callback is invoked according to the same rules as conditional triggering. 

```java
public class IdentifyTriggerParentById {
    public static class MyNode {
        private final String name;

        public MyNode(String name) {
            this.name = name;
        }

        @OnEventHandler
        public boolean handleStringEvent(String stringToProcess) {
            System.out.println(name + " event received:" + stringToProcess);
            return stringToProcess.equals("*") | stringToProcess.equals(name);
        }
    }

    public static class Child{
        private final MyNode myNode_a;
        private final MyNode myNode_b;

        public Child(MyNode myNode_a, MyNode myNode_b) {
            this.myNode_a = myNode_a;
            this.myNode_b = myNode_b;
        }

        @OnParentUpdate(value = "myNode_a")
        public void node_a_Updated(MyNode myNode_a){
            System.out.println("Parent A updated");
        }

        @OnParentUpdate("myNode_b")
        public void node_b_Updated(MyNode myNode_b){
            System.out.println("Parent B updated");
        }

        @OnTrigger
        public boolean triggered(){
            System.out.println("Child:triggered");
            return true;
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new Child(new MyNode("A"), new MyNode("B")))
                .build();

        processor.init();
        processor.onEvent("test");
        System.out.println();
        processor.onEvent("*");
        System.out.println();
        processor.onEvent("A");
        System.out.println();
        processor.onEvent("B");
    }
}
```

Output
```console
A event received:test
B event received:test

A event received:*
Parent A updated
B event received:*
Parent B updated
Child:triggered

A event received:A
Parent A updated
B event received:A
Child:triggered

A event received:B
B event received:B
Parent B updated
Child:triggered
```
