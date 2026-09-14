# SPRING_CONFIG_LOG_LEVEL_CONFLICT

> conflicting FluxtionSpringConfig logLevel values 'INFO' and 'DEBUG'

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_CONFIG` |

## The rule

Every FluxtionSpringConfig that declares a non-null logLevel must declare the same one.

## Why the compiler says this

The log level is a property of the PROCESSOR, not of a config bean, so two beans asking for different levels have no answer between them. Picking one silently would make the audit regime depend on bean ordering, and an audit regime nobody chose is worse than a build that stops.

## How to fix it

Set logLevel on one config bean, or set the same value on both. Leaving it null on the others is how a bean says it has no opinion.

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

    <!-- Two config beans, each declaring a DIFFERENT non-null log level. -->
    <bean id="configA" class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans"><list><value>stringHandler</value></list></property>
        <property name="logLevel" value="INFO"/>
    </bean>

    <bean id="configB" class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="logLevel" value="DEBUG"/>
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

    <!-- One audit regime for the graph: declare the level once. -->
    <bean id="configA" class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans"><list><value>stringHandler</value></list></property>
        <property name="logLevel" value="INFO"/>
    </bean>
</beans>
```

## Which builds raise it

**Spring configuration analysis**, on every build path that loads a Spring context. Non-Spring builds never raise it.

## Reading it programmatically

This diagnostic is also written to the machine-readable report — the opt-in sidecar (`-Dfluxtion.diagnostics.sidecar=true`), or `FluxtionDiagnostics.capture(...)` for a caller that wants the objects:

```json
{
  "code": "SPRING_CONFIG_LOG_LEVEL_CONFLICT",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_CONFIG", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
