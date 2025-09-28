# Tutorial Part‑5 — Feeds and sinks with file I/O
---

In this tutorial you will:

- Use a FileEventFeed to push file lines into a DataFlow.
- Use a FileMessageSink to write processed results to an output file.
- Run the processor and the feed in their own threads via DataFlowConnector.
- Observe live processing when you append new lines to the input file.

Source reference in examples repository:

- [TutorialPart5]({{ fluxtion_example_src
  }}/getting-started/src/main/java/com/telamin/fluxtion/example/tutorial/TutorialPart5.java)

## What we’ll build

We’ll subscribe to a named feed of String events, log them, transform to upper‑case, and then send the result to a named
sink bound to an output file.

Key building blocks:

- FileEventFeed — tails a file and publishes each new line as an event.
- DataFlow — a fluent pipeline that subscribes to the feed and processes messages.
- FileMessageSink — writes messages from a named sink to a file.
- DataFlowConnector — wires feeds and sinks to the DataFlow and runs them.

## Full example code

This mirrors the example in the examples repo. You can run it directly with jbang.

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
        // Feed: publish each line from input file as a String event into feed "myFeed"
        FileEventFeed myFileFeed = new FileEventFeed(
                "./tutorial4-input.txt",   // input file to tail
                "myFeed",                   // logical feed name
                ReadStrategy.EARLIEST       // start reading from beginning of file
        );

        // DataFlow: subscribe to the named feed, log, uppercase, log, then emit to a named sink
        DataFlow dataFlow = DataFlowBuilder
                .subscribeToFeed("myFeed", String.class)
                .console("read file in:{}")
                .map(String::toUpperCase)
                .console("write file out:{}\n")
                .sink("output")              // name the sink "output"
                .build();

        // Sink: bind sink name "output" to an output file
        FileMessageSink outputFile = new FileMessageSink("./tutorial4-output.txt");

        // Connector: owns threads for the processor and the feed, and wires everything together
        DataFlowConnector runner = new DataFlowConnector();
        runner.addDataFlow(dataFlow);
        runner.addFeed(myFileFeed);
        runner.addSink("output", outputFile);

        // Start: spins up threads and begins processing
        runner.start();
    }
}
```

## How it runs (threads and lifecycle)

- DataFlowConnector manages its own runtime threads.
    - One thread runs the DataFlow processor.
    - The FileEventFeed uses its own thread to watch the input file and push new lines into the connector.
- When runner.start() is called:
    - The connector starts the DataFlow processing thread.
    - The feed thread begins reading the input file according to the ReadStrategy. With EARLIEST, existing content is
      read immediately, then the file is tailed for new lines.
- Delivery is push‑based: as the feed reads a line, it publishes to the connector, which schedules it onto the processor
  thread, ensuring deterministic, single‑threaded processing inside the DataFlow.

## Try it yourself

You can run the example with JBang or from your IDE. Below is a minimal JBang flow.

1) Save the example as TutorialPart5.java.

```console
vi TutorialPart5.java
```

2) Create the input file with a few lines:

```console
echo -e "monday\ntuesday\nwednesday" > tutorial4-input.txt
```

3) Run it with JBang:

```console
jbang TutorialPart5.java
```

4) While it is running, append more lines to the input file. They will be pushed into the pipeline and processed
   immediately:

```console
echo "thursday" >> tutorial4-input.txt
```

5) Inspect the output file written by the sink. You can print it once with cat, or follow updates with tail -f:

- One‑off view:

```console
cat tutorial4-output.txt
```

- Follow as it grows:

```console
tail -f tutorial4-output.txt
```

You should see upper‑cased versions of the inputs, e.g.:

```console
READ FILE IN: monday
WRITE FILE OUT: MONDAY
READ FILE IN: tuesday
WRITE FILE OUT: TUESDAY
...
```

Notes:

- The console() steps print to stdout; FileMessageSink writes only the transformed message to the output file. Depending
  on your console format, you’ll see those messages prefixed accordingly.

## Variations

- Change ReadStrategy to LATEST to ignore existing file contents and only process new lines going forward.
- Replace FileMessageSink with your own sink implementation to publish to Kafka, an HTTP endpoint, or another system.
- Chain more operators (filter/map/window) before the sink to build richer pipelines.

## Key takeaways

- Feeds push data events into the runtime from external sources.
- Sinks bridge processed data out to external systems.
- DataFlowConnector hosts and wires both ends, running the feed(s) and the processor in their own threads and delivering
  events into the single‑threaded processing loop for consistency.
