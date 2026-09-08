/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.generation.config;

import com.telamin.fluxtion.builder.callback.*;
import com.telamin.fluxtion.builder.context.EventProcessorContextFactory;
import com.telamin.fluxtion.builder.context.InstanceSupplierFactory;
import com.telamin.fluxtion.builder.filter.EventHandlerFilterOverride;
import com.telamin.fluxtion.builder.meta.model.ClassName;
import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;
import com.telamin.fluxtion.builder.input.SubscriptionManagerFactory;
import com.telamin.fluxtion.builder.node.*;
import com.telamin.fluxtion.builder.output.SinkPublisherFactory;
import com.telamin.fluxtion.builder.time.ClockFactory;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.builder.Inject;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent.LogLevel;
import com.telamin.fluxtion.runtime.audit.BinaryLogRecord;
import com.telamin.fluxtion.runtime.audit.EventLogManager;
import com.telamin.fluxtion.runtime.node.EventHandlerNode;
import com.telamin.fluxtion.runtime.service.ServiceRegistryNode;
import com.telamin.fluxtion.runtime.time.Clock;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

/**
 * Configuration used by Fluxtion event stream compiler at generation time to
 * control the processing logic of the {@link CloneableDataFlow}
 *
 * @author Greg Higgins
 */
@ToString
public class EventProcessorConfig {

    private final Set<Class<?>> interfaces = new HashSet<>();
    private final Set<ClassName> interfacesClassName = new HashSet<>();
    private final Clock clock = Clock.DEFAULT_CLOCK;
    private final Map<String, String> class2replace = new HashMap<>();
    private final Map<Object, Integer> filterMap = new HashMap<>();
    private final Map<Class<?>, Function<FieldContext, String>> classSerializerMap = new HashMap<>();
    private String templateFile;
    private List<Object> nodeList;
    private HashMap<Object, String> publicNodes;
    private HashMap<String, Auditor> auditorMap;
    /** Names registered by {@link #addFrameworkAuditor}: framework plumbing, not authored nodes. */
    private final Set<String> frameworkAuditorNames = new HashSet<>();
    private NodeFactoryRegistration nodeFactoryRegistration;
    private RootNodeConfig rootNodeConfig;
    private boolean inlineEventHandling = false;
    private boolean supportDirtyFiltering = true;
    private boolean instanceOfDispatch = true;
    @Getter
    @Setter
    private boolean supportBufferAndTrigger = true;

    /**
     * M50/W4 — whether the generated processor keeps the re-entrancy wrapper on the event path.
     *
     * <p>{@code processEvent} runs on EVERY event and, with support on, tests a {@code processing}
     * flag, queues re-entrant events and drains the callback queue. When no node in the graph can
     * raise a re-entrant event the queue is provably always empty and all of that is dead code.
     * Round 58 measured the wrapper at <b>-26% on native-image</b> and -7% on a JIT.
     *
     * <p><b>Default true.</b> Turning it off trades a capability for throughput, so the safe default
     * is current behaviour. With it off the generated processor retains a guard that <b>throws</b>
     * rather than silently dropping a re-entrant event: build-time detection cannot be complete,
     * because a node can reach the dispatcher through a service or reflectively.
     */
    @Getter
    @Setter
    private boolean supportReentrancy = true;

    /**
     * M50/W4 — whether the generated processor registers itself with the subscription manager.
     *
     * <p>{@code subscriptionManager.setSubscribingEventProcessor(this)} runs in the CONSTRUCTOR, so
     * the processor escapes before it has handled an event and can never be scalar-replaced. Round 58
     * found the escape chain worth more under AOT than PGO: 1.55 ns vs 3+ ns for the same source.
     *
     * <p><b>Default true.</b> Set false only when the processor is driven directly through
     * {@code onEvent} and never subscribes to an event feed.
     */
    @Getter
    @Setter
    private boolean supportSubscriptions = true;

    /**
     * M50/W4 — whether every node is registered with each auditor at construction.
     *
     * <p>{@code initialiseAuditor} calls {@code auditor.nodeRegistered(node, name)} for EVERY node,
     * and {@code NodeNameAuditor} stores them in two {@code HashMap}s. That publishes every node into
     * a live heap structure, so no node can be scalar-replaced and the whole graph materialises.
     *
     * <p>It is the single largest cost in a generated processor and it is invisible without a profile,
     * because an unprofiled image is already slow for other reasons. Measured on a real generated
     * processor under accurate PGO: <b>5.07 ns with registration, 1.57 ns without</b> — the latter
     * matching hand-written flat code at 1.54.
     *
     * <p><b>Default true.</b> Set false only when nothing needs to resolve a node by name:
     * {@code getNodeById}, {@code DataFlow.getServiceById} and any auditor that uses
     * {@code nodeRegistered} (an audit log naming its nodes, for instance) all depend on it.
     */
    @Getter
    @Setter
    private boolean supportNodeNameLookup = true;
    private DISPATCH_STRATEGY dispatchStrategy = DISPATCH_STRATEGY.INSTANCE_OF;
    private List<String> compilerOptions = new ArrayList<>();

