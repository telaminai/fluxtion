/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.flowfunction.helpers;

import com.fluxtion.dataflow.runtime.annotations.Initialise;
import com.fluxtion.dataflow.runtime.annotations.NoTriggerReference;
import com.fluxtion.dataflow.runtime.annotations.Start;
import com.fluxtion.dataflow.runtime.annotations.builder.Inject;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection;
import com.fluxtion.dataflow.runtime.partition.LambdaReflection.SerializableFunction;
import com.fluxtion.dataflow.runtime.time.Clock;

public interface Peekers {

    /**
     * logs the contents of a streamed node to console:
     * <ul>
     *     <li>{} is replaced the to string of the node being peeked</li>
     *     <li>%e is replaced with millisecond event time stamp</li>
     *     <li>%t is replaced with millisecond wall clock time stamp</li>
     *     <li>%p is replaced with millisecond process time stamp</li>
     *     <li>%de is replaced with millisecond event time stamp delta from start</li>
     *     <li>%dt is replaced with millisecond wall clock time stamp delta from start</li>
     *     <li>%dp is replaced with millisecond process time stamp delta from start</li>
     * </ul>
     */
    static <T> LambdaReflection.SerializableConsumer<T> console(String message) {
        return new TemplateMessage<>(message, null)::templateAndLogToConsole;
    }

    static <T, R> LambdaReflection.SerializableConsumer<T> console(String message, SerializableFunction<T, R> transform) {
        return new TemplateMessage<>(message, transform)::templateAndLogToConsole;
    }

    static void println(Object message) {
        System.out.println(message);
    }


    class TemplateMessage<T> {
        @Inject
        @NoTriggerReference
        public Clock clock;
        private final String message;
        private final SerializableFunction<T, ?> transformFunction;
        private transient long initialTime;

        public TemplateMessage(String message, SerializableFunction<T, ?> transformFunction) {
            this.message = message;
            this.transformFunction = transformFunction;
        }

        public TemplateMessage(String message) {
            this.message = message;
            this.transformFunction = null;
        }

        @Initialise
        public void initialise() {
            initialTime = clock.getWallClockTime();
        }

        @Start
        public void start() {
            initialTime = clock.getWallClockTime();
        }

        public void templateAndLogToConsole(T input) {
            if (initialTime > clock.getWallClockTime()) {
                initialTime = clock.getWallClockTime();
            }
            String output = transformFunction == null ? input.toString() : transformFunction.apply(input).toString();
            System.out.println(
                    message.replace("{}", output).replace("%e", "" + clock.getEventTime())
                            .replace("%t", "" + clock.getWallClockTime())
                            .replace("%p", "" + clock.getProcessTime())
                            .replace("%de", "" + (clock.getEventTime() - initialTime))
                            .replace("%dt", "" + (clock.getWallClockTime() - initialTime))
                            .replace("%dp", "" + (clock.getProcessTime() - initialTime))
            );
        }
    }

}
