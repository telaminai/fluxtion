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

## Example

Both files below are executed against the real Spring adapter on every build, so the first is guaranteed to produce this diagnostic and the second to build cleanly.

### The configuration that causes it

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
    http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- This node declares NO @OnEventHandler at all. -->
    <bean id="deafNode"
          class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$DeafNode"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans">
            <list><value>deafNode</value></list>
        </property>
        <property name="eventTypes">
            <list><value>java.lang.String</value></list>
        </property>
        <property name="eventHandlers">
            <list>
                <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig$EventHandlerBinding">
                    <property name="event" value="java.lang.String"/>
                    <property name="nodeBeans">
                        <!-- deafNode cannot accept a String: the route would never fire -->
                        <list><value>deafNode</value></list>
                    </property>
                </bean>
            </list>
        </property>
    </bean>
</beans>
```

### The configuration that fixes it

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
    http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="stringHandler"
          class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$StringHandler"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans">
            <list><value>stringHandler</value></list>
        </property>
        <property name="eventTypes">
            <list><value>java.lang.String</value></list>
        </property>
        <property name="eventHandlers">
            <list>
                <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig$EventHandlerBinding">
                    <!-- the event the node actually declares an @OnEventHandler for -->
                    <property name="event" value="java.lang.String"/>
                    <property name="nodeBeans">
                        <list><value>stringHandler</value></list>
                    </property>
                </bean>
            </list>
        </property>
    </bean>
</beans>
```

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
