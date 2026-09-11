# FLXA — the binary audit log format

**Status: normative for format version 1, shipped in `fluxtion-runtime` 1.0.15.** The key words MUST,
MUST NOT, SHOULD and MAY are as in RFC 2119. Where this page and an implementation disagree, this
page is the bug report; where this page is silent, the implementation is not a promise.

This is the format `BinaryLogWriter` writes, `BinaryLogReader` and `AuditLogTool` read, the C++
runtime's `fluxtion_writer.h` writes, and the audit log analyser opens. It exists because five
review rounds found the same shape of defect: a consumer interpreting bytes the producer never
defined — a unit assumed from a comment, a string that became syntax, a check one emission path ran
and another did not. Everything a consumer may rely on is stated here, and a **conformance corpus**
(§13) says what passing means.

## 1. Conventions

- All multi-byte integers are **big-endian**. `u8`, `u16`, `i64` are unsigned 8- and 16-bit and
  signed 64-bit integers. A `double` is its IEEE-754 bit pattern carried as an `i64`.
- Text is **UTF-8**, never null-terminated, always length-prefixed.
- Offsets are from the start of the file. There is no padding or alignment anywhere.

## 2. File structure

```
file   := header frame*
header := magic "FLXA" (4 bytes) , formatVersion:u16 , timeUnit:u16        # 8 bytes
frame  := DICT | RECORD
```

A file is a header followed by any number of frames. Frames carry no length prefix and no checksum;
a reader parses them in order and knows a frame's length only from its tag. Consequences are stated
in §9 and §10.

| field | value | rule |
|---|---|---|
| `magic` | `46 4C 58 41` | MUST be present. A reader MUST refuse a file without it. |
| `formatVersion` | `1` | A reader MUST refuse a version it does not implement. This page defines version 1 only. |
| `timeUnit` | `0`, `1`, `2` | §7. A writer MUST NOT write any other value. |

## 3. Frames

### 3.1 DICT — a name and its id

```
DICT := 0x02 , id:u16 , length:u16 , utf8[length]
```

Defines that `id` names the string `utf8` for the rest of the file. §4 gives the rules.

### 3.2 RECORD — one audit cycle

```
RECORD := 0x01 , entryCount:u16 , eventTypeId:u16 , eventTime:i64 , logTime:i64 , endTime:i64 , entry[entryCount]
entry  := slot0:i64 , slot1:i64
slot0  := nodeId:16 bits (63..48) , keyId:16 bits (47..32) , reserved:24 bits (31..8) , tag:8 bits (7..0)
slot1  := value bits, meaning given by tag (§6)
```

The fixed part is 29 bytes; each entry is 16. `reserved` bits MUST be written as zero and MUST be
ignored by a reader: node, key and tag decode the same with every reserved bit set (f21).

| field | meaning |
|---|---|
| `entryCount` | number of `entry` pairs that follow. `0` is a record that happened and logged nothing (fixture f02). |
| `eventTypeId` | dictionary id of the event's **fully-qualified** class name (`Class.getName()`), so two events with one simple name stay distinct (f15). |
| `eventTime` | when the event was created, in the unit §7.2 assigns to it. |
| `logTime` | the processor clock's reading when the cycle began. The primary timeline. |
| `endTime` | the clock's reading when the cycle ended, or `0` when the producer does not record it. A consumer MUST treat `0` as *not recorded* — absent, not an instant — and MUST NOT place it on a timeline (f25). |

### 3.3 Tags

| tag | name | `slot1` carries | text rendering |
|---:|---|---|---|
| 1 | DOUBLE | IEEE-754 bits | `Double.toString` |
| 2 | LONG | the value | decimal |
| 3 | INT | the value, sign-extended | decimal |
| 4 | CHAR | the UTF-16 code unit in the low 16 bits | the character; **text, never a number** (§11) |
| 5 | CHARSEQ | a dictionary id of the string's text; `0` is `null` | the string |
| 6 | OBJECT | a dictionary id of the object's `toString()`; `0` is `null` | the string |
| 7 | BOOL | `0` or `1` | `false` / `true` |
| 8 | TRACE | ignored — a reader MUST decode the entry the same whatever the bits hold (f24) | no value: "this node ran". `keyId` MUST be `0`; a reader delivers a non-zero key as written, and a text constructor then renders that key with an empty string rather than `invoked`. |

