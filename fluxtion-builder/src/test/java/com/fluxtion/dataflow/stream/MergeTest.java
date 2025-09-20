package com.fluxtion.dataflow.stream;

import com.fluxtion.dataflow.builder.DataFlowBuilder;
import com.fluxtion.dataflow.test.util.MultipleSepTargetInProcessTest;
import com.fluxtion.dataflow.test.util.SepTestConfig;
import org.junit.Test;

import java.util.concurrent.atomic.LongAdder;

import static com.fluxtion.dataflow.builder.DataFlowBuilder.subscribe;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MergeTest extends MultipleSepTargetInProcessTest {
    public MergeTest(SepTestConfig testConfig) {
        super(testConfig);
    }

    @Test
    public void mergeTest() {
        LongAdder adder = new LongAdder();
        sep(c -> subscribe(Long.class)
                .merge(subscribe(String.class).map(EventStreamBuildTest::parseLong))
                .sink("integers"));
        addSink("integers", adder::add);
        onEvent(200L);
        onEvent("300");
        assertThat(adder.intValue(), is(500));
    }

    @Test
    public void mergeTestStaticMethod() {
        LongAdder adder = new LongAdder();
        sep(c ->
                DataFlowBuilder.merge(
                                subscribe(Long.class),
                                subscribe(String.class).map(EventStreamBuildTest::parseLong)
                        )
                        .sink("integers"));
        addSink("integers", adder::add);
        onEvent(200L);
        onEvent("300");
        assertThat(adder.intValue(), is(500));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeTestVarArgStaticMethod() {
        LongAdder adder = new LongAdder();
        sep(c ->
                DataFlowBuilder.merge(
                                subscribe(Long.class),
                                subscribe(String.class).map(EventStreamBuildTest::parseLong),
                                subscribe(Integer.class).map(Integer::longValue)
                        )
                        .sink("integers"));
        addSink("integers", adder::add);
        onEvent(200L);
        onEvent("300");
        onEvent(500);
        assertThat(adder.intValue(), is(1_000));
    }
}
