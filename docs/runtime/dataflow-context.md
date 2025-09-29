# Interacting with the DataFlow runtime

This guide describes how to interact with a running DataFlow graph using the runtime services provided by
fluxtion‑runtime. It summarizes the behavior demonstrated by the reference examples in the fluxtion‑examples repository
under:

 [Context examples]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context)

What you can do at runtime:

- Provide and read contextual parameters for your graph
- Dispatch additional events from inside handlers
- Mark parts of the graph as dirty and drive recomputation

## Context parameters (DataFlowContext)

- Purpose: pass configuration or dynamic values into the graph without changing the event stream.
- How it works:
    - The application adds key/value pairs to the processor before starting.
    - Nodes read those values via an injected runtime context.
    - Typical pattern: use a lifecycle "on start" callback to load values into node state.
- Example to check in the examples repo: [ContextParamInput.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/ContextParamInput.java)
    - Demonstrates adding two parameters from the application and reading them in a node at start.

## Emitting new events from handlers (EventDispatcher)

- Purpose: from within a handler, split or transform an input and send one or more new events back through the graph as
  separate event cycles.
- How it works:
    - A node receives an input (e.g., a delimited String) and, for each item, dispatches a new event of another type.
    - The dispatched items are processed as independent event cycles to keep processing deterministic.
- Example to check in the examples repo: [CallBackExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/CallBackExample.java)
    - Demonstrates receiving a String payload, splitting it, and dispatching the items as Integer events for downstream
      processing.

## Controlling recomputation (DirtyStateMonitor)

- Purpose: explicitly mark data as "dirty" and then trigger a compute pass so downstream triggered nodes run even if no
  new input events arrive.
- How it works:
    - Check whether a contribution is dirty.
    - Mark it dirty when you need to force dependent calculations to re‑run.
    - Ask the processor to perform a calculation pass.
- Example to check in the examples repo: [DirtyStateMonitorExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/DirtyStateMonitorExample.java)
    - Demonstrates reading the dirty flag, marking a flow as dirty, and initiating a recomputation cycle.

## Where to look in the code

- Context parameters: [ContextParamInput.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/ContextParamInput.java)
- Event callbacks: [CallBackExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/CallBackExample.java)
- Dirty state: [DirtyStateMonitorExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/context/DirtyStateMonitorExample.java)

Tip: the examples are small and self‑contained; open them in your IDE and run the main methods to see console output
that highlights each runtime feature.