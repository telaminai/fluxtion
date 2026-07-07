/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventLogManagerTest {

    @Test
    public void tracingOnNoneLeavesStructuredAuditThresholdUntouched() {
        List<String> records = captureStructuredInfo(LogLevel.NONE);

        assertTrue(String.join("\n", records).contains("auditKey"));
    }

    @Test
    public void tracingOnTraceAllowsStructuredInfoEntries() {
        List<String> records = captureStructuredInfo(LogLevel.TRACE);

        assertTrue(String.join("\n", records).contains("auditKey"));
    }

    private static List<String> captureStructuredInfo(LogLevel level) {
        List<String> records = new ArrayList<>();
        EventLogManager manager = new EventLogManager(record -> records.add(record.toString()))
                .tracingOn(level);
        manager.clock = new Clock();
        manager.clock.init();
        TestAuditSource source = new TestAuditSource();

        manager.init();
        manager.nodeRegistered(source, "source");
        manager.eventReceived("event");
        source.logInfo();
        manager.processingComplete();
        return records;
    }

    private static final class TestAuditSource implements EventLogSource {

        private EventLogger logger = NullEventLogger.INSTANCE;

        @Override
        public void setLogger(EventLogger log) {
            logger = log;
        }

        void logInfo() {
            logger.info("auditKey", "auditValue");
        }
    }
}
