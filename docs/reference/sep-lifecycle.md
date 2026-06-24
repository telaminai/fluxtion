# Fluxtion SEP Lifecycle — one page

A Static Event Processor (SEP) goes through six phases between build and event flow. Each phase has specific hooks; getting the order wrong is the single biggest source of "why isn't my node receiving X?" confusion.

## The phases at a glance

```
   ┌─────────┐    ┌────────────┐    ┌──────────────────┐    ┌────────────┐
   │ BUILD   │ ─▶ │ CONSTRUCT  │ ─▶ │ INIT             │ ─▶ │ START      │
   │ (offline│    │ (runtime)  │    │ (runtime)        │    │ (runtime)  │
   │ codegen)│    │            │    │                  │    │            │
   └─────────┘    └────────────┘    └──────────────────┘    └────────────┘
                                            │                       │
                                            ▼                       ▼
                                  ┌──────────────────┐    ┌────────────┐
                                  │ EVENT PROCESSING │ ─▶ │ TEARDOWN   │
                                  │ (runtime, hot)   │    │ (runtime)  │
                                  └──────────────────┘    └────────────┘
```

| Phase     | What happens                                                                  | Your hooks |
|-----------|-------------------------------------------------------------------------------|------------|
| BUILD     | Builder constructs the graph; codegen emits `.java` + `.graphml` + `.class`. | `FluxtionGraphBuilder.buildGraph(config)` |
| CONSTRUCT | SEP class instantiated. Nodes are field-initialised but unwired. | No-arg constructor on each node |
| INIT      | `Lifecycle.init()` runs. Auditors call `nodeRegistered` on every node. `DataFlowContextListener.currentContext` fires. `@ServiceRegistered` listener tables populate. `@Initialise` methods run **after** all `nodeRegistered` calls complete. | `@Initialise` |
| START     | `Lifecycle.start()` runs. `@Start` methods fire on every node. | `@Start` |
| EVENTS    | Hot path. Events dispatched to `@OnEventHandler`, `@OnTrigger`, exported services. | `@OnEventHandler`, `@OnTrigger`, `@ExportService` |
| TEARDOWN  | `Lifecycle.stop()` → `@Stop` methods. Then `Lifecycle.tearDown()` → `@TearDown` methods. | `@Stop`, `@TearDown` |

## Detailed firing order during INIT

The sequence callers most often need to reason about:

```
   sep.init()
       │
       ├─ ServiceRegistryNode.init()                   // clear callback tables
       │
       ├─ for each registered node N:
       │     ├─ ServiceRegistryNode.nodeRegistered(N, name)
       │     │     │
       │     │     ├─ if N implements DataFlowContextListener:
       │     │     │     N.currentContext(ctx)         ◀── context delivered HERE
       │     │     │
       │     │     └─ scan N.getClass() for @ServiceRegistered methods
       │     │           register callbacks under (type, name) keys
       │     │
       │     └─ other auditors' nodeRegistered (NodeNameLookup, EventLogManager, …)
       │
       └─ for each registered node N:
              N.@Initialise method runs                ◀── ctx is non-null by this point
                                                          query interfaces are populated
```

**Key invariant:** by the time `@Initialise` fires on any node, `currentContext` has already fired on every `DataFlowContextListener`, and the `ServiceRegistryQuery` graph is fully populated. Boot-time consistency checks belong here.

## Sample code at each phase

