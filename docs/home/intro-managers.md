# Fluxtion for managers and non‑technical readers
---

Fluxtion is a Java library your engineers embed in services to process real‑time data streams with low latency and high
reliability. It organizes business logic as a graph of small steps, so when new data arrives, only the necessary parts
update. This makes systems faster, simpler to understand, and cheaper to run.

## Business benefits

- Faster time‑to‑market: developers focus on business rules; Fluxtion handles the plumbing of event delivery.
- Lower running costs: the engine does only the minimum work per event and can be compiled ahead of time for extra
  speed.
- Predictable behavior: clear, deterministic execution—critical for compliance, operations, and user trust.
- Flexible deployment: no servers to manage—ships as a normal Java library inside your existing services or
  microservices.
- Infrastructure choice: not tied to Kafka Streams, Flink, or any specific platform—IT can choose or change the messaging/compute stack without rewriting business logic.

## When to use it

- Real‑time alerts, monitoring, or risk controls
- Live metrics and aggregations from event streams (e.g., transactions, telemetry)
- Enriching and correlating events from multiple data sources

## What Fluxtion is not

- Not a “big data” cluster or streaming platform. It doesn’t require Kafka Streams, Flink, or Spark (though it can work
  alongside them).
- Not a database. It computes derived values and signals in memory; persistence is up to your chosen storage.

## How teams work with it

- Engineers design a dataflow (a directed graph) that represents the business logic.
- The Fluxtion builder analyzes it and generates efficient code that runs inside your service.
- The result is easier to test, predict, and maintain than ad‑hoc event code.

## Learn more

- [Overview](introduction.md)
- [Why Fluxtion](why-fluxtion.md)
- [DataFlow basics](../reference/what-is-dataflow.md)
