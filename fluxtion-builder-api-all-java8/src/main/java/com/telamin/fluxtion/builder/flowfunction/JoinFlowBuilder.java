package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.runtime.flowfunction.Tuple;
import com.telamin.fluxtion.runtime.flowfunction.groupby.InnerJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.LeftJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.OuterJoin;
import com.telamin.fluxtion.runtime.flowfunction.groupby.RightJoin;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Tuples;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

// BROWSER-BUNDLE OVERRIDE — explicit lambdas instead of `new X()::join`.
//
// CHANGED FROM INTERFACE TO CLASS: javac compiles lambdas inside interface
// static methods to `private static` synthetic methods on the interface.
// JDK 8's MethodHandles.Lookup.findStatic cannot resolve those (private
// interface methods are a Java 9 feature), which blocks Retrolambda from
// reifying them. Lambdas inside class static methods compile to
// `private static` methods on the class — handled fine. No call site uses
// `JoinFlowBuilder` as an interface (only via static methods), so the
// switch is binary-compatible.
public final class JoinFlowBuilder {
    private JoinFlowBuilder() {}

    public static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> innerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        InnerJoin join = new InnerJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy);
    }

    public static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> innerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        InnerJoin join = new InnerJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> outerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        OuterJoin join = new OuterJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy);
    }

    public static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> outerJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        OuterJoin join = new OuterJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> leftJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        LeftJoin join = new LeftJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy);
    }

    public static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> leftJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        LeftJoin join = new LeftJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }

    public static <K1, V1, K2 extends K1, V2> GroupByFlowBuilder<K1, Tuple<V1, V2>> rightJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy) {
        RightJoin join = new RightJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy);
    }

    public static <K1, V1, K2 extends K1, V2, R> GroupByFlowBuilder<K1, R> rightJoin(
            GroupByFlowBuilder<K1, V1> leftGroupBy,
            GroupByFlowBuilder<K2, V2> rightGroupBy,
            LambdaReflection.SerializableBiFunction<V1, V2, R> mergeFunction) {
        RightJoin join = new RightJoin();
        return leftGroupBy.mapBiFunction((l, r) -> join.join(l, r), rightGroupBy).mapValues(Tuples.mapTuple(mergeFunction));
    }
}
