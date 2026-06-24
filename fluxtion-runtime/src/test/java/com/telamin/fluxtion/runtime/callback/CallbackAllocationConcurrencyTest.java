package com.telamin.fluxtion.runtime.callback;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CallbackAllocationConcurrencyTest {

    @After
    public void resetCounter() {
        AbstractCallbackNode.instanceFilterCounter = 0;
    }

    @Test
    public void abstractCallbackNodeFilterIdsAreUniqueUnderConcurrentConstruction() throws Exception {
        int threadCount = 8;
        int nodesPerThread = 128;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<Integer>>> futures = new ArrayList<>();
        AbstractCallbackNode.instanceFilterCounter = 0;

        for (int thread = 0; thread < threadCount; thread++) {
            futures.add(executor.submit(() -> {
                start.await();
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < nodesPerThread; i++) {
                    ids.add(new TestCallbackNode().filterId());
                }
                return ids;
            }));
        }

        start.countDown();
        Set<Integer> ids = new HashSet<>();
        try {
            for (Future<List<Integer>> future : futures) {
                ids.addAll(future.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(ids.size(), is(threadCount * nodesPerThread));
    }

    @Test
    public void instanceCallbackEventAllocationIsThreadLocal() throws Exception {
        int threadCount = 4;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Class<? extends InstanceCallbackEvent>>> futures = new ArrayList<>();

        for (int thread = 0; thread < threadCount; thread++) {
            futures.add(executor.submit(() -> {
                start.await();
                InstanceCallbackEvent.reset();
                return InstanceCallbackEvent.nextCallBackEvent().getClass();
            }));
        }

        start.countDown();
        try {
            for (Future<Class<? extends InstanceCallbackEvent>> future : futures) {
                assertThat(future.get().getName(), is(InstanceCallbackEvent.InstanceCallbackEvent_0.class.getName()));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static class TestCallbackNode extends AbstractCallbackNode<Object> {
    }
}
