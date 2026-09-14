/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures a node's scalar field state once, at model-build time, into {@link FieldValue}s.
 *
 * <p>This is the ONLY place a node instance is reflected over for state on behalf of targets. Doing it
 * here rather than in each generator is what lets a target that never sees the object — the C++ emitter,
 * or anything on the remote generation path where the instance has already been dropped as transient —
 * still reproduce the configured graph.
 *
 * <p><b>Scalars are carried; everything else is RECORDED AS UNSUPPORTED.</b> Rendering a collection
 * requires choosing a representation, and in a low-latency target that is a performance decision
 * belonging to the author rather than a serialisation detail — so those are a later tier with their own
 * mechanism. But they are not skipped: skipping is how a target ends up emitting a processor that
 * silently differs from the Java one, which is precisely the fault this work exists to fix. They cross
 * as {@link FieldValue.Kind#UNSUPPORTED} carrying their type name, and a target refuses by name.
 *
 * <p><b>Static, synthetic and transient fields are skipped.</b> Static fields are not instance state;
 * synthetic fields are the compiler's (a lambda capture, an outer-class reference); transient is the
 * author saying explicitly that this does not travel — the same word Java's own serialisation honours.
 */
public final class FieldValueCapture {

    private FieldValueCapture() {
    }

    public static List<FieldValue> capture(Object instance) {
        return capture(instance, o -> false);
    }

    /**
     * @param isGraphNode the MODEL's own answer to "is this object a node in this graph", which is
     *                    {@code graph.variableName(o) != null}. A field holding a node is a PARENT
     *                    REFERENCE, not state: the C++ target binds those as typed parent pointers and
     *                    the Java target writes the sibling's variable name.
     *                    <p>Asking the model rather than reconstructing the answer matters. The first
     *                    version took a snapshot {@code Map}'s key set and compared by identity, and
     *                    still refused genuine parent references — the snapshot was not the map the
     *                    model actually answers from. Fifty working graphs were refused, one of them
     *                    because the framework's own injected audit logger looked like user state.
     */
    public static List<FieldValue> capture(Object instance, java.util.function.Predicate<Object> isGraphNode) {
        if (instance == null) {
            return Collections.emptyList();
        }
        List<FieldValue> out = new ArrayList<>();
        for (Class<?> c = instance.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                final int mods = f.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
                    continue;
                }
                if (isAuthorExcluded(f)) {
                    continue;   // @FluxtionIgnore: the author says this is not configured state
                }
                if (isFrameworkOwned(f.getType())) {
                    continue;   // injected by the runtime - an EventLogger, a clock, a dispatcher
                }
                // THE VALUE IS READ FIRST, and this order is the point rather than an accident.
                // Deciding the kind from the declared TYPE first meant a field holding a parent node
                // was recorded as unsupported object state before the parent check ever ran - the
                // predicate was only ever reached for scalars, and twenty-eight working graphs were
                // refused for holding their own parents.
                final Object v;
                try {
                    f.setAccessible(true);
                    v = f.get(instance);
                } catch (RuntimeException | ReflectiveOperationException unreadable) {
                    // A field we cannot read is a field we cannot reproduce OR name a type for with
                    // any confidence. Skipping is the honest outcome.
                    continue;
                }
                if (v == null) {
                    continue;   // nothing to reproduce
                }
                if (isGraphNode.test(v)) {
                    continue;   // a parent reference, bound by the target, not serialised state
                }
                final FieldValue sequence = asSequence(f, v);
                if (sequence != null) {
                    out.add(sequence);
                    continue;
                }
                final FieldValue.Kind kind = kindOf(f.getType());
                if (kind == null) {
                    // Before refusing, offer it to any user serialiser registered for a NON-JAVA
                    // target. Rendering happens HERE because a serialiser needs the instance, and the
                    // instance does not survive the DTO — so the text travels instead, exactly as
                    // Java's own constructor source does.
                    final java.util.Map<String, String> rendered = renderCustom(f.getType(), v, f.getName());
                    if (!rendered.isEmpty()) {
                        out.add(new FieldValue(f.getName(), f.getType().getCanonicalName(),
                                FieldValue.Kind.CUSTOM, f.getType().getCanonicalName(),
                                null, Collections.emptyList(), rendered));
                        continue;
                    }
                    // NOT skipped. Java would serialise this; a target that cannot carry it must be
                    // able to say so by name rather than emit a processor that quietly differs.
                    out.add(new FieldValue(f.getName(), f.getType().getCanonicalName(),
                            FieldValue.Kind.UNSUPPORTED, f.getType().getCanonicalName()));
                    continue;
                }
                out.add(new FieldValue(f.getName(), f.getType().getCanonicalName(), kind,
                        literalOf(kind, v)));
            }
        }
        return out.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(out);
    }

    /**
     * {@code @FluxtionIgnore} by name rather than by import, so this module does not depend on the
     * runtime's annotation package for a decision it only needs to read.
     */
    private static boolean isAuthorExcluded(java.lang.reflect.Field f) {
        for (java.lang.annotation.Annotation a : f.getAnnotations()) {
            final String n = a.annotationType().getSimpleName();
            if ("FluxtionIgnore".equals(n) || "ExcludeNode".equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runtime-owned types the framework injects rather than the author configuring — the audit logger
     * being the one that exposed this: refusing it named the framework's own field as unsupported user
     * state, on every audited graph.
     */
    private static boolean isFrameworkOwned(Class<?> t) {
        final String n = t.getName();
        return n.startsWith("com.telamin.fluxtion.runtime.");
    }

    /**
     * An array, {@code List} or {@code Set} of one scalar kind, captured as DATA.
     *
     * <p>Returns null for anything else — a mixed-element collection, a {@code Map}, a collection of
     * objects — which then falls through to UNSUPPORTED and is refused by name. Refusing a mixed
     * collection is deliberate: there is no single element type to give a target, and picking the
     * widest would silently change what the author declared.
     */
    private static FieldValue asSequence(java.lang.reflect.Field f, Object v) {
        final List<Object> raw = new ArrayList<>();
        if (f.getType().isArray()) {
            final int n = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < n; i++) {
                raw.add(java.lang.reflect.Array.get(v, i));
            }
        } else if (v instanceof java.util.List || v instanceof java.util.Set) {
            raw.addAll((java.util.Collection<?>) v);
        } else {
            return null;
        }

        FieldValue.Kind elementKind = null;
        if (f.getType().isArray()) {
            elementKind = kindOf(f.getType().getComponentType());
            if (elementKind == null) {
                return null;   // an array of objects: not this tier
            }
        }
        final List<String> literals = new ArrayList<>(raw.size());
        for (Object e : raw) {
            if (e == null) {
                return null;   // a null element has no literal, and guessing one would be a lie
            }
            final FieldValue.Kind k = kindOf(e.getClass());
            if (k == null) {
                return null;
            }
            if (elementKind == null) {
                elementKind = k;
            } else if (elementKind != k && !bothNumericSameWidth(elementKind, k)) {
                return null;   // mixed elements: no single type to hand a target
            }
            literals.add(literalOf(k, e));
        }
        if (elementKind == null) {
            // An EMPTY array still has a component type, and an empty collection does not. An empty
            // collection carries no type information at all, so it is refused rather than guessed.
            return null;
        }
        return new FieldValue(f.getName(), f.getType().getCanonicalName(),
                FieldValue.Kind.SEQUENCE, f.getType().getCanonicalName(), elementKind, literals);
    }

    /** Boxed and primitive forms of one width are the same element type to a target. */
    private static boolean bothNumericSameWidth(FieldValue.Kind a, FieldValue.Kind b) {
        return a == b;
    }

    /**
     * Asks every registered {@code FieldToSourceSerializer} for a NON-JAVA language whether it can
     * render this type, and collects what they produce, keyed by language.
     *
     * <p>Java is excluded because the Java generator already has its own path through
     * {@code FieldSerializer} and re-rendering here would produce a second, competing answer.
     *
     * <p>Highest {@code priority()} wins per language, so a user serialiser can override a shipped
     * one — the same ordering Java's registry uses.
     */
    private static java.util.Map<String, String> renderCustom(Class<?> type, Object value,
                                                             String fieldName) {
        java.util.Map<String, String> best = null;
        java.util.Map<String, Integer> bestPriority = null;
        // The per-LANGUAGE registries first. ClassSerializerRegistry has carried targetLanguage()
        // since before this work; the Java one has always returned "java" and nothing had walked
        // through the door for another target. A registry is where a mapping with one obvious answer
        // belongs; the per-type SPI below is the escape hatch for everything else.
        try {
            for (com.telamin.fluxtion.builder.generation.config.ClassSerializerRegistry reg
                    : java.util.ServiceLoader.load(
                            com.telamin.fluxtion.builder.generation.config.ClassSerializerRegistry.class)) {
                final String lang = reg.targetLanguage();
                if (lang == null || lang.isEmpty() || "java".equals(lang)) {
                    continue;
                }
                final java.util.function.Function<
                        com.telamin.fluxtion.builder.generation.serialiser.FieldContext, String> fn =
                        reg.classSerializerMap().get(type);
                if (fn == null) {
                    continue;
                }
                try {
                    final String text = fn.apply(
                            new com.telamin.fluxtion.builder.generation.serialiser.FieldContext<Object>(
                                    value, Collections.emptyList(), new java.util.HashSet<>(), null,
                                    fieldName));
                    if (text != null && !text.isEmpty()) {
                        if (best == null) {
                            best = new java.util.LinkedHashMap<>();
                            bestPriority = new java.util.HashMap<>();
                        }
                        best.put(lang, text);
                        // A registry mapping is the SHIPPED answer and sits below any user serialiser,
                        // so a user can override it for their own project.
                        bestPriority.put(lang, Integer.MIN_VALUE);
                    }
                } catch (RuntimeException registryFailed) {
                    // fall through to the SPI, then to the refusal
                }
            }
        } catch (java.util.ServiceConfigurationError badProvider) {
            // a broken registry must not take every build down
        }
        try {
            for (com.telamin.fluxtion.builder.generation.serialiser.FieldToSourceSerializer<?> s
                    : java.util.ServiceLoader.load(
                            com.telamin.fluxtion.builder.generation.serialiser.FieldToSourceSerializer.class)) {
                final String lang = s.language();
                if (lang == null || "java".equals(lang) || !s.typeSupported(type)) {
                    continue;
                }
                if (best == null) {
                    best = new java.util.LinkedHashMap<>();
                    bestPriority = new java.util.HashMap<>();
                }
                final Integer held = bestPriority.get(lang);
                if (held != null && held >= s.priority()) {
                    continue;
                }
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final String text = ((com.telamin.fluxtion.builder.generation.serialiser.FieldToSourceSerializer)
                            s).mapToSource(
                            new com.telamin.fluxtion.builder.generation.serialiser.FieldContext<Object>(
                                    value, Collections.emptyList(), new java.util.HashSet<>(), null,
                                    fieldName));
                    if (text != null && !text.isEmpty()) {
                        best.put(lang, text);
                        bestPriority.put(lang, s.priority());
                    }
                } catch (RuntimeException serialiserFailed) {
                    // A serialiser that throws has not rendered anything. Falling through to the
                    // refusal is right: the alternative is emitting whatever it managed first.
                }
            }
        } catch (java.util.ServiceConfigurationError badProvider) {
            // A broken provider must not take the build down on every graph; the field is refused by
            // name instead, which says something actionable.
        }
        return best == null ? Collections.emptyMap() : best;
    }

    private static FieldValue.Kind kindOf(Class<?> t) {
        if (t == boolean.class || t == Boolean.class) { return FieldValue.Kind.BOOLEAN; }
        if (t == char.class || t == Character.class) { return FieldValue.Kind.CHAR; }
        if (t == byte.class || t == Byte.class) { return FieldValue.Kind.BYTE; }
        if (t == short.class || t == Short.class) { return FieldValue.Kind.SHORT; }
        if (t == int.class || t == Integer.class) { return FieldValue.Kind.INT; }
        if (t == long.class || t == Long.class) { return FieldValue.Kind.LONG; }
        if (t == float.class || t == Float.class) { return FieldValue.Kind.FLOAT; }
        if (t == double.class || t == Double.class) { return FieldValue.Kind.DOUBLE; }
        if (t == String.class) { return FieldValue.Kind.STRING; }
        if (t.isEnum()) { return FieldValue.Kind.ENUM; }
        return null;
    }

    /** Neutral text. Each target escapes for its own syntax; none is applied here. */
    private static String literalOf(FieldValue.Kind kind, Object v) {
        return kind == FieldValue.Kind.ENUM ? ((Enum<?>) v).name() : String.valueOf(v);
    }
}
