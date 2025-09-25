package com.telamin.fluxtion.builder.example;

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
                .filter(r -> r.value() >= 0)         // basic guard
                .peek(r -> System.out.println("reading=" + r))
                .push(device::onReading)         // feed imperative node
                .mapFromSupplier(device::status) // access value from imperative node
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
