package com.telamin.fluxtion.runtime.callback;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * M50/W2+W3 — {@code dispatchQueuedCallbacks()} runs on every event, and its EMPTY path used to
 * perform an interface {@code Deque.isEmpty()} plus an unconditional {@code dispatching = false}
 * store. The queue also held {@code Supplier<Boolean>}, boxing on every callback even though every
 * wrapper's {@code dispatch()} returns primitive {@code boolean}.
 *
 * <p>These tests pin the BEHAVIOUR that must be identical after those changes. They are not
 * performance tests — throughput is measured by the conformance bench, which asserts output
 * equivalence before it believes any timing.
 */
public class CallbackDispatcherEmptyPathTest {

    private CallbackDispatcherImpl dispatcher;
    private RecordingProcessor processor;

    @Before
    public void setUp() {
        dispatcher = new CallbackDispatcherImpl();
        processor = new RecordingProcessor();
        dispatcher.setEventProcessor(processor);
    }

    // ---- the empty fast path: the case that runs on every event ---------------------------

    @Test
    public void emptyQueueDispatchesNothing() {
        dispatcher.dispatchQueuedCallbacks();
        assertThat(processor.dispatched.size(), is(0));
    }

    @Test
    public void emptyQueueIsIdempotentAcrossManyEvents() {
        for (int i = 0; i < 1_000; i++) {
            dispatcher.dispatchQueuedCallbacks();
        }
        assertThat(processor.dispatched.size(), is(0));
        assertThat(dispatcher.myStack.isEmpty(), is(true));
    }

    @Test
    public void nullEventProcessorOnAnEmptyQueueIsSafe() {
        CallbackDispatcherImpl orphan = new CallbackDispatcherImpl();
        orphan.dispatchQueuedCallbacks();          // must not throw
        assertThat(orphan.myStack.isEmpty(), is(true));
    }

    @Test
    public void nullEventProcessorWithAQueuedItemDoesNotDispatchAndDoesNotThrow() {
        CallbackDispatcherImpl orphan = new CallbackDispatcherImpl();
        orphan.queueReentrantEvent("held");
        orphan.dispatchQueuedCallbacks();          // must not throw, must not drain
        assertThat("item is retained for a later dispatch", orphan.myStack.isEmpty(), is(false));
    }

    // ---- the non-empty path must be unchanged ---------------------------------------------

    @Test
    public void queuedReentrantEventIsDispatchedAndDrained() {
        dispatcher.queueReentrantEvent("a");
        dispatcher.dispatchQueuedCallbacks();
        assertThat(processor.dispatched, is(listOf("a")));
        assertThat(dispatcher.myStack.isEmpty(), is(true));
    }

    @Test
    public void queuedEventsDispatchInFifoOrder() {
        dispatcher.queueReentrantEvent("first");
        dispatcher.queueReentrantEvent("second");
        dispatcher.dispatchQueuedCallbacks();
        assertThat(processor.dispatched, is(listOf("first", "second")));
    }

    @Test
    public void processReentrantEventGoesToTheFrontOfTheQueue() {
        dispatcher.queueReentrantEvent("queued");
        dispatcher.processReentrantEvent("urgent");
        dispatcher.dispatchQueuedCallbacks();
        assertThat("addFirst semantics preserved", processor.dispatched, is(listOf("urgent", "queued")));
    }

    @Test
    public void iteratingPublishWrapperDrainsEveryElement() {
        List<Object> items = new ArrayList<>();
        items.add("x");
        items.add("y");
        items.add("z");
        dispatcher.processReentrantEvents(items);
        dispatcher.dispatchQueuedCallbacks();
        assertThat(processor.dispatched, is(listOf("x", "y", "z")));
        assertThat(dispatcher.myStack.isEmpty(), is(true));
    }

    @Test
    public void dispatchIsRepeatableAfterDraining() {
        dispatcher.queueReentrantEvent("one");
        dispatcher.dispatchQueuedCallbacks();
        dispatcher.dispatchQueuedCallbacks();       // empty path, must add nothing
        dispatcher.queueReentrantEvent("two");
        dispatcher.dispatchQueuedCallbacks();
        assertThat(processor.dispatched, is(listOf("one", "two")));
    }

    // ---- W3: the queue is primitive-valued, so a callback must not box ---------------------

    @Test
    public void queueHoldsPrimitiveBooleanSuppliers() throws NoSuchFieldException {
        assertThat("W3: Supplier<Boolean> boxed on every callback",
                CallbackDispatcherImpl.class.getDeclaredField("myStack").getType().getName(),
                is("java.util.ArrayDeque"));
        String generic = CallbackDispatcherImpl.class.getDeclaredField("myStack").getGenericType().getTypeName();
        assertThat("W3: element type must be BooleanSupplier, not Supplier<Boolean>",
                generic.contains("BooleanSupplier"), is(true));
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static List<Object> listOf(Object... items) {
        List<Object> l = new ArrayList<>();
        for (Object i : items) {
            l.add(i);
        }
        return l;
    }

    /** Minimal processor that records what was dispatched to it. */
    private static final class RecordingProcessor implements InternalEventProcessor {
        final List<Object> dispatched = new ArrayList<>();

        @Override
        public void onEvent(Object event) {
            dispatched.add(event);
        }

        @Override
        public void onEventInternal(Object event) {
            dispatched.add(event);
        }

        @Override
        public boolean isDirty(Object node) {
            return false;
        }

        @Override
        public java.util.function.BooleanSupplier dirtySupplier(Object node) {
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
