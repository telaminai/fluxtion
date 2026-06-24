/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level marker. The annotated type is intentionally not a Fluxtion
 * event-handling node — it is a data class, DTO, event type, helper or
 * launcher — and Substrate Lint should not flag it as missing a trigger
 * annotation.
 * <p>
 * The annotation has <b>no effect</b> on Fluxtion source-generation,
 * dispatch or runtime behaviour. It exists purely to suppress the
 * "missing trigger annotation" warning emitted by
 * {@code ValidateMissingTriggerAnnotations} and the equivalent
 * IDE / playground surfaces.
 *
 * <h2>When to use</h2>
 * Apply when a class is structurally similar to a node (instance fields,
 * non-abstract, no {@code main}) but is intentionally passive — for example
 * an event payload, a helper composed inside a real node, or a builder
 * fixture.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @FluxtionDataOnly
 * public class MarketTick {
 *     private final String symbol;
 *     private final double price;
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FluxtionDataOnly {
}
