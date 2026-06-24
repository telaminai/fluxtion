/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */

package com.telamin.fluxtion.runtime.eventfeed;

import com.telamin.fluxtion.runtime.annotations.feature.Experimental;
import com.telamin.fluxtion.runtime.input.NamedFeed;
import org.agrona.concurrent.Agent;

/**
 * To create a custom {@link EventFeedAgent} implement this class or extend the {@link BaseEventFeed}
 * @param <T>
 */
@Experimental
public interface EventFeedAgent<T> extends NamedFeed<T>, Agent {
}