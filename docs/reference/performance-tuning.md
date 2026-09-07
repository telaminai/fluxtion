# Tuning for lowest latency

How to configure a Fluxtion processor for the lowest achievable event cost, what each choice is worth,
and the traps that silently cost 3–5x.

!!! note "This is the tuning guide; [Performance results](performance.md) is the benchmark"
    That page measures a realistic graph end to end with JMH, including application logic. This one is
    about **dispatch cost and configuration** — how to get the generated processor itself out of the
    way. The two are complementary and their headline numbers are not comparable: they measure
    different things on different graphs.

**Evidence.** Every figure here is measured — ~700 runs in
[round 58](https://github.com/telaminai/fluxtionauditlog-analyser/blob/main/docs/experience/runs/round-58/NOTES.md) and a verification round in
[round 59](https://github.com/telaminai/fluxtionauditlog-analyser/blob/main/docs/experience/runs/round-59/NOTES.md), medians over 200M-event runs with
output verified identical across arms before any timing was believed.

## Read this first: every figure names its shape

The same processor source has measured anywhere from **1.4 to 29 ns per event** depending on
configuration and deployment shape. While round 58 was written up, **four successive drafts carried a
wrong headline figure** — each a true measurement of a different shape, quoted without naming it.

So: **a figure without its shape is not a result.** Every number below carries one.

---

## At a glance

**A generated Fluxtion processor runs at 1.57 ns/event — about 637M events per second — matching
hand-written flat Java and hand-optimised C++.** The nodes can come from a vendor jar the generator
only saw as bytecode, with `getNodeById`, `lookupInstanceName`, re-entrancy, subscriptions and
buffering all live.

**Best measured**, and it has been reached in five independent harnesses — but see the caveat below:

| | ns/event | events/sec |
|---|---|---|
| **generated Fluxtion processor** (native + PGO + inlining directive) | **1.57** | **637M** |
| hand-rolled flat Java, same arithmetic | 1.55 | 645M |
| hand-optimised C++ `-O3 -march=native` | 1.57 | 636M |
| the same processor, misconfigured | 5.6 – 29 | 34M – 179M |

**Measured across five JVMs and two native builds**, from the reusable kit
(`tools/bench/latency-kit`) — 10-node graph, nodes from a separately compiled jar, 200M events,
output verified identical, full configuration applied:

| runtime | generated | hand-rolled | ratio |
|---|---|---|---|
| Temurin 17.0.14 | 5.60 | 3.57 | 1.6× |
| Temurin 21.0.5 | 5.58 | 3.69 | 1.5× |
| Corretto 21.0.9 | 5.58 | 3.82 | 1.5× |
| OpenJDK 24 | 5.58 | 3.77 | 1.5× |
| **GraalVM 25.0.4 (Graal JIT)** | **5.47** | 2.18 | 2.5× |
| GraalVM 25.0.4 native-image, no PGO | 6.54 | 2.54 | 2.6× |
| **GraalVM 25.0.4 native-image + PGO** | **1.62** | **1.57** | **1.03×** |

The last row is the point: **with the full configuration, generated dispatch is within 3% of
hand-rolled flat Java.** Every JIT lands at 5.5–5.6 regardless of vendor.

!!! warning "The native + PGO figure is per-build, and the build is a lottery"
    Rebuilding the same source, with the same flags, gives **either ~1.6 ns or ~5.5 ns** and nothing in
    between. One session landed 11 of 13; the next landed 3, then missed 15 in a row on identical
    inputs. Feeding a build's own profile back in does not reproduce it. Nothing measurable
    distinguishes the two — same profile counters, same image size, same builder memory. Root cause is
    [oracle/graal#14387](https://github.com/oracle/graal/issues/14387).

    **So measure every build and keep the one that lands** — a binary reproduces its own mode exactly,
    for ever. `tools/bench/latency-kit/run.sh` exists for that. The JIT numbers need none of this: they
    are deterministic on all five JVMs.

!!! warning "Native-image is only faster if you configure it — otherwise it is SLOWER than a JIT"
    In the sweep above, **native without PGO (6.54 ns) is slower than every JIT measured**, including
    plain Temurin. Native-image beats a JIT only when an accurate PGO profile **and** the inlining
    directive **and** the configuration below are all present. If you cannot collect a representative
    profile, **a JIT is the safer choice** — Graal JIT at 5.47 ns is the best unconditional number on
    this graph, and no JIT vendor differs by more than 2%.

**Read those two tables together.** 1.6 ns needs native-image **and** an accurate profile **and** the
inlining directive **and** the configuration below — and then it still has to land. Miss any one of
them and you are at 5.5–6.5 *every* time, still correct, just 3–4× slower, with no diagnostic.
**Build it and measure it.**

Every step on this page is worth 2× or more, and getting one wrong is silent — the program stays
correct and simply runs slower.

### Checklist for maximal performance

Work down it. **Each item is silent when omitted** — the program stays correct and simply runs slower —
so the only way to know you have them all is to check.

**Build**

- [ ] `config.performanceProfile(LOWEST_LATENCY)` — one line, and it sets the three below for you
- [ ] &nbsp;&nbsp;↳ framework auditors dropped *(no `Clock` reading the system clock per event)*
- [ ] &nbsp;&nbsp;↳ `setSupportDirtyFiltering(false)` *(no dirty flags, no guards)*
- [ ] &nbsp;&nbsp;↳ `setSupportNodeNameLookup(false)` *(no node registration — the single largest cost)*
- [ ] **void triggers on every node** — `@OnTrigger(failBuildIfMissingBooleanReturn = false)` and the
      same on `@OnEventHandler`. **The profile cannot set this for you**; it lives on your classes.
- [ ] If you need the audit log instead: `performanceProfile(AUDITED)` +
      `addAuditedEventLog(LogLevel.INFO)` — keeps the log, drops the 208 bytes/event

**Runtime shape**

- [ ] processor constructed **inside** the method that runs the event loop, and never escapes it
- [ ] **nothing between the constructor and the loop** — no timing call, no logging, no registration
- [ ] if you kept the `Clock` auditor: supply a `ClockStrategy` **before** entering that method

**Native image** *(skip all of this if you deploy on a JIT — none of it applies)*

- [ ] `--pgo=<profile>` from a run that exercises **every path you deploy**
- [ ] `-H:PriorityForceInline=<YourProcessor>.*` — emitted for you in
      `META-INF/native-image/…/native-image.properties` when `generateReachabilityMetadata` is on
- [ ] whole-class wildcard, **not** a curated method list — naming methods individually does not work
- [ ] never reuse a profile across image kinds, from a different entry point, **or across a rebuild of
      the instrumented image** — a stale profile measured 8.0 ns, worse than no profile at all, and the
      build reports `PGO: user-provided` without a warning

**Verify — do not assume**

- [ ] run `tools/bench/latency-kit/run.sh` against your own graph
- [ ] compare arms with `tools/bench/dispatch-bench.py`, which refuses to report until the arms
      produce identical output
- [ ] **if a number surprises you, check this list before concluding anything about the compiler** —
      four times in round 59 a missing setting or a harness defect looked exactly like one

### The whole configuration

```java
// ---- build ------------------------------------------------------------------
@OnTrigger(failBuildIfMissingBooleanReturn = false)         // void trigger: no dirty flag, no guard
@OnEventHandler(failBuildIfMissingBooleanReturn = false)

config.performanceProfile(LOWEST_LATENCY);                  // auditors off, no dirty flags,
                                                            // no node registration
// or, if the audit log is the point:
// config.performanceProfile(AUDITED).addAuditedEventLog(LogLevel.INFO);

// ---- runtime ----------------------------------------------------------------
processor.onEvent(ClockStrategy.registerClockEvent(() -> myStreamTime));   // else it reads the
                                                                           // system clock per event
// ---- the event loop: construct the processor INSIDE the method that loops ----
static void run(long n) {
    MyProcessor p = new MyProcessor();      // must not escape this method
    MyEvent e = new MyEvent();
    for (long i = 0; i < n; i++) p.onEvent(e.set(...));
}
```

```bash
native-image --pgo=app.iprof \
  '-H:PriorityForceInline=com.your.pkg.MyProcessor.*' \
  -cp ... MyApp
```

**What each step is worth**, measured, on a real generated processor:

| step | cost of omitting it | where |
|---|---|---|
| force the dispatch chain to inline | **3.5×** (1.57 → 5.56) | native only |
| `setSupportNodeNameLookup(false)` | **3.5×** (1.62 → 5.61) | native only |
| supply a `ClockStrategy` (or drop the auditors) | **5.8×** (5.03 → 29.08) | both |
| accurate PGO profile | **4.0×** (1.62 → 6.54) | native only |
| non-escaping processor | **2–3×** | native only |
| void triggers + no dirty filtering | large; changes semantics | both |
| `printEventToString(false)` when auditing | 208 bytes/event, rules out epsilon GC | both |

**None of these is a micro-optimisation.** The first four are each worth more than everything else on
this page combined. Only the inlining directive is emitted automatically by the generator
(§*ship the directive*); **the rest you must ask for**, and omitting any one is silent.

---

## The baseline configuration, in one place

**This is the configuration to start from for best performance.** Five items. Two of them change
behaviour and are your decision; three are free.

```java
// ---- 1. SEMANTIC CHOICES — these change what the graph does ------------------------------

// 1a  NO DIRTY FLAGS: every node fires on every event, unconditionally
@OnTrigger(failBuildIfMissingBooleanReturn = false)          // void trigger, no dirty flag, no guard
@OnEventHandler(failBuildIfMissingBooleanReturn = false)
config.setSupportDirtyFiltering(false);                      // and no dirty-flag machinery at all

// 1b  IF YOU AUDIT: turn off event stringification and thread name.
//     This is BASELINE, not an optimisation to consider later: it is the difference between
//     zero allocation and 208 bytes/event, and it removes about a third of the audit cost.
config.addEventAudit(LogLevel.INFO, false, false);           // printEventToString, printThreadName

// ---- 2. FREE — no capability lost ---------------------------------------------------------

// 2a  SUPPLY A CLOCK: otherwise the Clock auditor reads the system clock on EVERY event
processor.onEvent(ClockStrategy.registerClockEvent(() -> myStreamTime));

// 2b  DEPLOYMENT SHAPE: construct the processor inside the method that runs the event loop,
//     and time from the caller. Nothing between the constructor and the loop.       (§below)

// 2c  BUILD WITH AN ACCURATE PGO PROFILE, collected from what you actually deploy.  (§below)

// 2d  FORCE THE DISPATCH CHAIN TO INLINE — without this you lose 3.5x:            (§below)
//     native-image -H:PriorityForceInline='com.your.pkg.YourProcessor.*'

// 2e  NO NODE REGISTRATION — the single largest cost, and you must ask for it:
//     otherwise every node is published into the auditor's HashMaps and none can be
//     scalar-replaced. Lookup still works: the generator emits getInstanceById and
//     lookupInstanceName as CODE, so nothing is lost.                                 (§6)
config.setSupportNodeNameLookup(false);

// 2f  NO AUDITORS, if you do not need the audit log: the Clock auditor alone reads the
//     system clock on every event, and the three framework auditors together consume the
//     escape-analysis budget the node graph needs.
//     config.getAuditorMap().keySet().removeAll(config.getFrameworkAuditorNames());

// ---- NOT needed — all measured free. Set only if you don't want the capability -------------
//   config.setSupportReentrancy(false);        // wrapper free, guard free
//   config.setSupportBufferAndTrigger(false);  // free
//   config.setSupportSubscriptions(false);     // free
```

**Measured end state:** ~1.6 ns/event unaudited, ~550 ns fully traced-and-audited, both
zero-allocation, both from generated code.

### 1 · No dirty flags

`setSupportDirtyFiltering(false)` plus void triggers. **Removes the guards.** A void trigger returns
no boolean, so there is no dirty flag to store, no guard to test, and no dirty-flag maps on the
processor.

**What you give up: conditional propagation.** Every node fires on every event. If your graph relies on
a node declining to propagate, this changes results, not just speed. Take it when the graph is a
pipeline that recomputes everything anyway.

### 2 · No auditors doing real work

Every generated processor carries the `Clock`, `NodeNameLookup` and `ServiceRegistry` auditors, and
calls them on every event. **Five of those six calls are free** — round 58 measured inherited no-op
default methods as indistinguishable from no call at all on every runtime, native included, so **do
not try to hand-optimise them away.**

The sixth is not free. `Clock.init()` sets `wallClock = System::currentTimeMillis`, and
`Clock.eventReceived` calls it **on every event**:

| | ns/event | events/sec |
|---|---|---|
| default clock (reads the system clock per event) | 29.08 | 34M |
| supplied `ClockStrategy` | **5.03** | 199M |

**The clock is 83% of the default cost on a JIT** — measured on a real generated processor
(round 59), and round 58 measured the same on all seven runtimes it tested (19.01 → 7.71 on Graal JIT).
**This is the lever you lose by doing nothing**, and it was undocumented until now.

Supply a stream time whenever your events carry their own. If they do not, you are performing an
ambient clock read inside the event path, which is also what makes a run non-reproducible — the
performance argument and the determinism argument coincide.

### 3 · Optimised re-entrancy

`setSupportReentrancy(false)`. Worth **−26% on native**, −7% on a JIT.

`processEvent` runs on every event and, with support on, tests a flag, may queue a re-entrant event,
and drains a callback queue. When no node in the graph can raise a re-entrant event that queue is
provably always empty and all of it is dead code. With the flag off the generated processor also
**dispatches the typed entry directly** — `onEvent(MarketTick)` calls `handleEvent(event)` instead of
widening to `Object` and recovering the type with an `instanceof` chain.

**A guard is retained and it throws.** Build-time detection cannot be complete — a node can reach the
dispatcher through a service or reflectively — so a re-entrant event fails loudly rather than
vanishing.

### 4 · No buffering  ·  5 · No subscriptions

`setSupportBufferAndTrigger(false)` removes the buffering branch.
`setSupportSubscriptions(false)` stops the constructor publishing the processor to the subscription
manager — which matters for more than one reason, see below.

---

### 6 · Node-name lookup generated as code — the largest single cost, and it costs you nothing

**Measured: 5.55 → 1.57 ns on a real generated processor, with lookup still working.**

`initialiseAuditor` registers **every node** with each auditor, and `NodeNameAuditor` stores them in
two `HashMap`s. Every node object is then published into a live heap structure, so none can be
scalar-replaced and the whole graph materialises as real allocations.

**It is invisible unless you build with PGO.** Without a profile the processor measures ~5.5 ns either
way — that configuration is already slow for other reasons — so a benchmark without PGO will tell you
this is free. It is not.

**You give up nothing.** The generator knows every name and field at build time, so it emits the
mapping as a switch that reads a field on demand and stores no reference. `getNodeById` keeps working.
The switch never runs on the event path, so its cost is irrelevant — what matters is that it holds
nothing.

**The one case still to pay for it:** an auditor that consumes `nodeRegistered` — an audit log that
names its nodes — still receives every node and still publishes them. An audited processor does not
reach 1.57 by this route, and that is a trade worth making.

## Deployment shape — worth more than every flag combined

**Construct the processor inside the method that drives the event loop.**

| processor reached via | native |
|---|---|
| a local that never escapes the method driving the loop | **1.41–1.53** |
| a `static` / `static final` field | 3.10 |
| an instance field of a statically-held object | 4.82 |

If the processor escapes, the compiler cannot dissolve the node objects. It escapes by being stored in
a static, a registry, a factory, a thread pool, or a getter that anything calls — **and the escape
happens at construction, so reading the field into a local before the loop does not recover it.**

On a JIT every shape measures about the same; this is an AOT consideration.

---

## Which runtime you deploy on decides which advice applies

**These are two different optimisation problems.** Measured on identical classes, same machine:

| | Graal JIT | native-image, no PGO | native-image + accurate PGO |
|---|---|---|---|
| processor carrying no framework fields | 4.64 | **1.86** | **1.56** |
| processor carrying all seven | 4.74 | **6.20** | **1.58** |
| hand-rolled flat equivalent | 2.09 | 2.45 | 1.55 |

**On a JIT, none of the structural tuning below matters.** Every configuration lands at ~4.6 ns,
because the JIT never dissolves the processor's object structure — so nothing you remove was being
optimised away in the first place. Its floor for a ten-node graph is ~4.6 and no flag reaches past it.

**On native-image the structure is everything**, because AOT *does* dissolve it — reaching 1.86 ns,
**faster than hand-rolled flat code at 2.45.** That is also what makes it fragile: see the cliff below.

Note the JIT beats AOT on the flat hand-rolled arm (2.09 vs 2.45). This is not "AOT is faster". It is
specifically that only AOT removes the graph's object structure.

## The escape-analysis cliff — native without PGO only

Without a profile, a native image's escape analysis has a **finite budget**, and past it the processor
stops being dissolved. It is a cliff, not a gradient — nothing lands between 1.9 and 6.2 ns:

| processor carries | ns |
|---|---|
| nothing extra | 1.87 |
| any ONE framework field (each tested separately) | 1.86–1.88 |
| `callbackDispatcher` + `clock` + `nodeNameLookup` | 1.87 |
| **`callbackDispatcher` + `nodeNameLookup` + `subscriptionManager`** | **6.22** |
| all seven | 6.20 |
| all seven **minus** `subscriptionManager` | 6.16 |

### It is cumulative — no single field is the culprit, in either direction

Tested exhaustively, both ways round, all arms in one binary:

| | ns |
|---|---|
| **adding** any ONE of the seven to a bare processor | 1.86–1.88 — every one free |
| **removing** any ONE of the seven from the full set | 6.12–6.29 — every one useless |
| the full set | 6.20 |
| none of them | **1.87** |

So there is no expensive field and no cheap win. Adding one costs nothing; removing one saves nothing.
What decides the outcome is the **total size of the allocation graph** the compiler must dissolve:

| framework object | objects it allocates |
|---|---|
| `ServiceRegistryNode` | 6 — four `HashMap`s plus a lock |
| `SubscriptionManagerNode` | 5 — an `ArrayList` and three `HashMap`s |
| `NodeNameAuditor` | 3 — two `HashMap`s |
| `CallbackDispatcherImpl` | 2 — an `ArrayDeque` |
| `Clock`, `ExportFunctionAuditEvent` | 1 each |

Three fields can be fine or fatal depending which three: `callbackDispatcher + clock + nodeNameLookup`
stays at 1.87, while `callbackDispatcher + nodeNameLookup + subscriptionManager` falls to 6.22.

**The practical consequence: you cannot shave your way back. You have to get under the budget.**
Elision has to be measured, not counted — and on an unprofiled native image it is close to
all-or-nothing.

### Why a build lands, and why the next one may not

This is the mechanism behind the repeatability caveat at the top of the page. It is shorter than you
would like, because the honest version is short.

**The mode is decided by the build.** Not by the source, and not by the profile. One build measured
1.60; its profile was kept and fed back to two more builds — same instrumented image, same classes,
same flags, nothing else changed — and they measured 6.66 and 5.66. Two builds from one profile also
differ in SHA while measuring the same, so `native-image` is not byte-reproducible, and the
nondeterminism reaches the decision that decides whether the processor dissolves.

**Once built, it is fixed.** A given binary reproduces its mode exactly, run after run. Padding the
environment to shift the stack and varying the heap size to shift the heap change nothing. So the
build is the lottery and the binary is the ticket: **keep the one that landed.**

!!! danger "Two things that look like this, and are not"
    **Nothing about your graph.** Round 59 published, and this page carried for a day, an explanation
    that the generated processor's ten node objects sit at a size threshold while hand-rolled is one
    object and therefore always lands. **Withdrawn** — in a bad build the hand-rolled arm does not land
    either, and it has nothing to dissolve that the processor could have spoiled.

    **Nothing about your configuration.** Round 59 also reported that dropping the last auditor changed
    the landing rate. **Withdrawn** — the same unmodified configuration measured 1.60/1.67/1.68 one
    hour and 5.58/5.70/5.71/5.73 the next. Neither sample was measuring the auditor.

    The general lesson under both: this outcome is bimodal, so a handful of builds cannot tell you the
    sign of a change. Round 59 concluded that it could, twice, and was wrong both times.

**How often does it land?** Not a stable number. 11 of 13 in one session; 3 of 18 in the next, with 15
consecutive misses on inputs that had just produced three hits. The outcomes are not even independent —
they arrive in runs — and nothing measurable separates a good build from a bad one: the profiles are
identical on every hot counter, the images are the same size, the builder gets the same memory.

**So verify the build you ship.** That is not a caution to add to the method; on this platform it *is*
the method. Build, measure with `tools/bench/latency-kit/run.sh`, keep the binary that lands, rebuild
when it doesn't.

### The knobs that do not work

`native-image --expert-options-all` offers a set of options whose names promise exactly what is wanted.
Each was measured on a full fresh cycle, at 4–6× its default, against a floor of 5.6:

| flag | result |
|---|---|
| `-H:IPEAMaxForce` · `-H:IPEAVirtualEscapeBoostSingle` | no effect |
| `-H:TuneInlinerExploration` | no effect, and +530 KB of image |
| `-H:BaseTargetSpending` · `-H:InliningCoefficient` family | no effect |
| `-H:MaximumInliningSize` · `-H:SmallCompiledLowLevelGraphSize` | no effect |
| `-H:EscapeAnalysisIterations` · `-H:EscapeAnalysisLoopCutoff` | no effect |
| `-H:PriorityForceInline` widened to the nodes, the framework, the loop's own class | no effect |
| **`-H:+InlineEverything`** | **no effect** |
| `-H:NumberOfThreads=1` or `=4` *(hoping for a deterministic build)* | no effect, and still not byte-reproducible |

`InlineEverything` failing to move it is the informative row: whatever bails is not reachable by
turning inlining up.

**`-H:PriorityForceInline=<YourProcessor>.*` is the only lever that works** — it is worth 3.5× and it
is not optional. It is also not sufficient, which is what the rest of this section is about.

## PGO — an accurate profile removes the cliff; a bad one is worse than none

**For an AOT Fluxtion processor a bad profile is worse than no profile.** This is not a caution, it is
a measurement:

| arm | exe, no PGO | exe, PGO | shared lib, no PGO | shared lib, PGO |
|---|---|---|---|---|
| non-escaping processor | 1.53 | 1.64 | **1.41** | **6.28** |
| processor in a `static final` field | 3.10 | 2.51 | 3.10 | 6.25 |

- **An accurate profile makes the cliff disappear.** With profiles collected from every arm and
  merged, the processor carrying **all seven** framework fields runs at **1.58 ns** — the same as one
  carrying none (1.56), and the same as hand-rolled (1.55). The structural sensitivity above is a
  property of *unprofiled* AOT, not of AOT.
- **A non-escaping processor reaches ~1.4–1.9 ns with no profile at all**, so PGO is not *required* —
  but it is what makes the result robust to structure rather than dependent on it.
- **A mismatched or INCOMPLETE profile is worse than no profile.** This is the single biggest trap
  here, and it was measured three separate times: an executable's profile applied to a shared library
  (1.41 → 6.28), and twice a path that was *in the image but not in the profile* (1.83 → 6.19 and
  1.59 → 7.20). In every case the result was **worse than building with no profile at all**, because
  GraalVM reads absent profile data as coldness and compiles that path for size — so the escape
  analysis that reaches 1.57 never runs.

  > **Every code path you deploy must be exercised during profile collection.**

  Collect from a run that exercises what you actually ship, and never carry a profile across image
  kinds. If one entry point is slow and the others are fast, suspect the profile before the code.
- Round 58 saw PGO make its fastest shape slightly *worse* (1.53 → 1.64) with a narrower profile.
  Both observations hold: the profile's accuracy is the variable, not PGO itself.

---

## What you do NOT need to do

All proven, so effort does not go the wrong way.

- **Interface separation between components is free** — ~0.03 ns per call site, and **zero** indirect
  branches with a single implementor, verified by counting `blr` in the disassembly. Structure your
  components for clarity. (With three implementations reachable in one image it costs +115%: AOT
  devirtualises what is provably monomorphic and nothing more.)
- **Event-type dispatch is not a scaling risk** — 2 to 16 event types cost +0.26 ns total.
- **Empty auditor calls are already free.** Do not hand-optimise them.
- **Flattening the graph into one method is not needed** — worth −1.8%, and it cannot be applied to
  components you do not own.

---

## Force the dispatch chain to inline — the single most important native-image setting

Without it you lose **3.5×**, and the loss is silent.

```
native-image -H:PriorityForceInline='com.your.pkg.YourProcessor.*'  ...
```

**Measured on a single-processor, single-event-loop application — the shape a real deployment has:**

| build | ns/event |
|---|---|
| **accurate PGO + `PriorityForceInline`** | **1.57** |
| accurate PGO only | 5.56 |
| `PriorityForceInline` only | 6.53 |
| neither | 6.79 |

**Both are required; neither alone is close.**

### Why

GraalVM's priority inliner decides, from its cost model, not to inline
`onEvent → processEvent → onEventInternal → handleEvent` into your loop. Any link left out of line
receives the processor as an argument, so **the processor escapes** and its node objects stop being
scalar-replaced. That is the entire 3.5×. Forcing the inline removes the decision.

**It is genuinely unreliable without the flag** — the same code measured 1.57 in a binary that happened
to contain a second hot loop over the same processor, and 5.5 in one that did not. Do not rely on the
inliner choosing correctly.

### How repeatable it is

Every shape below rebuilt with its own instrumented image and its own freshly collected profile,
output verified identical. **These are the builds that landed** — see *Why a build lands* above for
what that qualification is doing here:

| program shape | without flag | with flag |
|---|---|---|
| single processor, single event loop | 5.55 | **1.57** |
| one loop + an unrelated hot loop | 5.58 | **1.57** |
| one loop, nothing else | 5.55 | **1.58** |
| loop in its own class | 5.60 | **1.57** |
| **the reusable kit, `tools/bench/latency-kit`** | 6.54 | **1.62** |

**The flag is necessary and it is not sufficient.** Across two sessions of repeated
profile-and-build cycles on unchanged inputs: 11 of 13, then 3 of 18. Nothing lands between ~1.6 and
~5.5, and a build's own profile fed back in does not reproduce it. The residual is
[oracle/graal#14387](https://github.com/oracle/graal/issues/14387), where two images built from
identical classes, with byte-identical hot methods, measured 1.43 and 5.45.

**So measure the build you ship, and keep the binary that landed** — its mode is fixed once built.
The table above is what a landing build looks like, not a guarantee that a build lands.

!!! warning "A missing configuration setting looks exactly like an unstable compiler"
    The kit measured **6.22** for a long time with the directive correctly applied, and that was
    published here as evidence the directive was unreliable. It was not: the kit's generator was
    missing two settings — the framework auditors were still registered, and
    `setSupportNodeNameLookup(false)` was absent, so every node was still published into the auditor's
    maps. Adding them took it 6.22 → 5.61 → **1.62**.

    **Check the configuration before concluding anything about the compiler.** Every item in *The
    baseline configuration* is load-bearing, and omitting one is silent.
### Use the whole-class wildcard. Naming individual methods does NOT work.

The pattern is GraalVM's `MethodFilter` syntax, so it is tempting to force only the event-path methods.
**Measured, that fails** — and it fails silently, at full speed-loss:

| pattern | ns/event | image size |
|---|---|---|
| `YourProcessor.*` | **1.57** | 9706 KB |
| `YourProcessor.onEvent,…processEvent,…onEventInternal,…handleEvent` | 5.55 | 9706 KB |
| `YourProcessor.handleEvent` | 5.58 | 9706 KB |
| no flag at all | 5.56 | 9706 KB |

**The image is the same size either way**, so there is nothing to gain by narrowing it and a 3.5×
regression to lose. Use:

```
-H:PriorityForceInline='com.your.pkg.YourProcessor.*'
```

Adding the node classes as well (`,com.your.nodes.*.*`) is harmless but gains nothing — 1.56 against
1.55. **The processor class alone, with the wildcard, is the setting.** Quote it in a shell or `*` will
glob.

### Better: ship the directive with the processor, so nobody has to know

`native-image` reads `META-INF/native-image/**/native-image.properties` from the classpath. A generated
processor can carry its own directive:

```properties
# META-INF/native-image/com.telamin.fluxtion/generated-processor/native-image.properties
Args = -H:PriorityForceInline=com.your.pkg.YourProcessor.*
```

**Verified end to end**: with only that resource on the classpath and **no flag on the command line**,
the same application builds at **1.58 ns** instead of 5.55.

The generator knows its own fully-qualified class name at build time, so it can emit this file itself —
the same partial-evaluation move the rest of this page describes, applied to the compiler's own
configuration. That is open work (M50), not something the generator does today.

## Honest numbers

Measured on macOS/aarch64, Oracle GraalVM 25.0.4, output verified identical on every arm.

| shape | JIT | native (no PGO) |
|---|---|---|
| generated, default clock, all support on | 29.08 | 16.07 |
| generated + `ClockStrategy` | 5.03 | 8.92 |
| **generated, full baseline config above** | **5.16** | **7.03** |
| hand-rolled flat equivalent | 2.05 | 2.46 |

**A note on provenance.** Round 58's published figures of 1.41–1.55 ns were measured on
`BaseProcessor`, a hand-written stand-in, not on generated code. **That gap is closed**: with the
configuration above the generator itself produces **1.57 ns** — on a build that lands — matching that
control (1.58) and hand-rolled flat code (1.55). See *How repeatable it is*: in a build that does not
land, the hand-rolled control does not land either.

Quote the shape, not the best number in the table.

---

## Reproducing this

Nothing here should be taken on trust. `tools/bench/dispatch-bench.py` refuses to report unless both
arms ran in one binary, the runtime kind is single and recorded, every arm emits identical check
values before any timing is believed, and no arm falls below an elimination floor — a probe measuring
0.0000 ns is a deleted loop, not a result, and that happened twice in round 58.

**Example project: TO BE NAMED.** This page should point at a runnable repository containing the graph,
the two arms and the build scripts, so a reader reproduces rather than believes. That repository does
not exist yet and its home is an owner decision — round 58's own workspace did not survive, which is
precisely the argument for creating it.
