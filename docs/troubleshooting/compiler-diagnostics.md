# When the compiler refuses your graph

The rest of this section is about a DataFlow that is **running**. This page is about a build that
**never got that far** — Fluxtion looked at your graph and refused it, or built it and noticed
something worth telling you.

Every such finding has a stable code (`FLX-1001`, `SPRING_HANDLER_MISMATCH`), a rule, an explanation
of what the compiler concluded, and a repair. **[Browse the codes →](errors/index.md)**

## Three ways to read a finding

### 1. The build log

A refusal prints its code and message:

```
[ERROR] FLX-1001: cannot find a matching constructor for com.example.OrderRouter
        — no constructor accepts the mapped fields [scorecard]
```

Warnings appear the same way. A build that **succeeds** can still report something — `FLX-1008`
below is the common one — and that is worth reading rather than scrolling past.

### 2. The sidecar — a machine-readable report

Opt in with `-Dfluxtion.diagnostics.sidecar=true` (or `=<path>`) and the build writes a JSON document
beside your sources:

```json
{
  "diagnosticsVersion": "1.0",
  "truncated": false,
  "diagnostics": [
    {
      "code": "FLX-1008",
      "severity": "WARN",
      "message": "2 node(s) can record audit values but this graph has no event audit configured…",
      "rule": "…",
      "why": "…",
      "suggestedFix": "…",
      "element": { "kind": "NODE", "nodeName": "priceSource" }
    }
  ]
}
```

It is written **atomically** — a consumer watching the directory never sees a half-written document,
because it cannot tell truncation from a compiler that found nothing.

The document conforms to a published schema:
[`fluxtion-diagnostics-1.0.schema.json`](fluxtion-diagnostics-1.0.schema.json).

### 3. As objects, from a programmatic build

A CI gate, a repair loop or an IDE usually wants the findings rather than a file:

```java
FluxtionDiagnostics.Result<CloneableDataFlow<?>> result =
        FluxtionDiagnostics.capture(() -> Fluxtion.compile(cfg));

for (FluxtionDiagnostic d : result.getReport().getDiagnostics()) {
    System.out.println(d.getCode() + ": " + d.getSuggestedFix());
}

CloneableDataFlow<?> flow = result.valueOrThrow();
```

A failure is **returned rather than thrown**, because the report is the reason for calling — a caller
who only wanted the exception already had one. `valueOrThrow()` opts back in to throwing.

This matters most for warnings: a build that succeeds cannot report anything through an exception, so
`FLX-1008` and `SPRING_BEAN_NOT_SELECTED` are only visible here or in the sidecar.

## Reading a report correctly

**Select by `severity`, never by position.** A failing build's report contains the warnings it found
as well as the error that stopped it, and `diagnostics[0]` is frequently a warning. Reporting it as
the cause of the failure is the most common mistake a consumer makes.

**An absent finding is not a passing check.** Some diagnostics only run on some build paths — see
below.

## "It worked yesterday" — why an AOT build can refuse what interpreting accepted

Fluxtion builds a graph three ways: **interpreted** and **in-process**, which run from your live node
instances, and **AOT**, which renders those nodes as Java source and compiles it.

Some rejections belong only to that rendering. A node that cannot be written out as source is not a
broken program — it runs correctly interpreted — so demanding renderability everywhere would refuse
working graphs.

The consequence is the part to know: **a graph developed interpreted can meet a rejection for the
first time at an AOT build.** `FLX-1001` and `FLX-1009` are the usual ones. That is by design, not a
regression, and each code's page states its own scope.

If you want to meet them earlier, run the AOT build in CI against the same graph you interpret
locally.

## The common warning: `FLX-1008`

If a node extends `EventLogNode` (or otherwise implements `EventLogSource`) it can write to an
`auditLog`. Without an event audit registered there is no auditor to receive those records, so
anything written is discarded silently.

```java
config.addEventAudit(LogLevel.INFO);   // records what those nodes log
```

If those nodes never call `auditLog`, the warning is noise and nothing needs changing. The compiler
can see that they *could* write; it cannot see whether they *do*.
