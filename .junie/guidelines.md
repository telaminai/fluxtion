Fluxtion developer guidelines

Scope
- This document captures project-specific build, test, and development tips for the Fluxtion codebase (multi-module Maven project: fluxtion-runtime, fluxtion-builder).
- Audience: experienced Java developers. It omits generic Maven/Java material and focuses on Fluxtion-specific details and gotchas.

Prerequisites
- JDK: Java 21 toolchain. The parent POM sets maven.compiler.source/target to 21 and enforces Java >=17 via maven-enforcer.
- Maven: Use the provided Maven Wrapper (./mvnw) to guarantee plugin versions.
- OS: Any with a working Java 21 + Maven environment.

Repository layout
- Root Maven parent: pom.xml (packaging=pom). Modules:
  - fluxtion-runtime: core runtime for executing Fluxtion data streams.
  - fluxtion-builder: builder DSL and helpers for constructing data flows; depends on fluxtion-runtime.
- Documentation: mkdocs.yml with content under docs/. Not necessary for building the libraries.

Build and configuration
- Quick build (compile + package all modules):
  - ./mvnw -q -DskipTests package
- Full build with tests:
  - ./mvnw -q test
  - The parent configures surefire 3.2.5 with: --add-opens java.base/jdk.internal.misc=ALL-UNNAMED (needed by some dependencies). No extra flags required locally.
- Building a single module:
  - Compile runtime only: ./mvnw -q -pl fluxtion-runtime -am -DskipTests package
  - Compile builder (and its dependency runtime): ./mvnw -q -pl fluxtion-builder -am -DskipTests package
- Release profile:
  - A release profile exists that signs artifacts with maven-gpg-plugin. Do not activate it locally unless you have GPG set up. Normal dev builds do not require it.

Dependencies and shading
- fluxtion-runtime shades org.agrona via maven-shade-plugin and relocates packages to com.fluxtion.agrona. When debugging packaged jars or classpath conflicts, be aware of this relocation.

Testing
- Frameworks: JUnit 4.13.2 + Hamcrest 1.3 (declared in the parent pom’s dependencyManagement). Tests use org.junit.Test annotations and Hamcrest assertions.
- Test execution basics:
  - Run all tests across modules: ./mvnw -q test
  - Run tests for a specific module (builder), compiling upstream dependencies automatically:
    - ./mvnw -q -pl fluxtion-builder -am -Dsurefire.failIfNoSpecifiedTests=false test
      - Note: The -am flag builds dependent modules (fluxtion-runtime). If a module has no tests, surefire can fail when a -Dtest filter is present. Use -Dsurefire.failIfNoSpecifiedTests=false to suppress failures in modules without matching tests.
  - Run a single test class in builder (verified locally):
    - ./mvnw -q -pl fluxtion-builder -am -Dtest=com.telamin.fluxtion.builder.stream.ConsoleTest -Dsurefire.failIfNoSpecifiedTests=false test
      - This compiles fluxtion-runtime first, then runs ConsoleTest under fluxtion-builder.
  - Run a single test method (example):
    - ./mvnw -q -pl fluxtion-builder -am -Dtest=PrimitiveStreamBuilderTest#intTest -Dsurefire.failIfNoSpecifiedTests=false test

- Adding new tests:
  - Place tests under {module}/src/test/java in the appropriate package. The project uses plain JUnit 4; no Spring/JUnit5 extensions are used.
  - Preferred assertion style is Hamcrest (MatcherAssert.assertThat + matchers). JUnit Assert is used in a few places.
  - Common test scaffolding:
    - Many builder tests extend com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest, which provides utilities to:
      - Build a SEP (Stream Event Processor) via sep(c -> ...)
      - Drive events via onEvent(value)
      - Control test time via setTime(...) and advanceTime(...)
      - Register sinks via addIntSink/addLongSink/etc.
      - Access graph nodes/fields via getField(name)
    - Constructor pattern for such tests uses SepTestConfig injection:
      - public MyTest(SepTestConfig config) { super(config); }
  - Example minimal test (pattern in existing code ConsoleTest):
    - Extend MultipleSepTargetInProcessTest
    - Build a simple graph using DataFlowBuilder.subscribe(...)
    - Drive events and assert sink state or captured output.

- Creating and running a simple test (demonstrated flow):
  - We validated running the existing ConsoleTest to demonstrate the process:
    - Command executed successfully: ./mvnw -q -pl fluxtion-builder -am -Dtest=com.telamin.fluxtion.builder.stream.ConsoleTest -Dsurefire.failIfNoSpecifiedTests=false test
  - To add your own example, follow the pattern above, then run it with the same command shape by replacing the -Dtest target.

Logging in tests
- Parent declares slf4j-simple (test scope) for simple console logging. Some tests also use logcaptor and system-rules to capture output. If you add logging assertions, prefer these utilities.

Code style and conventions
- Java 21 language level. Use var and modern switch where it aids readability; keep APIs compatible with current module code.
- Nullability: IntelliJ @NotNull/@Nullable annotations are present in code; honor them in new code.
- Packages: Keep new runtime code in com.telamin.fluxtion.runtime.*, and builder code in com.telamin.fluxtion.builder.*.
- Immutability where practical; stateful nodes/components are explicitly modeled in the graph.

Performance and debugging tips
- Since fluxtion-runtime shades Agrona, profiling may show com.fluxtion.agrona.* rather than org.agrona.*.
- The surefire argLine opens jdk.internal.misc. If you run tests outside Maven (IDE), mirror this setting in your IDE run configuration if you encounter IllegalAccess errors.
- For large graph tests, keeping -q is helpful to reduce noise; drop -q for verbose troubleshooting.

Docs site (optional)
- MkDocs config is present (mkdocs.yml), with content in docs/. This is independent of the core build; you don’t need to build docs to work on the libraries.

Known gotchas
- Running a filtered test on a single module with -am: ensure -Dsurefire.failIfNoSpecifiedTests=false, or Maven may fail when upstream/downstream modules have no matching tests.
- The release profile uses GPG signing; avoid activating it locally unless you have GPG configured.

Verification summary for these guidelines
- Verified Java/Maven build via wrapper.
- Verified targeted test execution in fluxtion-builder using the exact command included above.