    public EventProcessorConfig() {
        clock();
        ServiceRegistryNode serviceRegistryNode = new ServiceRegistryNode();
        addFrameworkAuditor(serviceRegistryNode, ServiceRegistryNode.NODE_NAME);
        addNode(serviceRegistryNode, ServiceRegistryNode.NODE_NAME);
        this.nodeFactoryRegistration = new NodeFactoryRegistration(NodeFactoryConfig.required.getFactoryClasses());
        classSerializerMap.putAll(ClassSerializerRegistry.service("java").classSerializerMap());
    }

    /**
     * Add a node to the SEP. The node will have private final scope, the
     * variable name of the node will be generated from {@link NodeNameProducer}
     * strategy.<p>
     * Fluxtion will check if this node is already in the node set and will
     * return the previously added node.
     *
     * @param <T>  The type of the node to add to the SEP
     * @param node the node instance to add
     * @return The de-duplicated added node
     */
    @SuppressWarnings("unchecked")
    public <T> T addNode(T node) {
        if (getNodeList() == null) {
            setNodeList(new ArrayList<>());
        }
        if (!getNodeList().contains(node)) {
            getNodeList().add(node);
            return node;
        }
        return (T) getNodeList().get(getNodeList().indexOf(node));
    }

    public EventProcessorConfig addNode(Object node, Object... nodeList) {
        addNode(node);
        Arrays.asList(nodeList).forEach(this::addNode);
        return this;
    }

    /**
     * Add a node to the SEP. The node will have public final scope, the
     * variable name of the node will be generated from {@link NodeNameProducer}
     * strategy if the provided name is null.<p>
     * Fluxtion will check if this node is already in the node set and will
     * return the previously added node.
     *
     * @param <T>  The type of the node to add to the SEP
     * @param node the node instance to add
     * @param name the variable name of the node
     * @return The de-duplicated added node
     */
    @SuppressWarnings("unchecked")
    public <T> T addNode(T node, String name) {
        addNode(node);
        addPublicNode(node, name);
        return (T) getNodeList().get(getNodeList().indexOf(node));
    }

    /**
     * Add a node to the SEP. The node will have public final scope, the
     * variable name of the node will be generated from {@link NodeNameProducer}
     * strategy if the provided name is null.<p>
     * Fluxtion will check if this node is already in the node set and will
     * return the previously added node.
     *
     * @param <T>  The type of the node to add to the SEP
     * @param node the node instance to add
     * @param name the variable name of the node
     * @return The de-duplicated added node
     */
    public <T> T addPublicNode(T node, String name) {
        if (getPublicNodes() == null) {
            setPublicNodes(new HashMap<>());
        }
        getPublicNodes().put(node, name);
        return node;
    }

//    public void addNode(MethodReferenceReflection methodReference){
//
//    }

    /**
     * Adds an {@link Auditor} to this SEP. The Auditor will have public final
     * scope and can be accessed via the provided variable name.
     *
     * @param <T>      The type of the Auditor
     * @param listener Auditor instance
     * @param name     public name of Auditor
     * @return the added Auditor
     */
    public <T extends Auditor> T addAuditor(T listener, String name) {
        if (getAuditorMap() == null) {
            setAuditorMap(new HashMap<>());
        }
        getAuditorMap().put(name, listener);
        // The auditor map is single-slot per NAME, so this registration replaces whatever held the
        // name — and provenance must follow the replacement. An author who registers under a name the
        // framework already used owns the binding from here on; leaving the name marked would publish
        // their node as framework plumbing and drop it from the authored-node count.
        frameworkAuditorNames.remove(name);
        return listener;
    }

    /**
     * Registers an auditor the FRAMEWORK supplies, rather than one the author wrote.
     *
     * <p>The distinction is not cosmetic and cannot be recovered downstream. {@link #getAuditorMap()}
     * mixes both, so a consumer asking "did the compiler create this node, or did the author?" has
     * only the bean name to go on — and answering from a package prefix misclassifies a user class in
     * a framework-shaped package, and a framework class outside one. Recording it here, where the
     * framework registers its own, makes it a fact.
     *
     * <p>Consumed by artefact metadata (GraphML {@code fluxtion.framework}) so a coverage figure can
     * have an honest denominator: framework plumbing an author never wrote should not count against
     * them.
     */
    public <T extends Auditor> T addFrameworkAuditor(T listener, String name) {
        // Delegate FIRST, then mark. addAuditor clears the mark, because an author registration must
        // clear it; recording before the call would have the delegate immediately undo it. The order
        // is load-bearing in both directions — framework-over-author marks, author-over-framework
        // clears — and each direction is pinned by a test.
        T registered = addAuditor(listener, name);
        frameworkAuditorNames.add(name);
        return registered;
    }

