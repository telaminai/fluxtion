package com.telamin.fluxtion.runtime.audit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * M50/W15 — {@link Auditor#auditEventReceipt()} gates the event-path call sites the generator
 * emits, the way {@link Auditor#auditInvocations()} already gates {@code nodeInvoked}.
 *
 * <p>The two defaults point opposite ways, and each preserves the behaviour an auditor had before
 * its flag existed: {@code auditInvocations()} defaults false (opt in to per-node callbacks),
 * {@code auditEventReceipt()} defaults true (opt out of the event path). These tests pin that,
 * because getting either default backwards changes what every generated processor does.
 */
public class AuditEventReceiptTest {

    /** An auditor written before the flag existed: it must keep receiving the event path. */
    static class LegacyAuditor implements Auditor {
        @Override
        public void nodeRegistered(Object node, String nodeName) {/*NoOp*/}
    }

    @Test
    public void defaultIsTrueSoAnExistingAuditorKeepsItsEventCallbacks() {
        assertThat(new LegacyAuditor().auditEventReceipt(), is(true));
    }

    @Test
    public void theOtherFlagStillDefaultsFalse() {
        assertThat("auditInvocations opts IN; auditEventReceipt opts OUT — the defaults differ "
                        + "on purpose, and swapping either changes every generated processor",
                new LegacyAuditor().auditInvocations(), is(false));
    }

    @Test
    public void nodeNameAuditorOptsOutOfTheEventPath() {
        assertThat(new NodeNameAuditor().auditEventReceipt(), is(false));
    }

    /** What it opts out of is exactly the set of no-ops it inherits — nothing it implements. */
    @Test
    public void nodeNameAuditorInheritsTheCallbacksItOptsOutOf() throws NoSuchMethodException {
        assertThat("processingComplete must still be the inherited no-op",
                NodeNameAuditor.class.getMethod("processingComplete").getDeclaringClass().getName(),
                is(Auditor.class.getName()));
        assertThat(NodeNameAuditor.class.getMethod("eventReceived", Object.class)
                        .getDeclaringClass().getName(),
                is(Auditor.class.getName()));
    }

    /** Registration is the work it does, and opting out of the event path must not disturb it. */
    @Test
    public void optingOutDoesNotAffectNameLookup() throws NoSuchFieldException {
        NodeNameAuditor auditor = new NodeNameAuditor();
        Object node = new Object();
        auditor.nodeRegistered(node, "myNode");
        assertThat(auditor.lookupInstanceName(node), is("myNode"));
        assertThat(auditor.<Object>getInstanceById("myNode"), is(node));
    }

    @Test
    public void anAuditorThatWantsTheEventPathCanStillSaySo() {
        Auditor keen = new Auditor() {
            @Override
            public void nodeRegistered(Object node, String nodeName) {/*NoOp*/}

            @Override
            public boolean auditEventReceipt() {
                return true;
            }
        };
        assertThat(keen.auditEventReceipt(), is(true));
    }

    /**
     * The second auditor in the same position as {@link NodeNameAuditor}, missed when W15 landed.
     * {@code ServiceRegistryNode} does all its work in {@code registerService},
     * {@code deRegisterService} and {@code nodeRegistered}; it must not be on the event path.
     */
    @Test
    public void serviceRegistryOptsOutOfTheEventPath() {
        assertThat(new com.telamin.fluxtion.runtime.service.ServiceRegistryNode().auditEventReceipt(),
                is(false));
    }

    /**
     * The premise the opt-out depends on: it overrides none of the three per-event callbacks, so opting
     * out cannot lose behaviour. If someone later gives it a real {@code eventReceived}, this fails and
     * tells them to reconsider the flag — which is the failure mode W15's javadoc warns about.
     */
    @Test
    public void serviceRegistryInheritsTheCallbacksItOptsOutOf() throws NoSuchMethodException {
        Class<?> c = com.telamin.fluxtion.runtime.service.ServiceRegistryNode.class;
        assertThat(c.getMethod("processingComplete").getDeclaringClass().getName(),
                is(Auditor.class.getName()));
        assertThat(c.getMethod("eventReceived", Object.class).getDeclaringClass().getName(),
                is(Auditor.class.getName()));
        assertThat(c.getMethod("eventReceived", com.telamin.fluxtion.runtime.event.Event.class)
                        .getDeclaringClass().getName(),
                is(Auditor.class.getName()));
    }
}
