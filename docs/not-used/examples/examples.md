---
title: Examples
has_children: true
nav_order: 300
published: true
layout: default
---

# Examples
---

A set of examples that explore the usage of Fluxtion in a variety of scenarios. All examples are on github as a single 
project, cloning the repo will help the reader explore the code locally and improve the learning experience.

## Executing a DataFlow

All projects that build a Fluxtion DataFlow at runtime follow similar steps

1. **Build the DataaFLow using DataFlowBuilder utility**
   - Use DataFlow dsl to build a processor
   - Optionally integrate user agents into the DataFlow
   - An EventProcessor instance is returned ready to be used
2. **Integrate the event processor in the app and feed it events**
  - To publish events to the processor call DataFlow.onEvent(object)
  - To call exported service functions on the DataFlow
        - Lookup the exported service with DataFlow.getExportedService()
        - Store the service reference and call methods on it
