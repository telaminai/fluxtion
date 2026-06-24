# Console sink formatting tokens
---

The `console(String format)` sink prints values and optional timing information to standard out. It’s handy when
learning the API and for quick visibility in tests.

Supported placeholders:

| Token | Description                                              |
|-------|----------------------------------------------------------|
| {}    | Replaced with the actual output value using `toString()` |
| %e    | Current event time                                       |
| %t    | Current wall clock time                                  |
| %p    | Current process time                                     |
| %de   | Delta between current event time and initial event time  |
| %dt   | Delta between current wall clock time and initial time   |
| %dp   | Delta between current process time and initial time      |

Notes on time/clock

- `%dt` requires a clock to be registered with the DataFlow. If no clock is registered, it will behave as zero or not
  update depending on the runtime setup.
- Registering a clock:
  ```java
  import static com.telamin.fluxtion.runtime.time.ClockStrategy.registerClockEvent;
  
  dataFlow.onEvent(registerClockEvent(() -> System.currentTimeMillis()));
  ```
- In unit tests that extend Fluxtion’s test utilities, prefer the provided methods to control time deterministically:
  `setTime(...)`, `advanceTime(...)`, `tick()`.

Recommended usage

- Prefer `{}` in documentation and tutorials for consistency.
- Use `%dt` when explaining time‑based behavior, windowing, or latency deltas.

Example

```java
DataFlow df = DataFlowBuilder
        .subscribe(String.class)
        .map(String::toUpperCase)
        .console("msg:{} | dt:%dt")
        .build();

// Register a clock for %dt
import static com.telamin.fluxtion.runtime.time.ClockStrategy.registerClockEvent;

df.onEvent(registerClockEvent(() ->System.currentTimeMillis()));
df.onEvent("a");   // msg:A | dt:0 (first event)
df.onEvent("b");   // msg:B | dt:... (time since previous)
```