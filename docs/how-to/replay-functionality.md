# How to Use Event Replay

This guide explains how to use Fluxtion's event replay functionality to record and replay event streams with deterministic, data-driven time. Event replay is a powerful debugging and testing tool available in the **Fluxtion AOT Compiler** (commercial feature, not open source).

## Overview

Event replay enables you to:

- **Capture production event streams** to an event log with timestamps
- **Replay those events** in development with exact reproduction of production behavior
- **Debug production issues** locally with full IDE debugging capabilities
- **Test changes** against real production data
- **Achieve deterministic time** for reproducible time-dependent logic

For a complete working example, see the [Event Replay Demo]({{fluxtion_example_src}}/compiler/replay-events).

!!! info "Commercial Feature"
    Event replay is part of the Fluxtion AOT Compiler, which is a commercial product. It is not available in the open-source runtime.

## Key Concepts

### Event Sourcing Pattern

Event replay implements an event sourcing pattern with two phases:

1. **Recording phase**: Events flowing through the system are captured to an event log along with their timestamps
2. **Replay phase**: The recorded events are fed back into a fresh processor instance, recreating the exact same behavior

### Data-Driven Time

The key innovation is **data-driven time**. Instead of using wall clock time during replay, the processor's `Clock` is set to the recorded timestamp of each event. This ensures time-dependent logic (timeouts, scheduling, aggregations) executes deterministically during replay.

### AOT Compilation Advantages

Event replay leverages **AOT (Ahead-of-Time) compilation** to generate the event processor at build time. This provides critical advantages:

- **Static dataflow graph**: The exact same logic runs in production and during replay
- **Debuggable production code**: Set breakpoints in generated event handlers and step through production logic
- **No dynamic reconstruction**: The graph structure is frozen at compile time, guaranteeing identical behavior

## Use Cases

### 1. Debug Production Issues in Your IDE

The primary use case is bringing production problems into your development environment:

- Capture event logs from production systems experiencing issues
- Replay those exact events locally with full IDE debugging capabilities
- Set breakpoints, inspect variables, and step through the exact logic that ran in production
- **No need to guess** what happened by reading log files

### 2. Test Changes Against Real Production Data

Validate changes using actual production event streams:

- Verify bug fixes work against the actual event stream that caused the problem
- Ensure enhancements don't break existing behavior with real-world data
- Perform regression testing using historical event logs

### 3. Deterministic Time for Reproducible Results

Eliminate timing-related flakiness:

- Time-dependent business logic behaves identically on replay
- No flaky tests due to timing issues
- Audit logs from replay exactly match production audit logs

### 4. Reduced Support Costs

Streamline production support:

- Diagnose and fix production issues offline without direct production access
- Share reproducible test cases with the development team
- Validate fixes before deploying to production

## The Clock Abstraction

### Injecting Clock into Nodes

Fluxtion provides a `Clock` interface that nodes can inject to access time:

```java
public class GlobalPnl implements NamedNode {
    public Clock clock = Clock.DEFAULT_CLOCK;

    @OnTrigger
    public boolean calculate() {
        String time = dateFormat.format(new Date(clock.getProcessTime()));
        System.out.println(time + "," + calculateTotal());
        return true;
    }
}
```

### Default Behavior: Wall Clock Time

By default, `Clock.DEFAULT_CLOCK` returns system wall clock time—the actual current time when events are processed.

### Replay Behavior: Data-Driven Time

During replay, `YamlReplayRunner` automatically:

1. Replaces the default clock with a synthetic clock strategy
2. Before each event is dispatched, sets the clock to the recorded timestamp of that event
3. Ensures all time-dependent logic sees the historical time, not current time

This is why replay output shows **identical timestamps** to the original run, even when replayed hours or days later.

### Time as Event Stream Data

The recorded event log captures time as data:

```yaml
- eventTime: 1696857658794
  event: !com.telamin.fluxtion.example.compile.replay.replay.PnlUpdate
    bookName: "book1"
    amount: 200
```

During replay, `eventTime` becomes the clock value, making time deterministic and controllable.

## Recording Events

### YamlReplayRecordWriter

The `YamlReplayRecordWriter` auditor captures events to a YAML event log:

```java
import com.telamin.fluxtion.compiler.replay.YamlReplayRecordWriter;

// In your AOT graph builder
processorConfig.addAuditor(
    new YamlReplayRecordWriter().classWhiteList(PnlUpdate.class), 
    YamlReplayRecordWriter.DEFAULT_NAME
);
```

