# SPRING_RECONCILE_WRONG_CONSTRUCT

> Export annotation is on the wrong construct in com.example.PriceNode

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SOURCE_MEMBER` |

## The rule

@ExportService annotates an implemented interface use, not the class declaration.

## Why the compiler says this

The compiler discovers exported services from annotated interfaces; a class annotation is ignored.

## How to fix it

Move @ExportService to the interface use: implements @ExportService X, then re-run.

## Which builds raise it

**Local Spring source reconciliation only.** The starter checks source ownership before writing changes. This is not a compiler or XML-only validation result.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_RECONCILE_WRONG_CONSTRUCT",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SOURCE_MEMBER", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
