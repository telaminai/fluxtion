/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.util;

public interface Copyable<T> extends Cloneable {

    T clone();

    <S extends T> T copyFrom(S from);
}
