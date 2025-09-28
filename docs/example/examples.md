# Fluxtion examples

A practical, copy‑and‑run catalog of small examples that show how to build high‑performance, type‑safe, event‑driven
DataFlows with Fluxtion.

## [Fluxtion example repo](https://github.com/telaminai/fluxtion-examples)

Fluxtion lets you compose real‑time streaming logic as declarative DataFlows. You connect sources, transformations,
joins, windows, and sinks into an efficient runtime graph and push events through it with minimal overhead.

## Why these examples exist

- Give you a fast feel for the DataFlow API (minutes, not hours)
- Demonstrate the core patterns you’ll actually use: map/filter, groupBy, joins, windows, triggers, sinks
- Provide copy‑pasteable snippets you can adapt into your own project
- Offer a gentle path from a tiny hello‑world to feature‑focused “cookbook” samples

## Who this is for

- Java developers building event processing, streaming analytics, or stateful, low‑latency services
- Anyone exploring whether Fluxtion fits their problem (e.g., replacing ad‑hoc listeners with a clear dataflow)

If you’re new to Fluxtion, start with the getting‑started examples, then dip into the reference (cookbook) when you need
one feature in isolation.

## Getting Started [git repo]({{fluxtion_example_src}}/getting-started)

Quick introductions and progressive tutorials that build intuition fast. This module contains simple,
self‑contained examples to help you learn Fluxtion’s DataFlow API quickly. Start here if
you’re new: run a tiny hello‑world flow, then explore short snippets for windowing, triggers, and multi‑feed
joins. A tutorial series (Part 1–5) builds concepts step by step.

- Hello, Fluxtion: the minimal subscribe → map →
  print: [HelloFluxtion.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/HelloFluxtion.java)
- Quickstart: average speed by make with sliding window (groupBy +
  windowing): [GroupByWindowExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/quickstart/GroupByWindowExample.java)
- Front‑page snippets
    - Windowing: [WindowExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/windowing/WindowExample.java)
    - Triggering: [TriggerExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/triggering/TriggerExample.java)
    - Multi‑join: [MultiFeedJoinExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/multijoin/MultiFeedJoinExample.java)
- Tutorials (progressive walkthrough)
    - [TutorialPart1]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart1.java) — Basics: build a tiny flow to compute per-symbol running net quantity.
    - [TutorialPart2]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart2.java) — Sliding windows: per-symbol rolling average with thresholded alerts.
    - [TutorialPart3]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart3.java) — Mix DSL with an imperative stateful node and lifecycle callbacks.
    - [TutorialPart4]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart4.java) — Embed a DataFlow in a HTTP microservice.
    - [TutorialPart5]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart5.java) — Wire file feeds and sinks: read from a file, transform, and write to a file.

## Reference [git repo]({{fluxtion_example_src}}/reference)
  This module is a cookbook of small, focused samples that each demonstrate one Fluxtion feature in isolation. Browse by
  area (functional ops, group-by, windowing, triggers, nodes, event feeds) and open the corresponding Java file. All
  samples are runnable from your IDE (look for a public static void main)

### Functional building blocks
- Map: [MapSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/MapSample.java)
- Filter: [FilterSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/FilterSample.java)
- FlatMap: [FlatMapSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/FlatMapSample.java)
- Merge: [MergeSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/MergeSample.java)
- Merge and
  map: [MergeAndMapSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/MergeAndMapSample.java)
- Default
  value: [DefaultValueSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/DefaultValueSample.java)
- Bi-map: [BiMapSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/BiMapSample.java)
- Re‑entrant
  events: [ReEntrantEventSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/ReEntrantEventSample.java)
- Subscribe to
  event: [SubscribeToEventSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/SubscribeToEventSample.java)
- Reset
  function: [ResetFunctionSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/ResetFunctionSample.java)
- Sink: [SinkExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/SinkExample.java)
- Get node by
  id: [GetFlowNodeByIdExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/functional/GetFlowNodeByIdExample.java)

### Grouping and joins

- Basic groupBy: [GroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupBySample.java)
- Group by specific
  fields: [GroupByFieldsSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByFieldsSample.java)
- Group by map
  key: [GroupByMapKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByMapKeySample.java)
- Group by map
  values: [GroupByMapValuesSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByMapValuesSample.java)
- Reduce grouped
  values: [GroupByReduceSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByReduceSample.java)
- To
  list: [GroupByToListSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByToListSample.java)
- To
  set: [GroupByToSetSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByToSetSample.java)
- Inner
  join: [GroupByJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByJoinSample.java)
- Left outer
  join: [GroupByLeftOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByLeftOuterJoinSample.java)
- Right outer
  join: [GroupByRightOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByRightOuterJoinSample.java)
- Full outer
  join: [GroupByFullOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByFullOuterJoinSample.java)
- Multi‑join: [MultiJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/MultiJoinSample.java)
- Sliding
  groupBy: [SlidingGroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/SlidingGroupBySample.java)
- Sliding groupBy (compound
  key): [SlidingGroupByCompoundKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/SlidingGroupByCompoundKeySample.java)
- Tumbling
  groupBy: [TumblingGroupBySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/TumblingGroupBySample.java)
- Tumbling groupBy (compound
  key): [TumblingGroupByCompoundKeySample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/TumblingGroupByCompoundKeySample.java)
- Delete group
  entries: [GroupByDeleteSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByDeleteSample.java)

### Windowing and triggers

- Sliding
  window: [SlidingWindowSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/SlidingWindowSample.java)
- Tumbling
  window: [TumblingWindowSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/TumblingWindowSample.java)
- Tumbling window with
  trigger: [TumblingTriggerSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/windowing/TumblingTriggerSample.java)

### Triggers

- Update
  trigger: [TriggerUpdateSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerUpdateSample.java)
- Publish
  trigger: [TriggerPublishSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerPublishSample.java)
- Reset
  trigger: [TriggerResetSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerResetSample.java)
- Publish
  override: [TriggerPublishOverrideSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/trigger/TriggerPublishOverrideSample.java)

### Working with nodes

- Wrap
  functions: [WrapFunctionsSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/WrapFunctionsSample.java)
- Subscribe to
  node: [SubscribeToNodeSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/SubscribeToNodeSample.java)
- Push pattern: [PushSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/PushSample.java)
- Member variable
  supplier: [FlowSupplierAsMemberVariableSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/node/FlowSupplierAsMemberVariableSample.java)

### Event feeds and runners

- Data flow runner (file
  feed): [DataFlowRunnerSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/eventfeed/DataFlowRunnerSample.java)

## Prerequisites

- Java 21+ (examples include headers for newer JDKs and work on current LTS)
- Maven 3.9+
- jbang 

## Learn more

- [Fluxtion repository](https://github.com/telaminai/fluxtion)
- [Choosing Fluxtion](../home/choosing-fluxtion.md)
- [Quickstart guide](../getting-started/quickstart.md)
- [Examples repository](https://github.com/telaminai/fluxtion-examples)

## Contributing / feedback

Please open issues in the Fluxtion repository for questions, ideas, or any problems you find while using these examples.