A writer MUST NOT emit a tag outside this table. A reader MUST deliver an entry whose tag it does
not know, with its bits, and MUST NOT fail the record or the file (f12); rendering it is
diagnostic (`#tag<n>:<bits>`), and a text constructor treats it as text (§11).

## 4. The dictionary

- Ids are **file-scoped** and run from `1` to `65535`. Id `0` is reserved: it means *none* — the
  key of a TRACE entry, and a `null` CHARSEQ or OBJECT value. A reader MUST NOT count id `0` as an
  unresolved id (f03, f04).
- One id space names everything: event types, node names, keys, and String and Object **values**.
  A reader MUST NOT assume an id's role from the dictionary; a role is known only where the id is
  used.
- A writer MUST define an id with a DICT frame **before** the first frame that uses it (f01, f05).
  A reader MAY be given an id it was never told about — a rolled or damaged file — and MUST then
  render it as `#<id>`, count it, and continue (f11, f17). It MUST NOT throw. **Every role counts:**
  event type, node, key, CHARSEQ value and OBJECT value. The count is of **occurrences** (an
  undefined id used three times counts three), over the entries of records the visitor accepted —
  a record `onRecord` declined is not examined, so a filtered read's count is scoped to what it
  read. Id `0` is never an occurrence.
- A name is at most `65535` UTF-8 bytes. A writer MUST refuse a longer name (§8) rather than
  truncate the length field.
- **Redefinition.** A writer MUST NOT define an id twice. A reader MUST accept a second DICT frame
  for an id, use the most recent definition for the frames that follow it, deliver both definitions
  in order, and **count** the redefinition (`Result.redefinedIds`) so a consumer can report it
  (f18). This is stated so that a repaired or concatenated file has one defined reading, not so
  that writers may rely on it.
- **Empty names are legal.** A zero-length name is a valid dictionary entry — a logged empty String
  value is the common case, and an empty key is representable. A reader resolves it like any other;
  a text constructor quotes it (§11.5; f22).
- **Malformed UTF-8** in a name MUST NOT fail the file: the reader replaces undecodable bytes with
  U+FFFD and continues (f19). A writer never produces it; a reader must survive it.
- The record a processor logs into allocates its own **record-scoped** ids; the file's ids are the
  writer's. They coincide only while one record instance is reused, which the API does not promise.
  A writer MUST therefore map record ids to file ids **by name**, so a replacement record — or two
  records that interned the same names in a different order — is attributed correctly (f06). A
  consumer that emits a record's bytes directly (`BinaryLogRecord.encodeTo`) emits record-scoped ids
  and owns the dictionary itself; such output without DICT frames is exactly f11.

## 5. Records

- A record is delivered in file order. `logTime` SHOULD be non-decreasing across a file; a reader
  MUST NOT re-sort.
- **Entry order is the wire's, and MUST be preserved** by every consumer, through any text it
  constructs and into its model. The same node MAY appear again later in the record, and the same
  key MAY appear more than once under it; where a consumer needs one value per record, the **last
  occurrence wins**, and that rule depends on order having been kept (f23). Consecutive entries with
  the same `nodeId` are one node's contribution and a text constructor groups them (§11.7).
- `entryCount` bounds a record at `65535` entries. A writer MUST refuse a record with more (§8).

## 6. Values

Encodings are in §3.3. Two rules a consumer must not get wrong:

- **A String is a String.** CHARSEQ, OBJECT and CHAR are text whatever they spell: `"42.0"` logged
  as a String is not a figure, `"null"` is not null, `"true"` is not a flag, and `'7'` is a character.
  Any consumer that types values (a chart, a scorer, a diff) MUST take the type from the tag, and any
  text it constructs MUST preserve that (§11, f13, f14).
