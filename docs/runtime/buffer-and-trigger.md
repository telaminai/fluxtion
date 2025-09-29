# Buffering events and triggering calculation at runtime
---

This guide shows how to buffer incoming events without immediately completing a calculation, and then explicitly trigger
a calculation pass. It uses live code and the actual console output from the reference example.

Source: [BufferAndTriggerExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/buffertrigger/BufferAndTriggerExample.java)

## Example

```java
import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;

public class BufferAndTriggerExample {
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

        // this will be called when the parent node is updated AND the child node is triggered
        // with triggerCalculation
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

        System.out.println("------- start buffering events -----------");
        processor.bufferEvent("test");
        processor.bufferEvent(200);
        processor.bufferEvent(50);
        System.out.println("------- complete buffering events -----------\n");

        System.out.println("------ triggering calculation ------");
        processor.triggerCalculation();
    }
}
```

Console output when running the example:

```console
------- start buffering events -----------
MyNode2 event received:test
2 - myNode2 updated
MyNode event received:test
1 - myNode updated
MyNode2 conditional propagate:true
2 - myNode2 updated
MyNode2 conditional propagate:false
------- complete buffering events -----------

------ triggering calculation ------
Child:triggered
```

What’s happening:

- Events are buffered via processor.bufferEvent(...). The underlying nodes receive and process those events, and
  @OnParentUpdate callbacks fire as parents update, but the child’s @OnTrigger is not invoked yet.
- MyNode2’s integer handler conditionally propagates based on the value (> 100). The buffered 200 propagates and causes
  a second "myNode2 updated"; the buffered 50 does not propagate beyond MyNode2.
- Calling processor.triggerCalculation() executes a calculation pass that invokes the child’s @OnTrigger method exactly
  once, after all buffered updates have been applied.

Tips:

- Use bufferEvent when you want to stage multiple inputs and apply them in one triggered calculation, which can help
  avoid intermediate recomputations.
- Combine @OnParentUpdate for fine-grained reaction to parent changes and @OnTrigger to gate when downstream work
  actually happens.
- Conditional propagation from event handlers lets you control whether upstream changes should influence downstream
  computations before the final trigger.
