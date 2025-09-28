# Binding user classes (Imperative DataFlow)

This section documents how user agents modeled as Java classes or pure functions are integrated into a DataFlow.

## Event handling primer
User classes bound into an [DataFlow]({{fluxtion_src_runtime}}/DataFlow.java)
register for event callbacks with annotations. The generated EventProcessor
implements the onEvent method acting as a bridge between external event streams and bound processing logic.
User code reads the event streams calling onEvent with each new event received, the event processor then notifies
annotated callback methods according to the [dispatch rules](../../home/dataflow-fundamentals#event-dispatch-rules ).

## Examples
The source project for the examples can be found
[here]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/bindnode)
To process an event stream correctly the following requirements must be met:

!!! warning "Thread safety"
    EventProcessors are not thread safe a single event should be processed at one time.

---

Use the mini‑sections below for specific topics:

- Basics: handling events and multiple types
- Filtering: filtering events and filter variables
- Triggering: controlling child triggering and identifying parents
- Callbacks: after‑event/after‑trigger and push triggers
- Trigger reference: propagation and dirty/clean triggering controls
- Advanced: collections, concurrent forking, batch support
