/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.builder.meta.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shape of an event class, captured so a target can CHECK the type an author declared for it.
 *
 * <p><b>Why this exists.</b> A non-Java target cannot define the author's event types — only they know
 * the layout — so it declares handlers and leaves the definition to them. That works, and until now it
 * was checked by nothing: a Java {@code long} declared as a 32-bit field on the other side compiles
 * and truncates. Node state is refused by name when a target cannot carry it ({@code FLX-1031}), and
 * the reason no equivalent refusal was possible here is that the generator held no model of the type
 * to compare against. This is that model.
 *
 * <p><b>Captured on the client, like everything that needs a live Class.</b> A remote build ships a DTO
 * and the server never loads the author's classes, so a layout read by reflection has to be read where
 * the classes are. Same arrangement as {@link FieldValueCapture}, for the same reason.
 *
 * <p>Deliberately a SHAPE and not a serialisation format. It carries what a target can assert about a
 * field it did not declare — name, declared type, and width — and nothing about representation, because
 * choosing a representation is the author's business and a target that decided one would be wrong.
 */
public final class EventTypeLayout implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String javaName;
    private final List<Field> fields;

    public EventTypeLayout(String javaName, List<Field> fields) {
        this.javaName = javaName;
        this.fields = fields == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(fields));
    }

    /** Fully-qualified Java name — also the dispatch key, so it is what a target should quote. */
    public String javaName() { return javaName; }

    /** In DECLARATION order. Order is part of the shape a target may want to check. */
    public List<Field> fields() { return fields; }

    /** One field of an event class. */
    public static final class Field implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String name;
        private final String declaredType;
        private final FieldValue.Kind kind;
        private final int widthBytes;

        public Field(String name, String declaredType, FieldValue.Kind kind, int widthBytes) {
            this.name = name;
            this.declaredType = declaredType;
            this.kind = kind;
            this.widthBytes = widthBytes;
        }

        public String name() { return name; }

        public String declaredType() { return declaredType; }

        /** {@link FieldValue.Kind#UNSUPPORTED} for anything not a primitive or String. */
        public FieldValue.Kind kind() { return kind; }

        /**
         * Java's width for this field, or 0 when there is no single answer.
         *
         * <p>Zero for references, including String: a target may represent a string as a pointer, an
         * array or a view, and asserting any one size would be this class deciding that. Only the
         * primitives have a width every target must agree on, which is exactly the set where the
         * silent-truncation bug lives.
         */
        public int widthBytes() { return widthBytes; }
    }
}
