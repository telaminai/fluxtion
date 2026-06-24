# Error Handling in Fluxtion

Fluxtion is designed for deterministic execution, which means error handling is both predictable and critical. This guide explains how to manage exceptions, monitor failures, and implement recovery strategies in your DataFlow applications.

---

## The Determinism of Failure

In a Fluxtion DataFlow, an exception thrown during event processing has a deterministic impact:

1.  **Immediate Halt**: By default, an uncaught exception in any node or DSL operator stops the current event propagation pass.
2.  **No Partial State Glitches**: Because Fluxtion executes in a fixed topological order, you can precisely identify which nodes completed and which one failed.
3.  **Process Integrity**: If running inside `DataFlowConnector`, the exception is caught by the runner thread and passed to a centralized error handler, preventing the entire application from crashing silently.

---

## Centralized Error Handling

When using **`DataFlowConnector`** to run your DataFlows, you can register a global error handler to manage uncaught exceptions.

### Setting an Error Handler

```java
DataFlowConnector connector = new DataFlowConnector();
connector.addDataFlow(myDataFlow);

// Register a global error handler
connector.setErrorHandler(throwable -> {
    log.error("Unhandled exception in DataFlow execution loop", throwable);
    
    if (throwable instanceof FatalException) {
        System.exit(1); // Critical failure: shut down
    }
    // Otherwise: log and continue to the next event
});

connector.start();
```

### Why use a centralized handler?
- **Logging**: Ensure every failure is recorded with full stack traces.
- **Alerting**: Trigger external monitoring systems (e.g., PagerDuty, Slack).
- **Graceful Shutdown**: Close resources or flush state before exiting on fatal errors.

---

## In-Node Error Handling

For expected failures (e.g., I/O issues, parsing errors), it is often better to handle the exception directly within your node logic.

### Defensive Try-Catch

```java
public class PriceValidator {
    private final EventLogger logger;

    @OnEventHandler
    public boolean onPrice(PriceUpdate price) {
        try {
            validate(price);
            return true; // Continue propagation
        } catch (ValidationException e) {
            logger.error("Validation failed for {}: {}", price.symbol(), e.getMessage());
            return false; // Stop propagation for THIS event path
        }
    }
}
```

By returning `false` from an `@OnEventHandler`, you stop the event from propagating further down that branch of the graph, effectively "dropping" the invalid event while keeping the processor running.

---

## Resilience Patterns

### 1. Dead Letter Queues (DLQ)
Instead of just logging an error, you can route failed events to a dedicated **Error Sink**. This allows you to inspect and reprocess them later.

```java
DataFlow flow = DataFlowBuilder.subscribe(Order.class)
    .filter(order -> {
        if (order.quantity() <= 0) {
            // Route to DLQ
            return false; 
        }
        return true;
    })
    .sink("validOrders")
    .build();

// Bind the DLQ sink
flow.addSink("invalidOrders", order -> auditService.recordFailure(order));
```

### 2. Validation Filters
Use DSL `filter()` operators early in your pipeline to "clean" the data before it reaches complex business logic. This keeps your stateful nodes simple and focused on valid data.

### 3. Audit Logging
Fluxtion’s **Audit Logging** system is ideal for recording error context without GC overhead. Use `EventLogger` to record the state of the node at the moment of failure.

```java
@OnEventHandler
public void handle(Trade trade) {
    if (trade.price() < 0) {
        eventLogger.error("Negative price detected", "price", trade.price(), "sym", trade.symbol());
    }
}
```
*See the [Audit Logging Guide](audit-logging-dataflow.md) for more details.*

---

## Recovery and Debugging

The greatest advantage of Fluxtion's deterministic model is how it handles recovery:

1.  **Forensic Replay**: If an error occurs in production, you can replay the exact input stream (from a Kafka log or file) through a local debugger. Because the execution is deterministic, the error will reproduce exactly as it did in production.
2.  **State Reconstruction**: If a processor crashes, you can rebuild its state by replaying events from the last known good checkpoint.
3.  **Build-Time Validation**: Many "errors" in traditional systems (like missing dependencies or circular references) are caught by the Fluxtion compiler at **build time**, preventing them from ever reaching production.

---

## Best Practices

-   **Don't swallow fatal errors**: If a node enters an inconsistent state, it's often better to let the exception propagate to the `DataFlowConnector` and stop the process.
-   **Use Sinks for Side Effects**: Perform I/O (database writes, network calls) inside Sinks. This keeps the core DataFlow logic pure and makes error handling easier to isolate.
-   **Monitor Tail Latency**: Errors in downstream systems (like a slow database) often manifest as "jitter" or high tail latency in your DataFlow. Use the [Monitoring Guide](monitoring-dataflow-runtime.md) to detect these early.

---

## Related Resources

-   **[State and Recovery](state-and-recovery.md)**: Managing checkpoints and restart logic.
-   **[Deterministic Replay](replay-functionality.md)**: Using replay to debug production failures.
-   **[Audit Logging](audit-logging-dataflow.md)**: Recording high-context error logs.
-   **[DataFlow Connector](../integration/dataflow-connector.md)**: Configuring global error handlers.
