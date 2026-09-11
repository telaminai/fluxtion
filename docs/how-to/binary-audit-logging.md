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

`config` here is the `EventProcessorConfig` from the config-builder form of `Fluxtion.compileAot` — see
[Setting a profile in your build](../reference/performance-profiles.md#setting-a-profile-in-your-build).

```java
config.performanceProfile(EventProcessorConfig.PerformanceProfile.LOW_LATENCY_AUDIT);
config.addLowLatencyEventLog(LogLevel.INFO);                           // TEXT — the default
config.addLowLatencyEventLog(LogLevel.INFO, AuditRecordFormat.BINARY); // BINARY
```

**Then install a sink, or the first record refuses.** A binary record has no text form, so the default
sink cannot take it. The window is after construction and before the first event:

```java
DataFlow processor = Fluxtion.compileAot(cfg -> { /* as above */ });
EventLogManager audit = processor.getAuditorById(EventLogManager.NODE_NAME);
audit.setLogSink(new BinaryLogWriter(Files.newOutputStream(Path.of("audit.flxa"))));
processor.init();                                   // now events can flow
```

`EventLogManager` builds the chosen record at `init()`. Swapping the record on a *running* processor
through `EventLogControlEvent` still works and is still how you change format at runtime; this is how
you start in the right one.

**The writer states the file's time unit, once.** `new BinaryLogWriter(out)` declares epoch
milliseconds, which is what the default clock writes. If you install `ClockStrategy.nanoEpochClock()`,
construct the writer with `BinaryLogFile.TIME_UNIT_EPOCH_NANOS` so the header tells every reader; an
undefined code is refused before a header byte is written. The unit is fixed for the life of the writer,
so change the strategy before the writer exists, or start a new file. The unit describes the **clock
strategy's readings** — `logTime`, `endTime`, and `eventTime` for a plain event object. An event that
implements `Event` supplies its own `eventTime`, which its contract defines as epoch milliseconds at
construction, and the runtime records it as given: under a nanosecond strategy such a record carries
nanosecond `logTime` and millisecond `eventTime`. The analyser reads millisecond files only and refuses a
file whose header says otherwise, before it delivers a record.

**A file whose header states no unit is refused by the analyser, and by any `--from`/`--to` query.**
Code 0 means the writer stated none: older snapshots wrote it as the reserved value, some of them
under nanosecond readings, and the explicit-unit constructor still accepts it, so nothing assumes.
State the unit yourself, into the evidence:

```bash
java -cp fluxtion-runtime.jar com.telamin.fluxtion.runtime.audit.tools.AuditLogTool \
     old.flxa --declare-unit millis --out old-declared.flxa
```

The copy is byte-identical past the header, the tool fills in only a header that states none, and
every reader then trusts it. The format itself — header, frames, tags, bounds, what a writer must
refuse and a reader must deliver — is specified in [FLXA — the binary audit log format](../reference/flxa-format.md),
with a conformance corpus that ships in the runtime jar. The tool's own `--from`/`--to` are milliseconds whatever the file's unit:
it reads the header first, scales the bounds for a nanosecond file, and refuses a time query over a
file whose unit it cannot honour rather than report zero matches.

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
  dictionary id so the record stays complete, not because it is fast. A deployment targeting
  this profile should not be logging `Object`.
- **`asCharSequence()` throws.** A binary record has no text form. A sink written against the text record
  will fail loudly rather than emit something wrong.
- **The analyser opens it** through its `BinaryAuditReader`, as does the `AuditLogTool` command line.
  `TEXT` stays the default only because binary needs the sink step below and text needs nothing.

## Two things that have caught people out

!!! danger "`LOW_LATENCY_AUDIT` can be configured into producing no audit log at all"
    `LOWEST_LATENCY` turns off node-name lookup; `LOW_LATENCY_AUDIT` keeps it, because node registration is what supplies each node its
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
