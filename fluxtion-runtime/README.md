# fluxtion-runtime

The runtime library that executes a Fluxtion event processor.

A Fluxtion processor is generated ahead-of-time as a plain `.java` source file (the
"dispatcher"). At runtime that dispatcher is compiled to a regular class and dispatches
events through a statically resolved object graph — no reflection on the hot path. This
module is the only library the generated dispatcher needs at runtime.

## Coordinates

```xml
<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-runtime</artifactId>
    <version>${fluxtion.version}</version>
</dependency>
```

## What's in the jar

- The Fluxtion event-dispatch runtime (`EventProcessor`, `StaticEventProcessor`, lifecycle).
- Annotations consumed by the generated dispatcher (`@OnEventHandler`, `@OnTrigger`,
  `@ExportService`, `@AfterEvent`, `@Initialise`, etc.).
- Audit / replay / event-log infrastructure.

agrona is **not bundled** — it's declared as a normal compile-scope transitive
dependency (default: `org.agrona:agrona:2.3.0`). Consumers see the standard
`org.agrona.*` package; nothing relocated.

## Requirements

- **JDK at runtime:** 17 or later (with the default agrona 2.x).
- **Bytecode:** class file 52 (Java 8) for fluxtion classes themselves; the agrona 2.x
  transitive is class file 61 — hence the JDK 17 floor.
- **add-opens:** agrona uses `jdk.internal.misc.Unsafe` in both 1.x and 2.x; add
  `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED` on JDK 17+ to silence
  reflective-access warnings.

## Overriding agrona

The default transitive (`agrona:2.3.0`) targets JDK 17+. If your consumer is Java 8 /
CheerpJ, you need agrona 1.21.2 (the last class file 52 release of agrona):

```xml
<dependency>
    <groupId>com.telamin.fluxtion</groupId>
    <artifactId>fluxtion-runtime</artifactId>
    <version>${fluxtion.version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.agrona</groupId>
            <artifactId>agrona</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.agrona</groupId>
    <artifactId>agrona</artifactId>
    <version>1.21.2</version>
</dependency>
```

fluxtion-runtime's compiled bytecode is binary-compatible with both agrona 1.21.2 and
2.x for the surface it touches (verified across 5 constructors + 14 methods, May 2026).
No source changes needed in your code — the swap is transparent.

If you need the all-in-one Java 8 / CheerpJ fat jar (builder + runtime + Retrolambda-processed
deps), see `fluxtion-builder-all-java8` — it bundles the agrona-override for you.

## Building from source

```
mvn -pl fluxtion-runtime -am clean install
```

The build enforces Java 8 stdlib compatibility via `animal-sniffer-maven-plugin` so
accidental `String.repeat` / `List.of` / `Optional.isEmpty` calls fail the build
rather than `NoSuchMethodError` on a Java 8 consumer.

## License

GNU Affero General Public License v3. See `LICENSE`.
