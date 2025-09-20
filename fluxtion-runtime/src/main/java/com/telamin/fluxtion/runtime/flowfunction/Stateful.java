/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.flowfunction;

import com.telamin.fluxtion.runtime.annotations.OnParentUpdate;

/**
 * A marker interface used by {@link TriggeredFlowFunction} to mark a stream function as stateful and should be reset when
 * the reset trigger fires. The reset trigger is set via {@link TriggeredFlowFunction#setUpdateTriggerNode(Object)}.
 */
public interface Stateful<T> {
    T reset();

    abstract class StatefulWrapper {

        private final Object resetTrigger;

        public StatefulWrapper(Object resetTrigger) {
            this.resetTrigger = resetTrigger;
        }

        @OnParentUpdate("resetTrigger")
        public void resetTrigger(Object resetTrigger) {
            reset();
        }

        protected abstract void reset();

    }
}
