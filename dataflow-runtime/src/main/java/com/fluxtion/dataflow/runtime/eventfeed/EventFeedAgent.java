/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.eventfeed;

import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import com.fluxtion.dataflow.runtime.input.NamedFeed;
import org.agrona.concurrent.Agent;

/**
 * To create a custom {@link EventFeedAgent} implement this class or extend the {@link BaseEventFeed}
 * @param <T>
 */
@Experimental
public interface EventFeedAgent<T> extends NamedFeed<T>, Agent {
}