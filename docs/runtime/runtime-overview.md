# DataFlow runtime overview

The DataFlow runtime provides built‑in support for interacting with a running graph. In addition to submitting input
events, applications can:

- Pass contextual parameters into the graph and read them inside nodes via DataFlowContext
- Dispatch new events programmatically from within handlers using EventDispatcher
- Inspect and control calculation dirtiness using DirtyStateMonitor and then trigger recomputation

This runtime section describes and houses examples that demonstrate how to interact with a running DataFlow application.

See also:

- Interacting with the DataFlow runtime (context, callbacks, dirty state): [dataflow-context](dataflow-context.md)
- Working with clocks and time: [clocks-and-time](clocks-and-time.md)
