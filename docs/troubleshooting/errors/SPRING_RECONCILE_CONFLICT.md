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

## Common cases

Each case below is a different refusal under this code, with its own fix line; the Java and the record entry a fix names are computed from your project.

### A class withdrawn from the XML is still recorded as starter-owned

**Why:** No bean in the XML declares RiskCheck any more. Its recorded source has been removed. fluxtion-authoring.json still records RiskCheck under "ownership" as starter-generated code, and reconciliation never drops that record or deletes owned code by itself.

**Fix:** Run `java -jar .fluxtion/fluxtion-starter-core.jar withdraw --class 'com.example.myapp.node.RiskCheck'` (starter 1.0.79 and later), then re-run. It removes the "com.example.myapp.node.RiskCheck" entry from "ownership" in fluxtion-authoring.json and changes no Java source; afterwards the starter leaves RiskCheck alone, so keep RiskCheck.java as ordinary code or delete it. Editing the XML or the Java again will not clear this. No source files or authoring record were written.

### A withdrawn member is still recorded as starter-owned

**Why:** Cannot remove a field with changed initialization, source references, or no disposable baseline. fluxtion-authoring.json still records field:rootNode under "ownership" as starter-generated code, and reconciliation never drops that record or deletes owned code by itself.

**Fix:** Run `java -jar .fluxtion/fluxtion-starter-core.jar withdraw --class 'com.example.myapp.node.RiskCheck' --member 'field:rootNode'` (starter 1.0.79 and later), then re-run. It removes the "field:rootNode" key under "ownership" -&gt; "com.example.myapp.node.RiskCheck" in fluxtion-authoring.json and changes no Java source; the member becomes ordinary code the starter no longer manages, so make the Java match the XML yourself (for a withdrawn reference, remove the field, its constructor parameter or setter, and its uses). Editing the XML or the Java again will not clear this. No source files or authoring record were written.

### A class still declared as a bean is not selected in nodeBeans

**Why:** Its owned field rootNode cannot be removed safely (it has source references, changed initialization, or no disposable baseline). The XML declares RiskCheck as bean riskCheck but does not list it in nodeBeans, so it is not in the graph and its owned code would have to be withdrawn.

**Fix:** To keep it in the graph, add `<value>riskCheck</value>` to the nodeBeans list of FluxtionSpringConfig, then re-run. To take it out of the graph, delete the bean element (id riskCheck) from the XML, then run `java -jar .fluxtion/fluxtion-starter-core.jar withdraw --class 'com.example.myapp.node.RiskCheck'` (starter 1.0.79 and later) and re-run; that removes only its "ownership" entry in fluxtion-authoring.json. No source files or authoring record were written.

### An owned class's source was removed and the classpath supplies the class

**Why:** Recorded source src/main/java/com/example/myapp/node/RiskCheck.java has been removed, and the resolved classpath supplies a class named com.example.myapp.node.RiskCheck. fluxtion-authoring.json still records RiskCheck as starter-owned, so reconciliation would generate a new src/main/java/com/example/myapp/node/RiskCheck.java that shadows the supplied class.

**Fix:** Either restore src/main/java/com/example/myapp/node/RiskCheck.java to keep RiskCheck starter-owned, or run `java -jar .fluxtion/fluxtion-starter-core.jar withdraw --class 'com.example.myapp.node.RiskCheck' --still-declared` (starter 1.0.79 and later) to stop owning it (the XML keeps its bean), then re-run: the class is then used as the classpath supplies it (a supplier node, referenced as delivered and never reconciled). No source files or authoring record were written.

### The XML adds a reference to an existing class

**Why:** Cannot invent constructor/property wiring in an existing class. The XML adds a reference to RiskCheck, which already exists; reconciliation writes constructor and setter wiring only in a class it creates, because an existing constructor is developer code.

**Fix:** Write the reference yourself in the starter's form, then re-run: reconciliation finds it and adopts it as written. Add the field `private final com.example.myapp.node.CsvRejections csvRejections;`, give the constructor one parameter per &lt;constructor-arg&gt;, in constructor-argument order: `public RiskCheck(RootNode rootNode, com.example.myapp.node.CsvRejections csvRejections)`, and assign it in the constructor body: `this.csvRejections = csvRejections;`. No source files or authoring record were written.

### A hand-written field lacks the annotation a reference mode implies

**Why:** Unowned construct contradicts the declaration. In PriceMonitor, the field rootNode is hand-written: fluxtion-authoring.json records no starter-owned annotations for it, so reconciliation adopts it only as written and never edits it. The starter adds these annotations itself only to members it generated.

**Fix:** Edit PriceMonitor.java yourself, then re-run; reconciliation then adopts it as written: add `import com.telamin.fluxtion.runtime.annotations.NoTriggerReference;`, and write the field rootNode as `@NoTriggerReference private final RootNode rootNode;`. Or change the XML declaration to match the Java as written. No source files or authoring record were written.

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
