# SPRING_RECONCILE_CONFLICT

> Cannot reconcile com.example.PriceNode#field:priceSource

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SOURCE_MEMBER` |

## The rule

Reconciliation must preserve developer-owned source and refuse conflicting edits.

## Why the compiler says this

The owned annotation was edited after generation.

## How to fix it

Change the XML declaration or the Java declaration to resolve the conflict, then re-run. No source files or authoring record were written.

## Which builds raise it

**Local Spring source reconciliation only.** The starter checks source ownership before writing changes. This is not a compiler or XML-only validation result.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_RECONCILE_CONFLICT",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SOURCE_MEMBER", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
