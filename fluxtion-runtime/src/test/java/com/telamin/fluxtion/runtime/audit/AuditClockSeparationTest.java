package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.time.Clock;
import com.telamin.fluxtion.runtime.time.ClockStrategy;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The audit record can read a clock of its own, and by default does not.
 *
 * <p><b>Why a separate clock rather than a strategy.</b> The graph's {@link Clock} is a process-wide
 * singleton injected into every processor, and time-based nodes read it. Swapping its strategy to save
 * a clock read per audited event also changes what every tumbling and sliding window believes the time
 * is — so the audit path cannot borrow that dial. This gives it its own.
 */
public class AuditClockSeparationTest {

    /** A clock strategy whose value the test drives, so the source of a timestamp is identifiable. */
    private static final class Stepping implements ClockStrategy {
        long now;
        public long getWallClockTime() { return now; }
    }

    /** Captures published records through the SINK - the production path, not an internal accessor. */
    private static final class Captured implements LogRecordListener {
        String text;
        public void processLogRecord(LogRecord logRecord) { text = logRecord.toString(); }

        /** {@code logTime} as the published record states it. */
        long logTime() {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("logTime:\\s*(\\d+)").matcher(text);
            assertTrue("published record must carry logTime:\n" + text, m.find());
            return Long.parseLong(m.group(1));
        }
    }

    private EventLogManager managerOn(Clock graphClock, EventLogManager.AuditClock choice,
                                      Captured sink) {
        EventLogManager manager = new EventLogManager(sink).tracingOff().auditClock(choice);
        manager.clock = graphClock;
        manager.init();
        manager.nodeRegistered(new Object(), "node");
        return manager;
    }

    /**
     * DEFAULT: one clock. The record's logTime is the graph clock's processTime, so an audit timestamp
     * and a window's idea of now cannot disagree.
     */
    @Test
    public void byDefaultTheRecordReadsTheGraphClock() {
        Stepping strategy = new Stepping();
        Clock graphClock = new Clock();
        graphClock.init();
        graphClock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(strategy));

        Captured sink = new Captured();
        EventLogManager manager = managerOn(graphClock, EventLogManager.AuditClock.SHARED, sink);
        assertEquals("SHARED must be the default", EventLogManager.AuditClock.SHARED,
                new EventLogManager().auditClock);

        strategy.now = 111_000L;
        graphClock.eventReceived(new Object());
        manager.eventReceived(new Object());
        manager.publishLastRecord();

        assertEquals("the record must read the graph clock the build injected",
                111_000L, sink.logTime());
    }

    /**
     * OPT IN: the record reads its own clock, and the graph's is untouched — which is the whole point.
     * A test that only checked the record would not notice the graph clock being changed underneath it.
     */
    @Test
    public void fastProjectedGivesTheRecordItsOwnClockAndLeavesTheGraphAlone() {
        Stepping strategy = new Stepping();
        Clock graphClock = new Clock();
        graphClock.init();
        graphClock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(strategy));
        strategy.now = 111_000L;

        Captured sink = new Captured();
        EventLogManager manager = managerOn(graphClock, EventLogManager.AuditClock.FAST_PROJECTED, sink);
        graphClock.eventReceived(new Object());
        manager.eventReceived(new Object());
        manager.publishLastRecord();

        long recordTime = sink.logTime();
        assertNotEquals("the record must NOT be reading the graph's driven clock any more",
                111_000L, recordTime);
        assertTrue("and its own clock must report a real epoch-millisecond time",
                recordTime > 1_600_000_000_000L);

        // THE GUARANTEE THAT MATTERS: the graph's clock still answers what the graph set it to.
        assertEquals("the graph clock must be untouched - windows depend on it",
                111_000L, graphClock.getProcessTime());
        assertEquals("including its strategy", 111_000L, graphClock.getWallClockTime());
    }

    /** A record swapped in at runtime must keep reading the clock the build chose. */
    @Test
    public void aRecordSwappedAtRuntimeKeepsTheChosenClock() {
        Stepping strategy = new Stepping();
        Clock graphClock = new Clock();
        graphClock.init();
        graphClock.setClockStrategy(new ClockStrategy.ClockStrategyEvent(strategy));
        strategy.now = 222_000L;
        Captured sink = new Captured();
        EventLogManager manager =
                managerOn(graphClock, EventLogManager.AuditClock.FAST_PROJECTED, sink);

        LogRecord replacement = new LogRecord(graphClock);
        replacement.updateLogLevel(LogLevel.INFO);
        manager.calculationLogConfig(new EventLogControlEvent(replacement));

        manager.eventReceived(new Object());
        manager.publishLastRecord();
        assertNotEquals("the swapped record must keep the private clock, not revert to the graph's",
                222_000L, sink.logTime());
    }
}
