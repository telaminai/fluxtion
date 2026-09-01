# SPRING_HANDLER_MISMATCH

> Spring bean 'deafBean' is bound to event 'com.example.PriceUpdate' but has no handler that accepts it

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_SERVICE_BINDING` |

## The rule

An event-handler binding must name a node whose handler surface accepts the declared event type.

## Why the compiler says this

The binding tells Fluxtion to dispatch this event to this node. Fluxtion resolved the node's handler methods and none of them takes that event, so the generated processor would carry a route that can never fire. That is worse than a build failure: the graph would look wired and stay silent.

## How to fix it

Either add an @OnEventHandler method on deafBean taking com.example.PriceUpdate, or remove the binding if the event was meant for a different bean. If the handler exists but takes a supertype, name that type in the binding instead.

## Which builds raise it

**Spring configuration analysis**, on every build path that loads a Spring context. Non-Spring builds never raise it.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_HANDLER_MISMATCH",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_SERVICE_BINDING", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