- **`null` is id `0`**, not a dictionary entry spelling `null`.
- **A numeric equality verdict MUST NOT lose distinctions present in the source.** A LONG carries
  64 bits; a consumer that compares two records MUST compare the values it was given, not a
  narrower conversion of them — converting to a plotting `double` made `9007199254740992` and
  `9007199254740993` equal. Plotting conversion is not an equality definition. Whether two
  *spellings* of one number (`1` and `1.0` in text) are equal is the consumer's stated policy, and
  a tolerance-based score is a score, not an equality; the analyser's diff compares exactly and
  treats equal decimals as one figure, and its scorer's tolerance is documented as such.

## 7. Timestamps and the time unit

### 7.1 Codes

| code | meaning |
|---:|---|
| `0` | **unspecified.** The writer stated no unit. Older snapshots wrote it as the reserved value — some of them, Java and C++, under nanosecond readings — and the explicit-unit constructor of the current Java writer still accepts it; the default constructor writes `1`. A consumer needing a unit must obtain an explicit declaration (§7.4). |
| `1` | epoch milliseconds — the Java runtime's default clock. |
| `2` | epoch nanoseconds — `ClockStrategy.nanoEpochClock()`, the C++ `SystemNanoClock`. |

A writer MUST refuse any other code before writing a header byte (f10 exists only by byte edit).

### 7.2 Which fields the unit governs

The unit is the unit of the processor's **`ClockStrategy`**, which stamps `logTime` and `endTime`
on every record and `eventTime` on a record whose event is a plain object. An event that implements
`Event` supplies its own `eventTime`: `Event.getEventTime()` is defined as epoch milliseconds at
construction, or `-1` for none, and the runtime records it **as given**, because it is the
producer's statement of when the event happened and not a clock reading the runtime made. So under a
nanosecond strategy a file carries nanosecond `logTime`/`endTime` and millisecond `eventTime` for
`Event`-typed events. A consumer that needs `eventTime` in the header unit must know its events; the
wire does not say whether an event implemented `Event`.

### 7.3 One unit per writer

The unit is stated once, in the header, for the life of the writer. Changing the clock strategy
while a writer is open is unsupported: start a new writer with the new unit.

### 7.4 What a reader may do with it

- A reader MUST deliver the header — and so the unit — **before any frame** (`Visitor.onHeader`),
  so a consumer that presents a fixed unit decides then. Deciding after delivery means every record
  was already delivered in the wrong unit.
- A reader MUST NOT infer a unit from magnitude.
- A consumer MUST NOT assume what code `0` means. The unit of such a file is established by the
  **user**, into the file: `AuditLogTool <file> --declare-unit millis|nanos --out <copy>` fills in a
  header that states none, refuses to rewrite one that does, and changes nothing past the header. The
  declaration then travels with the evidence.
- A consumer that interprets a bound in a fixed unit (`--from`/`--to`, milliseconds) MUST scale it to
  the file's unit after reading the header, and MUST refuse the query — not answer it with nothing —
  when the unit is unstated or undefined. **Bounds are inclusive epoch-millisecond instants**, and
  the conversion MUST preserve the requested inequality when a bound lies outside the representable
  domain of the file's unit: a lower bound after every representable instant admits nothing, an
  upper bound before every one admits nothing. Clamping a bound to `Long.MAX_VALUE` had admitted a
  record stamped exactly there. "No bound" is the absence of the option, not a sentinel value: an
  explicitly supplied extreme is a bound.

## 8. Writer requirements

1. Write the header first, then frames; nothing else, ever.
2. Define every id before its first use (§4).
3. Decide every **semantic** refusal for a record before writing the first byte of it, on every
   path that emits a record. The refusals are:
    - a record that **overflowed** its buffer — its tail was dropped, and a short frame would look
      complete;
    - more than `65535` entries — the count would wrap;
    - a name over `65535` UTF-8 bytes — the length field would wrap;
    - more new names than the file has ids left — the file dictionary holds `65535`.
   (The Java writer implements the first two as one shared method, `BinaryLogRecord.checkEncodable()`,
   also run by `encodeTo`, and the last two as a preflight over the record's dictionary before any
   DICT frame. That is this implementation's shape, not the wire contract: an interoperable writer
   must make the same refusals before the same byte, however it is built.)