    /**
     * The names of auditors the framework registered itself. Never null.
     *
     * <p>Membership follows the LAST registration under a name, because the auditor map itself is
     * single-slot per name. An author who registers over a framework name owns the binding and the
     * name leaves this set; the framework registering over an author's name puts it back. An earlier
     * version only ever added, so replacing the clock left the author's own node published as
     * framework plumbing — the opposite of what this method promises.
     */
    /**
     * M50/W4 — a named bundle of the settings that decide dispatch cost.
     *
     * <p>These settings are individually documented, individually measured, and individually silent
     * when omitted: a processor missing one is still correct and simply runs 3-5x slower with no
     * diagnostic. Round 59 spent most of its time discovering that a figure which looked like
     * compiler instability was a missing setting. A named profile removes the possibility.
     *
     * <p>Each profile states what it GIVES UP, because none of them is free.
     */
    public enum PerformanceProfile {
        /**
         * Framework defaults. Every capability on. Nothing is given up and nothing is tuned.
         */
        DEFAULT,
        /**
         * The audit log is the product: keep auditors, but stop the two defaults that allocate.
         *
         * <p>Gives up: the event's {@code toString()} and the thread name in each record.
         * Keeps: everything else, including a fully populated node-name map.
         * Measured: 885 -> 550 ns/event with node tracing on, and 208 bytes/event -> zero, which is
         * what lets an audited processor run under a non-collecting GC.
         */
        AUDITED,
        /**
         * Lowest dispatch cost. <b>Gives up the audit log and conditional propagation.</b>
         *
         * <p>Drops the framework auditors — so no {@code Clock} reading the system clock on every
         * event, and no {@code NodeNameLookup} map — turns off dirty filtering, and stops node
         * registration. Node lookup still works: the generator emits it as code.
         *
         * <p><b>It also gives up buffer-and-trigger and subscriptions</b>, which removes the buffer
         * branch from every event and stops the constructor publishing the processor to the
         * subscription manager. <b>Measured, that is worth nothing on a 10-node graph</b> — five
         * interleaved JIT reps with output verified identical read 5.124 ns before and 5.143 after,
         * and a landed native build reads 1.7176 against a 1.54–1.69 band. They are set because the
         * generated code is smaller and because neither can change a result, not because they are
         * faster.
         *
         * <p><b>Re-entrancy is deliberately left alone.</b> {@code setSupportReentrancy(false)} is the
         * one setting in this family that can break a working graph — re-entrant dispatch stops being
         * queued and throws {@link IllegalStateException} instead — and it was measured at the same
         * time and bought nothing either. Build-time detection cannot be complete, because a node can
         * reach the dispatcher through a service or reflectively. Set it yourself if your graph
         * provably never re-enters; a profile should not spend that capability for you.
         *
         * <p><b>What giving up re-entrancy means, precisely.</b> A node that dispatches back into the
         * processor while an event is in flight is no longer queued — the generated code keeps a guard
         * and throws {@link IllegalStateException} naming the event. It fails loudly; it does not
         * silently drop or reorder. Build-time detection cannot be complete, because a node can reach
         * the dispatcher through a service or reflectively, which is why the runtime guard is retained.
         * If your graph re-enters, call {@code setSupportReentrancy(true)} after the profile.
         *
         * <p>Requires void triggers on the nodes themselves
         * ({@code failBuildIfMissingBooleanReturn = false}), which this profile cannot set for you.
         * Measured on a 10-node graph: ~4.9 ns on any JIT, ~1.6 ns native with PGO and the generated
         * inlining directive.
         */
        LOWEST_LATENCY,
        /**
         * <b>The audit log, at the lowest cost that keeps it.</b> M52.2.
         *
         * <p>{@link #AUDITED} keeps every capability; {@link #LOWEST_LATENCY} drops the audit log
         * entirely. Neither is the deployed case, which is "I want the audit log and I want it cheap".
         *
         * <p>Keeps: the {@link EventLogManager} auditor — the audit log is the point — the
         * {@link Clock}, which every timestamp in the record depends on, and node registration, which
         * is how every node gets its {@code EventLogger}. Re-entrancy is kept for the reason
         * {@link #LOWEST_LATENCY} documents at length.
         *
         * <p><b>Conditional propagation is given up.</b> Measured with the harness version held equal
         * across both arms: guards cost <b>7.6 ns/event on a graph with no auditing</b> and are
         * <b>indistinguishable once auditing</b> (144.11 against 142.83 on native, inside the build
         * lottery). On this graph they also decide nothing — tracing shows guards-on and guards-off
         * invoking identical nodes, because each event reaches its chain by topology.
         *
         * <p><b>What you give up:</b> an {@code @OnTrigger} method now runs whenever the wave reaches
         * it, not only when a parent is dirty. Invisible for pure recomputation; not invisible for a
         * node that accumulates or has side effects. If your graph has heavy nodes behind a
         * sometimes-cold join, call {@code setSupportDirtyFiltering(true)} after the profile — a guard
         * breaks even at about a 4% skip rate for a node doing real work.
         *
         * <p>Gives up: per-node method tracing, the event's {@code toString()}, the thread name in each
         * record, buffer-and-trigger and subscriptions. <b>It does not give up node-name lookup</b>,
         * because node registration is what supplies every node's {@code EventLogger} — dropping it
         * silently disables the audit log rather than making it cheaper. The first three are
         * what make a record readable when you do not know what you are looking for — the right trade
         * for a production hot path and the wrong one for a development run, where {@link #AUDITED}
         * remains the profile to use.
         *
         * <p><b>What keeping dirty filtering costs, measured.</b> On a 30-node, 5-event-type converging
         * graph where every node on the path logs, JIT, minimum of 6 interleaved reps:
         *
         * <pre>
         *   LOWEST_LATENCY, no auditors, dirty filtering OFF   12.05 ns
         *   LOW_LATENCY_AUDIT, no auditor installed            29.08 ns   <- +17.0 ns
         *   LOW_LATENCY_AUDIT with the auditor and a record    84.41 ns   <- +55.3 ns of audit
         * </pre>
         *
         * The middle row was measured before this profile turned guards off, and is what they cost:
         * <b>~17 ns on JIT and ~23 on native</b> for a ~12-node path on which they skipped nothing.
         * Turning them off is what closes that row against the {@code LOWEST_LATENCY} line.
         *
         * <p>Stating the split matters because the two were conflated: an earlier measurement compared
         * this profile against a {@code LOWEST_LATENCY} baseline and reported the whole 72 ns gap as
         * "audit cost". <b>It was 55 ns of audit and 17 ns of dirty filtering</b>, and the only way to
         * see that was to build a baseline whose sole difference from the audited processor was the
         * auditor itself — 172 {@code isDirty_} references on both sides instead of 172 against zero.
         *
         * <p>Measured on a 30-node, 5-event-type graph with a converging tail — see
         * {@code docs/experience/runs/round-63} in the analyser repo.
         */
        LOW_LATENCY_AUDIT
    }

