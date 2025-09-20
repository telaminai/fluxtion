/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.output;

import java.util.function.Function;

public abstract class AbstractMessageSink<T> implements MessageSink<T> {

    private Function<? super T, ?> valueMapper = Function.identity();

    @Override
    public final void accept(T t) {
        sendToSink(valueMapper.apply(t));
    }

    @Override
    public void setValueMapper(Function<? super T, ?> valueMapper) {
        this.valueMapper = valueMapper;
    }

    abstract protected void sendToSink(Object value);
}
