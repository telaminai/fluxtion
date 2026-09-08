/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.generation.config;

import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig.PerformanceProfile;
import com.telamin.fluxtion.runtime.audit.EventLogManager;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link PerformanceProfile#LOW_LATENCY_AUDIT} — pinned in both directions.
 *
 * <p>A profile is a promise about a set of settings, and the failure mode when one drifts is silent:
 * the processor stays correct and simply runs slower, or — worse for the two this profile deliberately
 * does <b>not</b> touch — quietly computes something different. So what it must set and what it must
 * leave alone are equally load-bearing, and both are asserted here.
 *
 * <p>The measured value of this profile over {@link PerformanceProfile#AUDITED} is 5.4 ns/event on JIT
 * and ~12 ns on native, on a 30-node 5-event-type converging-tail graph — see round 63 in the analyser
 * repo. That comes from removing the per-event buffer-and-trigger branch and the subscription publish.
 */
public class LowLatencyAuditProfileTest {

    @Test
    public void dropsTheThingsThatCostAndCannotChangeAResult() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        assertFalse("buffer-and-trigger is a branch on every event",
                config.isSupportBufferAndTrigger());
        assertFalse("the subscription publish is constructor work nobody in this profile uses",
                config.isSupportSubscriptions());
    }

    /**
     * <b>The regression this profile actually shipped with, and the reason the test suite grew.</b>
     *
     * <p>{@code setSupportNodeNameLookup(false)} reads like it drops a lookup map. It does not: it stops
     * <b>node registration</b>, and node registration is how {@link EventLogManager#nodeRegistered} gives
     * every node its {@code EventLogger}. Turn it off and every node's {@code auditLog} is the null
     * logger — the processor runs, looks correct, and publishes nothing.
     *
     * <p>The first version of {@code LOW_LATENCY_AUDIT} set it. The generated processor emitted zero
     * {@code nodeRegistered} calls against 33 for {@code AUDITED}, and a benchmark measuring "the cost
     * of auditing" was measuring a graph with no audit — and reported the missing work as a speed-up.
     *
     * <p>An audit profile that disables auditing is the worst failure this API can have, because
     * nothing reports it. This test exists so it cannot happen twice.
     */
    @Test
    public void mustNotDisableNodeRegistrationBecauseThatSilentlyKillsTheAuditLog() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        assertTrue("node registration is how every node gets its EventLogger — without it the audit "
                        + "log this profile exists to keep is silently dead",
                config.isSupportNodeNameLookup());
    }

    /**
     * The two the profile must NOT touch. Dirty filtering changes what the graph computes; re-entrancy
     * turns queued re-entrant dispatch into an exception. An audit profile that altered either would be
     * changing behaviour to buy speed, which is not a trade a profile gets to make on the author's
     * behalf — the same reasoning {@code LOWEST_LATENCY} documents for re-entrancy.
     */
    @Test
    public void leavesAloneTheTwoThatCanChangeBehaviour() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        assertTrue("an audit profile must not change what the graph computes",
                config.isSupportDirtyFiltering());
        assertTrue("re-entrancy is the author's call, not the profile's",
                config.isSupportReentrancy());
    }

    /** The whole point of the profile: unlike LOWEST_LATENCY, the audit log survives it. */
    @Test
    public void keepsTheAuditLog() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);
        config.addLowLatencyEventLog(com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel.INFO);

        assertNotNull("the audit log is the point of this profile",
                config.getAuditorMap().get(EventLogManager.NODE_NAME));
    }

    /**
     * The distinction from {@link PerformanceProfile#AUDITED}: tracing off, and neither of the two
     * defaults that allocate. Tracing is the expensive half — measured at ~184 ns/event on the
     * reference graph, on top of the record itself.
     */
    @Test
    public void theAuditLogItInstallsHasTracingOffAndNeitherAllocatingDefault() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.addLowLatencyEventLog(com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel.INFO);

        EventLogManager manager = (EventLogManager) config.getAuditorMap().get(EventLogManager.NODE_NAME);
        assertNotNull(manager);
        assertFalse("method tracing is the expensive half", manager.trace);
        assertFalse("the event's toString allocates", manager.printEventToString);
        assertFalse("the thread name allocates", manager.printThreadName);
    }

    /** LOWEST_LATENCY still drops the audit log — the two profiles must not converge. */
    @Test
    public void lowestLatencyStillDropsTheAuditLogSoTheProfilesRemainDistinct() {
        EventProcessorConfig audited = new EventProcessorConfig();
        audited.addLowLatencyEventLog(com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel.INFO);
        audited.performanceProfile(PerformanceProfile.LOWEST_LATENCY);

        assertFalse("LOWEST_LATENCY gives up the audit log; that is its documented trade",
                audited.getAuditorMap().containsKey(EventLogManager.NODE_NAME));
    }
}
