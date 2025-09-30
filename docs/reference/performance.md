# Performance
---

Fluxtion can generate ahead of time high performance event processors that are suitable for the most demanding low
latency environments. This section documents the performance results for a test project.

Jmh is used to measure throughput per second and latency for a single calculation cycle.

HdrHistogram is used to record latency percentiles for an execution run

!!! note "Results are in the nanosecond range"
    Fluxtion operates with sub-microsecond response times

## Summary results

* **50 million events processed per second**
* **Average latency is 20 nanoseconds to process one event including app logic**
* **The Fluxtion event processor is in the low nanosecond range for event processing overhead**
* **Zero gc**
* **Single threaded application**

## Test setup

The [test project]({{fluxtion_example_src}}/compiler/aot-compiler) process a market update and carries out a set of
calculation for each market data price ladder event:

```java
public class PriceLadder {
    private final int[] bidSizes = new int[5];
    private final int[] bidPrices = new int[5];
    private final int[] askSizes = new int[5];
    private final int[] askPrices = new int[5];
}
```

* A graph with four nodes each performing some calculations
* A set of 10,000 randomly generated data events is passed into the processor for processing
* Single threaded
* No core pinning or isolation
* AOT generated version of an event processor is under test

The goal is to create a representative test with randomly distributed input data set to remove bias in the results.

### Node description

| Eval order | Class                                                                                                                                                        | Description                                                                                         |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| Node 1     | [MidCalculator]({{fluxtion_example_src}}/compiler/aot-compiler/src/main/java/com/telamin/fluxtion/example/compile/aot/node/MidCalculator.java)               | mid price calculator                                                                                |
| Node 2     | [SkewCalculator]({{fluxtion_example_src}}/compiler/aot-compiler/src/main/java/com/telamin/fluxtion/example/compile/aot/node/SkewCalculator.java)             | Skew calculation, move all prices at each level by the skew amount                                  |
| Node 3     | [LevelsCalculator]({{fluxtion_example_src}}/compiler/aot-compiler/src/main/java/com/telamin/fluxtion/example/compile/aot/node/LevelsCalculator.java)         | Remove levels by setting to zero for price and volume <br/>if max levels < input ladder level count |
| Node 4     | [PriceLadderPublisher]({{fluxtion_example_src}}/compiler/aot-compiler/src/main/java/com/telamin/fluxtion/example/compile/aot/node/PriceLadderPublisher.java) | Publish the calculated PriceLadder to a consumer for distribution                                   |


### AOT generated event processor

Fluxtion generates an ahead-of-time (AOT) event processor that is optimized for the specific graph of nodes and their
relationships. The generated code:

- Creates a single class that handles all event routing
- Maintains node references and dependencies
- Executes nodes in the correct order based on dependencies
- Eliminates reflection and dynamic dispatch
- Removes all runtime overhead of graph traversal

The generated event processor for this test is here [PriceLadderProcessor.java]({{fluxtion_example_src}}/compiler/aot-compiler/src/main/java/com/telamin/fluxtion/example/compile/aot/generated/PriceLadderProcessor.java)


## Throughput and per operation results
```console
Benchmark                                            Mode  Cnt         Score   Error  Units
PriceLadderBenchmark.throughPut_BranchingProcessor  thrpt    2  50872626.050          ops/s
PriceLadderBenchmark.avgTime_BranchingProcessor      avgt    2        20.234          ns/op
```

The average time to process an event is 20 nanoseconds, this includes the application logic invoked for each node.

## Latency distribution

At 99.99% the latency is measured at 0.083 microseconds, which includes the underlying machine jitter. The results are
recorded using HdrHistogram which adds an overhead of several nanoseconds to each reading. The tail of the app latency
follows the tail for jitter on the test machine with no app code executing, this tells us the jitter at the higher
end is due to the machine and not the application.

[![](../images/aot_latency_histogram.png)](../images/aot_latency_histogram.png){:target="_blank"}

* blue line - total latency
* red line - machine jitter with no processing