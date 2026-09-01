# SPRING_BEAN_NOT_SELECTED

> these Spring beans are not part of the graph: [orphanBean]

| | |
|---|---|
| **Severity** | WARN |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_BEAN` |

## The rule

With a nodeBeans allowlist in force, a bean that is neither listed nor ignored is excluded from the graph.

## Why the compiler says this

nodeBeans is non-empty, so Fluxtion treats it as an allowlist: only the beans it names become nodes. The beans listed here are in neither nodeBeans nor ignoredBeans, so they were left out — silently, because being absent from a list is not an error the compiler could otherwise notice.

## How to fix it

Add each bean to nodeBeans if it should be part of the graph, or to ignoredBeans to record that leaving it out is deliberate. Listing it as ignored is how an intentional omission stops being reported.

## Which builds raise it

**Spring configuration analysis**, on every build path that loads a Spring context. Non-Spring builds never raise it.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_BEAN_NOT_SELECTED",
  "severity": "WARN",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_BEAN", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
