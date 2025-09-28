# Fluxtion
---

## Welcome to Fluxtion!

Fluxtion is a Java library and builder for real-time, in‑memory dataflow processing. It helps you express business logic
as a directed graph of small, composable functions and stateful components that update incrementally when new events
arrive.

Think of it like a spreadsheet for streams: when an input changes, only the dependent cells recompute. Fluxtion brings
this incremental, dependency-driven update model to event streams, enabling predictable, low‑latency processing with
clear dependencies and excellent performance.

## Highlights

- DataFlow programming model: declare how values depend on each other, not when to recompute.
- Deterministic execution: events propagate through a topologically sorted graph.
- Low overhead: avoids generic reactive machinery; directly invokes methods in dependency order.
- Scales down and up: great for microservices, trading systems, IoT gateways, or embedded analytics.
- Familiar Java: plain objects and methods; no special runtime server required.
- Infrastructure‑agnostic: not tied to Kafka Streams, Flink, or any specific platform—you choose the messaging/compute stack it runs on.

## Quickstart

Install the builder (which depends on the runtime) and try a 2‑minute example.

Maven (pom.xml):

```xml
<dependency>
  <groupId>com.telamin.fluxtion</groupId>
  <artifactId>fluxtion-builder</artifactId>
  <version>{{fluxtion_version}}</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}")
```

Hello world (Java 21):

```java
import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

public class HelloFluxtion {
    public static void main(String[] args) {
        DataFlow dataFlow = DataFlowBuilder
                .subscribe(String.class)           // accept String events
                .map(String::toUpperCase)          // transform
                .console("msg:{}")                 // print to console
                .build();                          // build the DataFlow

        dataFlow.onEvent("hello");  // prints: msg:HELLO
        dataFlow.onEvent("world");  // prints: msg:WORLD
    }
}
```

## Core building blocks

- Events: any Java object submitted into the DataFlow.
- Nodes: functions or stateful components wired together by dependencies.
- Graph: a DAG computed by the builder that determines dispatch order.
- Handlers and triggers: annotated methods that receive events or fire when dependencies update.

## Where Fluxtion fits

- Real‑time analytics and monitoring
- Complex event processing and enrichment
- Per‑key aggregations (counts, moving windows)
- Signal generation and alerting
- Incremental computation pipelines

!!! info "Choosing Fluxtion (compare to Kafka Streams, Reactor, Flink)"
    Not sure if Fluxtion is the right fit? Read the decision guide: [Choosing Fluxtion](home/choosing-fluxtion.md).

!!! example "Explore examples"
    Browse runnable samples in one click: [Examples catalog](example/examples.md).

## Code samples


=== "Windowing"

    ```java
    public class WindowExample {
        record CarTracker(String id, double speed) {}
        public static void main(String[] args) {

            //calculate average speed, sliding window 5 buckets of 200 millis
            DataFlow averageCarSpeed = DataFlowBuilder.subscribe(CarTracker::speed)
                    .slidingAggregate(Aggregates.doubleAverageFactory(), 200, 5)
                    .map(v -> "average speed: " + v.intValue() + " km/h")
                    .sink("average car speed")
                    .build();

            //register an output sink
            averageCarSpeed.addSink("average car speed", System.out::println);

            //send data from an unbounded real-time feed
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                    () -> averageCarSpeed.onEvent(
                        new CarTracker("car-reg", new Random().nextDouble(100))),
                        100, 100, TimeUnit.MILLISECONDS);
        }
    }
    ```

=== "Triggering"

    ```java
    public class TriggerExample {
        public static void main(String[] args) {
            DataFlow sumDataFlow = DataFlowBuilder.subscribe(Integer.class)
                    .aggregate(Aggregates.intSumFactory())
                    .resetTrigger(
                        DataFlowBuilder.subscribeToSignal("resetTrigger"))
                    .filter(i -> i != 0)
                    .publishTriggerOverride(
                        DataFlowBuilder.subscribeToSignal("publishSumTrigger"))
                    .console("Current sun:{}")
                    .build();
    
            sumDataFlow.onEvent(10);
            sumDataFlow.onEvent(50);
            sumDataFlow.onEvent(32);
            //publish
            sumDataFlow.publishSignal("publishSumTrigger");
    
            //reset sum
            sumDataFlow.publishSignal("resetTrigger");
    
            //new sum
            sumDataFlow.onEvent(8);
            sumDataFlow.onEvent(17);
            //publish
            sumDataFlow.publishSignal("publishSumTrigger");
        }
    }
    ```

