/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import java.util.Objects;

/**
 * A single key change within a {@link GroupByDelta}: a {@code (key, value, op)} triple.
 *
 * <p><b>Lifetime rule.</b> A {@code Change} (and the {@link GroupByDelta} that holds it) is, on the
 * single-key fast path, a <em>reused</em> object valid only for the duration of the current event
 * cycle — the producer may {@link #set} it in place on the next cycle. Any consumer that retains a
 * {@code Change} beyond the current cycle — audit, replay, an external sink, user code — MUST take a
 * {@link #copy()}. Operators that only read {@code key()}/{@code value()} within the cycle (the
 * common case) need not copy.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class Change<K, V> {

    private K key;
    private V value;
    private ChangeOp op;

    public Change() {
    }

    public Change(K key, V value, ChangeOp op) {
        this.key = key;
        this.value = value;
        this.op = op;
    }

    public static <K, V> Change<K, V> add(K key, V value) {
        return new Change<>(key, value, ChangeOp.ADD);
    }

    public static <K, V> Change<K, V> update(K key, V value) {
        return new Change<>(key, value, ChangeOp.UPDATE);
    }

    public static <K, V> Change<K, V> delete(K key) {
        return new Change<>(key, null, ChangeOp.DELETE);
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    public ChangeOp op() {
        return op;
    }

    /**
     * Mutate this instance in place — the single-key fast path that avoids per-cycle allocation. Honour
     * the lifetime rule: a reused instance is valid only for the current cycle.
     */
    public Change<K, V> set(K key, V value, ChangeOp op) {
        this.key = key;
        this.value = value;
        this.op = op;
        return this;
    }

    /** An independent snapshot, safe to retain beyond the current cycle. */
    public Change<K, V> copy() {
        return new Change<>(key, value, op);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Change)) {
            return false;
        }
        Change<?, ?> change = (Change<?, ?>) o;
        return Objects.equals(key, change.key)
                && Objects.equals(value, change.value)
                && op == change.op;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, op);
    }

    @Override
    public String toString() {
        return op + "(" + key + "=" + value + ")";
    }
}