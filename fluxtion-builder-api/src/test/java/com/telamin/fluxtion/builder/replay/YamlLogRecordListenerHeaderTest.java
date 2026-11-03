/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.replay;

import com.telamin.fluxtion.runtime.audit.EventLogManager;
import com.telamin.fluxtion.runtime.audit.EventLogNode;
import com.telamin.fluxtion.runtime.time.Clock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The stream header every stream now begins with is kept apart from the records, never parsed as one. */
public class YamlLogRecordListenerHeaderTest {

    static class Book extends EventLogNode {
        void publish(int qty) {
            auditLog.info("qty", qty);
        }
    }

    @Test
    public void theHeaderIsNotARecord() {
        YamlLogRecordListener listener = new YamlLogRecordListener();
        EventLogManager m = new EventLogManager(listener);
        Clock clock = new Clock();
        clock.init();
        m.clock = clock;
        m.init();
        Book book = new Book();
        m.nodeRegistered(book, "book");
        for (int i = 0; i < 2; i++) {
            Object e = new Object();
            clock.eventReceived(e);
            m.eventReceived(e);
            book.publish(i);
            m.processingComplete();
        }
        assertEquals(2, listener.getEventList().size());
        assertEquals(1, listener.getStreamHeaders().size());
        assertTrue(listener.getStreamHeaders().get(0).startsWith("auditStreamHeader:"));
    }
}
