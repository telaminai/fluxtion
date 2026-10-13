# Running host code in an event cycle
---

`DataFlow.runInEventCycle(Object auditEvent, Runnable action)` (since 1.1.0) runs a host's own code as one event
cycle of a processor. It is for code that must act on the graph but is not an event the graph handles. The typical
case is an operator command in a server, such as an admin command that resets an alarm or reloads a table. The
command then behaves like an event:

- its node audit-log lines land in a record of their own, named by `auditEvent`, instead of splicing into the next
  event's record;
- the clock gives the command a proper instant;
- anything the command raises through `onEvent` is **queued**, and dispatched after it as its own cycle, never
  re-entered mid-command.

`auditEvent` is the cycle's audit context only. It is **dispatched to no node** and marks nothing dirty, so nothing
downstream runs because of the command itself. If the graph should react, the action raises an event.

## Example

```java
import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;

public class RunInEventCycleExample {

    /** The command's audit context: it names the cycle in the audit log. */
    public record OperatorCommand(String name) { }

    public static class AlarmMonitor {
        public boolean raised;

        @OnEventHandler
        public boolean onReading(Double value) {
            raised = value > 10;
            System.out.println("reading " + value + " raised=" + raised);
            return true;
        }

        /** Not an event handler: the host calls it inside runInEventCycle. */
        public void reset(String by) {
            raised = false;
            System.out.println("reset by " + by);
        }
    }

    public static void main(String[] args) {
        AlarmMonitor monitor = new AlarmMonitor();
        var processor = DataFlowBuilder.subscribeToNode(monitor).build();

        processor.onEvent(12.0);
        processor.runInEventCycle(new OperatorCommand("alarm.reset"), () -> {
            monitor.reset("DEMO-operator");
            processor.onEvent(5.0);             // queued: dispatched after the action, as its own cycle
            System.out.println("action done");
        });
    }
}
```

Console output:

```console
reading 12.0 raised=true
reset by DEMO-operator
action done
reading 5.0 raised=false
```

The reading of 5.0 arrives inside the action, but it runs only after the action has finished and its cycle has
closed. With audit logging on, the command's cycle is a record of its own, named
`OperatorCommand[name=alarm.reset]`, between the records of the two readings. The audit-log lines of any
`EventLogNode` written inside the action are in that record.

## What happens, in order

1. A buffered calculation, if any is pending, runs first as its own cycle, so its record is closed before the
   command's opens.
2. The processor is marked as processing, so events the action raises are queued, as re-entrant events are.
3. Every auditor is told `auditEvent` was received: the clock takes the cycle's instant, and the audit log opens a
   record naming it.
4. The action runs.
5. In a `finally`, so also when the action throws, the cycle closes (event-end methods, the auditors' completion,
   dirty flags reset) and the queued events are dispatched.
6. The processing mark is cleared innermost, so a throw cannot leave the processor refusing later events.

## The clock

The command's cycle follows the same clock contract as the event path (see [Clocks and time](clocks-and-time.md)):

| `auditEvent` | auditors see | `clock.getEventTime()` inside the action | `clock.getProcessTime()` |
|---|---|---|---|
| implements `Event` | `eventReceived(Event)` | the context's own `getEventTime()` | the clock strategy's reading |
| any other object | `eventReceived(Object)` | the process time | the clock strategy's reading |

So a host that knows when a command was issued can say so, by passing an `Event` whose `getEventTime()` returns that
instant. Otherwise the command is timed as it runs.

## Rules

- **Not re-entrant.** Called while the processor is processing, for example from inside a node, it throws
  `IllegalStateException`. A node that wants follow-on work raises an event instead.
- **Not thread-safe**, like `onEvent`. Call it on the thread that drives the processor.
- **No new capability.** Anyone holding the processor can already call `onEvent`, its exported services and its
  nodes. What reaches `runInEventCycle` from outside a process (an admin transport, for example) should be a
  registered command name and its arguments, never code.
- **The audit context is written to the log.** Its `toString` appears in the record, so redact secrets in a
  command's arguments.

## Which processors implement it

- **Generated processors** built with a generator that has this feature implement it. That generated source also
  compiles against runtime 1.0.16: there the method is an ordinary public method on the processor class, and from
  1.1.0 it overrides the interface's default.
- **The interpreted processor and `DefaultEventProcessor`** implement it.
- **A processor that predates the method** gets the interface default, which throws
  `UnsupportedOperationException` without running the action. A host that must support such processors catches it,
  or checks for the override, and falls back to its own handling.

## Known limit

`runInEventCycle` adds no failure handling beyond closing its cycle and clearing the processing mark. A queued event
that throws while the cycle drains is handled as any failed event is. Recovery after a failed cycle (what a failed
cycle's queued work and state should become) is being defined in
[issue #36](https://github.com/telaminai/fluxtion/issues/36).
