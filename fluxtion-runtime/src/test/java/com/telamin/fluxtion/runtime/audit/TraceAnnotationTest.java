/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A trace entry names the annotation that dispatched the callback, when the generator supplies it, and is
 * unchanged when it does not.
 */
public class TraceAnnotationTest {

    interface Invocation {
        void invoke(EventLogManager manager, Object node);
    }

    private static String trace(LogLevel level, Invocation invocation) {
        return trace(new EventLogManager(), level, new TestAuditSource(), invocation);
    }

    private static String trace(EventLogManager manager, LogLevel level, Object source, Invocation invocation) {
        List<String> records = new ArrayList<>();
        manager.calculationLogConfig(new EventLogControlEvent(record -> records.add(record.toString())));
        manager.tracingOn(level);
        manager.clock = new Clock();
        manager.clock.init();
        manager.init();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("event");
        invocation.invoke(manager, source);
        manager.processingComplete();
        return String.join("\n", records);
    }

    @Test
    public void theSuppliedAnnotationIsWrittenAfterTheMethod() {
        String record = trace(LogLevel.INFO,
                (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event"));
        int method = record.indexOf("method: onTrigger");
        int annotation = record.indexOf("annotation: OnTrigger");
        assertTrue("the method is traced: " + record, method >= 0);
        assertTrue("the annotation is traced: " + record, annotation >= 0);
        assertTrue("the annotation follows the method: " + record, annotation > method);
    }

    @Test
    public void aProcessorThatSuppliesNoAnnotationWritesTheSameTraceAsBefore() {
        String record = trace(LogLevel.INFO, (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "event"));
        assertTrue(record, record.contains("method: onTrigger"));
        assertFalse("no annotation key without a supplied annotation: " + record, record.contains("annotation"));
    }

    @Test
    public void theAnnotationIsWrittenExactlyWhenTheMethodIs() {
        for (LogLevel level : LogLevel.values()) {
            String record = trace(level, (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event"));
            assertEquals(level + ": " + record, record.contains("method: onTrigger"), record.contains("annotation: OnTrigger"));
        }
    }

    @Test
    public void withoutAnAnnotationTheTraceIsByteIdenticalToTheFourArgumentForm() {
        for (boolean thread : new boolean[]{true, false}) {
            String four = trace(new EventLogManager().printThreadName(thread), LogLevel.INFO, new TestAuditSource(),
                    (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "event"));
            String fiveNull = trace(new EventLogManager().printThreadName(thread), LogLevel.INFO, new TestAuditSource(),
                    (m, node) -> m.nodeInvoked(node, "source", "onTrigger", null, "event"));
            assertEquals(stripTimes(four), stripTimes(fiveNull));
        }
    }

    @Test
    public void withoutTheThreadNameTheAnnotationStillFollowsTheMethod() {
        String record = trace(new EventLogManager().printThreadName(false), LogLevel.INFO, new TestAuditSource(),
                (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event"));
        assertFalse(record, record.contains("thread:"));
        assertTrue(record, record.contains("method: onTrigger, annotation: OnTrigger"));
    }

    @Test
    public void aForkedNodeWritesTheAnnotationAfterItsAsyncMethod() {
        String record = trace(new EventLogManager(), LogLevel.INFO, new TestForkedSource(),
                (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event"));
        assertTrue(record, record.contains("forkedExecution: true, asyncMethod: onTrigger, annotation: OnTrigger"));
        assertFalse("a forked node writes asyncMethod, not method: " + record, record.contains(" method: onTrigger"));
    }

    @Test
    public void aSubclassWrittenForTheFourArgumentFormSeesEveryInvocationAndTheAnnotationIsStillWritten() {
        List<String> seen = new ArrayList<>();
        EventLogManager counting = new EventLogManager() {
            @Override
            public void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
                seen.add(nodeName + "." + methodName);
                super.nodeInvoked(node, nodeName, methodName, event);
            }
        };
        String record = trace(counting, LogLevel.INFO, new TestAuditSource(), (m, node) -> {
            m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event");
            m.nodeInvoked(node, "source", "onEvent", "event");
        });
        assertEquals(List.of("source.onTrigger", "source.onEvent"), seen);
        assertTrue(record, record.contains("method: onTrigger, annotation: OnTrigger"));
        assertFalse("the annotation does not leak into the next, four-argument call: " + record,
                record.contains("method: onEvent, annotation"));
    }

    @Test
    public void aSubclassThatWantsTheAnnotationOverridesTheFiveArgumentFormAndCallsSuper() {
        List<String> annotations = new ArrayList<>();
        EventLogManager annotating = new EventLogManager() {
            @Override
            public void nodeInvoked(Object node, String nodeName, String methodName, String annotation, Object event) {
                annotations.add(String.valueOf(annotation));
                super.nodeInvoked(node, nodeName, methodName, annotation, event);
            }
        };
        String record = trace(annotating, LogLevel.INFO, new TestAuditSource(),
                (m, node) -> m.nodeInvoked(node, "source", "onTrigger", "OnTrigger", "event"));
        assertEquals(List.of("OnTrigger"), annotations);
        assertTrue(record, record.contains("method: onTrigger, annotation: OnTrigger"));
    }

    private static String stripTimes(String record) {
        return record.replaceAll("(eventTime|logTime|endTime|groupingId): [^\\n]*", "$1: _");
    }

    @Test
    public void anAuditorWrittenForTheFourArgumentFormStillSeesEveryInvocation() {
        List<String> seen = new ArrayList<>();
        Auditor legacy = new Auditor() {
            @Override
            public void nodeRegistered(Object node, String nodeName) {
            }

            @Override
            public void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
                seen.add(nodeName + "." + methodName + "/" + event);
            }
        };
        legacy.nodeInvoked(null, "source", "onTrigger", "OnTrigger", "event");
        assertEquals(List.of("source.onTrigger/event"), seen);
    }

    private static class TestAuditSource implements EventLogSource {
        @Override
        public void setLogger(EventLogger log) {
        }
    }

    private static final class TestForkedSource extends TestAuditSource implements com.telamin.fluxtion.runtime.node.ForkedTrigger {
    }
}