**Features**:

- Sees events **before** any business logic processes them
- Records each event with its timestamp
- Supports whitelist/blacklist/all filtering of event types
- Can write to any `Writer` (file, string, network stream)

### Configuring the Auditor

Add the auditor in your AOT graph builder class:

```java
public class GlobalPnlAOTGraphBuilder implements FluxtionGraphBuilder {
    @Override
    public void buildGraph(ProcessorConfig processorConfig) {
        // Build your dataflow graph
        BookPnl book1 = processorConfig.addNode(new BookPnl("book1"));
        BookPnl bookAAA = processorConfig.addNode(new BookPnl("bookAAA"));
        GlobalPnl globalPnl = processorConfig.addNode(
            new GlobalPnl(List.of(book1, bookAAA))
        );

        // Add replay recording auditor
        processorConfig.addAuditor(
            new YamlReplayRecordWriter().classWhiteList(PnlUpdate.class),
            YamlReplayRecordWriter.DEFAULT_NAME
        );
    }

    @Override
    public void configureGeneration(FluxtionCompilerConfig compilerConfig) {
        compilerConfig.setClassName("GlobalPnlProcessor");
        compilerConfig.setPackageName(
            "com.telamin.fluxtion.example.compile.replay.replay.generated"
        );
    }
}
```

### Recording to Different Targets

#### In-Memory (for testing)

```java
StringWriter eventLog = new StringWriter();

GlobalPnlProcessor processor = new GlobalPnlProcessor();
processor.init();

YamlReplayRecordWriter auditor = 
    processor.getAuditorById(YamlReplayRecordWriter.DEFAULT_NAME);
auditor.setTargetWriter(eventLog);

processor.start();
processor.onEvent(new PnlUpdate("book1", 200));
processor.onEvent(new PnlUpdate("bookAAA", 55));

// eventLog.toString() contains the YAML
```

#### File-Based (for production)

```java
FileWriter fileWriter = new FileWriter("events-" + LocalDate.now() + ".yaml");

YamlReplayRecordWriter auditor = 
    processor.getAuditorById(YamlReplayRecordWriter.DEFAULT_NAME);
auditor.setTargetWriter(fileWriter);

processor.start();
// Process events...

fileWriter.close();
```

### Event Type Filtering

Control which events are recorded:

```java
// Record only specific event types
new YamlReplayRecordWriter().classWhiteList(
    PnlUpdate.class, 
    TradeEvent.class, 
    MarketData.class
)

// Record all except specific types
new YamlReplayRecordWriter().classBlackList(HeartbeatEvent.class)

// Record all events (default)
new YamlReplayRecordWriter().recordAll()
```

## Replaying Events

### YamlReplayRunner

The `YamlReplayRunner` reads a YAML event log and replays events into a processor:

```java
import com.telamin.fluxtion.compiler.replay.YamlReplayRunner;

String eventLog = "..."; // Load from file or string

YamlReplayRunner.newSession(
        new StringReader(eventLog), 
        new GlobalPnlProcessor())
    .callInit()      // Initialize the processor
    .callStart()     // Call start lifecycle
    .runReplay();    // Replay all events with data-driven time
```

### Complete Replay Example

```java
private static void runReplay(String eventLog) {
    // Create a fresh processor instance
    GlobalPnlProcessor processor = new GlobalPnlProcessor();

    // Replay the events
    YamlReplayRunner.newSession(new StringReader(eventLog), processor)
        .callInit()
        .callStart()
        .runReplay();

    // Optionally inspect final state
    GlobalPnl globalPnl = processor.getNodeById("globalPnl");
    System.out.println("Final P&L: " + globalPnl.getCurrentPnl());
}
```

### Replay from File

```java
@Test
public void replayProductionIssue() throws Exception {
    // Load captured production events
    Reader eventLog = new FileReader("production-issue-2025-10-09.yaml");
    
    GlobalPnlProcessor processor = new GlobalPnlProcessor();
    
    YamlReplayRunner.newSession(eventLog, processor)
        .callInit()
        .callStart()
        .runReplay();
    
    // Verify expected behavior
    GlobalPnl globalPnl = processor.getNodeById("globalPnl");
    assertEquals(expectedValue, globalPnl.getCurrentPnl());
}
```

## Event Log Format

The YAML event log format is human-readable and includes timestamps:

