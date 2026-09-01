# SPRING_UNDECLARED_SERVICE_CALLBACK

> Spring bean 'pricingNode' registers service 'com.example.PricingService named 'primary'' but strict service bindings do not declare it

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_SERVICE_BINDING` |

## The rule

Under strictServiceBindings every service callback a selected node registers must be declared in the Spring configuration.

## Why the compiler says this

strictServiceBindings makes the Spring file the authority on which services a graph binds. This node registers one the file does not mention, so the file no longer describes the graph — which is the single thing strict mode exists to guarantee. Without strict mode this is a normal arrangement and is not reported.

## How to fix it

Declare the service on this node in the Spring configuration, or turn off strictServiceBindings if the file is not meant to be exhaustive.

## Which builds raise it

**Spring configuration analysis**, on every build path that loads a Spring context. Non-Spring builds never raise it.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_UNDECLARED_SERVICE_CALLBACK",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_SERVICE_BINDING", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
