# Creating an EventFeedAgent for DataFlowConnector

This guide shows how to implement a custom EventFeedAgent and wire it into a DataFlowConnector. You’ll learn what an
EventFeedAgent does, the threading model used by the connector, practical benefits, and see a minimal Java
implementation you can adapt.

Related pages:

- [DataFlowConnector overview](../integration/dataflow-connector.md)
- API Javadoc:
    - [EventFeedAgent](https://javadoc.io/doc/com.telamin.fluxtion/fluxtion-runtime/latest/com/telamin/fluxtion/runtime/eventfeed/EventFeedAgent.html)
    - [DataFlowConnector](https://javadoc.io/doc/com.telamin.fluxtion/fluxtion-runtime/latest/com/telamin/fluxtion/runtime/connector/DataFlowConnector.html)

## What is an EventFeedAgent?

EventFeedAgent<T> is the standard way to plug an external event source into a Fluxtion DataFlow. It combines two roles:

- NamedFeed<T>: identifies the source by id so a DataFlow can subscribe to it.
- Agrona Agent: supplies a non-blocking poll() method the runtime calls to fetch new events.

In short: an EventFeedAgent is a small active component that reads from “the outside world” (files, sockets, queues,
APIs) and publishes typed events into Fluxtion.

## Why use it with DataFlowConnector?

DataFlowConnector provides a pragmatic, single-threaded runner loop that:

- Polls one or more EventFeedAgent instances
- Pushes events into one or more DataFlows
- Publishes DataFlow outputs to named sinks (Consumers)
- Manages lifecycle (start/stop) and error handling
- Lets you tune CPU/latency via an IdleStrategy

This avoids writing your own polling thread and glue code while keeping the model explicit and efficient.

## Threading model (important)

- Single runner thread: DataFlowConnector uses an Agrona AgentRunner to invoke feed.poll(), flow.onEvent(...), and
  sink.accept(...) on a dedicated thread.
- Backpressure: A slow feed, heavy flow stage, or blocking sink will slow the entire loop. Keep poll() fast and
  non-blocking; hand off work to other executors inside your feed or sinks if needed.
- Safety: Within the connector, DataFlow execution is single-threaded. If your feed shares state with other threads (
  e.g., writing into a queue), use thread-safe structures and avoid blocking in poll().

## Minimal implementation pattern

Implement EventFeedAgent<T> directly, or extend a helper such as BaseEventFeed (if available in your version). The core
responsibilities:

- Provide an id (NamedFeed) so DataFlows can subscribe by that name.
- Implement Agent lifecycle: roleName(), onStart(), onClose(), and poll().
- In poll(), drain available items quickly and publish them to the DataFlow runtime via the connector’s internal
  plumbing (the connector calls DataFlow.onEvent for you when you return items from the feed).

Below is a simple, self-contained feed that bridges a concurrent queue into the connector. It shows a typical
non-blocking poll() loop.

```java
package com.example.feed;

import com.telamin.fluxtion.runtime.eventfeed.EventFeedAgent;
import org.agrona.concurrent.status.AtomicCounter;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * A simple queue-backed EventFeedAgent that publishes String events.
 * - put(String) can be called from any thread to enqueue new events.
 * - poll() runs on the connector’s AgentRunner thread to drain and publish events.
 */
public class QueueStringFeed implements EventFeedAgent<String> {

    private final String id; // NamedFeed id used by DataFlow.subscribe(id)
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    // Optional: you may track metrics, errors etc.
    private final AtomicCounter errorCounter = null; // placeholder; wire your own if needed

    public QueueStringFeed(String id) {
        this.id = Objects.requireNonNull(id);
    }

    // --- External API: enqueue events from your application threads ---
    public void put(String value) {
        queue.add(value);
    }

    // --- NamedFeed ---
    @Override
    public String id() {
        return id;
    }

    // --- Agent lifecycle ---
    @Override
    public int doWork() throws Exception {
        // Agrona Agent’s required method; often delegates to poll()
        return poll();
    }

    @Override
    public String roleName() {
        return "QueueStringFeed(" + id + ")";
    }

    @Override
    public void onStart() {
        // Initialize resources (open files/sockets, start timers, etc.)
    }

    @Override
    public void onClose() {
        // Clean up resources
        queue.clear();
    }

    // --- Core polling ---
    @Override
    public int poll() {
        int workDone = 0;
        String next;
        while ((next = queue.poll()) != null) {
            // Returning events to the connector:
            // Simply treat each dequeued item as a new event coming from this feed.
            // and publish it with super.publish(event);
            // The connector will route it to DataFlows that subscribe to this feed id.
            // In practice, you may call a callback exposed by a BaseEventFeed implementation. If
            // you’re implementing EventFeedAgent directly, the connector wraps the feed and will
            // read your items via poll(); just keep poll() non-blocking and fast.
            //
            // If your environment expects you to publish via a callback, replace this comment
            // with that publish call, e.g., publish(next).
            super.publish(event);
            workDone++;
        }
        return workDone;
    }
}
```

Notes:

- The exact publish mechanism differs slightly depending on helper base classes you use. If you extend a BaseEventFeed
  provided by fluxtion-runtime, you’ll typically call publish(event) inside poll() to hand off to the connector. If you
  implement EventFeedAgent directly, the connector’s composite agent will treat each polled item as work; ensure poll()
  reflects the number of items processed.
- Always avoid blocking in poll(): don’t sleep or wait; use non-blocking queues or time-sliced I/O.

## Wiring the feed into a DataFlowConnector

```java
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.connector.DataFlowConnector;

public class WiringExample {
    public static void main(String[] args) {
        var connector = new DataFlowConnector();

        var feed = new QueueStringFeed("in");
        connector.addFeed(feed);

        var flow = DataFlow.subscribe("in")
                .map(String::toUpperCase)
                .console("out:{}")
                .build();

        connector.addDataFlow(flow)
                .start();

        // produce a few events
        feed.put("hello");
        feed.put("fluxtion");

        // shutdown when finished
        connector.stop();
    }
}
```

## Design tips

- Keep poll() short: limit per-iteration work if you have bursty inputs to maintain latency predictability.
- Hand off blocking I/O: have a separate thread read from the network/disk and enqueue into a non-blocking buffer that
  the feed drains in poll().
- Use meaningful feed ids: the DataFlow subscribes by id; make it stable and explicit.
- Expose lightweight backpressure: if upstream can slow down, consider offering a queue size check or rejection policy.

## See also

- Examples repository [Repository]({{fluxtion_example_src}})
