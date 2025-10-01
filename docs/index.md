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

!!! tip "Performance at a glance"
    - 50 million events per second (thrpt)
    - ~20 ns average latency per event including app logic
    - Low‑nanosecond processing overhead, zero GC, single‑threaded
    
    See detailed benchmarks and methodology: [Performance results](reference/performance.md).

## Quickstart

Install [Jbang](https://www.jbang.dev/documentation/jbang/latest/installation.html) and try a Hello world (Java 25):

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

void main() {
    DataFlow dataFlow = DataFlowBuilder
            .subscribe(String.class)           // accept String events
            .map(String::toUpperCase)          // transform
            .console("msg:{}")                 // print to console
            .build();                          // build the DataFlow

    dataFlow.onEvent("hello");  // prints: msg:HELLO
    dataFlow.onEvent("world");  // prints: msg:WORLD
}
```

copy the code into a hello.java and run it with `jbang hello.java`

```console
> jbang hello.java 
[jbang] Building jar for hello.java...
msg:HELLO
msg:WORLD
```

## Where Fluxtion fits

- Real‑time analytics and monitoring
- Complex event processing and enrichment
- Per‑key aggregations (counts, moving windows)
- Signal generation and alerting
- Incremental computation pipelines

!!! info "Choosing Fluxtion (compare to Kafka Streams, Reactor, Flink)"
    Not sure if Fluxtion is the right fit? Read the decision guide: [Choosing Fluxtion](reference/choosing-fluxtion.md).

!!! example "Explore examples"
    Browse runnable samples in one click: [Examples catalog](example/examples.md).

## Core building blocks

- Events: any Java object submitted into the DataFlow.
- Nodes: functions or stateful components wired together by dependencies.
- Graph: a DAG computed by the builder that determines dispatch order.
- Handlers and triggers: annotated methods that receive events or fire when dependencies update.

## Code samples


=== "Windowing"
    See example [WindowExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/windowing/WindowExample.java)

    ```java
    //DEPS com.telamin.fluxtion:fluxtion-builder:0.9.6
    //JAVA 25
    
    import com.telamin.fluxtion.builder.DataFlowBuilder;
    import com.telamin.fluxtion.runtime.DataFlow;
    import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
    
    import java.util.Random;
    import java.util.concurrent.Executors;
    import java.util.concurrent.TimeUnit;
    
    record CarTracker(String id, double speed) { }
    
    void main() {
        //calculate average speed, sliding window 5 buckets of 200 millis
        DataFlow averageCarSpeed = DataFlowBuilder
            .subscribe(CarTracker::speed)
            .slidingAggregate(Aggregates.doubleAverageFactory(), 200, 5)
            .map(v -> "average speed: " + v.intValue() + " km/h")
            .sink("average car speed")
            .build();
    
        //register an output sink
        averageCarSpeed.addSink("average car speed", System.out::println);
    
        //send data from an unbounded real-time feed
        Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(
                        () -> averageCarSpeed.onEvent(
                            new CarTracker("car-reg", 
                            new Random().nextDouble(100))),
                100, 100, TimeUnit.MILLISECONDS);
    }
    ```

    console output:
    ```console
    jbang WindowExample.java
    [jbang] Building jar for WindowExample.java...
    average speed: 59 km/h
    average speed: 60 km/h
    average speed: 68 km/h
    ```

=== "Triggering"
    See example [TriggerExample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/triggering/TriggerExample.java)

    ```java
    //DEPS com.telamin.fluxtion:fluxtion-builder:0.9.6
    //JAVA 25
    
    import com.telamin.fluxtion.builder.DataFlowBuilder;
    import com.telamin.fluxtion.runtime.DataFlow;
    import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;
    
    void main() {
        var resetSignal = DataFlowBuilder.subscribeToSignal("resetTrigger");
        var publishSignal = DataFlowBuilder.subscribeToSignal("publishSumTrigger");
    
        DataFlow sumDataFlow = DataFlowBuilder
                .subscribe(Integer.class)
                .aggregate(Aggregates.intSumFactory())
                .resetTrigger(resetSignal)
                .filter(i -> i != 0)
                .publishTriggerOverride(publishSignal)
                .console("Current sum:{}")
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
    ```

    console output:
    ```console
    jbang Triggering.java
    [jbang] Building jar for Triggering.java...
    Current sum:92
    Current sum:25
    ```

=== "Stateful functions"
    See example [SubscribeToNodeSample.java]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/imperative/SubscribeToNodeSample.java)

    ```java
    //DEPS com.telamin.fluxtion:fluxtion-builder:0.9.6
    //JAVA 25
    
    import com.telamin.fluxtion.builder.DataFlowBuilder;
    import com.telamin.fluxtion.runtime.DataFlow;
    import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
    import com.telamin.fluxtion.runtime.flowfunction.helpers.Collectors;
    
    void main() {
        DataFlow processor = DataFlowBuilder
                .subscribeToNode(new MyComplexNode())
                .console("node triggered -> {}")
                .map(MyComplexNode::getIn)
                .aggregate(Collectors.listFactory(4))
                .console("last 4 elements:{}\n")
                .build();
    
        processor.onEvent("A");
        processor.onEvent("B");
        processor.onEvent("C");
        processor.onEvent("D");
        processor.onEvent("E");
        processor.onEvent("F");
    }
    
    public static class MyComplexNode {
        private String in;
    
        @OnEventHandler
        public boolean stringUpdate(String in) {
            this.in = in;
            return true;
        }
    
        public String getIn() {
            return in;
        }
    }
    ```

    console output:
    ```console
    jbang StatefulFunction.java
    [jbang] Building jar for StatefulFunction.java...
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[A]
    
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[A, B]
    
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[A, B, C]
    
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[A, B, C, D]
    
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[B, C, D, E]
    
    node triggered -> scratch$MyComplexNode@4923ab24
    last 4 elements:[C, D, E, F]
    ```
=== "Poll feeds"
    See example [PollFeedExample]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart5.java)

    ```java
    //DEPS com.telamin.fluxtion:fluxtion-builder:0.9.6
    //JAVA 21
    //JAVA_OPTIONS --add-opens java.base/jdk.internal.misc=ALL-UNNAMED
    
    import com.telamin.fluxtion.builder.DataFlowBuilder;
    import com.telamin.fluxtion.runtime.DataFlow;
    import com.telamin.fluxtion.runtime.connector.DataFlowConnector;
    import com.telamin.fluxtion.runtime.connector.FileEventFeed;
    import com.telamin.fluxtion.runtime.connector.FileMessageSink;
    import com.telamin.fluxtion.runtime.eventfeed.ReadStrategy;
    
    public class TutorialPart5 {
        public static void main(String[] args) throws Exception {
            // Feed: publish each line from input file
            // as a String event into feed "myFeed"
            FileEventFeed myFileFeed = new FileEventFeed(
                    "./tutorial4-input.txt",    // input file to tail
                    "myFeed",                   // logical feed name
                    ReadStrategy.EARLIEST       // tail from start of file
            );
    
            // DataFlow: subscribe to the named feed, 
            // log, uppercase, log, then emit to a named sink
            DataFlow dataFlow = DataFlowBuilder
                    .subscribeToFeed("myFeed", String.class)
                    .console("read file in:{}")
                    .map(String::toUpperCase)
                    .console("write file out:{}\n")
                    .sink("output")              // name the sink "output"
                    .build();
    
            // Sink: bind sink name "output" to an output file
            FileMessageSink outputFile = new FileMessageSink("./tutorial4-output.txt");
    
            // Connector: owns threads for the processor and the feed. 
            // Then wires everything together
            DataFlowConnector runner = new DataFlowConnector();
            runner.addDataFlow(dataFlow);
            runner.addFeed(myFileFeed);
            runner.addSink("output", outputFile);
    
            // Start: spins up threads and begins processing
            runner.start();
        }
    }
    ```

=== "Multifeed join"
    See example [MultiFeedJoinExample]({{fluxtion_example_src}}/getting-started/src/main/java/com/telamin/fluxtion/example/frontpage/multijoin/)

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

    console output:
    ```console
    Application started - wait four seconds for first machine readings
    
    ALARM UPDATE 09:45:20.685
    New alarms: ['server_GOOG@USA_EAST_1',  temp:'97.57', avgTemp:'50.07' SupportContactEvent[name=Jean, locationCode=USA_EAST_1, contactDetails=jean@fluxtion.com], 'server_TKM@USA_EAST_2',  temp:'5.15', avgTemp:'50.02' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com], 'server_MSFT@USA_EAST_2',  temp:'46.72', avgTemp:'50.00' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com]]
    Alarms to clear[]
    Current alarms[server_GOOG, server_TKM, server_MSFT]
    ------------------------------------
    
    ALARM UPDATE 09:45:21.680
    New alarms: []
    Alarms to clear[server_TKM, server_MSFT]
    Current alarms[server_GOOG]
    ------------------------------------
    
    ALARM UPDATE 09:45:26.680
    New alarms: ['server_MSFT@USA_EAST_2',  temp:'97.16', avgTemp:'49.91' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com]]
    Alarms to clear[]
    Current alarms[server_GOOG, server_MSFT]
    ------------------------------------
    ```

## Library dependencies

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

## Start here

- [Introduction](home/introduction.md)
- [Why Fluxtion](home/why-fluxtion.md)
- [What is DataFlow](reference/what-is-dataflow.md)
- [For engineers](home/intro-engineers.md)
- [For managers](home/intro-managers.md)

## Reference

- [DataFlow fundamentals](reference/dataflow-fundamentals.md)
- [Reference guide](reference/reference-documentation.md)