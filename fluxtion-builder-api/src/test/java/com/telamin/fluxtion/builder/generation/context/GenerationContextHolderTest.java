package com.telamin.fluxtion.builder.generation.context;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;

public class GenerationContextHolderTest {

    @Test
    public void nestedScopesRestorePreviousContext() {
        GenerationContext outer = context();
        GenerationContext inner = context();

        try (GenerationContextHolder.Scope ignored = GenerationContextHolder.open(outer)) {
            assertThat(GenerationContextHolder.current(), sameInstance(outer));
            try (GenerationContextHolder.Scope ignoredInner =
                         GenerationContextHolder.open(inner)) {
                assertThat(GenerationContextHolder.current(), sameInstance(inner));
            }
            assertThat(GenerationContextHolder.current(), sameInstance(outer));
        }

        assertThat(GenerationContextHolder.currentOrNull(), nullValue());
    }

    @Test
    public void implicitContextIsReusedThenCleared() {
        GenerationContext first = GenerationContextHolder.ensureInlineContext();
        GenerationContext second = GenerationContextHolder.ensureInlineContext();

        assertThat(second, sameInstance(first));
        GenerationContextHolder.clearInlineContext();
        assertThat(GenerationContextHolder.currentOrNull(), nullValue());
    }

    @Test
    public void clearingInlineContextDoesNotCloseExplicitScope() {
        GenerationContext explicit = context();
        try (GenerationContextHolder.Scope ignored =
                     GenerationContextHolder.open(explicit)) {
            assertThat(
                    GenerationContextHolder.ensureInlineContext(),
                    sameInstance(explicit));
            GenerationContextHolder.clearInlineContext();
            assertThat(GenerationContextHolder.current(), sameInstance(explicit));
        }
        assertThat(GenerationContextHolder.currentOrNull(), nullValue());
    }

    @Test
    public void contextsAreIsolatedAcrossThreads() throws Exception {
        CountDownLatch bothOpened = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<GenerationContext> first = new AtomicReference<GenerationContext>();
        AtomicReference<GenerationContext> second = new AtomicReference<GenerationContext>();

        Thread one = contextThread(first, bothOpened, release);
        Thread two = contextThread(second, bothOpened, release);
        one.start();
        two.start();
        bothOpened.await();
        release.countDown();
        one.join();
        two.join();

        assertThat(first.get(), not(sameInstance(second.get())));
        assertThat(GenerationContextHolder.currentOrNull(), nullValue());
    }

    @Test
    public void scopesCloseInLifoOrder() {
        GenerationContextHolder.Scope outer =
                GenerationContextHolder.open(context());
        GenerationContextHolder.Scope inner =
                GenerationContextHolder.open(context());
        try {
            outer.close();
            fail("Expected an out-of-order close failure");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("LIFO"));
        } finally {
            inner.close();
            outer.close();
        }
    }

    private static Thread contextThread(
            final AtomicReference<GenerationContext> result,
            final CountDownLatch bothOpened,
            final CountDownLatch release) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                GenerationContext context = GenerationContextHolder.ensureInlineContext();
                bothOpened.countDown();
                await(release);
                result.set(context);
                GenerationContextHolder.clearInlineContext();
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static GenerationContext context() {
        return (GenerationContext) Proxy.newProxyInstance(
                GenerationContext.class.getClassLoader(),
                new Class<?>[]{GenerationContext.class},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) {
                        return "test-context";
                    }
                    return null;
                });
    }
}
