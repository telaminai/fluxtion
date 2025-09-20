/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.output;

import com.telamin.fluxtion.runtime.event.DefaultEvent;

public class SinkDeregister extends DefaultEvent {

    private SinkDeregister(String sinkId) {
        super(sinkId);
    }

    public static SinkDeregister sink(String sinkId) {
        return new SinkDeregister(sinkId);
    }
}
