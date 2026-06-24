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

## 1. Getting an API Key

To use the Fluxtion cloud-hosted AOT compiler, you need a RapidAPI key.

1.  Visit [RapidAPI](https://rapidapi.com/).
2.  Search for **Fluxtion Source Generator**.
3.  Subscribe to a plan (free tier is available for testing).
4.  Navigate to the **Endpoints** tab in the RapidAPI dashboard for the Fluxtion API.
5.  Locate the **X-RapidAPI-Key** field—this is your API secret.
6.  Copy this key and paste it into your `fluxtion.apiKeyFile`.

### Configuration

By default, the fluxtion client looks for a configuration file at:
`~/.fluxtion/fluxtion.apiKeyFile`

The location of this file can be overridden by setting the following system property:
`-Dfluxtion.apiKeyFile=/path/to/your/config.file`

Create the configuration file with the following content:

```properties
# Fluxtion configuration
host=https://fluxtion-source-gen.p.rapidapi.com
port=443
apiKey=YOUR_RAPIDAPI_KEY_HERE
```

Replace `YOUR_RAPIDAPI_KEY_HERE` with the actual key you obtained from RapidAPI.

---

## 2. AOT Workflow

Create a simple AOT-compiled event pipeline. Save this as `aot/quickstart/Ex1.java`.

> **Note**: AOT compilation requires classes to be in a named package so that the generated code can import them correctly. 

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}, com.telamin.fluxtion:fluxtion-generator-restclient:1.4.1
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

When you call `Fluxtion.compileAot`, the following steps occur:

1.  **Analysis**: Fluxtion analyzes the relationship between your nodes (`StringHandler` in this case).
2.  **Remote Generation**: The analysis is sent to the cloud-hosted Fluxtion generator using your RapidAPI key.
3.  **Code Generation**: The generator produces optimized Java source code.
4.  **Local Compilation**: The generated source is compiled locally and loaded into your JVM.
5.  **Artifact Generation**: By default, several artifacts are created in your project:
    *   `src/main/java/aot/quickstart/generated/StringProcessor.java` - The generated event processor.
    *   `src/main/resources/aot/quickstart/generated/StringProcessor.graphml` - A visual representation of the event graph.
    *   `src/main/resources/aot/quickstart/generated/StringProcessor.png` - A PNG image of the event graph.

The generated `StringProcessor.java` is a plain Java class that you can inspect and even bundle with your application, removing the need for `fluxtion-builder` and `fluxtion-generator-restclient` at runtime.

---


The basic `EventHandlerNode` is just the start. Fluxtion allows for more complex graphs, conditional execution, and declarative configuration using annotations.


## 3. Implementing `TriggeredNode`

React to parent updates programmatically by implementing the `TriggeredNode` interface. Save this as `aot/quickstart/Ex2.java`.

```java
//REPOS central=https://repo1.maven.org/maven2,fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.telamin.fluxtion:fluxtion-builder:0.9.17, com.telamin.fluxtion:fluxtion-generator-restclient:1.4.1
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
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
//DEPS com.telamin.fluxtion:fluxtion-builder:{{fluxtion_version}}
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