```yaml
- eventTime: 1696857658794
  event: !com.telamin.fluxtion.example.compile.replay.replay.PnlUpdate
    bookName: "book1"
    amount: 200
- eventTime: 1696857659075
  event: !com.telamin.fluxtion.example.compile.replay.replay.PnlUpdate
    bookName: "bookAAA"
    amount: 55
```

Each record contains:

- **`eventTime`**: The wall clock time when the event was processed (milliseconds since epoch)
- **`event`**: The event object serialized to YAML with its fully-qualified class type

## Complete Working Example

The [Event Replay Demo]({{fluxtion_example_src}}/compiler/replay-events) demonstrates recording and replay of a P&L calculation system.

### Example Application Structure

```
replay-events/
├── src/main/java/com/telamin/fluxtion/example/compile/replay/replay/
│   ├── GlobalPnlAOTGraphBuilder.java  # AOT builder definition
│   ├── GeneraEventLogMain.java        # Demo: record & replay
│   ├── PnlUpdate.java                 # Event class
│   ├── BookPnl.java                   # Per-book P&L handler
│   ├── GlobalPnl.java                 # Aggregate P&L calculator
│   └── generated/
│       └── GlobalPnlProcessor.java    # AOT-generated processor
└── pom.xml
```

### Event Class: PnlUpdate

```java
public class PnlUpdate implements Event {
    String bookName;  // Which trading book
    int amount;       // P&L amount

    @Override
    public String filterString() {
        return bookName;  // Routes events to correct BookPnl
    }
}
```

### Node Class: BookPnl

Handles P&L updates for a specific trading book:

```java
public class BookPnl implements NamedNode {
    private final String bookName;
    private transient int pnl;

    @OnEventHandler(filterVariable = "bookName")
    public boolean pnlUpdate(PnlUpdate pnlUpdate) {
        pnl = pnlUpdate.getAmount();
        return true;  // Trigger downstream nodes
    }

    public int getPnl() {
        return pnl;
    }
}
```

The `filterVariable = "bookName"` ensures each `BookPnl` only processes events for its book.

### Aggregator: GlobalPnl

Aggregates P&L across all books with time-aware output:

```java
public class GlobalPnl implements NamedNode {
    public Clock clock = Clock.DEFAULT_CLOCK;  // Injected by Fluxtion
    private final List<BookPnl> bookPnlList;
    private final SimpleDateFormat dateFormat = 
        new SimpleDateFormat("HH:mm:ss.SSS");

    @OnTrigger
    public boolean calculate() {
        String time = dateFormat.format(new Date(clock.getProcessTime()));
        int pnl = bookPnlList.stream().mapToInt(BookPnl::getPnl).sum();
        System.out.println(time + "," + pnl);
        return true;
    }
}
```

**Key points**:

- Injects `Clock` to access process time
- Uses injected clock for timestamp formatting
- During replay, sees historical timestamps, not current time

### Main Demo: Recording and Replay

```java
public class GeneraEventLogMain {
    public static void main(String[] args) throws Exception {
        StringWriter eventLog = new StringWriter();

        // PHASE 1: Record events
        System.out.println("CAPTURE RUN:");
        System.out.println("time,globalPnl");
        generateEventLog(eventLog);

        // PHASE 2: Replay events
        System.out.println("\nREPLAY RUN:");
        System.out.println("time,globalPnl");
        runReplay(eventLog.toString());
    }

    private static void generateEventLog(Writer writer) throws Exception {
        GlobalPnlProcessor processor = new GlobalPnlProcessor();
        processor.init();

        // Configure auditor to write to our writer
        YamlReplayRecordWriter auditor =
            processor.getAuditorById(YamlReplayRecordWriter.DEFAULT_NAME);
        auditor.setTargetWriter(writer);

        processor.start();
        processor.onEvent(new PnlUpdate("book1", 200));
        Thread.sleep(250);  // Real time passes
        processor.onEvent(new PnlUpdate("bookAAA", 55));
    }

    private static void runReplay(String eventLog) {
        YamlReplayRunner.newSession(
                new StringReader(eventLog),
                new GlobalPnlProcessor())
            .callInit()
            .callStart()
            .runReplay();
    }
}
```

### Sample Output

```
CAPTURE RUN:
time,globalPnl
14:40:58.794,200
14:40:59.075,255

REPLAY RUN:
time,globalPnl
14:40:58.794,200
14:40:59.075,255
```

**Notice**: The timestamps are identical! Even though replay happens later, the data-driven clock ensures the same timestamps appear.

## Building the Example

### Generate the AOT Processor

First, generate the `GlobalPnlProcessor` by running the builder:

