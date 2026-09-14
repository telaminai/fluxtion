/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.annotations.AfterEvent;
import com.telamin.fluxtion.runtime.event.Event;
import com.telamin.fluxtion.runtime.lifecycle.Lifecycle;

/**
 * Audits runtime operations of a static event processor. An Auditor receives
 * callback notifications as the user interacts with the SEP. The auditing
 * notifications are:
 * <ul>
 * <li>node registrations during {@link Lifecycle#init() }
 * <li>receipt of events
 * <li>individual node invocations on the execution path.
 * </ul>
 * <p>
 * <p>
 * The {@link #auditInvocations() } controls the granularity of audit
 * information published to an Auditor. The boolean return has the following
 * effect:
 * <ul>
 * <li>true - auditor receives all lifecycle callbacks
 * <li>false - auditor receives all lifecycle callbacks except:
 * {@link #nodeInvoked(Object, String, String, Object) }
 * </ul>
 * <p>
 * <p>
 * An Auditor can provide various meta functions for the SEP they are registered
 * with, such as:
 * <ul>
 * <li> Generic event logger
 * <li> Node state persistence strategy
 * <li> Bespoke performance monitor
 * <li> Realtime property tracer
 * <li> Commit/rollback functionality
 * <li> A profiler
 * </ul>
 * <p>
 * <p>
 * Register an implementation of the Auditor interface with SepConfig.addAuditor
 * in the builder module, registration is a build time only operation. The
 * Fluxtion compiler automatically integrates the auditor into the generated SEP.
 *
 * @author greg higgins
 */
public interface Auditor extends Lifecycle {

    /**
     * Callback for each node registered in the SEP. This method will be invoked
     * after init, but before any event processing methods are invoked.
     *
     * @param node     The node instance in the SEP
     * @param nodeName The unique name of the node in the SEP
     */
    void nodeRegistered(Object node, String nodeName);

    /**
     * Callback indicating the Event to be processed by the SEP nodes. Will be
     * called before any node has processed the event.
     *
     * @param event the event to be processed
     */
    default void eventReceived(Event event) {
    }

    /**
     * Callback indicating the Event to be processed by the SEP nodes as an
     * Object. Will be called before any node has processed the event.
     *
     * @param event the event to be processed
     */
    default void eventReceived(Object event) {
    }

    /**
     * Callback to indicate all nodes have processed the Event and the execution
     * path for that event is complete.
     */
    default void processingComplete() {
    }

    @Override
    default void init() {
    }

    @Override
    default void tearDown() {
    }

    /**
     * Callback method received by the auditor due to processing an event. This
     * method is invoked before the node in the execution path receives a
     * notification.
     *
     * @param node       The next node to process in the execution path
     * @param nodeName   The name of the node, this is the same name as the
     *                   variable name of the node in the SEP
     * @param methodName The method of the node that is next to be invoked in
     *                   the execution path.
     * @param event      The event that is the root of the of this execution path.
     */
    default void nodeInvoked(Object node, String nodeName, String methodName, Object event) {
    }

    /**
     * Indicates whether an auditor is interested in receiving nodeInvoked event
     * callback. Some auditors are not interested in granular monitoring of the
     * execution path and can opt out of node invocation callbacks.
     * <ul>
     * <li>true - auditor receives all lifecycle callbacks</li>
     * <li>false - auditor receives all lifecycle callbacks except:
     * nodeInvoked</li>
     * </ul>
     *
     * @return intention to receive all lifecycle callbacks.
     */
    default boolean auditInvocations() {
        return false;
    }

    /**
     * Indicates whether an auditor is interested in the per-event callbacks
     * {@link #eventReceived(Object)}, {@link #eventReceived(Event)} and
     * {@link #processingComplete()}. Some auditors exist only for their
     * {@link #nodeRegistered(Object, String)} bookkeeping and do nothing on the event path.
     * <ul>
     * <li>true - auditor receives the per-event callbacks (the default, and the behaviour
     * of every auditor written before this method existed)</li>
     * <li>false - the generated event processor does not call this auditor on the event
     * path at all</li>
     * </ul>
     * <p>
     * This is the event-path counterpart of {@link #auditInvocations()}, and it is read at
     * <b>build time</b> against the live auditor instance, exactly as that method is. Returning
     * false does not make the calls cheap — it means the generated source never contains them.
     * {@link NodeNameAuditor} returns false: it maps nodes to names during registration and
     * inherits both {@code eventReceived} no-ops, so every generated processor was paying two
     * inherited virtual calls per event for an auditor that had nothing to do.
     *
     * <p><b>If you override any of the three callbacks, this must return true.</b> The flag is read
     * from the live instance at build time and is virtual like everything else, so a subclass that
     * overrides {@code eventReceived} but inherits a {@code false} from its parent silently loses
     * its callbacks — the generated source will not contain them and nothing will report it.
     *
     * <p>The gate is deliberately coarse: it covers all three per-event callbacks together, the way
     * {@link #auditInvocations()} covers every {@code nodeInvoked} with one boolean. An auditor that
     * wants only one of them returns true and takes all three.
     *
     * @return intention to receive the per-event callbacks
     */
    default boolean auditEventReceipt() {
        return true;
    }

    /**
     * An Auditor marked with this interface will have {@link #processingComplete()}
     * called before the event nodes {@link AfterEvent}'s are processed
     * <p>
     * Normally the {@link #processingComplete()} will be called following all the nodes
     * annotated with {@link AfterEvent} have been invoked.
     */
    interface FirstAfterEvent {
    }
}
