# Reference: Performance profiles

A profile is a named bundle of settings. This page states the whole bundle, what each setting costs, and
what you give up — so the trade is visible before you pick one.

Every figure is from the [performance benchmark](performance.md): a four-node market-data graph, one
thread, Apple M4, ns/event.

## Setting a profile in your build

A profile is a **generation-time** decision: it changes the source the compiler emits, so it is set where
you define the graph, not on the running processor. If you arrived from the
[AOT quickstart](../home/quickstart-aot.md), that quickstart's `Fluxtion.compileAot(node1, node2)` form
takes node instances and gives you nowhere to put one. Use the **config-builder** overload instead — the
lambda receives the `EventProcessorConfig` every snippet on this page calls `config`:

```java
var dataFlow = Fluxtion.compileAot(c -> {
    c.performanceProfile(PerformanceProfile.LOWEST_LATENCY);   // the profile
    c.addNode(new MyNode(...), "myNode");                      // then the graph, as usual
});
```

For a real build you normally want to name the output rather than have it derived from the enclosing
class and method:

```java
Fluxtion.compileAot(c -> {
    c.performanceProfile(PerformanceProfile.LOW_LATENCY_AUDIT)
     .addLowLatencyEventLog(LogLevel.INFO, AuditRecordFormat.BINARY);
    c.addNode(new MyNode(...), "myNode");
}, "com.example.trading", "PricingProcessor");
```

**Order matters when you override.** The profile sets a bundle; a per-setting call after it wins. So
`c.performanceProfile(LOWEST_LATENCY); c.setSupportReentrancy(true);` keeps everything the profile did
except re-entrancy. Reverse those two lines and the profile overwrites your override.

!!! warning "Not every profile suits every graph, and the build says so"
    `LOWEST_LATENCY` turns off node-name lookup, which the functional DSL requires. Applying it to a DSL
    graph is **refused at build time**, naming the flag (`setSupportNodeNameLookup(false)`) and a node it
    affects — rather than failing later as a `NullPointerException` inside the runtime. If you get that
    refusal, either build the graph imperatively or use `LOW_LATENCY_AUDIT`, which keeps the lookup.

Because the profile is baked into the emitted source, **a committed generated processor carries the
profile it was generated under**. Changing profile means regenerating; there is no runtime switch.

## The four profiles

```java
config.performanceProfile(EventProcessorConfig.PerformanceProfile.LOWEST_LATENCY);
```

| capability | `DEFAULT` | `AUDITED` | `LOW_LATENCY_AUDIT` | `LOWEST_LATENCY` |
|---|:---:|:---:|:---:|:---:|
| **Audit log** — *available*, still needs enabling | ✋ | ✋ | ✋ | ❌ |
| **Binary record** (`AuditRecordFormat.BINARY`) | ❌ | ❌ | **✋** | — |
| **Per-node method tracing** | ✅ | ✅ | ❌ | ❌ |
| Event `toString()` in each record | ✅ | ❌ | ❌ | — |
| Thread name in each record | ✅ | ❌ | ❌ | — |
| **`Clock`** — a system clock read per event | ✅ | ✅ | ✅ | ❌ |
| Which clock the AUDIT RECORD reads | ✋ | ✋ | ✋ | — |
| **Node registration** — supplies each node its `EventLogger` | ✅ | ✅ | ✅ | ❌ |
| Runtime node-name map (`getNodeById`) | ✅ | ✅ | ✅ | ❌ |
| **Dirty filtering** — conditional propagation | ✅ | ✅ | ❌ | ❌ |
| Buffer-and-trigger | ✅ | ✅ | ❌ | ❌ |
| Subscriptions | ✅ | ✅ | ❌ | ❌ |
| **Re-entrancy** | ✅ | ✅ | ✋ | ✋ |
| Void triggers | ✋ | ✋ | ✋ | ✋ |

✅ on · ❌ off · ✋ your call, never the profile's · — not applicable

!!! danger "No profile turns the audit log on for you — including `DEFAULT` and `AUDITED`"
    The audit row is ✋, not ✅, and the distinction is easy to get wrong. A profile decides whether
    auditing is *possible*; you still have to ask for it:

    ```java
    config.performanceProfile(AUDITED).addAuditedEventLog(LogLevel.INFO);
    config.performanceProfile(LOW_LATENCY_AUDIT).addLowLatencyEventLog(LogLevel.INFO, BINARY);
    ```

    Generate under `DEFAULT` without that second call and the processor contains **no `EventLogManager`
    at all** — asking for one throws. If you are wondering why your audit log is empty, this is the first
    thing to check.

