/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.describe;

import com.telamin.fluxtion.runtime.DataFlow;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Self-description of a Fluxtion processor's embeddable surface — its named event inputs, published sinks and
 * service contract — plus a factory to build a fresh instance. Generated (AOT) processors carry a compiler-emitted
 * descriptor; interpreted processors derive the same descriptor from the generation model at build time, so the
 * two bodies are interchangeable behind one identity.
 *
 * <p>Element types are carried as fully-qualified type NAMES so a descriptor can be read — catalogued, compared,
 * displayed — without loading any user class; call {@link Input#resolveType(ClassLoader)} (and friends) when a
 * {@code Class} is actually needed.
 *
 * <p><b>Lifecycle contract:</b> {@link #factory()} supplies a fresh, <b>un-initialised</b> {@link DataFlow}. The
 * caller owns the boot sequence: {@code init()} → register services → wire sinks → drive events. Post-init
 * configuration values (e.g. a calibration data set) are the host's job, supplied through the descriptor's
 * declared services — they are not modelled here.
 *
 * <p><b>Schema:</b> additive-only. Fields are never removed or repurposed, so a descriptor emitted by an older
 * compiler is always readable by a newer runtime and vice versa.
 */
public interface ProcessorDescriptor {

    /** The shape version of the {@code ProcessorDescriptor} contract itself — consumers gate on this before binding. */
    int CURRENT_SCHEMA_VERSION = 1;

    /**
     * The descriptor schema version this instance was built against. The contract is additive-only, so a reader
     * newer than the writer always succeeds; a reader older than the writer sees only the fields it knows.
     */
    default int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    /** Stable component identity for catalogues / registries. Defaults to the concrete DataFlow class name. */
    default String name() {
        Class<? extends DataFlow> c = dataFlowClass();
        return c == null ? null : c.getName();
    }

    /** The toolchain/generator version that produced this artifact, or {@code null} when unrecorded. */
    default String toolchainVersion() {
        return null;
    }

    /**
     * A fingerprint (hash) of the source graph this processor was generated from, or {@code null} when unrecorded.
     * The cache/staleness key for regenerate-if-changed flows.
     */
    default String sourceFingerprint() {
        return null;
    }

    /**
     * Classpath resource name of the graphml sidecar describing this processor's topology, or {@code null} when
     * none was generated. A POINTER to the sidecar — never the graphml content (runtime stays lean).
     */
    default String graphmlResource() {
        return null;
    }

    /** Named, typed event inputs the processor accepts (name = filter identity, or the simple type name when unfiltered). */
    List<Input> inputs();

    /** Named output sinks the processor publishes to ({@code SinkPublisher} registrations). */
    List<Sink> sinks();

    /** Services the processor EXPORTS ({@code @ExportService}) and REQUIRES ({@code @ServiceRegistered}). */
    List<Service> services();

    /** The concrete DataFlow class this describes. */
    Class<? extends DataFlow> dataFlowClass();

    /**
     * Factory for a fresh, UN-INITIALISED instance (see the class javadoc lifecycle contract). For an interpreted
     * vs AOT body this is the one place the two diverge — same descriptor, different {@code create}. May be
     * {@code null} when the construction context cannot re-create the processor (callers should treat the
     * descriptor as metadata-only in that case).
     */
    Supplier<? extends DataFlow> factory();

    /** Convenience: {@code factory().get()}. Throws {@link IllegalStateException} when no factory is available. */
    default DataFlow create() {
        Supplier<? extends DataFlow> f = factory();
        if (f == null) {
            throw new IllegalStateException("no factory available on this descriptor: " + dataFlowClass());
        }
        return f.get();
    }

    /** A named, typed event input. {@code filtered} = a genuinely named/filtered input (bind by name) vs plain-typed (bind by type). */
    final class Input {
        private final String name;
        private final String typeName;
        private final boolean filtered;

        public Input(String name, String typeName, boolean filtered) {
            this.name = name;
            this.typeName = typeName;
            this.filtered = filtered;
        }

        public String name() { return name; }
        public String typeName() { return typeName; }
        public boolean filtered() { return filtered; }

        /** Load the input's event type on demand — descriptor reads never force class loading. */
        public Class<?> resolveType(ClassLoader loader) throws ClassNotFoundException {
            return Class.forName(typeName, false, loader);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Input)) return false;
            Input other = (Input) o;
            return filtered == other.filtered
                    && Objects.equals(name, other.name)
                    && Objects.equals(typeName, other.typeName);
        }

        @Override
        public int hashCode() { return Objects.hash(name, typeName, filtered); }

        @Override
        public String toString() { return "Input{name='" + name + "', typeName='" + typeName + "', filtered=" + filtered + '}'; }
    }

    /** A named output sink. {@code typeName} is the published element type when the model retains it, else {@code java.lang.Object}. */
    final class Sink {
        private final String name;
        private final String typeName;

        public Sink(String name, String typeName) {
            this.name = name;
            this.typeName = typeName;
        }

        public String name() { return name; }
        public String typeName() { return typeName; }

        /** Load the sink's element type on demand — descriptor reads never force class loading. */
        public Class<?> resolveType(ClassLoader loader) throws ClassNotFoundException {
            return Class.forName(typeName, false, loader);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Sink)) return false;
            Sink other = (Sink) o;
            return Objects.equals(name, other.name) && Objects.equals(typeName, other.typeName);
        }

        @Override
        public int hashCode() { return Objects.hash(name, typeName); }

        @Override
        public String toString() { return "Sink{name='" + name + "', typeName='" + typeName + "'}"; }
    }

    /** A service the processor exports (implements for the host to call) or requires (host must register). */
    final class Service {
        public enum Direction { EXPORTED, REQUIRED }

        private final String name;
        private final String typeName;
        private final Direction direction;

        public Service(String name, String typeName, Direction direction) {
            this.name = name;
            this.typeName = typeName;
            this.direction = direction;
        }

        public String name() { return name; }
        public String typeName() { return typeName; }
        public Direction direction() { return direction; }

        /** Load the service interface on demand — descriptor reads never force class loading. */
        public Class<?> resolveType(ClassLoader loader) throws ClassNotFoundException {
            return Class.forName(typeName, false, loader);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Service)) return false;
            Service other = (Service) o;
            return Objects.equals(name, other.name)
                    && Objects.equals(typeName, other.typeName)
                    && direction == other.direction;
        }

        @Override
        public int hashCode() { return Objects.hash(name, typeName, direction); }

        @Override
        public String toString() { return "Service{name='" + name + "', typeName='" + typeName + "', direction=" + direction + '}'; }
    }
}
