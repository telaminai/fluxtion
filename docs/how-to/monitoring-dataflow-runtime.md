# How to monitor a DataFlow at runtime with Auditors

Monitoring a DataFlow at runtime is essential for understanding system behavior, troubleshooting issues, and meeting
SLOs. Fluxtion’s auditing framework provides non-intrusive hooks into the execution lifecycle, enabling you to:

- Capture metrics such as per-event latency, per-node invocation counts, and error rates
- Integrate with global monitoring systems (OpenTelemetry, Prometheus, statsd, custom HTTP services, etc.)
- Log event traversal for diagnostics and replay
- Implement persistence, tracing, and profiling strategies without changing the graph logic

This guide describes what an Auditor is, how it plugs into a DataFlow at runtime, and how to enable and use it, with
links to source, examples, and Javadocs.

## What is an Auditor?

An Auditor is a runtime observer that receives lifecycle callbacks as events are processed by the DataFlow graph. It can
monitor:

- Node registrations during initialization
- Incoming events (strongly-typed and Object variants)
- Node invocations on the execution path per event
- Completion of event processing

API: see the interface and docs:

- Auditor interface
  source: [Auditor.java]({{fluxtion_src_runtime}}/audit/Auditor.java)
- Audit package (all key
  classes): [com.telamin.fluxtion.runtime.audit]({{fluxtion_src_runtime}}/audit)
- API Javadoc:
  [Fluxtion-runtime Javadoc]({{fluxtion_javadoc}})

Key callbacks (simplified):

```java
public interface Auditor extends Lifecycle {
    void nodeRegistered(Object node, String nodeName);

    default void eventReceived(com.telamin.fluxtion.runtime.event.Event event) {
    }

    default void eventReceived(Object event) {
    }

    default void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
    }

    default void processingComplete() {
    }

    default boolean auditInvocations() {
        return false;
    } // true => get nodeInvoked callbacks
}
```

Set auditInvocations() to true if you want per-node traversal callbacks for every event, enabling fine-grained
metrics/tracing. If false, you still receive the other lifecycle callbacks.

## Why auditors are useful

- General-purpose monitoring: counters, timers, gauges per node or per event
- Seamless integration: forward telemetry to your global monitoring pipeline
- Deep visibility: with auditInvocations=true, trace the exact execution path of each event
- Alerting and SLOs: export latency histograms and error counters for alerting
- Persistence and replay: implement auditors that write reproducible traces
- Profiling and diagnostics: measure hotspots and call sequences without changing business code

## Example: Auditor-based monitoring demo

A complete runnable demo that publishes counters and timers (via a simple OpenTelemetry-like publisher) is available
here:

- Module: [auditor-monitoring-demo]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo)
- Key sources:
    - [MonitoringAuditor.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/MonitoringAuditor.java)
    - [SimpleOtelPublisher.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/SimpleOtelPublisher.java)
    - [AuditorMonitoringAOTGraphBuilder.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/AuditorMonitoringAOTGraphBuilder.java)
    - [AuditorMonitoringDemoRunner.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/AuditorMonitoringDemoRunner.java)
- Demo
  README: [auditor-monitoring-demo/README.md]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/README.md)

The demo auditor tracks per-node invocation counts and per-event duration (micros), then publishes to a logging-backed
“Otel” publisher.

For a logging-focused variant, also see:

- [audit-logging-demo]({{ config.extra.fluxtion_example_src }}/compiler/audit-logging-demo)
    - README: [audit-logging-demo/README.md]({{ config.extra.fluxtion_example_src }}/compiler/audit-logging-demo/README.md)

## How an Auditor plugs into the DataFlow

Auditors are registered with the generated event processor during build/compile time. The compiler wires the auditor so
it receives lifecycle callbacks at runtime.

With the AOT builder API (imperative registration), use EventProcessorConfig.addAuditor to add your auditor, and
optionally enable event audit log records to see traversal in logs:

```java
import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;

public class AuditorMonitoringAOTGraphBuilder implements FluxtionGraphBuilder {
    @Override
    public void buildGraph(EventProcessorConfig processorConfig) {
        // Register your nodes
        CalcNode calc = new CalcNode();
        processorConfig.addNode(calc);

        // Register your monitoring auditor (implements Auditor)
        processorConfig.addAuditor(new MonitoringAuditor(new SimpleOtelPublisher()), "monitoringAuditor");

        // Optional: enable traversal logging (levels: TRACE/DEBUG/INFO)
        processorConfig.addEventAudit(EventLogControlEvent.LogLevel.INFO);
    }
}
```

At runtime, once the generated processor is instantiated and init() is called, your Auditor will start receiving
callbacks:

- nodeRegistered for each node
- eventReceived before processing each event
- nodeInvoked for each node on the execution path (if auditInvocations=true)
- processingComplete after all nodes complete for an event

## Implementing your own Auditor

A minimal Auditor that counts invocations and measures event latency might look like:

```java
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.event.Event;

public class MonitoringAuditor implements Auditor {
    private final MetricsPublisher publisher;
    private long startNanos;

    public MonitoringAuditor(MetricsPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void nodeRegistered(Object node, String nodeName) {
        publisher.counter("node.registered").inc();
    }

    @Override
    public void eventReceived(Event event) {
        startNanos = System.nanoTime();
    }

    @Override
    public boolean auditInvocations() {
        return true; // enable per-node traversal callbacks
    }

    @Override
    public void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
        publisher.counter("node.invoked." + nodeName).inc();
    }

    @Override
    public void processingComplete() {
        long durationMicros = (System.nanoTime() - startNanos) / 1_000;
        publisher.timer("event.duration.micros").record(durationMicros);
    }
}
```

You are free to map these callbacks into your preferred telemetry system (OpenTelemetry, Prometheus, statsd, etc.).

## Tips for integrating with global monitoring systems

- Use stable metric names and tags (nodeName, methodName, event type)
- Consider sampling for very high-throughput graphs
- Use histogram/timer instruments to capture latency quantiles
- Combine Auditor callbacks with event audit logs for easy debugging
- For replayability, pair an Auditor with a recorder that writes traces (e.g., YAML/JSON)

## References

- Auditor interface
  source: [Auditor.java]({{fluxtion_src_runtime}}/audit/Auditor.java)
- Audit package
  sources: [com.telamin.fluxtion.runtime.audit]({{fluxtion_src_runtime}}/audit)
- API Javadoc:
  [Fluxtion-runtime Javadoc]({{ config.extra.fluxtion_javadoc }})
- Example module: [auditor-monitoring-demo]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo)
    - [MonitoringAuditor.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/MonitoringAuditor.java)
    - [AuditorMonitoringAOTGraphBuilder.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/AuditorMonitoringAOTGraphBuilder.java)
    - [AuditorMonitoringDemoRunner.java]({{ config.extra.fluxtion_example_src }}/compiler/auditor-monitoring-demo/src/main/java/com/telamin/fluxtion/example/sampleapps/auditmon/AuditorMonitoringDemoRunner.java)
- Logging variant: [audit-logging-demo]({{ config.extra.fluxtion_example_src }}/compiler/audit-logging-demo)