## What each profile costs

Same graph, same work, same result, both toolchains:

| configuration | audit | JIT ns | native ns |
|---|:---:|---:|---:|
| hand-written Java, no framework | no | 6.4 | 4.3 |
| `LOWEST_LATENCY` | no | 9.4 | **4.5** |
| no configuration at all | no | 15.1 | 14.7 |
| `LOW_LATENCY_AUDIT` + `BINARY` | yes | 20.4 | **18.2** |
| `LOW_LATENCY_AUDIT` + `TEXT` | yes | 42.3 | 50.6 |
| `AUDITED` + tracing | yes | **112.3** | 200.8 |

**Configuring nothing costs 3.3× the tuned configuration** on native. Nothing about the default is
wrong; all of it is optional, and none of it announces itself. Per event it is a clock read, three
dirty-flag stores, three guard checks and three resets on this four-node graph:

```java
// no configuration                            // LOWEST_LATENCY
clock.eventReceived(typedEvent);               // (empty method)
isDirty_mid = mid.newPriceLadder(arg0);        mid.newPriceLadder(arg0);
if (guardCheck_skew())   isDirty_skew   = …;   skew.calculateSkewedLadder();
if (guardCheck_levels()) isDirty_levels = …;   levels.calculateLevelsForLadder();
if (guardCheck_publish())                 …;   publisher.publishPriceLadder();
clock.processingComplete();                    // (empty method)
isDirty_… = false;  ×3                         // nothing to reset
```

## Choosing a toolchain: it depends on the profile

This is the least obvious result on this page.

- **Lean path → native AOT, decisively.** 4.5 ns against 9.4, with a spread across independent builds of
  0.1 ns. Native is also *closer to hand-written Java* than the JIT manages — 4% against 47%.
- **Text-heavy audit path → JIT, decisively.** At `AUDITED` the JIT wins by 1.8× (112.3 against 200.8),
  and on a text record by 1.2×. Closed-world compilation cannot speculate its way through string
  formatting the way HotSpot does.
- **Binary audit → native.** 18.2 against 20.4.

So the toolchain follows the profile. A native image is not uniformly faster, and choosing it for a
tracing-heavy configuration makes things worse.

## The settings that matter most, individually

### 1 · Code shape beats every flag — construct the processor inside the hot method

Worth **4.3× under native AOT** and nothing at all on JIT. If the processor escapes the method that
drives it, escape analysis cannot scalar-replace the node graph and every field access becomes a real
load. Nothing warns you.

### 2 · `-H:-SpawnIsolates` on the native build

Worth **~24 ns/event** on an audited path. It is a build flag, not a config setting, and it was the
single largest win found in a round of work that also rewrote three data structures.

### 3 · Node-name lookup

`setSupportNodeNameLookup(false)` — the largest single config cost. It is what `LOWEST_LATENCY` turns off
that the audit profiles cannot: node registration is how each node receives its `EventLogger`, so
**turning it off turns the audit log off**. A profile that did both once benchmarked extremely well by
recording nothing.

### 4 · The clock

Every profile except `LOWEST_LATENCY` reads a system clock per event, and an audited record reads one
again for `endTime`.

**The graph's clock is not a profile's to change.** It is a process-wide singleton the generator injects
into every processor, and time-based nodes read it — `FixedRateTrigger.atMillis`, tumbling and sliding
windows. Swapping its strategy to save a clock read per audited event would also change what every window
believes the time is.

**So the audit record can have its own clock instead**, chosen at build time and leaving the graph's
alone:

```java
config.addLowLatencyEventLog(LogLevel.INFO, AuditRecordFormat.BINARY,
                             EventLogManager.AuditClock.FAST_PROJECTED);
```

`SHARED` is the default — one clock in the system, so an audit timestamp and a window's idea of now
cannot disagree. `FAST_PROJECTED` gives the record a private
`ClockStrategy.fastEpochMillisClock()`, worth ~4.9 ns per audited event.

!!! warning "What FAST_PROJECTED costs, and why it is not the default"
    A projected clock anchors once and never sees a later NTP or manual wall-clock correction. Every
    `logTime` written after a correction is on the old timeline and the drift accumulates for the life
    of the process — **the graph's clock stays right while the log goes wrong**, which is the wrong way
    round for a record whose job is saying when things happened. Choose it when nothing correlates these
    timestamps with anything outside the JVM.

**The default is `System::currentTimeMillis`** — epoch milliseconds, read fresh every time. Two cheaper
strategies exist and both are **opt-in**, because both trade away something the default promises:

