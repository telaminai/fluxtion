# Two gaps between a generated processor and a runnable one

Both found on 2026-09-14 running a generated Mongoose-hosted DataFlow end to end — build, package,
boot the server, push events through the graph. Both are **pre-existing**: each reproduces identically on
runtime 1.0.13 and 1.0.15, so neither is a regression from the 1.0.15 line.

Neither is caught by a build. The project compiled, packaged and passed its tests in every case below;
the failures appear only when the packaged artifact is actually run.

---

## 1. A registered `Auditor` can live outside `fluxtion-runtime`

`AGENTS.md` states the invariant plainly:

> Keep generated processors dependent on `fluxtion-runtime` only.

`YamlReplayRecordWriter` makes that impossible to honour for any graph that uses it:

- it `implements Auditor` — `fluxtion-builder-api/src/main/java/com/telamin/fluxtion/builder/replay/YamlReplayRecordWriter.java:58`
- it lives in **`fluxtion-builder-api`**, a build-time artifact
- an `Auditor` is a **runtime** concept: registering one makes the generated processor construct it

So a graph doing this — which is what the project starter's generated `MyProcessorBuilder` does:

```java
cfg.addAuditor(new YamlReplayRecordWriter().classWhiteList(PriceUpdate.class),
               YamlReplayRecordWriter.DEFAULT_NAME);
```

produces a processor whose constructor holds the class:

```java
import com.telamin.fluxtion.builder.replay.YamlReplayRecordWriter;
...
public transient final YamlReplayRecordWriter yamlReplayRecordWriter =
        new com.telamin.fluxtion.builder.replay.YamlReplayRecordWriter(clock);
```

### What it costs

Generated projects scope the builder `provided` — correctly, because AOT generation is a build-time
step. The starter's own pom says so:

```xml
<!-- provided: AOT generates the processor at build time,
     none of this is on the runtime classpath. -->
```

The processor therefore cannot construct:

```
java.lang.NoClassDefFoundError: com/telamin/fluxtion/builder/replay/YamlReplayRecordWriter
  at ...generated.MyProcessor.<init>(MyProcessor.java:82)
Caused by: java.lang.ClassNotFoundException: com.telamin.fluxtion.builder.replay.YamlReplayRecordWriter
```

The graph builds, the jar packages, the server boots — and the processor agent dies on first
construction. Adding the builder-api jar to the runtime classpath fixes it, which is how the end-to-end
runs behind this document were obtained; that is a workaround, not the contract.

### Options

1. **Move it to `fluxtion-runtime`.** Correct by the invariant — it is an `Auditor`. It changes the
   package (`...builder.replay` → `...runtime.replay`), so it is a **breaking change for anyone
   importing it directly**. A deprecated forwarding class in the old package would bridge that.
2. **Leave it and document the consequence** — registering this auditor puts `fluxtion-builder-api` on
   the runtime classpath, so a project using it must not scope the builder `provided`. Cheapest, but it
   makes the stated invariant conditional, and the failure stays a runtime surprise.
3. **Make the generator refuse or warn** when a registered auditor's class is not reachable from
   `fluxtion-runtime`. Turns a `NoClassDefFoundError` at boot into a build-time diagnostic. Complements
   either of the above rather than replacing them.

Option 1 is the only one that makes the invariant true. Whatever is chosen, the starter template
registers this auditor by default, so the decision reaches every generated project.

---

## 2. The fat-jar manifest `Add-Opens` is not taking effect

A generated Mongoose project ships a shaded jar whose manifest carries `Add-Opens`, and its README
states that `java -jar` therefore needs no flags. **It does need them.**

```
Exception in thread "main" java.lang.IllegalAccessError:
  class org.agrona.UnsafeApi (in unnamed module @0x...) cannot access class
  jdk.internal.misc.Unsafe (in module java.base) because module java.base does not
  export jdk.internal.misc to unnamed module @0x...
    at org.agrona.UnsafeApi.getUnsafe(UnsafeApi.java)
    at com.telamin.mongoose.internal.AgronaCountersService.<init>(AgronaCountersService.java:67)
```

`fluxtion-runtime` pins **agrona 2.3.0**, whose `UnsafeApi` reaches `jdk.internal.misc.Unsafe`. Any
consumer packaging a fat jar over this runtime meets the same requirement.

### What was ruled out

The obvious explanations do not hold. The manifest is **well-formed**:

```
 5 len= 72 b'Add-Opens: java.base/jdk.internal.misc=ALL-UNNAMED java.base/java.lang.r'
 6 len= 72 b' eflect=ALL-UNNAMED java.base/java.io=ALL-UNNAMED java.base/java.nio=ALL'
 7 len= 72 b' -UNNAMED java.base/sun.nio.ch=ALL-UNNAMED java.base/jdk.internal.ref=AL'
 8 len= 50 b' L-UNNAMED java.base/jdk.internal.util=ALL-UNNAMED'
```

Correct 72-byte folding with leading-space continuations, in the main section, single `MANIFEST.MF`
entry, not a multi-release jar. And the A/B is clean — same jar, same JDK 21, same `-jar` launch:

| launch | result |
|---|---|
| `java -jar app.jar` (manifest only) | `IllegalAccessError` |
| `java --add-opens … -jar app.jar` (identical values, explicit) | boots, runs, 0 errors |

So the values are right and the manifest is right; the JVM is not applying them. **Why is not
established** — worth an isolated reproduction before choosing a fix, rather than guessing.

### What this means today

- The launcher script with explicit flags is the **only** supported way to run a generated bundle. Any
  documentation claiming `java -jar` suffices is wrong and should be corrected.
- Note `--add-opens` is what works here. The error text says *export*, and JEP 261 also defines an
  `Add-Exports` manifest attribute — but since `Add-Opens` is already being ignored, there is no reason
  to assume `Add-Exports` would be honoured. Test before relying on it.

---

## Why a build never catches either

Worth stating on its own, because it is the shared cause:

| stage | gap 1 | gap 2 |
|---|---|---|
| compile | passes — builder-api is on the **compile** classpath | passes |
| package | passes | passes |
| unit tests | pass — they build a graph, they do not boot a server | pass |
| **run** | `NoClassDefFoundError` | `IllegalAccessError` |

A test that constructs the generated processor **with the runtime classpath only**, and a smoke test
that boots the packaged jar the way a user launches it, would each turn one of these from a user-visible
runtime failure into a build failure.
