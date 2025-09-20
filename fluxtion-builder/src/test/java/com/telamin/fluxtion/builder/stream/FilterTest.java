package com.telamin.fluxtion.builder.stream;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.flowfunction.FlowBuilder;
import com.telamin.fluxtion.builder.stream.EventStreamBuildTest.FilterConfig;
import com.telamin.fluxtion.builder.stream.EventStreamBuildTest.MyData;
import com.telamin.fluxtion.builder.stream.EventStreamBuildTest.NotifyAndPushTarget;
import com.telamin.fluxtion.builder.test.util.MultipleSepTargetInProcessTest;
import com.telamin.fluxtion.builder.test.util.SepTestConfig;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Mappers;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Predicates;
import com.telamin.fluxtion.runtime.node.SingleNamedNode;
import com.telamin.fluxtion.runtime.util.MutableNumber;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Test;

import static com.telamin.fluxtion.builder.DataFlowBuilder.subscribe;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FilterTest extends MultipleSepTargetInProcessTest {

    public FilterTest(SepTestConfig compiledSep) {
        super(compiledSep);
    }

    @Test
    public void filterTest() {
        sep(c -> subscribe(String.class)
                .filter(FilterTest::isTrue)
                .notify(new NotifyAndPushTarget())
        );
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("86");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("true");
        assertThat(notifyTarget.getOnEventCount(), is(1));
    }

    @Test
    public void filterInstanceNoArgumentTest() {
        sep(c -> subscribe(String.class)
                .filter(new FilterNoArgs("filter")::isValid)
                .notify(new NotifyAndPushTarget())
        );
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("86");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("true");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        getField("filter", FilterNoArgs.class).setValid(true);
        onEvent("86");
        assertThat(notifyTarget.getOnEventCount(), is(1));
        onEvent("true");
        assertThat(notifyTarget.getOnEventCount(), is(2));
    }

    @Test
    public void filterNoArgumentTest() {
        sep(c -> subscribe(String.class)
                .filter(FilterTest::alwaysTrue)
                .notify(new NotifyAndPushTarget())
        );
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("86");
        assertThat(notifyTarget.getOnEventCount(), is(1));
        onEvent("true");
        assertThat(notifyTarget.getOnEventCount(), is(2));
    }

    @Test
    public void filterByPropertyTest() {
        sep(c -> subscribe(String.class)
                .filterByProperty(String::length, FilterTest::gt5)
                .notify(new NotifyAndPushTarget())
        );
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("short");
        assertThat(notifyTarget.getOnEventCount(), is(0));
        onEvent("loooong");
        assertThat(notifyTarget.getOnEventCount(), is(1));
    }

    @Test
    public void dynamicFilterTest() {
        sep(c -> subscribe(MyData.class)
                .filter(EventStreamBuildTest::myDataTooBig, subscribe(FilterConfig.class))
                .map(MyData::getValue)
                .push(new NotifyAndPushTarget()::setIntPushValue));
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        onEvent(new FilterConfig(10));
        onEvent(new MyData(5));
        assertThat(notifyTarget.getIntPushValue(), is(0));
        assertThat(notifyTarget.getOnEventCount(), is(0));

        onEvent(new MyData(50));
        assertThat(notifyTarget.getIntPushValue(), is(50));
        assertThat(notifyTarget.getOnEventCount(), is(1));
    }

    @Test
    public void dynamicFilterByPropertyTest() {
        sep(c -> subscribe(MyData.class)
                .filterByProperty(EventStreamBuildTest::myDataIntTooBig, MyData::getValue, subscribe(FilterConfig.class))
                .map(MyData::getValue)
                .push(new NotifyAndPushTarget()::setIntPushValue));
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        onEvent(new FilterConfig(10));
        onEvent(new MyData(5));
        assertThat(notifyTarget.getIntPushValue(), is(0));
        assertThat(notifyTarget.getOnEventCount(), is(0));

        onEvent(new MyData(50));
        assertThat(notifyTarget.getIntPushValue(), is(50));
        assertThat(notifyTarget.getOnEventCount(), is(1));
    }

    @Test
    public void dynamicFilterWithDefaultValueTest() {
        sep(c -> subscribe(MyData.class)
                .filter(EventStreamBuildTest::myDataTooBig,
                        subscribe(FilterConfig.class).defaultValue(new FilterConfig(4)))
                .map(MyData::getValue)
                .push(new NotifyAndPushTarget()::setIntPushValue));
        NotifyAndPushTarget notifyTarget = getField("notifyTarget");
        onEvent(new MyData(5));
        assertThat(notifyTarget.getIntPushValue(), is(5));
        assertThat(notifyTarget.getOnEventCount(), is(1));

        onEvent(new FilterConfig(10));
        onEvent(new MyData(5));
        assertThat(notifyTarget.getIntPushValue(), is(5));
        assertThat(notifyTarget.getOnEventCount(), is(1));

        onEvent(new MyData(50));
        assertThat(notifyTarget.getIntPushValue(), is(50));
        assertThat(notifyTarget.getOnEventCount(), is(2));
    }

    @Test
    public void filterDynamicWithPrimitiveIntPropertyTest() {
        sep(c -> {
            FlowBuilder<MutableNumber> numberStream = DataFlowBuilder.subscribe(MutableNumber.class);
            numberStream.filterByProperty(
                            Predicates::greaterThanInt, MutableNumber::intValue, DataFlowBuilder.subscribeToIntSignal("number"))
                    .mapToInt(Mappers.count())
                    .id("count");

        });

        publishIntSignal("number", 100);
        onEvent(MutableNumber.fromLong(50));
        onEvent(MutableNumber.fromLong(150));
        onEvent(MutableNumber.fromLong(500));
        onEvent(MutableNumber.fromLong(50));

        Assert.assertEquals(2, (int) getStreamed("count"));
    }

    @Test
    public void filterDynamicWithPrimitiveDoublePropertyTest() {
        sep(c -> {
            FlowBuilder<MutableNumber> numberStream = DataFlowBuilder.subscribe(MutableNumber.class);
            numberStream.filterByProperty(
                            Predicates::greaterThanDouble, MutableNumber::doubleValue, DataFlowBuilder.subscribeToDoubleSignal("number"))
                    .mapToInt(Mappers.count())
                    .id("count");

        });

        publishDoubleSignal("number", 100);
        onEvent(MutableNumber.fromLong(50));
        onEvent(MutableNumber.fromLong(150));
        onEvent(MutableNumber.fromLong(500));
        onEvent(MutableNumber.fromLong(50));

        Assert.assertEquals(2, (int) getStreamed("count"));
    }

    @Test
    public void filterDynamicWithPrimitiveLongPropertyTest() {
        sep(c -> {
            FlowBuilder<MutableNumber> numberStream = DataFlowBuilder.subscribe(MutableNumber.class);
            numberStream.filterByProperty(
                            Predicates::greaterThanLong, MutableNumber::longValue, DataFlowBuilder.subscribeToLongSignal("number"))
                    .mapToInt(Mappers.count())
                    .id("count");

        });

        publishLongSignal("number", 100);
        onEvent(MutableNumber.fromLong(50));
        onEvent(MutableNumber.fromLong(150));
        onEvent(MutableNumber.fromLong(500));
        onEvent(MutableNumber.fromLong(50));

        Assert.assertEquals(2, (int) getStreamed("count"));
    }

    @Test
    public void filterFunctionWithPrimitiveArgumentTest() {
        sep(c -> {
            FlowBuilder<MutableNumber> numberStream = DataFlowBuilder.subscribe(MutableNumber.class);
            numberStream.filter(
                            FilterTest::filterMutableNumber, DataFlowBuilder.subscribeToIntSignal("number"))
                    .mapToInt(Mappers.count())
                    .id("count");

        });

        publishIntSignal("number", 100);
        onEvent(MutableNumber.fromLong(50));
        onEvent(MutableNumber.fromLong(150));
        onEvent(MutableNumber.fromLong(500));
        onEvent(MutableNumber.fromLong(50));

        Assert.assertEquals(2, (int) getStreamed("count"));
    }

    @Setter
    @Getter
    public static class FilterNoArgs extends SingleNamedNode {
        private boolean valid = false;

        public FilterNoArgs(@AssignToField("name") String name) {
            super(name);
        }

    }

    public static boolean filterMutableNumber(MutableNumber number, int check) {
        return number.intValue() > check;
    }

    public static boolean isTrue(String in) {
        return Boolean.parseBoolean(in);
    }

    public static boolean alwaysTrue() {
        return true;
    }

    public static boolean gt5(int val) {
        return val > 5;
    }
}