| clock source | cost/call | resolution | tracks wall-clock corrections? |
|---|---:|---|---|
| **`System::currentTimeMillis` — the default** | 12.9 ns | 1 ms | **yes** |
| `ClockStrategy.fastEpochMillisClock()` | 8.0 ns | 1 ms | no |
| `ClockStrategy.nanoEpochClock()` | 8.0 ns | 1 ns | no |

The two fast strategies sample the wall clock **once**, at construction, and advance from
`System.nanoTime()` thereafter. That makes them monotonic — they will not step backwards over an NTP
correction, which `currentTimeMillis` can — but it also means they never step *forwards* over one. A
correction from NTP, an operator or a VM resume is invisible to them, and a long-lived process keeps
stamping a pre-correction timeline with drift that is never reconciled.

That matters because the runtime is itself an absolute-time consumer: `Clock.eventReceived` stores the
reading and every audit record emits it as `logTime`. Good for durations, wrong for timestamps anyone
correlates with something outside the JVM — so the accurate clock is the default and the fast ones are
chosen deliberately.

```java
// cheaper, same unit, will not track a wall-clock correction.
// Worth pairing with LOW_LATENCY_AUDIT, which reads the clock on every event.
processor.onEvent(ClockStrategy.registerClockEvent(ClockStrategy.fastEpochMillisClock()));
```

Decide it against the log's readers, not the latency alone: under a projected clock the `logTime` on
every record drifts from wall-clock for the life of the process. If nothing correlates those timestamps
with anything outside the JVM, take the saving.

!!! warning "`nanoEpochClock()` changes the unit, and time-windowed nodes name theirs"
    `getWallClockTime()` returns nanoseconds under it where the default returns milliseconds, and
    `FixedRateTrigger.atMillis()` means milliseconds by construction. Installing it on a graph with a
    tumbling or sliding window stops the window rolling — silently, with the arithmetic out by a factor
    of a million. Use it when sub-millisecond timestamps matter and the graph has no time-windowed nodes.

**`endTime` is on by default**, as it has been in every release. It is the *second* clock read on an
audited event path and exists only for `endTime - logTime`, so suppress it if you do not consume the
duration:

```java
logRecord.setRecordEndTime(false);
```

Taking both savings — the fast clock and no `endTime` — is worth **14.2 ns on JIT and 11.2 on native**
on the audited binary arm. Both were briefly defaults during development, which is where that figure was
measured; they are opt-in now because each changes a documented contract, so the saving is available on
request rather than applied to everyone.

!!! note "A nanosecond timestamp costs more to format in a text record"
    Nineteen decimal digits instead of thirteen. A binary record stores the raw `long` and pays nothing
    for the extra digits; a text record formats them on the event path. It is one more reason the binary
    record is the right choice for a latency profile.

### 5 · Dirty filtering and re-entrancy carry semantic consequences

`LOW_LATENCY_AUDIT` deliberately **keeps** both, because an audit profile must not change what the graph
computes. `LOWEST_LATENCY` gives up dirty filtering: an `@OnTrigger` method then runs whenever the wave
reaches it, not only when a parent is dirty. Invisible for pure recomputation; **not** invisible for a
node that accumulates or has side effects.

`setSupportReentrancy(false)` is never set by a profile. It is the one setting that can turn a working
graph into an `IllegalStateException` — re-entrant dispatch stops being queued and throws instead.
Measured at approximately zero benefit. Set it only if your graph provably never re-enters.

### 6 · Void triggers

`@OnTrigger(failBuildIfMissingBooleanReturn = false)` lives on your node classes, so no profile can set
it for you.

## A recommended starting point

**No audit trail, lowest latency:**

```java
config.performanceProfile(LOWEST_LATENCY);
// build native with --gc=epsilon -H:-SpawnIsolates and a PGO profile
// construct the processor inside the method that drives it
```

**Audit trail, lowest latency:**

```java
config.performanceProfile(LOW_LATENCY_AUDIT)
      .addLowLatencyEventLog(LogLevel.INFO, AuditRecordFormat.BINARY);
processor.onEvent(new ClockStrategy.ClockStrategyEvent(ClockStrategy.nanoEpochClock()));
```

…and note that `BINARY` produces a log the analyser UI cannot yet open, though the command-line reader
can. See [Binary audit logging](../how-to/binary-audit-logging.md).

**Developing, and you do not yet know what you are looking for:** `AUDITED` with tracing. It is 30× the
cost of the tuned configuration and it tells you which node did what, which is worth far more than
nanoseconds while you are still finding out.
