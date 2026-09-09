# Reference: Performance profiles

A profile is a named bundle of settings. This page states the whole bundle, what each setting costs, and
what you give up — so the trade is visible before you pick one.

Every figure is from the [performance benchmark](performance.md): a four-node market-data graph, one
thread, Apple M4, ns/event.

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
again for `endTime`. The default strategy is `System::currentTimeMillis`:

| clock source | cost/call | resolution |
|---|---:|---|
| `System::currentTimeMillis` | 12.9 ns | 1 ms |
| **`ClockStrategy.nanoEpochClock()` — the default** | **8.0 ns** | 1 ns |

The default was `System::currentTimeMillis`. It is both slower and unable to represent what it is read
for: it advances a thousand times a second, so a duration taken across two readings is **always exactly
zero** for any event faster than a millisecond. The default is now a monotonic, epoch-anchored
nanosecond clock.

**The unit changed with it.** `getWallClockTime()` returns nanoseconds where it returned milliseconds.
Restore the old behaviour, or drive the clock from your own data for replay, with:

```java
processor.onEvent(new ClockStrategy.ClockStrategyEvent(() -> System.currentTimeMillis()));
```

**`endTime` is now off by default.** It is the *second* clock read on an audited event path, and its only
purpose is `endTime - logTime`. Turn it on when you consume the duration:

```java
logRecord.setRecordEndTime(true);
```

Together these two changes are worth **14.2 ns on JIT and 11.2 on native** on the audited binary arm —
34.6 → 20.4 and 29.3 → 18.2.

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
