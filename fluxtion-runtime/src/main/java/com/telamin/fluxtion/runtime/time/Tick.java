/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.time;

import com.telamin.fluxtion.runtime.event.Event;
import lombok.Data;

/**
 * A tick event notifies a clock to update its wall clock time. Any nodes that
 * depend upon clock will check their state and fire events as necessary.
 *
 * @author Greg Higgins greg.higgins@v12technology.com
 */
@Data
public class Tick implements Event {

    private long eventTime;
}
