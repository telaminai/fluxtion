/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
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