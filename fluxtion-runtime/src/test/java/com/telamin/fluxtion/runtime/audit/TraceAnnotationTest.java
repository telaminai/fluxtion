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
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString())).tracingOn(level);
        manager.clock = new Clock();
        manager.clock.init();
        TestAuditSource source = new TestAuditSource();
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
    public void tracingOffMeansTheProcessorIsNotAskedToTrace() {
        // A generated processor calls nodeInvoked only when auditInvocations() is true; the attribute adds
        // nothing to a processor that does not trace.
        assertFalse(new EventLogManager().tracingOff().auditInvocations());
        assertFalse(new EventLogManager().tracingOn(LogLevel.NONE).auditInvocations());
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

    private static final class TestAuditSource implements EventLogSource {
        @Override
        public void setLogger(EventLogger log) {
        }
    }
}
