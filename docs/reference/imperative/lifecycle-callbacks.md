# Lifecycle callbacks
---

The section documents the lifecycle api and its behaviour. 

Classes bound into an [EventProcessor]({{fluxtion_src_runtime}}/runtime/src/main/java/com/fluxtion/runtime/EventProcessor.java) register for lifecycle callbacks with annotations on methods. 
A [Lifecycle]({{fluxtion_src_runtime}}/lifecycle/Lifecycle.java) interface is implemented by the generated EventProcessor, lifecycle method calls are routed to 
annotated callback methods.

The source project for the examples can be found [here]({{fluxtion_example_src}}/runtime-execution/src/main/java/com/fluxtion/example/reference/lifecycle)


## Lifecycle - init
`EventProcessor#init` Calls init on any node in the graph that has registered for an init callback. The init calls
are invoked in topological order.

## Lifecycle - teardown
`EventProcessor#tearDown` Calls tearDown on any node in the graph that has registered for an tearDown callback.
The tearDown calls are invoked reverse topological order.

## Lifecycle - start
`EventProcessor#start` Calls start on any node in the graph that has registered for an onStart callback. The start calls
are invoked in topological order. Start must be called after init

## Lifecycle - stop
`EventProcessor#stop` Calls stop on any node in the graph that has registered for an onStop callback.
The stop calls are invoked reverse topological order.

## Attaching a user node to lifecycle callback
User nodes that are added to the processing graph can attach to the lifecycle callbacks by annotating methods with 
the relevant annotations.

See sample - [LifecycleCallback.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/bindnode/callback/LifecycleCallback.java)

```java
public class LifecycleCallback {
    public static class MyNode {

        @Initialise
        public void myInitMethod() {
            System.out.println("Initialise");
        }

        @Start
        public void myStartMethod() {
            System.out.println("Start");
        }

        @Stop
        public void myStopMethod() {
            System.out.println("Stop");
        }

        @TearDown
        public void myTearDownMethod() {
            System.out.println("TearDown");
        }
    }

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new MyNode())
                .build();

        //init is implicitly called
        processor.start();
        processor.stop();
        processor.tearDown();
    }
}
```

Output
```console
Initialise
Start
Stop
TearDown
```