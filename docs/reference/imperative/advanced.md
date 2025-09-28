# Advanced topics

## Collection support
Collections or arrays of references are supported, if any element in the collection fires a change notification the 
trigger method will be called. The trigger method is invoked only once per event cycle whatever the number of 
parent's updating. 

Parent change identity can be tracked using the `@OnParentUpdate` annotation.

```java
public static class MyNode {
    @FilterId
    private final String filter;
    private final String name;

    public MyNode(String filter, String name) {
        this.filter = filter;
        this.name = name;
    }

    @OnEventHandler
    public boolean handleIntSignal(IntSignal intSignal) {
        System.out.printf("MyNode-%s::handleIntSignal - %s%n", filter, intSignal.getValue());
        return true;
    }
}

public static class Child {
    private final MyNode[] nodes;
    private int updateCount;

    public Child(MyNode... nodes) {
        this.nodes = nodes;
    }

    @OnParentUpdate
    public void parentUpdated(MyNode updatedNode) {
        updateCount++;
        System.out.printf("parentUpdated '%s'%n", updatedNode.name);
    }

    @OnTrigger
    public boolean triggered() {
        System.out.printf("Child::triggered updateCount:%d%n%n", updateCount);
        updateCount = 0;
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new Child(
            new MyNode("A", "a_1"),
            new MyNode("A", "a_2"),
            new MyNode("B", "b_1")));
    processor.init();
    processor.publishIntSignal("A", 10);
    processor.publishIntSignal("B", 25);
    processor.publishIntSignal("A", 12);
    processor.publishIntSignal("C", 200);
}
```

Output
```console
MyNode-A::handleIntSignal - 10
parentUpdated 'a_1'
MyNode-A::handleIntSignal - 10
parentUpdated 'a_2'
Child::triggered updateCount:2

MyNode-B::handleIntSignal - 25
parentUpdated 'b_1'
Child::triggered updateCount:1

MyNode-A::handleIntSignal - 12
parentUpdated 'a_1'
MyNode-A::handleIntSignal - 12
parentUpdated 'a_2'
Child::triggered updateCount:2
```

## Forking concurrent trigger methods
Forking trigger methods is supported. If multiple trigger methods are fired from a single parent they can be forked to 
run in parallel using the fork join pool. Only when all the forked trigger methods have completed will an event notification
be propagated to their children. 

To for a trigger callback use `@OnTrigger(parallelExecution = true)` annotation on the callback method.

```java
public static class MyNode {
    @OnEventHandler
    public boolean handleStringEvent(String stringToProcess) {
        System.out.printf("%s MyNode::handleStringEvent %n", Thread.currentThread().getName());
        return true;
    }
}

public static class ForkedChild {
    private final MyNode myNode;
    private final int id;

    public ForkedChild(MyNode myNode, int id) {
        this.myNode = myNode;
        this.id = id;
    }

    @OnTrigger(parallelExecution = true)
    public boolean triggered() {
        int millisSleep = new Random(id).nextInt(25, 200);
        String threadName = Thread.currentThread().getName();
        System.out.printf("%s ForkedChild[%d]::triggered - sleep:%d %n", threadName, id, millisSleep);
        try {
            Thread.sleep(millisSleep);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("%s ForkedChild[%d]::complete %n", threadName, id);
        return true;
    }
}

public static class ResultJoiner {
    private final ForkedChild[] forkedTasks;

    public ResultJoiner(ForkedChild[] forkedTasks) {
        this.forkedTasks = forkedTasks;
    }

    public ResultJoiner(int forkTaskNumber){
        MyNode myNode = new MyNode();
        forkedTasks = new ForkedChild[forkTaskNumber];
        for (int i = 0; i < forkTaskNumber; i++) {
            forkedTasks[i] = new ForkedChild(myNode, i);
        }
    }

    @OnTrigger
    public boolean complete(){
        System.out.printf("%s ResultJoiner:complete %n%n", Thread.currentThread().getName());
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new ResultJoiner(5));
    processor.init();

    Instant start = Instant.now();
    processor.onEvent("test");

    System.out.printf("duration: %d milliseconds%n", Duration.between(start, Instant.now()).toMillis());
}

```

Output
```console
main MyNode::handleStringEvent
ForkJoinPool.commonPool-worker-1 ForkedChild[0]::triggered - sleep:135
ForkJoinPool.commonPool-worker-2 ForkedChild[1]::triggered - sleep:85
ForkJoinPool.commonPool-worker-3 ForkedChild[2]::triggered - sleep:58
ForkJoinPool.commonPool-worker-4 ForkedChild[3]::triggered - sleep:184
ForkJoinPool.commonPool-worker-5 ForkedChild[4]::triggered - sleep:112
ForkJoinPool.commonPool-worker-3 ForkedChild[2]::complete
ForkJoinPool.commonPool-worker-2 ForkedChild[1]::complete
ForkJoinPool.commonPool-worker-5 ForkedChild[4]::complete
ForkJoinPool.commonPool-worker-1 ForkedChild[0]::complete
ForkJoinPool.commonPool-worker-4 ForkedChild[3]::complete
main ResultJoiner:complete

duration: 184 milliseconds
```

## Batch support
Batch callbacks are supported through the BatchHandler interface that the generated EventHandler implements. Any methods 
that are annotated with, `@OnBatchPause` or `@OnBatchEnd` will receive calls from the matching BatchHandler method. 

```java
public static class MyNode {
    @OnEventHandler
    public boolean handleStringEvent(String stringToProcess) {
        System.out.println("MyNode event received:" + stringToProcess);
        return true;
    }

    @OnBatchPause
    public void batchPause(){
        System.out.println("MyNode::batchPause");
    }

    @OnBatchEnd
    public void batchEnd(){
        System.out.println("MyNode::batchEnd");
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new MyNode());
    processor.init();

    processor.onEvent("test");

    //use BatchHandler service
    BatchHandler batchHandler = (BatchHandler)processor;
    batchHandler.batchPause();
    batchHandler.batchEnd();
}
```

Output
```console
MyNode event received:test
MyNode::batchPause
MyNode::batchEnd
```
