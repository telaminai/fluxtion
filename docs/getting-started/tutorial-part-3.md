# Tutorial Part‑3 — Combine DSL with imperative nodes
---

In this tutorial you will:

- Combine the fluent DSL with a custom, stateful component (imperative node).
- Use lifecycle callbacks to initialize and clean up state.
- Route events through the DSL into your component and emit results.

## Prerequisites

- JDK 21+
- JBang or Maven

## About imperative nodes in Fluxtion

- The runtime can invoke user components directly as part of the graph.
- You expose methods for inputs and outputs; Fluxtion wires calls based on dependencies.
- Lifecycle callbacks (such as onStart/onStop) let your component initialize or reset state.

## Option A — Run with JBang

1) Create a file TutorialPart3.java with the code below.

```console
vi TutorialPart3.java
```
2. Run with jBang

```console 
jbang TutorialPart3.java 
```

```java
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
//JAVA 25

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.annotations.Start;
import com.telamin.fluxtion.runtime.annotations.Stop;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TutorialPart3 {
    // Event
    public record SensorReading(String deviceId, double value) {
    }

    // Imperative, stateful component
    public static class DeviceState {
        private String deviceId;
        private double lastValue;
        private double runningSum;
        private int count;
        private boolean started;

        // lifecycle callback annotation (called once when graph starts)
        @Start
        public void start() {
            started = true;
            System.out.println("[DeviceState] onStart " + Instant.now());
        }

        // lifecycle callback annotation (called when graph stops)
        @Stop
        public void stop() {
            started = false;
            System.out.println("[DeviceState] onStop " + Instant.now());
        }

        // input from DSL
        public void onReading(SensorReading reading) {
            if (!started) return; // guard
            this.deviceId = reading.deviceId();
            this.lastValue = reading.value();
            this.runningSum += reading.value();
            this.count++;
        }

        // derived output called by runtime when dependents need it
        public double average() {
            return count == 0 ? 0.0 : runningSum / count;
        }

        public String status() {
            return "device=" + deviceId + ", last=" + lastValue + ", avg=" + Math.round(average() * 100.0) / 100.0;
        }
    }

    public static void main(String[] args) {
        System.out.println("Building DataFlow: DSL + imperative DeviceState");

        DeviceState device = new DeviceState();

        DataFlow flow = DataFlowBuilder
                .subscribe(SensorReading.class)
                .filter(r -> r.value() >= 0)     // basic guard
                .peek(r -> System.out.println("reading=" + r))
                .push(device::onReading)         // feed user node
                .mapFromSupplier(device::status) // access value from user node
                .sink("deviceStatus")
                .build();

        flow.addSink("deviceStatus", System.out::println);

        flow.start();

        var exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            double v = Math.random() < 0.1 ? -1.0 : (20 + Math.random() * 5); // sometimes filtered
            flow.onEvent(new SensorReading("dev-1", v));
        }, 50, 250, TimeUnit.MILLISECONDS);

        System.out.println("Publishing sensor readings every 250 ms...\n");
    }
}
```

## Option B — Maven

- Add the dependency (see Part‑1), create the class, and run it from your IDE.

## What you should see

- onStart prints once as the graph becomes active.
- Device status lines containing last value and running average.
- Inputs with negative values are filtered out by the DSL before reaching the component.

```console
fluxtion-exmples % jbang TutorialPart3.java 
[jbang] Building jar for TutorialPart3.java...
Building DataFlow: DSL + imperative DeviceState
[DeviceState] onStart 2025-09-27T08:40:28.626489Z
Publishing sensor readings every 250 ms...

reading=SensorReading[deviceId=dev-1, value=21.429319588260345]
device=dev-1, last=21.429319588260345, avg=21.43
reading=SensorReading[deviceId=dev-1, value=20.630519222967056]
device=dev-1, last=20.630519222967056, avg=21.03
reading=SensorReading[deviceId=dev-1, value=24.67064434358356]
device=dev-1, last=24.67064434358356, avg=22.24
reading=SensorReading[deviceId=dev-1, value=23.083275479199557]
device=dev-1, last=23.083275479199557, avg=22.45
```

## Key ideas reinforced

- Imperative nodes can hold state and expose methods; the DSL can call into them.
- Lifecycle callbacks provide clean initialization/shutdown hooks.
- You can mix declarative operators (filter/map) with direct calls to your own classes.

## Next steps

- Proceed to Part‑4 to package and embed a DataFlow in a microservice with logging, health, and metrics hooks.