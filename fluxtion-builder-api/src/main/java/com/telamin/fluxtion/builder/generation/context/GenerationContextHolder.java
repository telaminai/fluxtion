package com.telamin.fluxtion.builder.generation.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holds generation state for one build thread. Explicit compiler scopes may
 * nest; DSL authoring can create an implicit inline scope that is released by
 * its terminal operation.
 */
public final class GenerationContextHolder {

    private static final ThreadLocal<Deque<Frame>> CONTEXTS =
            new ThreadLocal<Deque<Frame>>() {
                @Override
                protected Deque<Frame> initialValue() {
                    return new ArrayDeque<Frame>();
                }
            };

    private GenerationContextHolder() {
    }

    public static Scope open(GenerationContext context) {
        Frame frame = new Frame(Objects.requireNonNull(context, "context"), false);
        CONTEXTS.get().push(frame);
        return new Scope(Thread.currentThread(), frame);
    }

    public static GenerationContext ensureInlineContext() {
        GenerationContext current = currentOrNull();
        if (current != null) {
            return current;
        }
        GenerationContext inlineContext = new InlineGenerationContext(
                Thread.currentThread().getContextClassLoader());
        CONTEXTS.get().push(new Frame(inlineContext, true));
        return inlineContext;
    }

    public static void clearInlineContext() {
        Deque<Frame> contexts = CONTEXTS.get();
        Frame current = contexts.peek();
        if (current != null && current.implicit) {
            contexts.pop();
            removeThreadLocalWhenEmpty(contexts);
        }
    }

    public static GenerationContext current() {
        GenerationContext context = currentOrNull();
        if (context == null) {
            throw new IllegalStateException("No active Fluxtion GenerationContext");
        }
        return context;
    }

    public static GenerationContext currentOrNull() {
        Frame frame = CONTEXTS.get().peek();
        return frame == null ? null : frame.context;
    }

    private static void removeThreadLocalWhenEmpty(Deque<Frame> contexts) {
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
        }
    }

    private static final class Frame {
        private final GenerationContext context;
        private final boolean implicit;

        private Frame(GenerationContext context, boolean implicit) {
            this.context = context;
            this.implicit = implicit;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Thread owner;
        private final Frame expected;
        private boolean closed;

        private Scope(Thread owner, Frame expected) {
            this.owner = owner;
            this.expected = expected;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException(
                        "GenerationContext scope closed by a different thread");
            }
            Deque<Frame> contexts = CONTEXTS.get();
            if (contexts.peek() != expected) {
                throw new IllegalStateException(
                        "GenerationContext scopes must close in LIFO order");
            }
            contexts.pop();
            closed = true;
            removeThreadLocalWhenEmpty(contexts);
        }
    }

    private static final class InlineGenerationContext implements GenerationContext {
        private static final class ClassCountKey {
        }

        private final Map<Object, Map> cacheMap = new HashMap<Object, Map>();
        private final List<Object> nodeList = new ArrayList<Object>();
        private final Map<Object, String> publicNodes = new HashMap<Object, String>();
        private final ClassLoader classLoader;

        private InlineGenerationContext(ClassLoader classLoader) {
            this.classLoader = classLoader == null
                    ? GenerationContextHolder.class.getClassLoader()
                    : classLoader;
        }

        @Override
        public int nextId(String className) {
            Map<String, Integer> classCounts = getCache(ClassCountKey.class);
            Integer previous = classCounts.get(className);
            int next = previous == null ? 0 : previous + 1;
            classCounts.put(className, next);
            return next;
        }

        @Override
        public List<Object> getNodeList() {
            return nodeList;
        }

        @Override
        public Map<Object, String> getPublicNodes() {
            return publicNodes;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T addOrUseExistingNode(T node) {
            int index = nodeList.indexOf(node);
            if (index >= 0) {
                return (T) nodeList.get(index);
            }
            nodeList.add(node);
            return node;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> Map<K, V> getCache(Object key) {
            Map<K, V> cache = cacheMap.get(key);
            if (cache == null) {
                cache = new HashMap<K, V>();
                cacheMap.put(key, cache);
            }
            return cache;
        }

        @Override
        public <T> T nameNode(T node, String name) {
            publicNodes.put(node, name);
            return node;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> Map<K, V> removeCache(Object key) {
            return (Map<K, V>) cacheMap.remove(key);
        }
    }
}
