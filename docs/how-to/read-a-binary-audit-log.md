# How-to: Reading a binary audit log

A binary audit log is not text, so `less` and `grep` are no help. `AuditLogTool` is the command-line
reader: it decodes a log, resolves ids back to names, filters, and prints.

See [Binary audit logging](binary-audit-logging.md) for how to produce one.

## Running it

The tool ships in `fluxtion-runtime`, so there is nothing extra to install:

```bash
java -cp fluxtion-runtime.jar \
     com.telamin.fluxtion.runtime.audit.tools.AuditLogTool <file> [options]
```

| option | meaning |
|---|---|
| `--from <millis>` · `--to <millis>` | inclusive `logTime` bounds, **always epoch milliseconds**: the tool reads the file's header first and scales them to the file's unit. A file whose header states no unit, or an undefined one, refuses a time query (exit 2) rather than compare milliseconds to something else |
| `--event <glob>` | event type name |
| `--node <glob>` | node name |
| `--key <glob>` | property key |
| `--limit <n>` | stop after *n* matching records |
| `--sink text\|null` | output; `text` by default |
| `--stats` | counts, the file's time unit, unresolved ids, redefined ids, unreadable bytes |
| `--declare-unit millis\|nanos --out <copy>` | for a file whose header states no unit: write a copy that states it. Fills in only a header that states none, never rewrites a stated unit, changes nothing past the header |
| `-h`, `--help` | usage |

Globs are matched **once, when the dictionary entry arrives** — not per entry. A pattern resolves to a
set of integer ids, and filtering afterwards is an integer-set test. That is why filtering a large log
costs almost nothing, and it is worth knowing if you extend the tool: match on the id, use the name only
to print.

```bash
# everything one node did, in a five-second window
java -cp fluxtion-runtime.jar com.telamin.fluxtion.runtime.audit.tools.AuditLogTool trades.flog \
     --node 'riskCheck*' --from 1757000000000 --to 1757000005000

# is this file intact?
java -cp fluxtion-runtime.jar com.telamin.fluxtion.runtime.audit.tools.AuditLogTool trades.flog --stats
```

## Read `--stats` before you trust a filtered result

`--stats` reports the two conditions under which a log **silently tells you less than the truth**:

- **unresolved ids** — an entry names an id the dictionary never described. A log that was rolled mid-run
  can legitimately start mid-dictionary, so this is not automatically corruption; a non-zero count on a
  file that should be complete is.
- **unreadable trailing bytes** — the file was truncated, most often because the writer was killed. The
  reader decodes everything it can and reports the remainder rather than failing the whole read, so a
  truncated file is still worth something.
- **the time unit** — `epoch milliseconds` for anything the default writer wrote; `unspecified` for a
  file from a pre-release runtime, which a millisecond consumer must not assume anything about — declare
  it with `--declare-unit`. The analyser refuses an unstated unit for the same reason.

Neither shows up in filtered output. A `--node` pattern that matches nothing and a `--node` pattern whose
node was never named in the dictionary both print nothing.

**A trace entry is not an unresolved id.** Method traces carry a node id and no key, and the reader
reports that absent key as *no key* rather than as an id that failed to resolve — otherwise every traced
log would look corrupt. An entry with no key that is **not** a trace is a value logged under a null
key: kept as a keyless value, never turned into a trace.

**A missing `endTime` reads as `0`.** `LOW_LATENCY_AUDIT` elects not to take that second clock reading;
the format defines `0` as *not recorded*, and the analyser shows it as absent.

## Using the decoder directly

For anything the CLI does not do, `BinaryLogReader` takes a visitor:

```java
BinaryLogReader.Result r = BinaryLogReader.read(path, new BinaryLogReader.Visitor() {
    @Override public boolean onRecord(int typeId, String eventType,
                                      long eventTime, long logTime, long endTime, int entries) {
        return logTime >= from;          // false skips the record's entries without decoding them
    }
    @Override public void onEntry(int nodeId, String node, int keyId, String key,
                                  int tag, long rawBits) {
        System.out.println(node + "." + key + "=" + BinaryRecordDecoder.renderValue(tag, rawBits));
    }
});
```

Returning `false` from `onRecord` skips the whole record cheaply — every entry is a fixed two slots, so
the reader steps over them without decoding.

`BinaryRecordDecoder.knownTag(tag)` tells you whether a tag is one this version understands. **Check it.**
Tags are added over time — `TAG_TRACE` was added after the first release — and a reader that assumes it
knows every tag will mis-render a newer log rather than say it cannot read it.

## What it cannot do

It reads a binary log for people and `grep`. **Its text output is raw inspection, not analyser input**:
values are printed bare, so a logged String such as `"ok, invented: 42.0"` would read back as a second,
numeric entry. Open the `.flxa` file in the [audit log analyser](https://telaminai.github.io/fluxtionauditlog-analyser/)
instead — it opens binary logs directly, keeps the wire's types, identity and unit, and refuses a file
whose unit it cannot present. The format itself — header, frames, tags, bounds, what a writer must
refuse and a reader must deliver — is specified in [FLXA — the binary audit log format](../reference/flxa-format.md).