```bash
cd {{fluxtion_example_src}}/compiler/replay-events
mvn -q compile exec:java \
  -Dexec.mainClass="com.telamin.fluxtion.example.compile.replay.replay.GlobalPnlAOTGraphBuilder"
```

This creates `src/main/java/.../generated/GlobalPnlProcessor.java`.

### Run the Demo

```bash
mvn -q compile exec:java \
  -Dexec.mainClass="com.telamin.fluxtion.example.compile.replay.replay.GeneraEventLogMain"
```

You'll see the capture run followed by the replay run with identical outputs.

## Debugging with Replay

One of the most powerful uses of event replay is debugging production issues in your IDE.

### Workflow

1. **Capture production events**: Deploy with `YamlReplayRecordWriter` configured to write to a file
2. **Retrieve the event log**: Copy the YAML file from production to your development machine
3. **Create a test case**: Load the file and replay into your processor
4. **Debug in IDE**:
    - Set breakpoints in your business logic (e.g., `BookPnl.pnlUpdate`)
    - Set breakpoints in the generated code (e.g., `GlobalPnlProcessor.handleEvent`)
    - Step through the exact event sequence that occurred in production
    - Inspect all variable states and call stacks

### Example Debug Test

```java
@Test
public void debugProductionIssue() throws Exception {
    // Load captured production events
    Reader eventLog = new FileReader("production-issue-2025-10-09.yaml");

    GlobalPnlProcessor processor = new GlobalPnlProcessor();

    // Set breakpoint on next line and step into
    YamlReplayRunner.newSession(eventLog, processor)
        .callInit()
        .callStart()
        .runReplay();

    // Assertions to verify fix
    GlobalPnl globalPnl = processor.getNodeById("globalPnl");
    assertEquals(expectedValue, globalPnl.getCurrentPnl());
}
```

### Generated Code is Debuggable

Because the AOT compiler generates normal Java source code, you can:

- Set breakpoints directly in the generated event handlers
- Step through the exact dispatch logic that ran in production
- Inspect the complete call stack and variable states
- The generated code is readable and understandable

This is a unique advantage of AOT compilation—**what you debug is exactly what ran in production**.

## Static Dataflow Benefits

The AOT compilation approach provides unique advantages for replay:

### Identical Logic Guarantee

- The `.class` files deployed to production are the same as in your development environment
- No configuration files that might differ between environments
- No dynamic graph assembly that might vary based on runtime conditions
- What you debug is **exactly** what ran in production

### No Dynamic Reconstruction

Traditional dynamic dataflow systems reconstruct graphs at runtime, which can lead to:

- Different behavior if configuration or dependencies have changed
- Inability to reproduce production issues if the dynamic construction differs
- Complex debugging as the runtime graph structure may not match production

With AOT compilation, the graph structure is **frozen at compile time**, guaranteeing identical behavior during replay.

### Performance

- No runtime graph construction overhead
- Direct method calls instead of reflection
- JIT-friendly code patterns
- Generated code is optimized for the specific dataflow

## Advanced Usage

### Custom Replay Formats

While the demo uses YAML for readability, production systems might benefit from:

- **Binary formats**: Avro, Protobuf for better performance and smaller size
- **Compressed storage**: GZIP or LZ4 compression of event logs
- **Database backends**: Store events in PostgreSQL, MongoDB, or time-series databases
- **Streaming replay**: Replay from Kafka topics or other message systems

The `YamlReplayRecordWriter` and `YamlReplayRunner` serve as reference implementations you can customize.

### Partial Replay

Replay a subset of events by pre-processing the YAML:

```java
// Load and filter events
List<String> allEvents = Files.readAllLines(Path.of("events.yaml"));
List<String> filteredEvents = allEvents.stream()
    .filter(line -> line.contains("book1"))  // Only book1 events
    .collect(Collectors.toList());

String filteredLog = String.join("\n", filteredEvents);
runReplay(filteredLog);
```

### Replay with State Inspection

Inspect intermediate state during replay:

```java
YamlReplayRunner runner = YamlReplayRunner.newSession(
    new StringReader(eventLog),
    processor
);

runner.callInit();
runner.callStart();

// Replay events one at a time with inspection
while (runner.hasMoreEvents()) {
    runner.replayNextEvent();
    
    // Inspect state after each event
    GlobalPnl state = processor.getNodeById("globalPnl");
    System.out.println("Current P&L: " + state.getCurrentPnl());
}
```

### Integration Testing

Use replay for integration testing with production data:

