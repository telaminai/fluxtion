/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The premise a build-time profile has to respect: <b>an {@link EventLogSource} that was never passed
 * through {@link EventLogManager#nodeRegistered} can never log.</b>
 *
 * <p>This is pinned in the runtime because the consequence bites in the builder. A profile that turns
 * off node registration to save a map turns off the audit log too, and nothing anywhere reports it —
 * the processor runs, the sink is installed, and no record is ever published. It happened; see
 * {@code LowLatencyAuditProfileTest}.
 *
 * <p>Both directions are asserted, because "registered nodes log" on its own would still pass if the
 * dependency were removed.
 */
public class AuditNeedsNodeRegistrationTest {

    static class LoggingNode extends EventLogNode {
        void work() {
            auditLog.info("v", 42);
        }
    }

    private static EventLogManager managerWithSink(List<String> published) {
        EventLogManager manager = new EventLogManager(r -> published.add(r.asCharSequence().toString()));
        manager.clock = new Clock();
        manager.clock.init();
        manager.init();
        return manager;
    }

    @Test
    public void aRegisteredNodeLogs() {
        List<String> published = new ArrayList<>();
        EventLogManager manager = managerWithSink(published);
        LoggingNode node = new LoggingNode();

        manager.nodeRegistered(node, "node");
        manager.eventReceived(new Object());
        node.work();
        manager.processingComplete();

        assertEquals(1, published.size());
        assertTrue("the value the node logged must reach the record: " + published.get(0),
                published.get(0).contains("v: 42"));
    }

    @Test
    public void anUnregisteredNodePublishesNothingAndSaysNothingAboutIt() {
        List<String> published = new ArrayList<>();
        EventLogManager manager = managerWithSink(published);
        LoggingNode node = new LoggingNode();

        // no nodeRegistered call — exactly what disabling node registration produces
        manager.eventReceived(new Object());
        node.work();
        manager.processingComplete();

        assertEquals("an unregistered node logs nothing — and this is the silent failure a build-time "
                + "profile can cause by switching node registration off", 0, published.size());
    }
}
