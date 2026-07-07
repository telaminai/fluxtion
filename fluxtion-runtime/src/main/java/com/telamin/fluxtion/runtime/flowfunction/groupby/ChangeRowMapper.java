/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.groupby;

import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy.KeyValue;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;

/**
 * Adapts a {@code (key,value) -> record} row mapper into a {@link Change} re-mapper so a delete-aware
 * sink receives the IVM op alongside the typed row: {@code Change<K, V>} (key, aggregate, op) becomes
 * {@code Change<K, Record>} (key, mapped record, op). For a DELETE the {@code value} is the group's
 * previous aggregate (carried by the delta), so the previous record is delivered — the sink knows which
 * row to remove.
 *
 * <p>Used as a bare instance method reference ({@code wrapper::mapChange}) in {@code map}, so the
 * signatures are raw — the same AOT lowering rule as {@link GroupByDeltaFlowFunctions} (a generic method
 * there cannot have its diamond inferred from the raw upstream the generator emits).
 *
 * <p>The constructor's input is typed {@code KeyValue} (raw, no {@code <K,V>}) rather than {@code Object}
 * deliberately: the generator emits the row mapper as a bare {@code Provider::Record__build} method
 * reference straight into {@code new ChangeRowMapper(...)} in RAW generated code. That method takes a
 * {@code KeyValue}, so a fully-raw {@code SerializableFunction} param (erased to {@code apply(Object)})
 * would reject the reference — {@code KeyValue} is more specific than {@code Object}. Keeping the param
 * a {@code KeyValue} lets the reference bind (with an unchecked conversion) while still needing no diamond
 * inference. This trap fires only in compiled/AOT modes, never interpreted.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ChangeRowMapper {

    private final SerializableFunction<KeyValue, Object> rowMapper; // KeyValue<K, V> -> Record

    public ChangeRowMapper(SerializableFunction<KeyValue, Object> rowMapper) {
        this.rowMapper = rowMapper;
    }

    public Change mapChange(Change change) {
        Object record = rowMapper.apply(new KeyValue(change.key(), change.value()));
        return new Change(change.key(), record, change.op());
    }
}
