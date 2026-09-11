/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import java.io.Serializable;

/**
 * One node field's captured state, in a form every target can render and the wire can carry.
 *
 * <h2>Why this exists</h2>
 * Java's {@code FieldSerializer} reflects over the built node instance and writes its non-transient
 * fields back out as Java source. That works because the Java generator runs in the same process as the
 * builder. Two things break it for anyone else:
 *
 * <ul>
 *   <li>{@link Field#getInstance()} is <b>transient</b>, so the object does not survive serialisation —
 *       the remote generation path has no instance to reflect over at all.</li>
 *   <li>{@link SourceField}, the meta model a target emitter is handed, exposes names and flags and
 *       <b>no values</b>. The C++ target therefore emitted none of a node's state, and not by dropping
 *       it: it was never given any. A graph declaring {@code new MarketState(cycle, 64)} produced Java
 *       carrying the literal and C++ containing zero occurrences of it.</li>
 * </ul>
 *
 * Capturing state here, once, at model-build time, puts it where every target can reach it and where
 * the wire can carry it — and means no target reflects.
 *
 * <h2>What a value is</h2>
 * {@link #kind()} says how to read {@link #literal()}. For this first tier every kind is a scalar whose
 * textual form is unambiguous in any target language: the decimal digits of an integer are the same
 * digits in Java and C++. Types that need a representation DECISION — collections, arrays, the
 * {@code java.time} family — are deliberately absent, because choosing {@code std::unordered_map} over
 * a flat array on an author's behalf is a latency decision, not a serialisation one.
 */
public final class FieldValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How to read {@link #literal()}. Scalars only; see the class comment. */
    public enum Kind {
        BOOLEAN, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, ENUM,
        /**
         * State Java WOULD serialise and this tier cannot carry — a collection, an array, a
         * {@code java.time} value, an arbitrary object.
         *
         * <p>Recorded rather than skipped, so a target can REFUSE BY NAME instead of emitting a
         * processor that silently differs from the Java one. {@link #literal()} holds the declared
         * type, which is what the refusal message needs to be useful.
         *
         * <p>Fields the author marked {@code transient} or {@code @FluxtionIgnore} never reach here:
         * those are the author stating the field is not part of the configured state, and Java's own
         * serialisation honours the same words.
         */
        UNSUPPORTED
    }

    private final String name;
    private final String declaredType;
    private final Kind kind;
    private final String literal;

    public FieldValue(String name, String declaredType, Kind kind, String literal) {
        this.name = name;
        this.declaredType = declaredType;
        this.kind = kind;
        this.literal = literal;
    }

    /** The field's name on the node class. */
    public String name() { return name; }

    /** Canonical name of the declared type — {@code int}, {@code java.lang.String}, an enum's FQN. */
    public String declaredType() { return declaredType; }

    public Kind kind() { return kind; }

    /**
     * The value in a neutral textual form: decimal digits for integers, {@code true}/{@code false} for
     * booleans, the raw characters for a string (UNESCAPED — each target escapes for its own syntax),
     * and the constant name for an enum.
     */
    public String literal() { return literal; }

    @Override
    public String toString() {
        return name + "(" + declaredType + ")=" + literal;
    }
}
