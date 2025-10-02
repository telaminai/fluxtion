# Fluxtion dispatch diagrams
---

This page provides small, text‑first diagrams you can skim to build intuition.
It covers the minimal conceptual set:

- Execution graph (nodes and edges)
- Event dispatch order (topological execution)
- Trigger types (update, publish, reset) with tiny timelines
- Windowing (sliding vs tumbling) with bucket visuals

You can use this alongside the runnable examples in this repository.

## 1) Execution graph: nodes and edges

A DataFlow is compiled into an execution graph. Nodes are operators (map, filter, groupBy, window, aggregate, sink).
Edges represent dependency from upstream to downstream.

Example: subscribe → map → groupBy → window → aggregate → sink

```
[String events]
     │
     ▼
  subscribe ──► map ──► groupBy(key) ──► window(1m sliding) ──► aggregate(avg) ──► sink(print)
```

- Each node has inputs and outputs.
- When an upstream node produces a value, downstream dependents may run.
- Fluxtion builds a minimal, ordered execution plan so nodes run only when needed.

---

## 2) Event dispatch order (topological execution)

On each input event, Fluxtion walks the dependency DAG in topological order. Upstreams execute before their dependents.
Side branches are evaluated only if affected by the incoming change.

Example with a shared upstream feeding two branches:

```
 source
   │
   ▼
  map ──► filter ──► sink A
   │
   └────► aggregate ──► sink B
```

Typical dispatch for one event (high‑level):

1. source deserializes/produces value
1. map transforms
1. Branch 1: filter decides to forward or drop, then sink A possibly runs
1. Branch 2: aggregate updates state, then sink B runs if a publish is triggered

Only nodes impacted by the event are scheduled. This keeps work minimal and predictable.

---

## 3) Trigger types: update, publish, reset (timelines)

Triggers control when downstream consumers see state changes. Think of them as small policies attached to nodes.

Key ideas:

- update: propagate updates downstream as they occur
- publish: hold changes until an explicit publish trigger fires
- reset: clear state (often a special event or schedule) without publishing a value

Tiny timelines below show value changes over time (t0..t5). A solid dot (●) marks when a value is emitted to downstream.

### a) Update trigger

```
Upstream value:   a ──── b ──── c ──── d
Downstream emit: ●a ─── ●b ─── ●c ─── ●d   (every change flows immediately)
Time:             t0   t1   t2   t3
```

### b) Publish trigger (defer until publish event)

```
Upstream value:   a ──── b ──── c ──── d
Publish events:   P                    P
Downstream emit: ●a ───────────────── ●d   (only on publish; latest buffered value)
Time:             t0   t1   t2   t3   t4
```

- The node accepts updates a,b,c,d but emits only on P. If multiple updates occur between publishes, only the latest is
  emitted.

### c) Reset trigger (clear state without emitting)

```
State value:      a ──── b ──── X ──── c
Reset events:            R
Downstream emit: ●a ─── ●b            ●c   (reset clears state; no value emitted at reset time)
Time:             t0   t1   t2   t3
```

- Reset is orthogonal: you can combine update/publish policies with resets for lifecycle control.

See runnable samples in reference/trigger:

- Update trigger: [TriggerUpdateSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerUpdateSample.java)
- Publish trigger: [TriggerPublishSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerPublishSample.java)
- Reset trigger: [TriggerResetSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerResetSample.java)
---

## 4) Windowing: sliding vs tumbling (bucket visuals)

Windows partition time (or count) into buckets. Aggregations read one or more buckets to compute a result.

### a) Sliding window

- Overlapping buckets move forward by a step smaller than the window size.
- Good for continuously updated metrics (moving average, rolling sum).

```
Time ─────────────────────────────────────────►
Buckets (size=5s, step=1s):
[0–5) [1–6) [2–7) [3–8) [4–9) [5–10) ...
  │       │      │      │      │
  ▼       ▼      ▼      ▼      ▼
  compute at each step using the overlapping range
```

### b) Tumbling window

- Non‑overlapping buckets that cover the timeline back‑to‑back.
- Good for periodic reports (per‑minute counts, per‑hour sums).

```
Time ─────────────────────────────────────────►
Buckets (size=5s, step=5s):
[0–5) [5–10) [10–15) [15–20) [20–25) ...
  │       │        │        │
  ▼       ▼        ▼        ▼
  compute once per bucket when it closes
```

### Visualizing bucket contribution

For a moving average in a 5s sliding window, bucket membership changes gradually:

```
Event times: 0s 1s 2s 3s 4s 5s 6s 7s 8s
Window [t-5,t): at t=6s covers 1–6s; at t=7s covers 2–7s, etc.
Overlaps fade out old events as new ones enter.
```

See runnable samples in reference/windowing:

- Sliding window: [SlidingWindowSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/SlidingWindowSample.java)
- Tumbling window: [TumblingWindowSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/TumblingWindowSample.java)
- Tumbling window with trigger: [TumblingTriggerSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/TumblingTriggerSample.java)

---

## Putting it together


1. Event arrives at a source and gets mapped to a typed value
1. The scheduler evaluates only impacted nodes in topological order
1. Triggers determine when a new value is visible downstream
1. Windows manage time‑scoped state; aggregates compute results over buckets
1. Sinks receive final values (push to console, DB, message bus, etc.)

