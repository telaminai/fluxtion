# Event Processor Model

Fluxtion is based on DataFlow programming, expressing computations as a graph of values and the dependencies between them. A DataFlow in Fluxtion is a deterministic event processor constructed from a directed acyclic graph (DAG) of stateful components.

Execution order and propagation paths are derived from the dependency structure rather than explicitly defined by the developer. This derivation process is referred to as execution inference, where propagation paths and execution order are computed from the dependency graph at build time. Instead of telling the program when to recompute, you describe how outputs depend on inputs. When inputs change, the runtime updates only what is needed in a single, topologically ordered pass.

!!! warning "Thread safety"
    EventProcessors are not thread safe; a single event should be processed at one time.

### Key ideas

- **Push-based**: Events push changes through the graph; no polling.
- **Incremental updates**: Only affected nodes recompute when an event arrives.
- **Deterministic order**: Execution follows a topological order derived from the graph.
- **Bounded side effects**: State changes occur only within declared nodes, making behavior easier to reason about and test.

### Mental model

A DataFlow behaves like a spreadsheet:

- components are cells
- dependencies are formula references
- updates propagate only to affected components

When an input changes, only the dependent parts of the graph are recomputed, in a fixed and deterministic order.

### Structural model

The processor is represented as a directed acyclic graph (DAG):

- nodes represent stateful components
- edges represent dependencies
- execution order is derived via topological sorting

This ensures that all dependencies of a node are evaluated before the node itself executes.

This guarantees glitch-free propagation: each node observes a fully resolved upstream state.

## Why it matters for streams

- **Minimal work**: DataFlow ensures each event triggers the minimal work to keep outputs correct, which is essential for processing unbounded sequences of events.
- **Predictability**: Determinism reduces race conditions and surprises common in async callback chains.
- **Transparency**: Explicit dependencies make performance and correctness more predictable.

## Event dispatch rules

Event propagation follows these rules:

- Top-level event handlers are invoked with the input event
- A handler returns a boolean indicating whether propagation should continue
- Dependent nodes are notified only after all parent nodes complete
- Each node is invoked at most once per event cycle
- Execution follows strict topological order
- Nodes not connected to the active event path are not invoked
- Propagation continues recursively through dependent nodes
- Dependencies may be direct or indirect (via reference chains)

## Construction model

The processor is constructed using dependency injection principles:

- components declare dependencies through fields, constructors, or annotations
- the system builds the dependency graph from these relationships
- event handlers and trigger methods define propagation entry points

No separate coordination logic or pipeline definition is required.

## Common patterns

- **Per-key state**: Maintain state per entity (user, device, symbol) and propagate updates only to dependent calculations.
- **Windows and aggregations**: Compute moving averages, counts, or sums; update incrementally.
- **Joins and enrichment**: Merge streams by key and derive new signals.
- **Alerting**: Trigger alerts when derived signals cross thresholds.
