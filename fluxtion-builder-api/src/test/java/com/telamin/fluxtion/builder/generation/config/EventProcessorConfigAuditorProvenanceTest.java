package com.telamin.fluxtion.builder.generation.config;

import com.telamin.fluxtion.runtime.audit.Auditor;
import org.junit.Assert;
import org.junit.Test;

/**
 * Auditor PROVENANCE — which auditors the framework supplied, and which the author did.
 *
 * <p>The auditor map is single-slot per NAME, so a registration replaces whatever held that name.
 * Provenance has to follow the replacement, and an earlier version of this API only ever ADDED to
 * the framework set. The consequence was concrete rather than theoretical: an author who registered
 * their own clock under {@code "clock"} — the name {@link EventProcessorConfig} uses for its own —
 * had their node published in artefact metadata as framework plumbing, and dropped from the
 * authored-node count that a coverage figure divides by. The javadoc promised the opposite.
 *
 * <p>Both directions are pinned here because the fix is an ORDERING inside two methods that call
 * each other, and an ordering is exactly the kind of thing a later edit reverses without noticing.
 */
public class EventProcessorConfigAuditorProvenanceTest {

    public static class ProbeAuditor implements Auditor {
        @Override
        public void nodeRegistered(Object node, String nodeName) {
        }
    }

    /** The framework's own, registered in the constructor and by {@code clock()}. */
    @Test
    public void theFrameworksOwnAuditorsAreMarkedFramework() {
        EventProcessorConfig config = new EventProcessorConfig();
        Assert.assertTrue("the clock is registered by EventProcessorConfig, not by the author",
                config.getFrameworkAuditorNames().contains("clock"));
    }

    /** An auditor the author registers under a fresh name is theirs. */
    @Test
    public void anAuthorsAuditorIsNotMarked() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.addAuditor(new ProbeAuditor(), "authorAudit");
        Assert.assertFalse(config.getFrameworkAuditorNames().contains("authorAudit"));
    }

    /**
     * THE REGRESSION. The author replaces the framework's clock, so the binding under that name is
     * now theirs and the name must leave the set.
     */
    @Test
    public void anAuthorRegisteringOverAFrameworkNameTakesOwnershipOfIt() {
        EventProcessorConfig config = new EventProcessorConfig();
        Assert.assertTrue("precondition: the framework owns this name first",
                config.getFrameworkAuditorNames().contains("clock"));

        config.addAuditor(new ProbeAuditor(), "clock");

        Assert.assertSame("the author's instance must be the one registered",
                ProbeAuditor.class, config.getAuditorMap().get("clock").getClass());
        Assert.assertFalse("the author now owns this binding, so the name must not stay marked as "
                        + "framework — otherwise their own node is published as framework plumbing "
                        + "and excluded from the authored-node count",
                config.getFrameworkAuditorNames().contains("clock"));
    }

    /**
     * And the other direction, which the fix's ordering could just as easily have broken: the
     * framework registering over a name the author used must MARK it, not leave it cleared.
     */
    @Test
    public void theFrameworkRegisteringOverAnAuthorNameTakesOwnershipBack() {
        EventProcessorConfig config = new EventProcessorConfig();
        config.addAuditor(new ProbeAuditor(), "swappable");
        Assert.assertFalse(config.getFrameworkAuditorNames().contains("swappable"));

        config.addFrameworkAuditor(new ProbeAuditor(), "swappable");

        Assert.assertTrue("addFrameworkAuditor delegates to addAuditor, which CLEARS the mark — so "
                        + "marking has to happen after the delegate call, not before it",
                config.getFrameworkAuditorNames().contains("swappable"));
    }

    /** The set is a view the caller must not be able to corrupt. */
    @Test(expected = UnsupportedOperationException.class)
    public void theReportedSetIsUnmodifiable() {
        new EventProcessorConfig().getFrameworkAuditorNames().add("smuggled");
    }
}
