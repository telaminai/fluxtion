
# DataFlow functional DSL
---

The DataFlowBuilder supports functional construction of event processing logic, this allows developers to bind
functions into the processor without having to construct classes marked with annotations. The goal of using the
functional DSL is to have no Fluxtion api calls in the business logic only vanilla java.

All examples are located in [dataflow-examples project](https://github.com/telaminai/dataflow-examples) reference
module

**Advantages of using DataFlow functional DSL**

- Business logic components are re-usable and testable outside DataFlow
- Clear separation between event notification and business logic, event logic is removed from business code
- Complex library functions like windowing and aggregation are well tested and natively supported
- Increased developer productivity, less code to write and support
- New functionality is simple and cheap to integrate, DataFlowBuilder pays the cost of rewiring the event flow
- No vendor lock-in, business code is free from any Fluxtion library dependencies

###API overview

DataFlowBuilder offers a DSL to bind functions into the event processor using the familiar map/filter/peek similar to the java
stream api. Bound functions are invoked in accordance to the [dispatch rules](../../home/dataflow-fundamentals.md#event-dispatch-rules).
A DataFlow starts a calculation cycle when there is a matching subscriber dispatch rule.

The [DataFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/DataFlow.java) class provides builder methods to
create and bind event streams to [DataFlow]. There is no restriction on the number of DataFlows bound inside a host
DataFlow.

## DataFlow
---
A DataFlow is a live structure where new events trigger a set of dispatch operations. We create a DataFlow with:

```java
DataFlowBuilder.[subscribe operation].build()
```
