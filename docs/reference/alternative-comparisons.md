# Head‑to‑head comparison: Fluxtion vs alternatives

Scope note: Kafka Streams and Flink are stream processing frameworks tightly integrated with distributed runtimes (Kafka
broker / cluster). Reactor/RxJava/Akka Streams are reactive libraries for composing async pipelines. Fluxtion is an
in‑process, deterministic dataflow library focused on predictable dispatch and low latency.

- Kafka Streams
    - Strengths: Kafka‑native DSL, state stores with changelogs, exactly‑once semantics (EOS), repartitioning, windowed
      aggregations, scale‑out across partitions, fault tolerance via Kafka.
    - Trade‑offs: Requires Kafka; higher operational footprint; backpressure tied to Kafka throughput; more moving
      parts; not ideal for ultra‑low‑latency in‑process use where durability isn’t required.
    - Fluxtion vs KS: Fluxtion wins for embedded, single‑process low latency, deterministic per‑event updates, minimal
      allocations, and tests that run in milliseconds. KS wins for durable, partitioned, horizontally scalable
      processing where Kafka is the event backbone and state recovery is mandatory.

- Apache Flink (and Spark Structured Streaming)
    - Strengths: Distributed, fault‑tolerant, complex windowing, exactly‑once sinks, rich connectors.
    - Trade‑offs: Heavyweight to operate; cluster needed; higher latency compared to in‑memory library.
    - Fluxtion vs Flink: Fluxtion is ideal for edge, microservices, and embedded analytics where a JVM process suffices.
      Flink is for large‑scale, distributed streaming with durability and complex state.

- Reactor / RxJava (and Mutiny)
    - Strengths: Rich operator sets, async composition, backpressure (Reactive Streams), vast ecosystem.
    - Trade‑offs: Execution order can be non‑obvious; concurrency introduces nondeterminism; allocation and context
      switching overhead; testing can be trickier.
    - Fluxtion vs Reactor/Rx: Fluxtion provides static dependency analysis, deterministic topological dispatch, and
      “at‑most‑once per node per event” semantics. Use Reactor/Rx for async I/O pipelines and backpressure across
      boundaries; use Fluxtion for deterministic, CPU‑bound in‑process computation.

- Akka Streams
    - Strengths: Powerful graph model, supervision, backpressure, distributed story with Akka Cluster.
    - Trade‑offs: Actor overhead and operational complexity; learning curve.
    - Fluxtion vs Akka: Fluxtion is leaner and simpler for single‑JVM deterministic computation; Akka is stronger for
      complex, distributed streaming with supervision/backpressure needs.