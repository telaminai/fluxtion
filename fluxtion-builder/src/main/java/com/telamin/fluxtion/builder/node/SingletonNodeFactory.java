/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.node;

import java.util.Map;

public class SingletonNodeFactory<T> implements NodeFactory<T> {
    private final T instance;
    private final Class<T> classType;
    private final String factoryName;

    public <S extends T> SingletonNodeFactory(S instance, Class<T> classType, String factoryName) {
        this.instance = instance;
        this.classType = classType;
        this.factoryName = factoryName;
    }

    @Override
    public T createNode(Map<String, Object> config, NodeRegistry registry) {
        return registry.registerNode(instance, factoryName);
    }

    @Override
    public String factoryName() {
        return factoryName;
    }

    @Override
    public Class<T> injectionType() {
        return classType;
    }
}