    /**
     * Applies a {@link PerformanceProfile}. Call it FIRST, then override individual settings if you
     * need to — a profile is a starting point, not a lock.
     *
     * @return this config, for chaining
     */
    public EventProcessorConfig performanceProfile(PerformanceProfile profile) {
        if (profile == null || profile == PerformanceProfile.DEFAULT) {
            return this;
        }
        if (profile == PerformanceProfile.LOWEST_LATENCY) {
            setSupportDirtyFiltering(false);
            setSupportNodeNameLookup(false);
            // Added 2026-09-07. Both remove work from the generated event path — the buffer branch,
            // and the constructor publishing the processor to the subscription manager — and neither
            // can change a result: you either use the capability or you do not.
            //
            // Measured, and the measurement is the point: on a 10-node graph this is worth NOTHING.
            // Five interleaved JIT reps read 5.124 before and 5.143 after; a landed native build reads
            // 1.7176 against a 1.54-1.69 band. They are here because the generated code is smaller and
            // the profile's own documentation already lists them as baseline configuration, not
            // because they are faster.
            //
            // setSupportReentrancy(false) is deliberately NOT set here. It is the one of the three
            // that can break a working graph: re-entrant dispatch stops queueing and throws instead.
            // It was measured at the same time and bought nothing either, so the profile does not
            // spend a capability on it. Set it yourself if your graph provably never re-enters.
            setSupportBufferAndTrigger(false);
            setSupportSubscriptions(false);
            if (getAuditorMap() != null) {
                getAuditorMap().keySet().removeAll(new HashSet<>(getFrameworkAuditorNames()));
            }
        }
        if (profile == PerformanceProfile.LOW_LATENCY_AUDIT) {
            // Keep the audit log and the clock, and the two capabilities LOWEST_LATENCY drops purely
            // for generated-code size.
            //
            // setSupportNodeNameLookup(false) is deliberately NOT set, and the reason is the whole
            // point of this profile. That flag does not merely drop a name map: it stops NODE
            // REGISTRATION, and node registration is how EventLogManager.nodeRegistered() hands each
            // node its EventLogger. Without it every node's auditLog is the null logger, no node ever
            // records anything, and the audit log this profile exists to keep is silently dead — the
            // processor still runs, still looks right, and simply publishes nothing.
            //
            // An earlier version of this profile did set it. The generated processor emitted ZERO
            // nodeRegistered calls against 33 for AUDITED, and the benchmark that was supposed to be
            // measuring the cost of auditing was measuring a graph with no audit at all - and duly
            // reported it as a speed-up. Found only because the harness was made to assert that the
            // sink actually saw records. LowLatencyAuditProfileTest now pins this both ways.
            setSupportBufferAndTrigger(false);
            setSupportSubscriptions(false);
            // Guards OFF, on measurements that took three attempts to get right.
            //
            // Interleaved, minimum of 5-8, both arms built from the SAME harness version:
            //
            //                            guards ON   guards OFF    delta
            //   no audit,     JIT            27.99        20.33    -7.66
            //   no audit,     native         25.66        18.02    -7.63
            //   AUDITED,      native        144.11       142.83    -1.28   (inside the lottery)
            //
            // So: clearly better with no audit, and indistinguishable once auditing. Free on the path
            // this profile serves, worth 7.6 ns on the path it does not - an easy call.
            //
            // On this graph the guards also decide nothing, and that is a measured fact rather than an
            // assumption: with tracing on, guards-on and guards-off invoke IDENTICAL nodes (13/10/11),
            // because each event reaches its chain by topology. Guards only decide anything where a
            // node has several parents and only some are dirty.
            //
            // An intermediate measurement said guards-off was 23% SLOWER on the audited native path.
            // It compared a guards-on binary built from a pre-h3 harness against a guards-off binary
            // built from h3 - two variables, not one. Rebuilt with the harness held equal, and two
            // builds per configuration to bound the lottery, the difference vanished. The audit output
            // was identical throughout (recPerEvent 1.000, 180.8 B/record, same checksum), which is
            // what said the timing difference had to be an artifact.
            //
            // WHAT THIS GIVES UP: an @OnTrigger method now runs whenever the wave reaches it, not only
            // when a parent is dirty. Invisible for pure recomputation; NOT invisible for a node that
            // accumulates or has side effects. An author with heavy nodes behind a sometimes-cold join
            // calls setSupportDirtyFiltering(true) after the profile - it breaks even at about a 4%
            // skip rate for a node doing real work.
            setSupportDirtyFiltering(false);
            // NOT setSupportReentrancy(false): see LOWEST_LATENCY. It is the one setting here that can
            // turn a working graph into an exception, and a profile should not spend that.
            // The EventLogManager's own settings are applied by addLowLatencyEventLog() below.
            return this;
        }
        // AUDITED intentionally changes nothing here: its two settings live on the EventLogManager
        // and are applied by addEventAudit(level, printEventToString, printThreadName). Naming the
        // profile still documents the choice, and auditedEventLogConfig() below applies it.
        return this;
    }