```java
public class MyNode implements DataFlowContextListener {

    // ── CONSTRUCT phase ────────────────────────────────────────────
    private DataFlowContext ctx;
    private MyDependency dep;

    public MyNode() {
        // No-arg ctor. Don't touch fluxtion infrastructure here — it
        // isn't wired yet. Just initialise plain fields.
    }

    // ── INIT phase ─────────────────────────────────────────────────

    @Override
    public void currentContext(DataFlowContext c) {
        // Fires during nodeRegistered, BEFORE @Initialise.
        // ctx becomes available; safe to stash for later use.
        this.ctx = c;
    }

    @ServiceRegistered("my-sink")
    public void onSink(MyDependency d) {
        // Fires when registerService delivers a matching binding.
        // For external services, may fire at any time; for internal
        // (self-published) services, fires during init.
        this.dep = d;
    }

    @Initialise
    public void verifyWiring() {
        // Runs AFTER all currentContext / @ServiceRegistered callbacks
        // have fired across every node. The dependency graph is fully
        // populated — boot-time consistency checks belong here.
        ServiceRegistryQuery q = ctx.serviceRegistryQuery().orElseThrow();
        if (q.findNamedDependency(MyDependency.class, "my-sink").isEmpty()) {
            throw new IllegalStateException("my-sink not bound");
        }
    }

    // ── START phase ────────────────────────────────────────────────

    @Start
    public void warmCaches() {
        // Runs once after init completes. Use for work that depends on
        // ALL nodes being initialised — e.g., pre-warming a cache from
        // a sibling node's state.
    }

    // ── EVENT phase ────────────────────────────────────────────────

    @OnEventHandler
    public boolean onEvent(MyEvent e) {
        // Hot path. ctx, dep, and any @Initialise-set fields are stable.
        return true;
    }

    // ── TEARDOWN phase ─────────────────────────────────────────────

    @Stop
    public void drain() {
        // Called before tearDown; chance to flush state.
    }

    @TearDown
    public void releaseResources() {
        // Final cleanup.
    }
}
```

## Lookup APIs and when they're safe

| API | Returns | Safe to call from |
|-----|---------|--------------------|
| `dataFlow.getNodeById(id)` | The named node instance (throws if missing) | After `init()` |
| `dataFlow.getAuditorById(id)` | The named auditor (throws if missing) | After `init()` |
| `dataFlow.getServiceById(id, type)` | `Optional<T>` — node OR auditor, cast to type | After `init()` |
| `dataFlow.serviceRegistryQuery()` | `Optional<ServiceRegistryQuery>` | After `init()` |
| `ctx.serviceRegistryQuery()` (from inside a node) | `Optional<ServiceRegistryQuery>` | `@Initialise` onwards (NOT in `currentContext`) |
| `ctx.getParentDataFlow()` | The enclosing DataFlow | `@Initialise` onwards |

## Common gotchas

1. **Build-time instance ≠ runtime instance.** When using `c.addNode(new MyNode(), "x")` in a builder, the instance you pass in is used at codegen time only. Compiled SEPs instantiate their own copy via the no-arg constructor. To inspect runtime state from a test, use `dataFlow.getNodeById("x")` rather than the instance you passed in.

2. **`ctx` is null inside the constructor.** The no-arg ctor runs at CONSTRUCT phase; `currentContext` fires at INIT. Don't deref `ctx` until `@Initialise` (or later).

3. **`@Initialise` order is not user-defined.** Don't rely on `MyNode.init()` running before `OtherNode.init()`. If you need ordering, use `@Start` for the later step — by then init is fully complete across all nodes.

4. **`@ServiceRegistered` listeners can fire after `@Initialise`.** A `@ServiceRegistered MySink` callback fires when `registerService` delivers the binding, which may be during init OR later, when an external system pushes the service in. Don't assume the dependency is bound during `@Initialise` unless you control the registration path.

5. **`@ExportService` requires void-returning methods.** The codegen drops the export silently if any method on the interface returns a value. For typed query interfaces, expose via `getNodeById` / `getServiceById` instead, or wrap the read in a void-returning callback.

6. **Auditors live on a different lookup path in compiled SEPs.** Pre-1.0.2 compiled SEPs route auditors through `getAuditorById` (reflection on a public field), not `nodeNameLookup`. Use `dataFlow.getServiceById(id, type)` to abstract over both paths.

## When auditors are involved

Auditors (`Auditor` subtypes added via `EventProcessorConfig.addAuditor`) get one extra hook: `nodeRegistered(node, nodeName)` fires for every regular node visited. This is how `ServiceRegistryNode` scans `@ServiceRegistered` annotations and how `EventLogManager` records every dispatch. If you're writing an auditor, your scanning logic goes in `nodeRegistered`, not `@Initialise`, because by `@Initialise` time you'd be one phase too late.

## TL;DR

- Stash `ctx` in `currentContext` (CONSTRUCT-cheap, fires early).
- Do boot-time validation in `@Initialise` (graph fully populated).
- Do "everyone is initialised" work in `@Start`.
- Don't dereference `ctx` in the constructor.
- Don't assume the test-time node instance is the runtime instance.
