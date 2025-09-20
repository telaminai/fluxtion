package com.telamin.fluxtion.builder.stream;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.flowfunction.JoinFlowBuilder;
import com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest;
import com.telamin.fluxtion.builder.test.util.SepTestConfig;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.util.MutableNumber;
import lombok.Value;
import org.junit.Assert;
import org.junit.Test;

public class ResetJoinTest extends MultipleSepTargetInProcessTest {
    public ResetJoinTest(SepTestConfig testConfig) {
        super(testConfig);
    }

    @Test
    public void resetJoin() {
        MutableNumber mutableNumber = new MutableNumber();
        sep(c -> {
            JoinFlowBuilder.innerJoin(
                            DataFlowBuilder.groupBy(LeftData::getName),
                            DataFlowBuilder.groupBy(RightData::getName)
                    )
                    .resetTrigger(DataFlowBuilder.subscribeToSignal("reset"))
                    .sink("joined");
        });
        addSink("joined", (GroupBy g) -> mutableNumber.set(g.toMap().size()));

        onEvent(new LeftData("greg", 47));
        Assert.assertEquals(0, mutableNumber.intValue());

        onEvent(new RightData("greg", "UK"));
        Assert.assertEquals(1, mutableNumber.intValue());

        onEvent(new RightData("Bill", "UK"));
        Assert.assertEquals(1, mutableNumber.intValue());

        onEvent(new LeftData("Bill", 28));
        Assert.assertEquals(2, mutableNumber.intValue());
        //

        publishSignal("reset");
        Assert.assertEquals(0, mutableNumber.intValue());
        onEvent(new LeftData("greg", 47));
        onEvent(new RightData("greg", "UK"));
    }

    @Value
    public static class LeftData {
        String name;
        int age;
    }

    @Value
    public static class RightData {
        String name;
        String country;
    }
}