    /**
     * The audit configuration {@link PerformanceProfile#AUDITED} means: records, node tracing, and
     * neither of the two defaults that allocate.
     */
    public EventProcessorConfig addAuditedEventLog(LogLevel tracingLogLevel) {
        return addEventAudit(tracingLogLevel, false, false);
    }

    /**
     * The audit configuration {@link PerformanceProfile#LOW_LATENCY_AUDIT} means: records on, method
     * tracing <b>off</b>, and neither of the two defaults that allocate.
     *
     * <p>Tracing is the expensive half. Measured on a 30-node converging-tail graph: tracing on costs
     * ~184 ns/event more than tracing off, on top of the record itself.
     *
     * @param entryLevel threshold for {@code EventLogger} entries the nodes themselves write
     */
    /**
     * Which record the audit log builds. Selected as part of
     * {@link PerformanceProfile#LOW_LATENCY_AUDIT} rather than swapped at runtime, so the choice is a
     * build input like every other setting in a profile.
     */
    public enum AuditRecordFormat {
        /**
         * The YAML text record — what every Fluxtion processor has always produced, and what the
         * analyser and every existing reader can open.
         */
        TEXT,
        /**
         * {@link BinaryLogRecord} — ids and raw bits instead of characters. <b>3.2× faster at one
         * logging node and 5.1× when every node on the path logs</b>, and 54 bytes per record against
         * 193.
         *
         * <p><b>Nothing can read it yet.</b> The analyser registers only a YAML reader and the
         * Chronicle reader is unfiled, so a processor built with this produces a log no existing tool
         * can open. That is why {@link PerformanceProfile#LOW_LATENCY_AUDIT} does not select it for you.
         */
        BINARY
    }

