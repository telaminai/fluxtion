/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads the shape of the author's event classes, on the machine that has them.
 *
 * <p>Runs beside {@link FieldValueCapture} and for the same reason: a remote build ships a DTO and the
 * server never loads these classes, so anything needing a live {@code Class} is read here or not at all.
 * The difference is that node state captures VALUES from an instance and this captures a TYPE's shape —
 * there is no event instance at build time and none is needed.
 */
public final class EventLayoutCapture {

    private EventLayoutCapture() {
    }

    /**
     * Captures every event class that has at least one field worth checking.
     *
     * <p>A class with no capturable fields is omitted rather than recorded empty: an empty layout says
     * "this type has no fields", which is a claim, and an absent one says "nothing to check here",
     * which is the truth. A target asserting against the first would reject a marker event that is
     * legitimately empty on one side and carries a padding byte on the other.
     */
    public static List<EventTypeLayout> capture(Collection<Class<?>> eventClasses) {
        List<EventTypeLayout> out = new ArrayList<>();
        if (eventClasses == null) {
            return out;
        }
        for (Class<?> type : eventClasses) {
            if (type == null || type.isPrimitive() || type.isArray() || type.isInterface()) {
                continue;
            }
            // Framework event types are not the author's to declare, so there is nothing to check.
            if (type.getName().startsWith("com.telamin.fluxtion.runtime.")) {
                continue;
            }
            List<EventTypeLayout.Field> fields = new ArrayList<>();
            // DECLARED fields only, in declaration order. Inherited fields belong to the supertype and
            // a target that flattened them would assert a layout the author never wrote.
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) {
                    continue;
                }
                FieldValue.Kind kind = kindOf(f.getType());
                fields.add(new EventTypeLayout.Field(
                        f.getName(), f.getType().getCanonicalName(), kind, widthOf(f.getType())));
            }
            if (!fields.isEmpty()) {
                out.add(new EventTypeLayout(type.getCanonicalName(), fields));
            }
        }
        // SORTED, because the caller hands us a HashSet and its iteration order is not stable across
        // JVM runs. Without this the DTO's bytes differ between two builds of one unchanged graph -
        // which breaks reproducible builds, any digest taken over the model, and the wire golden that
        // caught it here. Field order within a type is left alone: that is declaration order, which is
        // the author's and is meaningful.
        out.sort(java.util.Comparator.comparing(EventTypeLayout::javaName,
                java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        return out;
    }

    /**
     * Java's width, or 0 where no single width is right.
     *
     * <p>{@code boolean} is 0 on purpose. The JLS does not define its size, a C++ {@code bool} is
     * typically 1 byte and a Rust {@code bool} is exactly 1, but asserting any of those would turn a
     * legal representation choice into a build failure. The widths below are the ones every target must
     * agree on or silently compute different answers — which is the whole point of capturing this.
     */
    private static int widthOf(Class<?> t) {
        if (t == byte.class || t == Byte.class) { return 1; }
        if (t == char.class || t == Character.class || t == short.class || t == Short.class) { return 2; }
        if (t == int.class || t == Integer.class || t == float.class || t == Float.class) { return 4; }
        if (t == long.class || t == Long.class || t == double.class || t == Double.class) { return 8; }
        return 0;
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
        return FieldValue.Kind.UNSUPPORTED;
    }
}
