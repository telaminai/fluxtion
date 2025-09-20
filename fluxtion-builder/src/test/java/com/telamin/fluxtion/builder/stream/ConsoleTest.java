package com.telamin.fluxtion.builder.stream;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest;
import com.telamin.fluxtion.builder.test.util.SepTestConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

public class ConsoleTest extends MultipleSepTargetInProcessTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    public ConsoleTest(SepTestConfig testConfig) {
        super(testConfig);
    }

    @Test
    public void testConsole() {
        systemOutRule.muteForSuccessfulTests();
        sep(c -> {
            DataFlowBuilder.subscribe(String.class).console("deltaTime:%dt");
        });
        setTime(0);
        onEvent("TEST");
        Assert.assertEquals("deltaTime:0", systemOutRule.getLog().trim());

        systemOutRule.clearLog();
        setTime(100);
        onEvent("TEST");
        Assert.assertEquals("deltaTime:100", systemOutRule.getLog().trim());

        systemOutRule.clearLog();
        advanceTime(250);
        onEvent("TEST");
        Assert.assertEquals("deltaTime:350", systemOutRule.getLog().trim());
    }
}