```java
@Test
public void testAgainstProductionData() throws Exception {
    // Arrange: Load last week's production events
    Reader eventLog = new FileReader("prod-events-2025-10-02.yaml");
    GlobalPnlProcessor processor = new GlobalPnlProcessor();
    
    // Act: Replay production events
    YamlReplayRunner.newSession(eventLog, processor)
        .callInit()
        .callStart()
        .runReplay();
    
    // Assert: Verify system behavior
    GlobalPnl globalPnl = processor.getNodeById("globalPnl");
    assertTrue(globalPnl.getCurrentPnl() > 0, "Should have positive P&L");
    
    // Regression check: Compare against expected results
    String expectedResults = Files.readString(
        Path.of("prod-results-2025-10-02.txt")
    );
    assertEquals(expectedResults, capturedOutput);
}
```

## Best Practices

### Production Recording

1. **Use file rotation**: Don't let event logs grow indefinitely
   ```java
   String filename = "events-" + LocalDate.now() + ".yaml";
   FileWriter writer = new FileWriter(filename, true); // append mode
   ```

2. **Filter carefully**: Record only events needed for debugging
   ```java
   new YamlReplayRecordWriter().classWhiteList(CriticalEvent.class)
   ```

3. **Monitor storage**: Event logs can grow large in high-throughput systems

4. **Include metadata**: Add application version, environment info to log files

### Development Replay

1. **Use version control**: Store example event logs in your repository
   ```
   src/test/resources/replay/
   ├── bug-123-reproduction.yaml
   ├── edge-case-scenario.yaml
   └── performance-test.yaml
   ```

2. **Automate tests**: Create test cases that replay known scenarios

3. **Document scenarios**: Include README files explaining what each replay demonstrates

4. **Sanitize data**: Remove sensitive information from production logs before committing

### Testing Strategy

1. **Unit tests**: Test individual nodes without replay
2. **Integration tests**: Use replay to test complete dataflows with realistic sequences
3. **Regression tests**: Replay logs from past bugs to prevent regressions
4. **Performance tests**: Replay high-volume production logs to identify bottlenecks

## Troubleshooting

### Clock Not Injected

**Problem**: Time-dependent logic doesn't respect replay timestamps.

**Solution**: Ensure your nodes use the injected `Clock`:

```java
public class MyNode {
    public Clock clock = Clock.DEFAULT_CLOCK;  // Must be public field
    
    @OnTrigger
    public boolean process() {
        long time = clock.getProcessTime();  // Use clock, not System.currentTimeMillis()
        // ...
    }
}
```

### Event Not Recorded

**Problem**: Some events don't appear in the event log.

**Solution**: Check your event type filter:

```java
// Make sure your event class is whitelisted
new YamlReplayRecordWriter().classWhiteList(MyEvent.class)

// Or use recordAll() to capture everything
new YamlReplayRecordWriter().recordAll()
```

### Replay Behavior Differs

**Problem**: Replayed output doesn't match original.

**Possible causes**:

1. **Non-injected clock**: Using `System.currentTimeMillis()` instead of injected `Clock`
2. **External dependencies**: Code depends on external state (databases, files) that differs
3. **Random number generation**: Using non-deterministic random without seeding
4. **Different processor version**: Ensure same generated code version

### YAML Parsing Errors

**Problem**: `YamlReplayRunner` fails to parse the event log.

**Solution**:

1. Verify YAML format is valid
2. Ensure event classes are on the classpath
3. Check for custom serialization requirements
4. Use YAML linter to validate format

## Related Documentation

- [State and Recovery](state-and-recovery.md) - Managing state and checkpoints in Fluxtion
- [Unit Testing DataFlow](unit-testing-dataflow.md) - Testing strategies for DataFlow applications
- [Event Replay Demo]({{fluxtion_example_src}}/compiler/replay-events) - Complete working example
- [Fluxtion AOT Compiler](https://fluxtion.com/compiler) - Commercial compiler documentation

## Summary

Event replay is a powerful tool for:

- **Debugging production issues** by replaying exact event sequences in your IDE
- **Testing with real data** using captured production event streams
- **Achieving deterministic behavior** through data-driven time
- **Reducing support costs** by enabling offline diagnosis of production problems

The combination of AOT compilation and event replay provides a unique debugging experience where:

- The code you debug is exactly what ran in production
- Time is deterministic and controllable
- You can step through production event sequences with full IDE support

This makes Fluxtion event replay an invaluable tool for building reliable, debuggable event-driven systems.
