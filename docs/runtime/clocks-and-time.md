# Working with clocks and time at runtime
---

This guide explains how to read and control time in a running DataFlow graph using the fluxtion-runtime Clock and a
custom clock strategy, with live code and the actual console output from the reference example.

Source: [ClockExample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/runtime/clock/ClockExample.java)

## Example 

```java
import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import com.telamin.fluxtion.runtime.time.Clock;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.LongAdder;

public class ClockExample {

    public static void main(String[] args) {
        var processor = DataFlowBuilder
                .subscribeToNode(new TimeLogger())
                .build();

        //PRINT CURRENT TIME
        processor.onEvent(DateFormat.getDateTimeInstance());

        //USE A SYNTHETIC STRATEGY TO SET TIME FOR THE PROCESSOR CLOCK
        LongAdder syntheticTime = new LongAdder();
        processor.setClockStrategy(syntheticTime::longValue);

        //SET A NEW TIME - GOING BACK IN TIME!!
        syntheticTime.add(1_000_000_000);
        processor.onEvent(DateFormat.getDateTimeInstance());

        //SET A NEW TIME - BACK TO THE FUTURE
        syntheticTime.add(1_800_000_000_000L);
        processor.onEvent(DateFormat.getDateTimeInstance());
    }


    public static class TimeLogger {
        public Clock wallClock = Clock.DEFAULT_CLOCK;

        @OnEventHandler
        public boolean publishTime(DateFormat dateFormat) {
            System.out.println("time " 
                    + dateFormat.format(new Date(wallClock.getWallClockTime())));
            return true;
        }
    }
}
```

Console output when running the example:

```console
time 29 Sept 2025, 11:45:58
time 12 Jan 1970, 14:46:40
time 26 Jan 2027, 21:46:40
```

What’s happening:

- A node (TimeLogger) reads time from the runtime clock and prints a formatted date using a DateFormat provided as the
  event.
- Initially, the processor uses the default wall clock (system time), so the first line shows “now”.
- The example then sets a synthetic clock strategy via<br/> `processor.setClockStrategy(syntheticTime::longValue)`<br/> allowing
  tests or simulations to control time deterministically.
- Adding 1,000,000,000 milliseconds moves the synthetic clock to about 11.6 days after the epoch (1970-01-12), producing
  the second line.
- Adding 1,800,000,000,000 milliseconds advances to a future instant, producing the third line.

Tips:

- Use Clock.DEFAULT_CLOCK for real time, or supply a custom strategy for reproducible tests and simulations.
- Any component can access the clock via injection or by holding a Clock reference as shown.
- Feed different DateFormat instances (or other formatting logic) as events to control how timestamps are presented.
