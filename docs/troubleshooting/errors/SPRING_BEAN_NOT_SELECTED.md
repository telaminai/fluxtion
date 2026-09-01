# SPRING_BEAN_NOT_SELECTED

> these Spring beans are not part of the graph: [orphanBean]

| | |
|---|---|
| **Severity** | WARN |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_BEAN` |

## The rule

With a nodeBeans allowlist in force, a bean that is neither listed nor ignored is excluded from the graph.

## Why the compiler says this

nodeBeans is non-empty, so Fluxtion treats it as an allowlist: only the beans it names become nodes. The beans listed here are in neither nodeBeans nor ignoredBeans, so they were left out — silently, because being absent from a list is not an error the compiler could otherwise notice.

## How to fix it

Add each bean to nodeBeans if it should be part of the graph, or to ignoredBeans to record that leaving it out is deliberate. Listing it as ignored is how an intentional omission stops being reported.

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

    <bean id="stringHandler" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$StringHandler"/>

    <!-- Declared, in neither list, and referenced by nothing: silently excluded. -->
    <bean id="orphanBean" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$OrphanBean"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans"><list><value>stringHandler</value></list></property>
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

    <bean id="stringHandler" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$StringHandler"/>
    <bean id="orphanBean" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$OrphanBean"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <!-- Either list it, so it joins the graph... -->
        <property name="nodeBeans">
            <list>
                <value>stringHandler</value>
                <value>orphanBean</value>
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
  "code": "SPRING_BEAN_NOT_SELECTED",
  "severity": "WARN",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_BEAN", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
