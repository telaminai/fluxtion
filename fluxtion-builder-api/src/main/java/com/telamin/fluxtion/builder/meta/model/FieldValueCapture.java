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
 * <p><b>Scalars only, deliberately.</b> Collections, arrays and the {@code java.time} family are absent
 * because rendering them requires choosing a representation, and in a low-latency target that is a
 * performance decision belonging to the author rather than a serialisation detail. They are a later
 * tier with their own mechanism; silently emitting a container here would be the target deciding for
 * the author, which is the failure this whole design avoids.
 *
 * <p><b>Static, synthetic and transient fields are skipped.</b> Static fields are not instance state;
 * synthetic fields are the compiler's (a lambda capture, an outer-class reference); transient is the
 * author saying explicitly that this does not travel — the same word Java's own serialisation honours.
 */
public final class FieldValueCapture {

    private FieldValueCapture() {
    }

    public static List<FieldValue> capture(Object instance) {
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
                final FieldValue.Kind kind = kindOf(f.getType());
                if (kind == null) {
                    continue;   // not a scalar: a later tier's problem, not silently mangled here
                }
                try {
                    f.setAccessible(true);
                    final Object v = f.get(instance);
                    if (v == null) {
                        continue;   // nothing to reproduce
                    }
                    out.add(new FieldValue(f.getName(), f.getType().getCanonicalName(), kind,
                            literalOf(kind, v)));
                } catch (RuntimeException | ReflectiveOperationException ignored) {
                    // A field we cannot read is a field we cannot reproduce. Skipping is right: the
                    // alternative is failing a build over state the target may not even want, and the
                    // capability grid records that non-scalar state does not cross at all.
                }
            }
        }
        return out.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(out);
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
