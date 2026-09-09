# How-to: Binary audit logging (the low-latency record)

The default audit record formats a node name, a property key and a value into text **inside the event
cycle**. That is readable and it is the right default. It is also, on a graph where every node logs,
about nine times the cost of writing the same information as bits.

This guide covers the binary record: when to choose it, how to turn it on, what it costs, and the one
reason it is not the default.

It assumes you have read [Audit logging in a DataFlow](audit-logging-dataflow.md), which explains
`EventLogger`, `EventLogManager` and `LogRecordListener`. Nothing here changes how a node logs.

## When to choose it

Choose the binary record when **audit density is high** — many nodes logging many values per event — and
the event path is latency-sensitive. Audit density, not graph size, is the variable that decides:

| record | JIT | native AOT | bytes/record |
|---|---:|---:|---:|
| text | 42.3 ns · 24 M/s | 50.6 ns · 20 M/s | 404 |
| **binary** | **20.4 ns · 49 M/s** | **18.2 ns · 55 M/s** | 188 |

Measured on the four-node price-ladder graph from [Performance results](../reference/performance.md),
no-op sink, zero allocation, **excluding the disk or network write** — and on a graph whose nodes log
**nothing explicitly**, so this is the cost of the machinery. Binary is **2.1× cheaper on JIT and 2.8× on
native** before a single value is logged.

The gap widens with audit density, because the text record formats a node name, a key and a value
*inside the event cycle* for every entry while the binary record writes two aligned longs. On a graph
where every node logs, the ratio measured around 8×.

## Turning it on

The record format is a **build input**:

```java
config.performanceProfile(EventProcessorConfig.PerformanceProfile.LOW_LATENCY_AUDIT)
      .addLowLatencyEventLog(LogLevel.INFO);                           // TEXT — the default
      .addLowLatencyEventLog(LogLevel.INFO, AuditRecordFormat.BINARY); // BINARY
```

`EventLogManager` builds the chosen record at `init()`. Swapping the record on a *running* processor
through `EventLogControlEvent` still works and is still how you change format at runtime; this is how
you start in the right one.

**Node code is identical either way.** `auditLog.info("v", v)` is unchanged. There is no binary-specific
logging API, and there was briefly an indexed one that was removed for being slower — see
[Performance results](../reference/performance.md).

## What it writes

Each logged value is **exactly two aligned 64-bit slots**, 16 bytes:

```
slot0 :  bits 63..48   nodeId   (u16)
         bits 47..32   keyId    (u16)
         bits 31..8    reserved, zero
         bits  7..0    tag      (u8)

slot1 :  the value's raw bits
```

Names are never written. A node name or property key is resolved to a `u16` id **once per logger**, and
only the id goes into the record; the dictionary travels with the file so a reader can name them again.

**Every entry is two slots whatever the value type**, including a method trace, which carries a node id,
no key and no value. That fixed size is the property a reader depends on: it can skip an entry without
decoding it, which is what makes filtering cheap.

Three bytes per entry are unused. That is the cost of whole-`long` alignment, and it bought 51 ns/event
against assembling the same bytes one at a time — a 23% larger record for a much faster write.

## What it cannot do

- **`Object` values still cost text.** The `Object` overload has to call `toString()`; it is encoded as a
  length-prefixed string so the record stays complete, not because it is fast. A deployment targeting
  this profile should not be logging `Object`.
- **`asCharSequence()` throws.** A binary record has no text form. A sink written against the text record
  will fail loudly rather than emit something wrong.
- **The analyser UI cannot open a binary log yet.** The command-line reader can — see
  [Reading a binary audit log](read-a-binary-audit-log.md) — but a binary log does not open in the
  analyser. **This is why `TEXT` is still the default**: a default that changes what your existing
  tooling can read is not a default.

## Two things that have caught people out

!!! danger "`LOW_LATENCY_AUDIT` can be configured into producing no audit log at all"
    The profile turns off node-name lookup, and node registration is what supplies each node its
    `EventLogger`. An early version of this profile disabled registration and therefore the audit log,
    while still reporting excellent numbers — because a benchmark measuring nothing is very fast.

    **Assert `recPerEvent > 0` in any harness**, and count `auditor.nodeRegistered` calls in the
    generated source. The current profile is correct; the failure mode is silent, so check anyway.

!!! warning "`clock.eventReceived` must run before the audit manager sees the event"
    `logTime` is taken from `Clock.getProcessTime()` — the reading `Clock.eventReceived` already took —
    rather than a second, later call. The generator emits them in that order. If they are ever reordered,
    `logTime` quietly becomes the *previous* event's timestamp: wrong, plausible, and silent.

## Cost, in one line

On the measured graph the audit machinery costs **29.2 ns/event on JIT and 39.0 on native** over the same
graph with auditing off — for 11.75 recorded values, timestamps, and a published record. That number was
43.4 / 46.5 until the record's hot path was profiled; see
[Performance results](../reference/performance.md) for what was found and why the profile, not the
benchmark, found it.
