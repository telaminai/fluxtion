/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.node;

import com.fluxtion.dataflow.runtime.audit.EventLogControlEvent.LogLevel;
import com.fluxtion.dataflow.runtime.audit.EventLogSource;
import com.fluxtion.dataflow.runtime.audit.EventLogger;
import com.fluxtion.dataflow.runtime.audit.NullEventLogger;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableSupplier;

import java.util.concurrent.RecursiveTask;

/**
 * Wraps a trigger method and executes it using the ForkJoin framework.
 */
public class ForkedTriggerTask extends RecursiveTask<Boolean> implements EventLogSource {

    private final transient String methodName;
    private final SerializableSupplier<Boolean> nodeTask;
    private final String delegateName;
    protected EventLogger auditLog = NullEventLogger.INSTANCE;
    private volatile boolean executingInCycle = false;

    public ForkedTriggerTask(SerializableSupplier<Boolean> nodeTask, String delegateName) {
        this.nodeTask = nodeTask;
        this.methodName = nodeTask.method().getName();
        this.delegateName = delegateName;
    }

    public void onTrigger() {
        executingInCycle = true;
        fork();
    }

    public boolean afterEvent() {
        if (executingInCycle) {
            executingInCycle = false;
            return join();
        }
        return executingInCycle;
    }

    @Override
    public void reinitialize() {
        afterEvent();
        super.reinitialize();
    }

    @Override
    protected Boolean compute() {
        if (auditLog.canLog(LogLevel.DEBUG)) {
            auditLog.debug("thread", Thread.currentThread().getName())
                    .debug("delegate", delegateName)
                    .debug("delegateMethod", methodName);
        }
        return nodeTask.get();
    }

    @Override
    public void setLogger(EventLogger log) {
        this.auditLog = log;
    }

}
