package com.telamin.fluxtion.runtime.callback;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * M50/W1 — the generated processor decides whether to drain by reading a field it owns, and this
 * dispatcher is what keeps that field honest.
 *
 * <p><b>The failure this optimisation could introduce is a silently lost callback:</b> queue work,
 * forget to mark, and the processor never drains it. So the last test enumerates the dispatcher's
 * public methods by reflection and fails on any that grows the queue without marking — covering a
 * queueing method added later, which a hand-written list would not.
 */
public class CallbackDispatcherPendingTest {

    private CallbackDispatcherImpl dispatcher;
    private RecordingProcessor processor;

    @Before
    public void setUp() {
        dispatcher = new CallbackDispatcherImpl();
        processor = new RecordingProcessor();
        dispatcher.setEventProcessor(processor);
    }

    @Test
    public void queueingMarksPending() {
        dispatcher.queueReentrantEvent("a");
        assertThat(processor.pending, is(true));
    }

    @Test
    public void drainingToEmptyClearsPending() {
        dispatcher.queueReentrantEvent("a");
        dispatcher.dispatchQueuedCallbacks();
        assertThat("a drained queue must clear the flag, or the processor drains for ever",
                processor.pending, is(false));
    }

    @Test
    public void anEmptyDispatchDoesNotTouchTheFlag() {
        processor.pending = true;
        dispatcher.dispatchQueuedCallbacks();
        assertThat("the early return must not clear a flag it did not act on",
                processor.pending, is(true));
    }

    @Test
    public void aNullProcessorIsSafeToQueueAgainst() {
        CallbackDispatcherImpl orphan = new CallbackDispatcherImpl();
        orphan.queueReentrantEvent("held");     // must not NPE marking a processor that is not set
        assertThat(orphan.myStack.isEmpty(), is(false));
    }

    @Test
    public void eachQueueingMethodMarks() {
        List<String> missed = new ArrayList<>();
        mark("fireCallback(int)", d -> d.fireCallback(1), missed);
        mark("fireCallback(int,T)", d -> d.fireCallback(1, "x"), missed);
        mark("fireIteratorCallback", d -> d.fireIteratorCallback(1, Arrays.asList("a").iterator()), missed);
        mark("processReentrantEvent", d -> d.processReentrantEvent("e"), missed);
        mark("processReentrantEvents", d -> d.processReentrantEvents(Arrays.<Object>asList("e")), missed);
        mark("queueReentrantEvent", d -> d.queueReentrantEvent("e"), missed);
        assertThat("queueing methods that did not mark pending: " + missed, missed.isEmpty(), is(true));
    }

    /** Catches a queueing method added later that forgets to mark — a silently lost callback. */
    @Test
    public void noPublicMethodQueuesWithoutMarking() {
        List<String> unmarked = new ArrayList<>();
        for (Method m : CallbackDispatcherImpl.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || m.isSynthetic()) {
                continue;
            }
            Object[] args = argsFor(m.getParameterTypes());
            if (args == null) {
                continue;
            }
            CallbackDispatcherImpl d = new CallbackDispatcherImpl();
            RecordingProcessor rec = new RecordingProcessor();
            d.setEventProcessor(rec);
            try {
                m.invoke(d, args);
            } catch (Exception ignored) {
                continue;
            }
            if (!d.myStack.isEmpty() && !rec.pending) {
                unmarked.add(m.getName());
            }
        }
        assertThat("queued work without marking — that callback would never be dispatched: " + unmarked,
                unmarked.isEmpty(), is(true));
    }

    private void mark(String name, java.util.function.Consumer<CallbackDispatcherImpl> queueing,
                      List<String> missed) {
        CallbackDispatcherImpl d = new CallbackDispatcherImpl();
        RecordingProcessor rec = new RecordingProcessor();
        d.setEventProcessor(rec);
        queueing.accept(d);
        if (!rec.pending) {
            missed.add(name);
        }
    }

    private static Object[] argsFor(Class<?>[] types) {
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == int.class) {
                args[i] = 1;
            } else if (types[i] == boolean.class) {
                args[i] = Boolean.TRUE;
            } else if (types[i] == java.util.Iterator.class) {
                args[i] = Arrays.asList("a").iterator();
            } else if (types[i] == Iterable.class) {
                args[i] = Arrays.<Object>asList("a");
            } else if (types[i] == Object.class || types[i] == String.class) {
                args[i] = "e";
            } else {
                return null;
            }
        }
        return args;
    }

    private static final class RecordingProcessor implements InternalEventProcessor {
        boolean pending;

        @Override
        public void callbacksPending(boolean p) {
            pending = p;
        }

        @Override
        public void onEvent(Object event) {/*NoOp*/}

        @Override
        public void onEventInternal(Object event) {/*NoOp*/}

        @Override
        public boolean isDirty(Object node) {
            return false;
        }

        @Override
        public BooleanSupplier dirtySupplier(Object node) {
            return () -> false;
        }

        @Override
        public void setDirty(Object node, boolean dirtyFlag) {/*NoOp*/}

        @Override
        public <T> T getNodeById(String id) {
            return null;
        }

        @Override
        public void bufferEvent(Object event) {/*NoOp*/}

        @Override
        public void triggerCalculation() {/*NoOp*/}
    }
}
