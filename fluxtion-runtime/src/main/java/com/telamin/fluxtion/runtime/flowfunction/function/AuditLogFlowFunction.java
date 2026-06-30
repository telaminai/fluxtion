/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.function;

import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;
import com.telamin.fluxtion.runtime.audit.EventLogger;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableBiConsumer;
import com.telamin.fluxtion.runtime.partition.MethodReferenceInfo;

/**
 * Pass-through tap that emits structured key/value entries to the graph audit log on each trigger.
 *
 * <p>Mirrors {@link PeekFlowFunction} but, instead of an arbitrary side-effect consumer, runs a
 * <b>named emitter</b> against this node's injected {@link EventLogger} ({@code auditLog}, inherited from
 * {@link com.telamin.fluxtion.runtime.audit.EventLogNode} via {@link AbstractFlowFunction}). The emitter
 * receives the logger plus the streamed value and writes the desired keys, e.g.
 * {@code (log, t) -> log.info("symbol", t.symbol()).info("qty", t.qty())}. Holding a single emitter (a
 * named method reference) rather than an array of per-key accessors keeps the construct regenerable under
 * AOT — a method reference must sit in a generic position to carry its target type.
 *
 * <p>Observation only: the upstream value flows downstream unchanged, the node never halts propagation,
 * and audit logging is purely side-effecting (no graph-state change) — so determinism and replay are
 * preserved.
 *
 * <p><b>Level semantics.</b> The tap is level-agnostic: it does <em>not</em> set the logger level. The
 * <b>severity of each emitted entry</b> is chosen by the emitter (which calls {@code log.info(...)},
 * {@code log.debug(...)}, etc.), while the <b>filter</b> is the host-configured {@link EventLogger} level
 * (via {@code EventProcessorConfig.addEventAudit}/{@code DataFlow.setAuditLogLevel}). So the host stays
 * authoritative: it can suppress {@code debug} taps, or raise the level to {@code TRACE} to also capture
 * node-invocation traces — without the tap overriding it on every event.
 *
 * @param <T> the streamed value type
 * @param <S> the upstream {@link FlowFunction} type
 */
public class AuditLogFlowFunction<T, S extends FlowFunction<T>> extends AbstractFlowFunction<T, T, S> {

    @NoTriggerReference
    final SerializableBiConsumer<EventLogger, ? super T> auditEmitter;

    public AuditLogFlowFunction(S inputEventStream,
                                SerializableBiConsumer<EventLogger, ? super T> auditEmitter) {
        super(inputEventStream, auditEmitter);
        this.auditEmitter = auditEmitter;
    }

    /**
     * Closed-world constructor — method-reference metadata supplied by the generator (no runtime
     * {@code SerializedLambda}), mirroring {@link PeekFlowFunction}'s closed-world path.
     */
    public AuditLogFlowFunction(S inputEventStream,
                                SerializableBiConsumer<EventLogger, ? super T> auditEmitter,
                                MethodReferenceInfo methodReferenceInfo) {
        super(inputEventStream, auditEmitter, methodReferenceInfo);
        this.auditEmitter = auditEmitter;
    }

    @OnTrigger
    public void tap() {
        auditEmitter.accept(auditLog, get());
    }

    @Override
    public T get() {
        return getInputEventStream().get();
    }
}