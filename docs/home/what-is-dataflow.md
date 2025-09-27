# What is DataFlow programming?

DataFlow programming expresses computations as a graph of values and the dependencies between them. Instead of telling
the program when to recompute, you describe how outputs depend on inputs. When inputs change, the runtime updates only
what is needed.

## Key ideas

- Graph of nodes: nodes are functions or stateful components; edges indicate dependencies.
- Incremental updates: only affected nodes recompute when an event arrives.
- Deterministic order: execution follows a topological order derived from the graph.
- Push‑based: events push changes through the graph; no polling.
- Bounded side effects: state updates occur where you declare them; easier to reason and test.

## Why it matters for streams

- Streaming systems process unbounded sequences of events. DataFlow ensures each event triggers the minimal work to keep
  outputs correct.
- Determinism reduces race conditions and surprises common in async callback chains.
- Explicit dependencies make performance and correctness more predictable.

## How Fluxtion applies DataFlow

- You build graphs using a builder DSL or by annotating POJOs with handler/trigger methods.
- The builder computes the dependency DAG and generates a DataFlow with precomputed dispatch.
- At runtime, events are delivered to handlers; changes propagate to dependents; each node runs at most once per event.

## Common patterns

- Per‑key state: maintain state per entity (user, device, symbol) and propagate updates only to dependent calculations.
- Windows and aggregations: compute moving averages, counts, or sums; update incrementally.
- Joins and enrichment: merge streams by key and derive new signals.
- Alerting: trigger alerts when derived signals cross thresholds.

## Learn more

- [Fundamentals](dataflow-fundamentals.md)
- [Why Fluxtion](why-fluxtion.md)