4. Refuse an undefined unit code before the header (§7.1).
5. **What a refusal guarantees.** A semantically refused record leaves the stream and the writer's
   dictionary **exactly as they were**: no DICT frame, no partial RECORD, no id consumed. Refusal is
   loud — an exception — never a silently shorter file. This is validation, not a transaction: an I/O
   failure from the stream mid-frame is outside it and leaves a partial frame, which a reader
   reports as an unreadable tail (§9.3).
6. Two limits that are easy to confuse: the **file** dictionary holds `65535` names (this section);
   a Java `BinaryLogRecord`'s own intern table holds `32767`, so one record can never exhaust the
   file alone, but several record instances can.

## 9. Reader requirements

1. Refuse a file without the magic or with a version this reader does not implement.
2. Deliver the header before any frame (§7.4).
3. Deliver every **whole** frame in order. A frame that is incomplete at end of file — a truncated
   tail, the normal end state of a crash — MUST NOT fail the file: everything before it is delivered
   and the unusable byte count is reported (f07).
4. Stop at a frame tag this reader does not know, after delivering every frame before it, and report
   it (f16). Frames carry no length, so it cannot be skipped.
5. Deliver an entry whose value tag is unknown, with its bits (f12).
6. Render an unresolved id as `#<id>`, count it, continue (f11). Id `0` is never unresolved.
7. Never guess a unit (§7.4).

## 10. Where a file comes from is not in the file

The format carries no producer identity, thread, grouping id, host, or processor name. A consumer
that presents those MUST take them from outside the file and say so; it MUST NOT invent them. The
analyser's provenance model (its *§E provenance*) is the consumer-side answer, and the text it
constructs omits `thread` and `groupingId` rather than writing `null`. The consequence to state
plainly: **two files written by two processors with the same graph are not distinguishable by
content.** Which process wrote a file is evidence that must be kept beside the file — a file name,
a directory, a deployment record — and a dispute that turns on it cannot be settled from the bytes.

## 11. Constructing text from a file

