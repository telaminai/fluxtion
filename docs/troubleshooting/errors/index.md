# Fluxtion compiler diagnostics

Every diagnostic the compiler emits carries a stable code and a `documentationUrl` pointing at one of these pages.

| code | severity | what it means |
|---|---|---|
| [`FLX-1001`](FLX-1001.md) | ERROR | Constructor parameters must correspond to mapped fields, in order. |
| [`FLX-1002`](FLX-1002.md) | ERROR | An @ExportService method must return void, or boolean to control propagation. |
| [`FLX-1005`](FLX-1005.md) | ERROR | Every node has a unique name in the generated processor. |
| [`FLX-1007`](FLX-1007.md) | ERROR | The dependency graph must be acyclic. |
| [`FLX-1008`](FLX-1008.md) | WARN | A graph with audit-capable nodes and no event audit records nothing they log. |
| [`FLX-1009`](FLX-1009.md) | ERROR | Every constructor-mapped field must be accepted by a constructor, or explicitly excluded from generated-source reproduction. |
| [`FLX-1021`](FLX-1021.md) | ERROR | Checked-in generated source must be regenerated when the graph model changes. |
| [`SPRING_BEAN_NOT_SELECTED`](SPRING_BEAN_NOT_SELECTED.md) | WARN | With a nodeBeans allowlist in force, a bean that is neither listed nor ignored is excluded from the graph. |
| [`SPRING_CONFIG_LOG_LEVEL_CONFLICT`](SPRING_CONFIG_LOG_LEVEL_CONFLICT.md) | ERROR | Every FluxtionSpringConfig that declares a non-null logLevel must declare the same one. |
| [`SPRING_HANDLER_MISMATCH`](SPRING_HANDLER_MISMATCH.md) | ERROR | An event-handler binding must name a node whose handler surface accepts the declared event type. |
| [`SPRING_UNDECLARED_SERVICE_CALLBACK`](SPRING_UNDECLARED_SERVICE_CALLBACK.md) | ERROR | Under strictServiceBindings every service callback a selected node registers must be declared in the Spring configuration. |

## Scope, and the "it worked yesterday" report

Fluxtion builds a graph three ways — interpreted, in-process and AOT. Some rejections belong only to rendering a node as Java source, so an AOT build asks one question the others do not: *can this node be written out?* A graph developed interpreted can therefore meet a rejection for the first time at AOT build. Each page states its own scope.

---

*These pages are generated from the compiler's own diagnostics, so each one shows exactly the text a build emits.*
