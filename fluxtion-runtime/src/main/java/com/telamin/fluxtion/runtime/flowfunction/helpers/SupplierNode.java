package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.partition.LambdaReflection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

/**
 * A node in the dataflow graph that supplies values of type T using a provided supplier function.
 * This class acts as a wrapper around a SerializableSupplier to integrate it into the dataflow.
 *
 * @param <T> the type of value this supplier node produces
 */
@EqualsAndHashCode
@ToString

public class SupplierNode<T> {

    private final LambdaReflection.SerializableSupplier<T> defaultSupplier;

    /**
     * Creates a new SupplierNode with the given supplier function.
     *
     * @param defaultSupplier the supplier function that will provide values of type T
     */
    public SupplierNode(LambdaReflection.SerializableSupplier<T> defaultSupplier) {
        Objects.requireNonNull(defaultSupplier, "defaultSupplier cannot be null");
        this.defaultSupplier = defaultSupplier;
    }

    /**
     * Returns a value of type T from the supplier, ignoring the input parameter.
     *
     * @param input ignored input parameter
     * @return the value produced by the supplier
     */
    public T get(Object input) {
        return defaultSupplier.get();
    }
}