A consumer that renders a file as the text record format (the analyser's `BinaryAuditReader`) is
writing typed values into a grammar that types by inspection. These rules keep the wire's types:

1. **Encoding is selected from the reader's declared context; logged content MUST NOT select or
   change it.** The consumer's reader declares the quoted-scalar grammar at its adapter boundary
   (the analyser: `AuditLogReader.textEncoding()`), and its parser applies that declaration to every
   record the reader delivers. Nothing in the text may switch grammar — an earlier draft put a
   declaration scalar in each record, and a multiline legacy value containing that line was
   promoted into a control field. Text that carries no such declaration is legacy, in which a quote
   mark is the producer's character.
2. DOUBLE, LONG, INT and BOOL are written bare: their renderings cannot be syntax and the text
   grammar types them as the wire did.
3. **Everything else is text** — CHARSEQ, OBJECT, CHAR, and an unknown tag's diagnostic — and is
   written in the quoted form whenever the bare form would split, nest, end the line, strip, or
   type as a number, boolean or null. `"` is the quote; the escapes are `\\` `\"` `\n` `\r` `\t`.
4. A `null` value (id `0`) is the bare literal `null`; the **string** `null` is quoted.
5. Keys and node names outside `[A-Za-z0-9_$.-]` are quoted.
6. `event:` carries the simple class name and `eventType:` the full one; neither may contain a line
   break, so one in the dictionary is made visible rather than allowed to start a scalar.
7. Consecutive entries of one node are one `- node: { … }` item; a TRACE entry is `invoked: true`.

Fixtures f13 and f14 are the test: every value MUST parse back to exactly the string logged, with
no entry manufactured and none lost.

8. **Damage travels with the evidence.** What the runtime reader could not read (§9.3, §9.6) MUST
   reach the consumer beside the records it did read — the analyser carries it through the SPI's
   diagnostic consumer into the store and shows it as a *source damage* finding — and MUST NOT be
   delivered as a synthetic node or a fabricated event.

## 12. The command-line tool

`AuditLogTool` is the reference consumer for §7.4 and §9. Its `--from`/`--to` are milliseconds
whatever the file says; it reads the header, scales them, and refuses a time query over an
unstated or undefined unit with exit code 2 and nothing printed. `--stats` labels the unit code.
`--declare-unit` is §7.4's declaration. Its text output is **raw inspection**, for people and
`grep`: it has the text runtime's shape with every value written bare, and the difference from
analyser input is semantic, not cosmetic — a logged String `"ok, invented: 42.0"` parses back as a
second, numeric entry. Open the `.flxa` file in the analyser for typed reading; do not pipe the
tool's output into it. The tool's help says so.

## 13. Conformance

The corpus is generated by `FlxaConformanceCorpus` from the shipped writer and a counting clock,
committed under `fluxtion-runtime/src/main/resources/com/telamin/fluxtion/runtime/audit/conformance/`,
and shipped in the runtime jar. Every fixture is reproducible; the "derived" ones (a wrong unit code,
a cut tail, an undefined tag) are byte edits of a produced one. **A diff in a committed fixture is a
format change and belongs on this page first.**

| fixture | pins | Java | analyser | C++ |
|---|---|:---:|:---:|:---:|
| f01 minimal | header, ids before use, one entry, the three timestamps | ✅ | ✅ | writer parity¹ |
| f02 empty record | `entryCount 0` is a record | ✅ | ✅ | — |
| f03 every tag | each tag's encoding and rendering; TRACE has key 0 and is not unresolved | ✅ | ✅ | writer parity¹ |
| f04 null values | id 0 is `null`, not unresolved | ✅ | ✅ | — |
| f05 dictionary growth | names first used later are defined between records | ✅ | ✅ | — |
| f06 two records, two dictionaries | file ids are by name; a swapped record is attributed correctly | ✅ | ✅ | deferred² |
| f07 truncated tail | whole frames delivered, tail reported, file not failed; the consumer shows the report beside the records | ✅ | ✅ | — |
| f08 unit nanos | header says so before any record; a millisecond consumer refuses, delivering nothing | ✅ | ✅ | writer parity¹ |
| f09 unit unspecified | delivered as code 0; a consumer does not assume; `--declare-unit` | ✅ | ✅ | — |
| f10 unit undefined | writer cannot produce it; reader delivers the code; consumer refuses | ✅ | ✅ | deferred² |
| f11 unresolved ids | `#id`, counted, never thrown | ✅ | ✅ | — |
| f12 unknown value tag | delivered with bits; diagnostic rendering; text treats it as text | ✅ | ✅ | — |
| f13 hostile strings | every string round-trips; nothing manufactured or lost | ✅ | ✅ | — |
| f14 hostile chars | a char is text; the figure after it survives | ✅ | ✅ | — |
| f15 same simple name | full identity recorded and kept distinct | ✅ | ✅ | — |
| f16 unknown frame | frames before it delivered; then reported | ✅ | ✅ | — |
| f17 unresolved value ids | CHARSEQ/OBJECT value ids count like every other role; rendered `#id`; reported beside the evidence | ✅ | ✅ | — |
| f18 duplicate dict id | both definitions delivered; the latest names what follows | ✅ | ✅ | — |
| f19 malformed UTF-8 | replaced with U+FFFD, never fatal | ✅ | ✅ | — |
| f20 damage, both kinds | an undefined value id and a cut tail: both counted, both reported, in a stable order | ✅ | ✅ | — |
| f21 reserved bits | all ones in the reserved 24 bits: node, key, tag decode unchanged | ✅ | ✅ | — |
| f22 empty names | an empty key and an empty String value resolve; the text quotes them | ✅ | ✅ | — |
| f23 entry order | a key logged three times under one node, with another node between: order kept, last wins | ✅ | ✅ | — |
| f24 trace bits | non-zero bits on a TRACE entry are ignored; it is still "invoked" | ✅ | ✅ | — |
| f25 no endTime | `0` reads as not recorded — absent in the model, not an instant | ✅ | ✅ | — |
| f26 concatenated | the first file's records, then the second header reported as an unknown frame | ✅ | ✅ | — |
| writer refusals (no file) | overflow, 65,536 entries, oversize LATER name, a full FILE dictionary across record instances (and exactly filling it is allowed), a name of exactly 65,535 bytes, undefined unit: refused before any byte, stream and dictionary unchanged; a refused record instance is reusable after its next trigger; the preflight length equals the encoded length for surrogate pairs and lone surrogates | ✅ `BinaryAuditEndToEndTest` | — | overflow ✅, others source-inspected² |
| header refusals (no file) | bad magic, unknown version | ✅ `BinaryLogFileRoundTripTest` | ✅ (`canOpen`) | — |
| read paths (no file) | mapped and streamed reads agree, including a frame straddling a chunk | ✅ `BinaryLogFileReadPathTest` | — | — |
| CLI (no file) | bounds scaled; out-of-domain bounds keep their inequality on both sides, together, and with `--limit`; an explicit extreme is a bound; refusal on unstated/undefined unit; `--declare-unit` | ✅ `AuditLogToolTest` | — | — |

¹ The C++ writer is held to the Java writer by the compiler's Java-to-C++ audit parity tests, not yet
by byte-equality against this corpus.
² Deferred to the C++ round: unit-code validation, id exhaustion and oversize-name execution tests,
and by-name id translation.

The Java suite is `FlxaConformanceTest` in `fluxtion-runtime`; the analyser's is its
`FlxaConformanceTest` over the same bytes, loaded from this jar, through its reader, store, record
parser and tokenizer. **Passing both means these named cases pass.** The corpus is an executable
baseline, not a proof that every MUST on this page is pinned: for each rule, the table names the
fixture or test that would fail if the rule were broken, and a rule with no such name is a rule
this page asks for and no test yet enforces. Those are listed here so they are not mistaken for
tested:

| obligation | expected fact | what a broken implementation would show | status |
|---|---|---|---|
| §4 redefinition by a writer is forbidden | the Java writer never emits two DICT frames for one id | a second frame for an id in a writer-produced file | reader side pinned and counted (f18); no writer-side mutation test |
| §7.3 one unit per writer | a strategy change mid-file is not honoured by the header | a file whose readings change unit after some record | stated policy, not mechanically prevented |
| §8.5 I/O failure | a partial frame after a stream error reads as a truncated tail | a reader failing the whole file | not tested against a failing stream |
| §9.5 unknown tag in every consumer | the CLI prints the diagnostic form | a consumer throwing on tag 9 | Java reader and analyser pinned (f12); CLI not separately |

## 14. Versioning

- `formatVersion` is `1`. A reader MUST refuse any other value; there is no forward tolerance at
  the frame level, because frames have no length prefix.
- A new **value tag** MAY be added without a version change, because §9.5 requires readers to
  deliver unknown tags. A new **frame type**, a change to any fixed layout, or a change to the
  meaning of an existing field requires `formatVersion 2`.
- The `timeUnit` field replaced a reserved field that was always `0`; that is why `0` means
  *unspecified* and not *milliseconds* (§7.1).

## 15. Known limits, stated rather than discovered

- No checksum. A flipped byte inside a frame is not detected; a flipped tag byte becomes an unknown
  frame (§9.4) and stops the read there.
- No producer identity, thread or grouping id (§10).
- No per-record unit and no record of whether an event implemented `Event` (§7.2).
- A String value interned as a dictionary name exhausts the dictionary at `65535` distinct
  strings; log an identifier, not free text, on a hot path.
- **Concatenation is not rolling.** Two files joined with `cat` are not one file: the second header
  is a frame tag the reader does not know (`0x46`, the `F` of `FLXA`), and the read stops there after
  delivering the first file's records (f26). Rolling is the writer's job — a new writer, a new file,
  a new dictionary — and a rolled file's dictionary is not carried into the next one; ids used in a
  later file whose names were defined in an earlier one are unresolved there (§4, f11).