    public EventProcessorConfig addLowLatencyEventLog(LogLevel entryLevel) {
        return addLowLatencyEventLog(entryLevel, AuditRecordFormat.TEXT);
    }

    /**
     * The low-latency audit log, with the record format chosen explicitly.
     *
     * <p>Measured on a 30-node, 5-event-type converging graph where every node on the path logs
     * (11.75 entries per record), minimum of interleaved reps:
     *
     * <pre>
     *                       JIT        native
     *   TEXT            403.0 ns      698.6 ns
     *   BINARY           54.6 ns      115.8 ns
     * </pre>
     *
     * <p>The gap widens with audit density — 3.2× when one node logs, 5.1× when every node does —
     * because the text record formats a node name, a key and a double <em>inside the event cycle</em>
     * at about 26 ns per entry, against 3.4 for bits.
     *
     * @param entryLevel threshold for the entries nodes themselves write
     * @param format     {@link AuditRecordFormat#TEXT} unless you have a reader for the binary form
     */
    public EventProcessorConfig addLowLatencyEventLog(LogLevel entryLevel, AuditRecordFormat format) {
        EventLogManager manager = new EventLogManager()
                .tracingOff()
                .logLevel(entryLevel == null ? LogLevel.INFO : entryLevel)
                .printEventToString(false)
                .printThreadName(false);
        if (format == AuditRecordFormat.BINARY) {
            manager.binaryRecord(true);
        }
        addFrameworkAuditor(manager, EventLogManager.NODE_NAME);
        return this;
    }

    public Set<String> getFrameworkAuditorNames() {
        return Collections.unmodifiableSet(frameworkAuditorNames);
    }

    /**
     * Maps a class name from one String to another in the generated output.
     *
     * @param originalFqn Class name to replace
     * @param mappedFqn   Class name replacement
     */
    public EventProcessorConfig mapClass(String originalFqn, String mappedFqn) {
        getClass2replace().put(originalFqn, mappedFqn);
        return this;
    }

    /**
     * adds a clock to the generated SEP.
     *
     * @return the clock in generated SEP
     */
    public Clock clock() {
        addFrameworkAuditor(clock, "clock");
        return clock;
    }

    /**
     * Add an {@link EventLogManager} auditor to the generated SEP. Specify
     * the level at which method tracing will take place.
     */
    public EventProcessorConfig addEventAudit(LogLevel tracingLogLevel) {
        if (tracingLogLevel != null) {
            addFrameworkAuditor(new EventLogManager().tracingOn(tracingLogLevel), EventLogManager.NODE_NAME);
        }
        return this;
    }

    /**
     * Add an {@link EventLogManager} auditor to the generated SEP without method tracing
     */
    public EventProcessorConfig addEventAudit() {
        addFrameworkAuditor(new EventLogManager().tracingOff(), EventLogManager.NODE_NAME);
        return this;
    }

    public EventProcessorConfig addEventAudit(LogLevel tracingLogLevel, boolean printEventToString) {
        addEventAudit(tracingLogLevel, printEventToString, true);
        return this;
    }

    public EventProcessorConfig addEventAudit(LogLevel tracingLogLevel, boolean printEventToString, boolean printThreadName) {
        addFrameworkAuditor(
                new EventLogManager()
                        .tracingOn(tracingLogLevel)
                        .printEventToString(printEventToString)
                        .printThreadName(printThreadName),
                EventLogManager.NODE_NAME);
        return this;
    }

    public EventProcessorConfig addInterfaceImplementation(Class<?> clazz) {
        interfaces.add(clazz);
        interfacesClassName.add(ClassName.of(clazz));
        return this;
    }

    public EventProcessorConfig addInterfaceImplementation(ClassName className) {
        interfacesClassName.add(className);
        return this;
    }

    public Set<Class<?>> interfacesToImplement() {
        return interfaces;
    }

    public Set<ClassName> interfacesToImplementClassName() {
        return interfacesClassName;
    }

    /**
     * Users can override this method and add SEP description logic here. The
     * buildConfig method will be called by the Fluxtion generator at build
     * time.
     */
    public void buildConfig() {
    }

    /**
     * the name of the template file to use as an input
     */
    public String getTemplateFile() {
        return templateFile;
    }

