/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.flowfunction.helpers;

import com.telamin.fluxtion.runtime.annotations.builder.SepNode;
import com.telamin.fluxtion.runtime.flowfunction.DefaultValueSupplier;
import com.telamin.fluxtion.runtime.flowfunction.Stateful;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableSupplier;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class DefaultValue<T> implements DefaultValueSupplier, Stateful<T> {

    @SepNode
    private final T defaultValue;

    public DefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public T getOrDefault(T input) {
        return input == null ? defaultValue : input;
    }

    @Override
    public T reset() {
        return defaultValue;
    }

    public static class DefaultValueFromSupplier<T> implements DefaultValueSupplier, Stateful<T> {

        private final SerializableSupplier<T> defaultSupplier;
        //    @SepNode
        private transient final T defaultValue;

        public DefaultValueFromSupplier(SerializableSupplier<T> defaultSupplier) {
            this.defaultSupplier = defaultSupplier;
            this.defaultValue = defaultSupplier.get();
        }

        public T getOrDefault(T input) {
            return input == null ? defaultValue : input;
        }

        @Override
        public T reset() {
            return defaultValue;
        }
    }

    public static class DefaultInt implements DefaultValueSupplier, Stateful<Integer> {
        private final int defaultValue;
        private boolean inputUpdatedAtLeastOnce;

        public DefaultInt(int defaultValue) {
            this.defaultValue = defaultValue;
        }

        public int getOrDefault(int input) {
            inputUpdatedAtLeastOnce |= input != 0;
            if (inputUpdatedAtLeastOnce) {
                return input;
            }
            return defaultValue;
        }

        @Override
        public Integer reset() {
            inputUpdatedAtLeastOnce = false;
            return defaultValue;
        }
    }

    public static class DefaultDouble implements DefaultValueSupplier, Stateful<Double> {
        private final double defaultValue;
        private boolean inputUpdatedAtLeastOnce;

        public DefaultDouble(double defaultValue) {
            this.defaultValue = defaultValue;
        }

        public double getOrDefault(double input) {
            inputUpdatedAtLeastOnce |= input != 0;
            if (inputUpdatedAtLeastOnce) {
                return input;
            }
            return defaultValue;
        }

        @Override
        public Double reset() {
            inputUpdatedAtLeastOnce = false;
            return defaultValue;
        }
    }

    public static class DefaultLong implements DefaultValueSupplier, Stateful<Long> {
        private final long defaultValue;
        private boolean inputUpdatedAtLeastOnce;

        public DefaultLong(long defaultValue) {
            this.defaultValue = defaultValue;
        }

        public long getOrDefault(long input) {
            inputUpdatedAtLeastOnce |= input != 0;
            if (inputUpdatedAtLeastOnce) {
                return input;
            }
            return defaultValue;
        }

        @Override
        public Long reset() {
            inputUpdatedAtLeastOnce = false;
            return defaultValue;
        }
    }
}
