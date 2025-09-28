# Filtering
---

## Filtering events
User events can implement [Event]({{fluxtion_src_runtime}}/event/Event.java), which provides an optional filtering 
field. Event handlers can specify the filter value, so they only see events with matching filters

```java
public static class MyNode {
    @OnEventHandler(filterString = "CLEAR_SIGNAL")
    public boolean allClear(Signal<String> signalToProcess) {
        System.out.println("allClear [" + signalToProcess + "]");
        return true;
    }

    @OnEventHandler(filterString = "ALERT_SIGNAL")
    public boolean alertSignal(Signal<String> signalToProcess) {
        System.out.println("alertSignal [" + signalToProcess + "]");
        return true;
    }

    @OnEventHandler()
    public boolean anySignal(Signal<String> signalToProcess) {
        System.out.println("anySignal [" + signalToProcess + "]");
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new MyNode());
    processor.init();
    processor.onEvent(new Signal<>("ALERT_SIGNAL", "power failure"));
    System.out.println();
    processor.onEvent(new Signal<>("CLEAR_SIGNAL", "power restored"));
    System.out.println();
    processor.onEvent(new Signal<>("HEARTBEAT_SIGNAL", "heartbeat message"));
}
```

Output
```console
alertSignal [Signal: {filterString: ALERT_SIGNAL, value: power failure}]
anySignal [Signal: {filterString: ALERT_SIGNAL, value: power failure}]

allClear [Signal: {filterString: CLEAR_SIGNAL, value: power restored}]
anySignal [Signal: {filterString: CLEAR_SIGNAL, value: power restored}]

anySignal [Signal: {filterString: HEARTBEAT_SIGNAL, value: heartbeat message}]
```

## Filter variables
The filter value on the event handler method can be extracted from an instance field in the class. Annotate the event
handler method with an attribute that points to the filter variable `@OnEventHandler(filterVariable = "[class variable]")`

```java
public static class MyNode {
    private final String name;

    public MyNode(String name) {
        this.name = name;
    }


    @OnEventHandler(filterVariable = "name")
    public boolean handleIntSignal(Signal.IntSignal intSignal) {
        System.out.printf("MyNode-%s::handleIntSignal - %s%n", name, intSignal.getValue());
        return true;
    }
}

public static void main(String[] args) {
    var processor = Fluxtion.interpret(new MyNode("A"), new MyNode("B"));
    processor.init();

    processor.publishIntSignal("A", 22);
    processor.publishIntSignal("B", 45);
    processor.publishIntSignal("C", 100);
}
```

Output
```console
MyNode-A::handleIntSignal - 22
MyNode-B::handleIntSignal - 45
```