=== "Multifeed join"

    ```java
    import java.util.Random;
    import java.util.concurrent.Executors;
    import java.util.concurrent.TimeUnit;
    
    public class MultiFeedJoinExample {
        public static void main(String[] args) {
            //stream of realtime machine temperatures grouped by machineId
            DataFlow currentMachineTemp = DataFlowBuilder
                    .groupBy(MachineReadingEvent::id, MachineReadingEvent::temp);
            
            //create a stream of averaged machine sliding temps,
            //with a 4-second window and 1 second buckets grouped by machine id
            DataFlow avgMachineTemp = DataFlowBuilder
                    .subscribe(MachineReadingEvent.class)
                    .groupBySliding(
                        MachineReadingEvent::id,
                        MachineReadingEvent::temp,
                        DoubleAverageFlowFunction::new,
                        1000,4);
            
            //join machine profiles with contacts and then with readings.
            //Publish alarms with stateful user function
            DataFlow tempMonitor = DataFlowBuilder
                    .groupBy(MachineProfileEvent::id)
                    .mapValues(MachineState::new)
                    .mapBi(DataFlowBuilder.groupBy(SupportContactEvent::locationCode), Helpers::addContact)
                    .innerJoin(currentMachineTemp, MachineState::setCurrentTemperature)
                    .innerJoin(avgMachineTemp, MachineState::setAvgTemperature)
                    .publishTriggerOverride(FixedRateTrigger.atMillis(1_000))
                    .filterValues(MachineState::outsideOperatingTemp)
                    .map(GroupBy::toMap)
                    .map(new AlarmDeltaFilter()::updateActiveAlarms)
                    .filter(AlarmDeltaFilter::isChanged)
                    .sink("alarmPublisher")
                    .build();
    
            runSimulation(tempMonitor);
        }
    
        private static void runSimulation(DataFlow tempMonitor) {
            //any java.util.Consumer can be used as sink
            tempMonitor.addSink("alarmPublisher", Helpers::prettyPrintAlarms);
    
            //set up machine locations
            tempMonitor.onEvent(
                new MachineProfileEvent("server_GOOG", LocationCode.USA_EAST_1, 70, 48));
            tempMonitor.onEvent(
                new MachineProfileEvent("server_AMZN", LocationCode.USA_EAST_1, 99.999, 65));
            tempMonitor.onEvent(
                new MachineProfileEvent("server_MSFT", LocationCode.USA_EAST_2,92, 49.99));
            tempMonitor.onEvent(
                new MachineProfileEvent("server_TKM", LocationCode.USA_EAST_2,102, 50.0001));
    
            //set up support contacts
            tempMonitor.onEvent(
                new SupportContactEvent("Jean", LocationCode.USA_EAST_1, "jean@fluxtion.com"));
            tempMonitor.onEvent(
                new SupportContactEvent("Tandy", LocationCode.USA_EAST_2, "tandy@fluxtion.com"));
    
            //Send random MachineReadingEvent using `DataFlow.onEvent` 
            Random random = new Random();
            final String[] MACHINE_IDS = new String[]{
                "server_GOOG", "server_AMZN", "server_MSFT", "server_TKM"};

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                        String machineId = MACHINE_IDS[
                            random.nextInt(MACHINE_IDS.length)];

                        double temperatureReading = random.nextDouble() * 100;

                        tempMonitor.onEvent(
                            new MachineReadingEvent(machineId, temperatureReading));
                    },
                    10_000, 1, TimeUnit.MICROSECONDS);
    
            System.out.println("Simulation started - wait four seconds for first machine readings\n");
        }
    }
    ```

## Start here

- [Introduction](home/introduction.md)
- [Why Fluxtion](home/why-fluxtion.md)
- [What is DataFlow](home/what-is-dataflow.md)
- [For engineers](home/intro-engineers.md)
- [For managers](home/intro-managers.md)

## Reference

- [DataFlow fundamentals](home/dataflow-fundamentals.md)
- [Reference guide](reference/reference-documentation.md)