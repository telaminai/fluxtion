/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.aggregate.function;

import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import com.telamin.fluxtion.runtime.flowfunction.aggregate.AggregateFlowFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableFunction;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-group "top-N rows": an aggregate that keeps the {@code count} highest input rows by a
 * {@link Comparable} ranking key (descending), as a bounded {@code List<T>}. Unlike
 * {@link com.telamin.fluxtion.runtime.flowfunction.groupby.TopNByValue} (a stateless {@code map}-stage
 * filter that ranks the <em>groups</em> of an already-materialized {@code GroupBy} by their aggregate
 * value), this is a stateful {@link AggregateFlowFunction} — used as the aggregate inside
 * {@code groupBy(keyFn, RankedTopNFlowFunction.topN(rankFn, N))} it yields, per group key, the N
 * highest rows ordered by {@code rankFn} descending.
 *
 * <p>The ordering is carried as a {@link SerializableFunction} accessor (not a {@code Comparator} —
 * there is no serializable-comparator type, and a comparator lambda would not survive AOT), and the
 * {@code compareTo} is done on the accessor's natural {@link Comparable} order, exactly as
 * {@code TopNByValue} does. Ties keep arrival order (FIFO among equal keys).
 *
 * <p><b>Not invertible</b> ({@link #deductSupported()} returns {@code false}): an ordered bounded
 * insert/trim has no inverse, so a sliding window recomputes from the live buckets. {@link #combine}
 * is a correct merge for that recompute path — the union of the per-bucket top-N lists always contains
 * the global top-N, so inserting one bucket's rows into another and re-trimming is exact.
 *
 * @param <T> the row type
 */
public class RankedTopNFlowFunction<T>
        implements AggregateFlowFunction<T, List<T>, RankedTopNFlowFunction<T>> {

    @SuppressWarnings("rawtypes")
    private final SerializableFunction ranking;
    private final int count;
    private transient final List<T> top = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    public RankedTopNFlowFunction(@AssignToField("ranking") SerializableFunction ranking,
                                  @AssignToField("count") int count) {
        this.ranking = ranking;
        this.count = count;
    }

    @Override
    public List<T> aggregate(T input) {
        insert(input);
        return top;
    }

    @Override
    public void combine(RankedTopNFlowFunction<T> add) {
        for (T t : add.top) {
            insert(t);
        }
    }

    /** Ordered-insert by descending ranking key, then trim to {@code count}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void insert(T input) {
        Comparable inKey = (Comparable) ranking.apply(input);
        int pos = 0;
        while (pos < top.size() && ((Comparable) ranking.apply(top.get(pos))).compareTo(inKey) >= 0) {
            pos++;
        }
        top.add(pos, input);
        while (count > 0 && top.size() > count) {
            top.remove(top.size() - 1);
        }
    }

    /** Non-invertible: ordered bounded insert/trim has no inverse, so sliding windows recompute. */
    @Override
    public boolean deductSupported() {
        return false;
    }

    @Override
    public List<T> get() {
        return top;
    }

    @Override
    public List<T> reset() {
        top.clear();
        return top;
    }

    /**
     * The {@link SerializableSupplier} to pass as the {@code groupBy} aggregate factory:
     * {@code groupBy(keyFn, RankedTopNFlowFunction.topN(Row::getScore, 3))} keeps, per group, the 3
     * highest rows by {@code getScore} descending. The accessor is carried in a generic factory so AOT
     * source generation re-emits the method reference against a parameterized target.
     */
    public static <T> SerializableSupplier<RankedTopNFlowFunction<T>> topN(
            SerializableFunction<T, ? extends Comparable<?>> ranking, int count) {
        return new RankedTopNFactory<>(ranking, count)::newRanked;
    }

    /** Generic factory holding the ranking accessor + bound N; one aggregate instance per group key. */
    public static final class RankedTopNFactory<T> {
        private final SerializableFunction<T, ? extends Comparable<?>> ranking;
        private final int count;

        public RankedTopNFactory(@AssignToField("ranking") SerializableFunction<T, ? extends Comparable<?>> ranking,
                                 @AssignToField("count") int count) {
            this.ranking = ranking;
            this.count = count;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public RankedTopNFlowFunction<T> newRanked() {
            return new RankedTopNFlowFunction<>((SerializableFunction) ranking, count);
        }
    }
}
