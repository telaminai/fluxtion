/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.annotations.feature;

/**
 * Marks a class or method as a preview feature. Mirrors the use of jdk preview features:
 * <p/>
 * A preview feature is a new feature of the Java language, Java Virtual Machine, or Java SE API that is fully specified,
 * fully implemented, and yet impermanent. It is available in a JDK feature release to provoke developer feedback based
 * on real world use; this may lead to it becoming permanent in a future Java SE Platform.
 */
public @interface Preview {
}
