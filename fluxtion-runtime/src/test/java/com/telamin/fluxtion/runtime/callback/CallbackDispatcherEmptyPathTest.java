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

    // ---- W2: what the empty-path early return depends on ------------------------------------

    /**
     * The early return skips {@code dispatching = false}, which the old code executed
     * unconditionally. That is only safe if a nested call cannot reach the early return WHILE an
     * outer dispatch is running — because {@code dispatching} decides whether
     * {@link CallbackDispatcherImpl#fireIteratorCallback} queues to the front or the back.
     *
     * <p>It cannot: the in-flight item is removed only AFTER its {@code dispatch()} returns false,
     * so the queue is never empty from inside a callback. This test pins that premise; if a future
     * change removes the item first, the early return silently becomes a reordering bug and this
     * fails rather than the ordering drifting unnoticed.
     */
    @Test
    public void theInFlightItemStaysOnTheQueueDuringItsOwnDispatch() {
        boolean[] seenEmpty = {true};
        dispatcher.queueReentrantEvent("only");
        processor.onDispatch = e -> seenEmpty[0] = dispatcher.myStack.isEmpty();
        dispatcher.dispatchQueuedCallbacks();
        assertThat("a nested dispatchQueuedCallbacks() must never observe an empty queue — "
                        + "this is what makes the W2 early return behaviour-preserving",
                seenEmpty[0], is(false));
    }

    /** The contract {@code dispatching} exists for, and which the early return must not disturb. */
    @Test
    public void fireIteratorCallbackDuringDispatchGoesToTheFrontOfTheQueue() {
        dispatcher.queueReentrantEvent("outer");
        dispatcher.queueReentrantEvent("tail");
        processor.onDispatch = e -> {
            if ("outer".equals(e)) {
                List<String> items = new ArrayList<>();
                items.add("i1");
                items.add("i2");
                dispatcher.fireIteratorCallback(7, items.iterator());
            }
        };
        dispatcher.dispatchQueuedCallbacks();
        assertThat("dispatching==true must put the iterator ahead of already-queued work",
                processor.dispatched, is(listOf("outer", "i1", "i2", "tail")));
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
        /** run on every dispatch, so a test can act from INSIDE an in-flight callback */
        java.util.function.Consumer<Object> onDispatch = e -> {/*NoOp*/};

        @Override
        public void onEvent(Object event) {
            record(event);
        }

        @Override
        public void onEventInternal(Object event) {
            record(event);
        }

        /** iterator callbacks arrive wrapped; record the payload so tests read as the caller wrote it */
        private void record(Object event) {
            Object payload = event instanceof CallbackEvent ? ((CallbackEvent<?>) event).getData() : event;
            dispatched.add(payload);
            onDispatch.accept(payload);
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
