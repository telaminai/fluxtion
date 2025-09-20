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

@Preview
public interface ServiceRegistry extends ServiceListener {

    default <T> void registerService(T service) {
        registerService(new Service<T>(service));
    }

    default <T> void registerService(T service, String serviceName) {
        registerService(new Service<T>(service, serviceName));
    }

    default <S, T extends S> void registerService(T service, Class<S> serviceClass) {
        registerService(new Service<S>(service, serviceClass));
    }

    default <S, T extends S> void registerService(T service, Class<S> serviceClass, String serviceName) {
        registerService(new Service<S>(service, serviceClass, serviceName));
    }

    default <T> void deRegisterService(T service, String serviceName) {
        deRegisterService(new Service<T>(service, serviceName));
    }

    default <S, T extends S> void deRegisterService(T service, Class<S> serviceClass) {
        deRegisterService(new Service<S>(service, serviceClass));
    }

    default <S, T extends S> void deRegisterService(T service, Class<S> serviceClass, String serviceName) {
        deRegisterService(new Service<S>(service, serviceClass, serviceName));
    }
}
