/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.util;

public interface Copyable<T> extends Cloneable {

    T clone();

    <S extends T> T copyFrom(S from);
}
