# Fluxtion AOT Quickstart

This guide introduces Fluxtion through an Ahead-of-Time (AOT) workflow. Unlike the dynamic builder, the AOT workflow generates highly optimized Java source code for an event processor at compile time.

Fluxtion infers the execution pipeline from the relationships between components, and the AOT compiler produces a specialized class that implements your business logic with zero overhead.

---

## Install JBang

Examples use **JBang** so they can run without creating a full project.

Install JBang:

```bash
# macOS / Linux
curl -Ls https://sh.jbang.dev | bash

# Windows (powershell)
iex "& { $(iwr https://ps.jbang.dev) }"
```

More details: [https://www.jbang.dev](https://www.jbang.dev)

---

<a id="1-getting-an-api-key"></a>

## 1. Sign in to the compiler

!!! note "Generation is remote and needs a compiler sign-in"
    {{fluxtion_remote_build_note}} {{fluxtion_sign_in_line}}

The hosted Fluxtion compiler generates your processor for a signed-in account. A new account starts with a
one-time allowance of compile and interpret credits. There are three ways to sign in:

**On your machine, with Maven.** Sign in once with the Fluxtion Maven plugin (1.3.1 or later), giving it the
compiler's address. This command works from any directory:

```bash
mvn com.telamin.fluxtion:fluxtion-maven-plugin:{{fluxtion_maven_plugin_version}}:login -Dfluxtion.compilerUrl={{fluxtion_compiler_url}}
```

The goal prints a short code and an address. Open the address, enter the code and approve; the plugin stores a
credential for this machine and prints your plan and remaining credits. Inside a project that declares the
plugin, `mvn fluxtion:login -Dfluxtion.compilerUrl={{fluxtion_compiler_url}}` is the short form.
`mvn fluxtion:logout` revokes and removes the credential. Maven builds that run the plugin's generation goals
(see [Quick start (Maven)](quickstart-maven.md)) then use it; they need `fluxtion-builder` 1.0.82 or later on
the build classpath.

**In CI (Maven plugin builds).** CI never signs in interactively, and `fluxtion:login` refuses to run there. An account administrator
creates an automation credential; store it in the CI secret store and expose it to the build as
`FLUXTION_API_TOKEN`, with the compiler's address in `FLUXTION_COMPILER_URL`:

```yaml
# GitHub Actions
env:
  FLUXTION_API_TOKEN: ${{ '{{' }} secrets.FLUXTION_API_TOKEN {{ '}}' }}
  FLUXTION_COMPILER_URL: {{fluxtion_compiler_url}}
```

`FLUXTION_COMPILER_URL` is read by the Maven plugin only; the builder on its own does not read it.

Never put a credential in a POM or a committed file: the plugin refuses a build that does.

**In the browser.** The [Fluxtion playground](https://fluxtion-playground.dev) builds and runs graphs for a
signed-in account (Google, Microsoft or GitHub), with nothing to install.

**JBang and plain `main` programs** (the examples on this page) call the builder directly, without the Maven
plugin. With fluxtion-builder 1.0.89 the JBang and plain-`main` examples need a compiler token in
`FLUXTION_API_TOKEN` (with `FLUXTION_REMOTE_AUTH=token` and
`-Dfluxtion.remote.host={{fluxtion_compiler_url}}`), because this builder does not read the Maven sign-in. A
personal token cannot yet be issued self-service, so until fluxtion-builder 1.0.90 use the
[Maven quick start](quickstart-maven.md) or the playground. A credential is sent only over https, or plain http to
this machine.

!!! warning "The API-key route is retired"
    The earlier route, a marketplace API key kept in a key file under `~/.fluxtion`, was retired on
    2026-11-05. Signing in is the only way to generate. From `fluxtion-builder` 1.0.90 key files are no longer
    read. Links in older builders' messages still land on this section.

What a build sends is listed in the README section
[Cloud generation and your compiler sign-in](https://github.com/telaminai/fluxtion#cloud-generation-and-your-compiler-sign-in).

---

## 2. AOT Workflow

Create a simple AOT-compiled event pipeline. Save this as `aot/quickstart/Ex1.java`.

> **Note**: AOT compilation requires classes to be in a named package so that the generated code can import them correctly. 

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.node.EventHandlerNode;

public class Ex1 {
    void main(String[] args) {
        // Compile and load the processor AOT
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "StringProcessor",
                new StringHandler());

        dataFlow.init();

        dataFlow.onEvent("Hello, World!");
    }

    public static class StringHandler implements EventHandlerNode<String> {
        @Override
        public boolean onEvent(String event) {
            System.out.println("Received event: " + event);
            return true;
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex1.java
```

Output:

```text
Received event: Hello, World!
```

---

### What happens during AOT compilation?

!!! tip "Tuning the generated processor"
    The `compileAot(node1, node2)` form above takes node instances. To change how the processor is
    *generated* — dropping dirty flags, auditors or the node-name map for lower latency — use the
    config-builder overload instead: see
    [Setting a profile in your build](../reference/performance-profiles.md#setting-a-profile-in-your-build).

When you call `Fluxtion.compileAot`, the following steps occur:

1.  **Analysis**: Fluxtion analyzes the relationship between your nodes (`StringHandler` in this case).
2.  **Remote Generation**: The analysis is sent to the cloud-hosted Fluxtion compiler under your compiler sign-in.
3.  **Code Generation**: The generator produces optimized Java source code.
4.  **Local Compilation**: The generated source is compiled locally and loaded into your JVM.
5.  **Artifact Generation**: By default, several artifacts are created in your project:
    *   `src/main/java/aot/quickstart/generated/StringProcessor.java` - The generated event processor.
    *   `src/main/resources/aot/quickstart/generated/StringProcessor.graphml` - A visual representation of the event graph.
    *   `src/main/resources/aot/quickstart/generated/StringProcessor.png` - A PNG image of the event graph.

The generated `StringProcessor.java` is a plain Java class that you can inspect and even bundle with your application, removing the need for `fluxtion-builder` at runtime.

---


The basic `EventHandlerNode` is just the start. Fluxtion allows for more complex graphs, conditional execution, and declarative configuration using annotations.


## 3. Implementing `TriggeredNode`

React to parent updates programmatically by implementing the `TriggeredNode` interface. Save this as `aot/quickstart/Ex2.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.node.EventHandlerNode;
import com.telamin.fluxtion.runtime.node.TriggeredNode;

public class Ex2 {
    void main(String[] args) {
        // Compile and load the processor AOT
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "Ex2Processor",
                new TriggeredByParent(new StringHandler()));

        dataFlow.init();

        dataFlow.onEvent("Hello, World!");
    }

    public static class StringHandler implements EventHandlerNode<String> {
        @Override
        public boolean onEvent(String event) {
            System.out.println("Received event: " + event);
            return true;
        }
    }

    public static class TriggeredByParent implements TriggeredNode {
        private final StringHandler parent;

        public TriggeredByParent(StringHandler parent) {
            this.parent = parent;
        }

        @Override
        public boolean triggered() {
            System.out.println("triggering child node");
            return false;
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex2.java
```

Output:

```text
Received event: Hello, World!
triggering child node
```

## 4. Using Annotations

Use declarative annotations for cleaner code and flexible event handling. Save this as `aot/quickstart/Ex3.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;

public class Ex3 {
    void main(String[] args) {
        // Compile and load the processor AOT
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "Ex3Processor",
                new TriggeredByParent(new StringHandler()));

        dataFlow.init();

        dataFlow.onEvent("Hello, World!");
    }

    public static class StringHandler {
        @OnEventHandler
        public boolean onEvent(String event) {
            System.out.println("Received event: " + event);
            return true;
        }
    }

    public static class TriggeredByParent  {
        private final StringHandler parent;

        public TriggeredByParent(StringHandler parent) {
            this.parent = parent;
        }

        @OnTrigger
        public boolean triggered() {
            System.out.println("triggering child node");
            return false;
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex3.java
```

Output:

```text
Received event: Hello, World!
triggering child node
```



## 5. Multiple Parents and `DataFlowBuilder.push`

Nodes can have multiple parents. Use `DataFlowBuilder.push` to explicitly connect multiple streams to a single method in a target node. The `push` operation only triggers the target node once all its parent streams have received at least one update. Save this as `aot/quickstart/Ex4.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.builder.DataFlowBuilder;

public class Ex4 {
    void main(String[] args) {
        // Compile and load the processor AOT using the builder API
        var dataFlow = Fluxtion.compileAot(
                c -> {
                    var stringFlow = DataFlowBuilder.subscribe(String.class);
                    var intFlow = DataFlowBuilder.subscribe(Integer.class);

                    DataFlowBuilder.push(MultiParentNode::update, stringFlow, intFlow);
                },
                "aot.quickstart.generated",
                "MultiProcessor"
        );

        dataFlow.init();

        dataFlow.onEvent("The answer is: ");
        dataFlow.onEvent(42);
    }

    public static class MultiParentNode {
        public void update(String s, Integer i) {
            System.out.println("Push target received: " + s + i);
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex4.java
```

Output:

```text
Push target received: The answer is: 42
```

## 6. Filtering

You can filter events based on static strings, integers, or even classes. Events must implement the `com.telamin.fluxtion.runtime.event.Event` interface to provide filtering metadata. Save this as `aot/quickstart/Ex5.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.event.Event;

public class Ex5 {
    void main(String[] args) {
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "FilterProcessor",
                new FilteredHandler());

        dataFlow.init();

        dataFlow.onEvent(new MyEvent("important", "Critical Update"));
        dataFlow.onEvent(new MyEvent("noisy", "Irrelevant Data"));
    }

    public static class MyEvent implements Event {
        private final String filter;
        private final String data;
        public MyEvent(String filter, String data) { 
            this.filter = filter; 
            this.data = data;
        }
        @Override public String filterString() { return filter; }
        public String getData() { return data; }
    }

    public static class FilteredHandler {
        @OnEventHandler(filterString = "important")
        public void handleImportant(MyEvent event) {
            System.out.println("Processing important event: " + event.getData());
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex5.java
```

Output:

```text
Processing important event: Critical Update
```

## 7. Dirty Strategy

The `@OnTrigger` annotation has a `dirty` property. By default, it's `true`, meaning the method is called if **any** parent signals a change. If set to `false`, it's called only if **no** change was signaled by parents. Note that dirty and non-dirty trigger methods cannot be in the same class. Save this as `aot/quickstart/Ex6.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;

public class Ex6 {
    void main(String[] args) {
        InputHandler handler = new InputHandler();
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "DirtyProcessor",
                new NegativeFilter(handler),
                new PositiveFilter(handler)
        );

        dataFlow.init();

        System.out.println("Sending changing event:");
        dataFlow.onEvent("change");

        System.out.println("\nSending non-changing event:");
        dataFlow.onEvent("stable");
    }

    public static class InputHandler {
        @OnEventHandler
        public boolean onEvent(String event) {
            return event.equals("change");
        }
    }

    public static class PositiveFilter {
        private final InputHandler parent;
        public PositiveFilter(InputHandler parent) { this.parent = parent; }

        @OnTrigger
        public void onDirty() {
            System.out.println("PositiveFilter Triggered: Parent signaled a change");
        }
    }

    public static class NegativeFilter {
        private final InputHandler parent;
        public NegativeFilter(InputHandler parent) { this.parent = parent; }

        @OnTrigger(dirty = false)
        public void onNotDirty() {
            System.out.println("NegativeFilter Triggered: Parent did NOT signal a change");
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex6.java
```

Output:

```text
Sending changing event:
PositiveFilter Triggered: Parent signaled a change

Sending non-changing event:
NegativeFilter Triggered: Parent did NOT signal a change
```

## 8. Identifying Parents

Fluxtion automatically identifies parents by analyzing constructors or fields. If you use a constructor that takes other nodes as arguments, Fluxtion treats those as parents. Save this as `aot/quickstart/Ex7.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_builder_version}}
//JAVA 25

package aot.quickstart;

import com.telamin.fluxtion.Fluxtion;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;
import com.telamin.fluxtion.runtime.annotations.OnTrigger;

public class Ex7 {
     void main(String[] args) {
        var dataFlow = Fluxtion.compileAot(
                "aot.quickstart.generated",
                "ParentChildProcessor",
                new ChildNode(new ParentNodeInteger(), new ParentNodeString()));

        dataFlow.init();

        dataFlow.onEvent(100);
        dataFlow.onEvent("hello world");
    }

    public static class ParentNodeInteger {
        private int value;
        @OnEventHandler
        public void onEvent(Integer i) { this.value = i; }
        public int getValue() { return value; }
    }

    public static class ParentNodeString {
        private String value;
        @OnEventHandler
        public void onEvent(String i) { this.value = i; }
        public String getValue() { return value; }
    }

    public static class ChildNode {
        private final ParentNodeInteger parentNodeInt;
        private final ParentNodeString parentNodeString;
        public ChildNode(ParentNodeInteger parent, ParentNodeString parentNodeString) {
            this.parentNodeInt = parent;
            this.parentNodeString = parentNodeString;
        }

        @OnParentUpdate
        public void onParentUpdate(ParentNodeInteger parent) {
            System.out.println("ParentNodeInteger updated value:" + parentNodeInt.getValue());
        }

        @OnParentUpdate
        public void onParentUpdate(ParentNodeString parentNodeString) {
            System.out.println("ParentNodeString updated value:" + parentNodeString.getValue());
        }

        @OnTrigger
        public void onTrigger() {
            System.out.println("Child triggered");
        }
    }
}
```

Run:

```bash
jbang aot/quickstart/Ex7.java
```

Output:

```text
ParentNodeInteger updated value:100
Child triggered
ParentNodeString updated value:hello world
Child triggered
```

---

# Next steps

After completing this guide you can explore:

* advanced AOT configuration
* bundling generated code for production
* visualising complex event graphs
* performance comparison between dynamic and AOT processors

See the main **Fluxtion documentation** for deeper examples and architecture details.
