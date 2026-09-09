package com.telamin.fluxtion.runtime.flowfunction.aggregate.function.primitive;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * An aggregate wrapper must name the method it actually calls.
 *
 * <p>The double and long wrappers were copied from the int one and kept its audit name, so every double
 * or long aggregate wrote {@code "->aggregateInt"} into the audit log while calling
 * {@code aggregateDouble} or {@code aggregateLong}.
 *
 * <p>Harmless to the computation and invisible from inside Java: the string is only ever read out of an
 * audit log, and a log claiming {@code aggregateInt} for a double sum looks entirely reasonable until
 * something else disagrees. It was found by a C++ target deriving the name from the wrapper it was
 * emitting — the two logs matched on every value and differed on one line.
 */
public class AggregateWrapperAuditNameTest {

    @Test
    public void eachWrapperNamesItsOwnAggregateMethod() throws Exception {
        assertEquals("IntSumFlowFunction->aggregateInt",
                auditInfo(new AggregateIntFlowFunctionWrapper<>(null, IntSumFlowFunction::new)));
        assertEquals("DoubleSumFlowFunction->aggregateDouble",
                auditInfo(new AggregateDoubleFlowFunctionWrapper<>(null, DoubleSumFlowFunction::new)));
        assertEquals("LongSumFlowFunction->aggregateLong",
                auditInfo(new AggregateLongFlowFunctionWrapper<>(null, LongSumFlowFunction::new)));
    }

    private static String auditInfo(Object wrapper) throws Exception {
        for (Class<?> c = wrapper.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField("auditInfo");
                f.setAccessible(true);
                return (String) f.get(wrapper);
            } catch (NoSuchFieldException ignored) {
                // keep walking
            }
        }
        throw new AssertionError("no auditInfo on " + wrapper.getClass());
    }
}