    public EventProcessorConfig setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
        return this;
    }

    /**
     * the nodes included in this graph
     */
    public List<Object> getNodeList() {
        return nodeList;
    }

    public EventProcessorConfig setNodeList(List<Object> nodeList) {
        this.nodeList = nodeList;
        return this;
    }

    /**
     * Variable names overrides for public nodes, these will be well known and
     * addressable from outside the SEP.
     */
    public HashMap<Object, String> getPublicNodes() {
        return publicNodes;
    }

    public <T> T getNode(String name) {
        Object[] obj = new Object[1];
        publicNodes.entrySet().stream()
                .filter(e -> e.getValue().equals(name))
                .findFirst()
                .ifPresent(e -> obj[0] = e.getKey());
        return (T) obj[0];
    }

    public EventProcessorConfig setPublicNodes(HashMap<Object, String> publicNodes) {
        this.publicNodes = publicNodes;
        return this;
    }

    public HashMap<String, Auditor> getAuditorMap() {
        return auditorMap;
    }

    public EventProcessorConfig setAuditorMap(HashMap<String, Auditor> auditorMap) {
        this.auditorMap = auditorMap;
        return this;
    }

    /**
     * Node Factory configuration
     */
    public NodeFactoryRegistration getNodeFactoryRegistration() {
        return nodeFactoryRegistration;
    }

    public EventProcessorConfig setNodeFactoryRegistration(NodeFactoryRegistration nodeFactoryRegistration) {
        //add defaults
        nodeFactoryRegistration.factoryClassSet.addAll(NodeFactoryConfig.required.getFactoryClasses());
        this.nodeFactoryRegistration = nodeFactoryRegistration;
        return this;
    }

    /**
     * Makes available in the graph an injectable instance that other nodes can inject see {@link Inject}.
     * The factoryName parameter must match the factoryName attribute in the inject annotation
     * <pre>
     * {@literal }@Inject(factoryName = "someUniqueName")
     *  public RoomSensor roomSensor2;
     *
     * </pre>
     * If no inject annotations reference the instance it will not be added to the graph
     *
     * @param factoryName        The unique name for this instance
     * @param injectionType      The type of injection
     * @param injectableInstance The instance to inject
     * @param <T>                The concrete type of the injected instance
     * @param <S>                The type of the injected instance
     * @return
     */
    public <T, S extends T> EventProcessorConfig registerInjectable(String factoryName, Class<T> injectionType, S injectableInstance) {
        nodeFactoryRegistration.factorySet.add(new SingletonNodeFactory<>(injectableInstance, injectionType, factoryName));
        return this;
    }

    /**
     * Makes available in the graph an injectable instance that other nodes can inject see {@link Inject}.
     * The factoryName parameter must match the factoryName attribute in the inject annotation
     * <pre>
     * {@literal }@Inject(factoryName = "someUniqueName")
     *  public RoomSensor roomSensor2;
     *
     * </pre>
     * If no inject annotations reference the instance it will not be added to the graph
     *
     * @param factoryName        The unique name for this instance
     * @param injectableInstance The instance to inject
     * @param <T>                The concrete type of the injected instance and the type of the injected instance
     * @return
     */
    public <T> EventProcessorConfig registerInjectable(String factoryName, T injectableInstance) {
        registerInjectable(factoryName, (Class<T>) injectableInstance.getClass(), injectableInstance);
        return this;
    }

    public RootNodeConfig getRootNodeConfig() {
        return rootNodeConfig;
    }

    public EventProcessorConfig setRootNodeConfig(RootNodeConfig rootNodeConfig) {
        this.rootNodeConfig = rootNodeConfig;
        return this;
    }

    /**
     * overrides the filter integer id's for a set of instances
     */
    public Map<Object, Integer> getFilterMap() {
        return filterMap;
    }

    public EventProcessorConfig setFilterMap(Map<Object, Integer> filterMap) {
        this.filterMap.clear();
        this.filterMap.putAll(filterMap);
        return this;
    }

    /**
     * Overrides the filterId for any methods annotated with {@link OnEventHandler} in
     * an instance or for an {@link EventHandlerNode}.
     * <p>
     * If a single {@link OnEventHandler} annotated method needs to be overridden then
     * use {@link this#overrideOnEventHandlerFilterId(Object, Class, int)}
     *
     * @param eventHandler the event handler instance to override filterId
     * @param newFilterId  the new filterId
     */
    public EventProcessorConfig overrideOnEventHandlerFilterId(Object eventHandler, int newFilterId) {
        getFilterMap().put(eventHandler, newFilterId);
        return this;
    }

    /**
     * Overrides the filterId for a method annotated with {@link OnEventHandler} in
     * an instance handling a particular event type.
     *
     * @param eventHandler the event handler instance to override filterId
     * @param eventClass   The event handler methods of this type to override filterId
     * @param newFilterId  the new filterId
     */
    public EventProcessorConfig overrideOnEventHandlerFilterId(Object eventHandler, Class<?> eventClass, int newFilterId) {
        getFilterMap().put(new EventHandlerFilterOverride(eventHandler, eventClass, newFilterId), newFilterId);
        return this;
    }

    /**
     * Register a custom serialiser that maps a field to source at generation time
     *
     * @param classToSerialize      the class type to support custom serialisation
     * @param serializationFunction The instance to source function
     * @return current {@link EventProcessorConfig}
     */
    @SuppressWarnings("unchecked")
    public <T> EventProcessorConfig addClassSerializer(
            Class<T> classToSerialize, Function<FieldContext<T>, String> serializationFunction) {
        classSerializerMap.put(classToSerialize, (Function<FieldContext, String>) (Object) serializationFunction);
        return this;
    }

    public Map<Class<?>, Function<FieldContext, String>> getClassSerializerMap() {
        return classSerializerMap;
    }

    /**
     * configures generated code to inline the event handling methods or not.
     */
    public boolean isInlineEventHandling() {
        return inlineEventHandling;
    }

    public EventProcessorConfig setInlineEventHandling(boolean inlineEventHandling) {
        this.inlineEventHandling = inlineEventHandling;
        return this;
    }

    /**
     * configures generated code to support dirty filtering
     */
    public boolean isSupportDirtyFiltering() {
        return supportDirtyFiltering;
    }

    public EventProcessorConfig setSupportDirtyFiltering(boolean supportDirtyFiltering) {
        this.supportDirtyFiltering = supportDirtyFiltering;
        return this;
    }

    /**
     * Map an original fully qualified class name into a new value. Can be
     * useful if generated code wants to remove all dependencies to Fluxtion
     * classes and replaced with user classes.
     */
    public Map<String, String> getClass2replace() {
        return class2replace;
    }

    public boolean isInstanceOfDispatch() {
        return instanceOfDispatch;
    }

    public EventProcessorConfig setInstanceOfDispatch(boolean instanceOfDispatch) {
        this.instanceOfDispatch = instanceOfDispatch;
        return this;
    }

    public DISPATCH_STRATEGY getDispatchStrategy() {
        return dispatchStrategy;
    }

    public EventProcessorConfig setDispatchStrategy(DISPATCH_STRATEGY dispatchStrategy) {
        Objects.requireNonNull(dispatchStrategy, "Dispatch strategy must be non null");
        try {
            Object version = Runtime.class.getMethod("version").invoke(Runtime.class);
            Integer featureId = (Integer) version.getClass().getMethod("feature").invoke(version);
            if (featureId > 21 && featureId < 25) {
                enablePreviewFeatures();
                javaTargetRelease("" + featureId);
            } else if (featureId >= 25) {
                //do nothing
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }
        this.dispatchStrategy = dispatchStrategy;
        return this;
    }

    public List<String> getCompilerOptions() {
        return compilerOptions;
    }

    public EventProcessorConfig setCompilerOptions(List<String> compilerOptions) {
        Objects.requireNonNull(compilerOptions);
        this.compilerOptions = compilerOptions;
        return this;
    }

    public EventProcessorConfig enablePreviewFeatures() {
        compilerOptions.add("--enable-preview");
        return this;
    }

    public EventProcessorConfig javaTargetRelease(String release) {
        Objects.requireNonNull(release);
        compilerOptions.add("--release");
        compilerOptions.add(release);
        return this;
    }

    public enum DISPATCH_STRATEGY {
        CLASS_NAME,
        INSTANCE_OF,
        PATTERN_MATCH
    }

    enum NodeFactoryConfig {
        required(
                CallBackDispatcherFactory.class,
                CallbackNodeFactory.class,
                ClockFactory.class,
                InstanceSupplierFactory.class,
                DirtyStateMonitorFactory.class,
                EventDispatcherFactory.class,
                EventProcessorCallbackInternalFactory.class,
                EventProcessorContextFactory.class,
                NodeNameLookupFactory.class,
                SubscriptionManagerFactory.class,
                SinkPublisherFactory.class
        );

        private final HashSet<Class<? extends NodeFactory<?>>> defaultFactories = new HashSet<>();

        NodeFactoryConfig(Class<? extends NodeFactory<?>>... factoryClasses) {
            Arrays.asList(factoryClasses).forEach(defaultFactories::add);
        }

        public Set<Class<? extends NodeFactory<?>>> getFactoryClasses() {
            return new HashSet<>(defaultFactories);
        }
    }
}
