# SPRING_UNDECLARED_SERVICE_CALLBACK

> Spring bean 'pricingNode' registers service 'com.example.PricingService named 'primary'' but strict service bindings do not declare it

| | |
|---|---|
| **Severity** | ERROR |
| **Category** | SPRING_CONFIG |
| **Element** | `SPRING_SERVICE_BINDING` |

## The rule

Under strictServiceBindings every service callback a selected node registers must be declared in the Spring configuration.

## Why the compiler says this

strictServiceBindings makes the Spring file the authority on which services a graph binds. This node registers one the file does not mention, so the file no longer describes the graph — which is the single thing strict mode exists to guarantee. Without strict mode this is a normal arrangement and is not reported.

## How to fix it

Declare the service on this node in the Spring configuration, or turn off strictServiceBindings if the file is not meant to be exhaustive.

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

    <bean id="declaredRegistrar" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$DeclaredRegistrar"/>
    <!-- Also registers PricingService, but is not listed in the binding below. -->
    <bean id="undeclaredRegistrar" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$UndeclaredRegistrar"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans">
            <list>
                <value>declaredRegistrar</value>
                <value>undeclaredRegistrar</value>
            </list>
        </property>
        <!-- The service TYPE is declared. It is supplied at runtime, so it is NOT a bean. -->
        <property name="serviceTypes">
            <list><value>com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$PricingService</value></list>
        </property>
        <!-- strict mode: the file must name every node that registers a declared service -->
        <property name="strictServiceBindings" value="true"/>
        <property name="serviceRegistrations">
            <list>
                <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig$ServiceBinding">
                    <property name="service" value="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$PricingService"/>
                    <property name="nodeBeans">
                        <list><value>declaredRegistrar</value></list>
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

    <bean id="declaredRegistrar" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$DeclaredRegistrar"/>
    <!-- Registers PricingService, and is now listed in the binding below. -->
    <bean id="undeclaredRegistrar" class="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$UndeclaredRegistrar"/>

    <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig">
        <property name="nodeBeans">
            <list>
                <value>declaredRegistrar</value>
                <value>undeclaredRegistrar</value>
            </list>
        </property>
        <!-- The service TYPE is declared. It is supplied at runtime, so it is NOT a bean. -->
        <property name="serviceTypes">
            <list><value>com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$PricingService</value></list>
        </property>
        <!-- strict mode: the file must name every node that registers a declared service -->
        <property name="strictServiceBindings" value="true"/>
        <property name="serviceRegistrations">
            <list>
                <bean class="com.telamin.fluxtion.builder.extern.spring.FluxtionSpringConfig$ServiceBinding">
                    <property name="service" value="com.telamin.fluxtion.builder.extern.spring.errors.SpringErrorFixtures$PricingService"/>
                    <property name="nodeBeans">
                        <list>
                            <value>declaredRegistrar</value>
                            <value>undeclaredRegistrar</value>
                        </list>
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
  "code": "SPRING_UNDECLARED_SERVICE_CALLBACK",
  "severity": "ERROR",
  "category": "SPRING_CONFIG",
  "element": { "kind": "SPRING_SERVICE_BINDING", … }
}
```

Select findings by `severity`, never by position: a failing build's report also contains any warnings it found, and `diagnostics[0]` is not the cause of the failure.

---

*This page is generated from the compiler itself, so the wording above is exactly what a build emits. Do not edit it by hand — it is overwritten whenever the diagnostics are regenerated.*
