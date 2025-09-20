/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.service;

import com.telamin.fluxtion.runtime.annotations.feature.Preview;
import com.telamin.fluxtion.runtime.lifecycle.Lifecycle;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@Preview
@ToString
public class Service<T> implements Lifecycle {

    private final Class<T> serviceClass;
    private final String serviceName;
    private final T instance;

    public <S extends T> Service(S instance, Class<T> serviceClass, String serviceName) {
        this.serviceClass = serviceClass;
        this.serviceName = serviceName;
        this.instance = instance;
    }

    @SuppressWarnings("unchecked")
    public <S extends T> Service(S instance, String serviceName) {
        this.serviceClass = (Class<T>) instance.getClass();
        this.serviceName = serviceName;
        this.instance = instance;
    }

    public <S extends T> Service(S instance, Class<T> serviceClass) {
        this(instance, serviceClass, serviceClass.getCanonicalName());
    }

    @SuppressWarnings("unchecked")
    public <S extends T> Service(S instance) {
        this(instance, (Class<T>) instance.getClass());
    }


    @Override
    public void init() {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).init();
        }
    }

    @Override
    public void start() {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).start();
        }
    }

    @Override
    public void startComplete() {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).startComplete();
        }
    }

    @Override
    public void stop() {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).stop();
        }
    }

    @Override
    public void tearDown() {
        if (instance instanceof Lifecycle) {
            ((Lifecycle) instance).tearDown();
        }
    }
}
