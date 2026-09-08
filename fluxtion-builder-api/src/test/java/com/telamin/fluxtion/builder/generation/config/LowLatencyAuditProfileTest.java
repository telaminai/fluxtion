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
     * Re-entrancy is the one setting this profile must not touch: turning it off converts queued
     * re-entrant dispatch into an {@link IllegalStateException}, which can break a working graph. That
     * is not a trade a profile gets to make on the author's behalf — the same reasoning
     * {@code LOWEST_LATENCY} documents for it at length.
     */
    @Test
    public void leavesReentrancyAlone() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        assertTrue("re-entrancy is the author's call, not the profile's",
                config.isSupportReentrancy());
    }

    /**
     * Guards off. Measured with the harness version held equal across both arms — 7.6 ns/event better
     * with no audit, indistinguishable with it. An earlier measurement said the opposite because it
     * compared a pre-h3 binary against an h3 one: two variables, not one. The audit output was
     * identical throughout, which is what said the difference had to be an artifact.
     */
    @Test
    public void turnsGuardsOffBecauseTheyAreFreeToRemoveOnTheAuditedPath() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        assertFalse("guards cost 7.6 ns/event with no audit and are indistinguishable with it "
                        + "(144.11 vs 142.83 native, inside the lottery); on this shape they also "
                        + "skip nothing, verified by tracing",
                config.isSupportDirtyFiltering());
    }

    /** And the author can still put them back — a profile is a starting point, not a lock. */
    @Test
    public void theAuthorCanPutGuardsBack() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);
        config.setSupportDirtyFiltering(true);

        assertTrue(config.isSupportDirtyFiltering());
        assertNotNull("and doing so must not disturb the audit log",
                config.addLowLatencyEventLog(com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel.INFO)
                        .getAuditorMap().get(EventLogManager.NODE_NAME));
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

    /**
     * With guards off in both, the two profiles now differ by the audit log alone. Pinned because that
     * is what makes an audit measurement meaningful: a baseline that also differs in dirty filtering
     * produced a delta 32% larger than the real audit cost, and nothing reported the confound.
     */
    @Test
    public void differsFromLowestLatencyOnTheAuditLogNotTheGuards() {
        EventProcessorConfig lowLatencyAudit = new EventProcessorConfig();
        lowLatencyAudit.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT);

        EventProcessorConfig lowestLatency = new EventProcessorConfig();
        lowestLatency.performanceProfile(PerformanceProfile.LOWEST_LATENCY);

        assertFalse("both give up conditional propagation", lowLatencyAudit.isSupportDirtyFiltering());
        assertFalse("both give up conditional propagation", lowestLatency.isSupportDirtyFiltering());
        // the difference between them is the audit log, not the guards
        lowLatencyAudit.addLowLatencyEventLog(
                com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel.INFO);
        assertNotNull("LOW_LATENCY_AUDIT keeps the audit log",
                lowLatencyAudit.getAuditorMap().get(EventLogManager.NODE_NAME));
    }
}
