/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.runtime.service;

import com.telamin.fluxtion.runtime.context.DataFlowContextListener;
import com.telamin.fluxtion.runtime.annotations.ExportService;
import com.telamin.fluxtion.runtime.annotations.builder.FluxtionIgnore;
import com.telamin.fluxtion.runtime.annotations.feature.Preview;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceDeregistered;
import com.telamin.fluxtion.runtime.annotations.runtime.ServiceRegistered;
import com.telamin.fluxtion.runtime.audit.Auditor;
import com.telamin.fluxtion.runtime.node.SingleNamedNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Manages service registrations and de-registrations pushing services into nodes that have methods annotated with:
 * {@code @ServiceRegistryNode}
 * {@code @ServiceDeregisteredNode}
 */
@Preview
public class ServiceRegistryNode
        extends SingleNamedNode
        implements
        Auditor,
        @ExportService(propagate = false) ServiceListener {


    public static final String NODE_NAME = "serviceRegistry";
    @FluxtionIgnore
    private final Map<RegistrationKey, List<Callback>> serviceCallbackMap = new HashMap<>();
    @FluxtionIgnore
    private final Map<RegistrationKey, List<Callback>> serviceDeregisterCallbackMap = new HashMap<>();
    @FluxtionIgnore
    private final Map<Class<?>, List<Callback>> serviceWithNameCallbacks = new HashMap<>();
    @FluxtionIgnore
    private final Map<Class<?>, List<Callback>> serviceDeregisterWithNameCallbacks = new HashMap<>();
    @FluxtionIgnore
    private final RegistrationKey tempKey = new RegistrationKey();

    public ServiceRegistryNode() {
        super(NODE_NAME);
    }

    @Override
    public void registerService(Service<?> service) {
        auditLog.info("registerService", service);
        tempKey.serviceClass(service.serviceClass())
                .serviceName(service.serviceName());
        List<Callback> callBackMethods = serviceCallbackMap.get(tempKey);
        if (callBackMethods != null) {
            for (int i = 0; i < callBackMethods.size(); i++) {
                Callback callBackMethod = callBackMethods.get(i);
                callBackMethod.invoke(service.instance(), service.serviceName());
            }
        }

        for (Callback nameCallback : serviceWithNameCallbacks.getOrDefault(service.serviceClass(), Collections.emptyList())) {
            nameCallback.invoke(service.instance(), service.serviceName());
        }
    }

    @Override
    public void deRegisterService(Service<?> service) {
        auditLog.info("deRegisterService", service);
        tempKey.serviceClass(service.serviceClass())
                .serviceName(service.serviceName());
        List<Callback> callBackMethods = serviceDeregisterCallbackMap.get(tempKey);
        if (callBackMethods != null) {
            for (int i = 0; i < callBackMethods.size(); i++) {
                Callback callBackMethod = callBackMethods.get(i);
                callBackMethod.invoke(service.instance(), service.serviceName());
            }
        }

        for (Callback nameCallback : serviceDeregisterWithNameCallbacks.getOrDefault(service.serviceClass(), Collections.emptyList())) {
            nameCallback.invoke(service.instance(), service.serviceName());
        }

    }

    @Override
    public void init() {
        serviceCallbackMap.clear();
    }

    @Override
    public void nodeRegistered(Object node, String nodeName) {
        if (node instanceof DataFlowContextListener) {
            ((DataFlowContextListener) node).currentContext(getDataFlowContext());
        }

        Class<?> clazz = node.getClass();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {

            ServiceRegistered registerAnnotation = method.getAnnotation(ServiceRegistered.class);
            final int parameterCount = method.getParameterCount();
            final boolean namedService = parameterCount == 2 && CharSequence.class.isAssignableFrom(method.getParameterTypes()[1]);
            if (registerAnnotation != null
                    && Modifier.isPublic(method.getModifiers())
                    && (parameterCount == 1 || namedService)
            ) {

                Class<?> parameterType = method.getParameterTypes()[0];
                RegistrationKey key = new RegistrationKey(
                        parameterType,
                        registerAnnotation.value().isEmpty() ? parameterType.getCanonicalName() : registerAnnotation.value());
                if (namedService) {
                    serviceWithNameCallbacks.compute(parameterType,
                            (k, v) -> {
                                List<Callback> list = v == null ? new ArrayList<>() : v;
                                list.add(new Callback(method, node, namedService));
                                return list;
                            });
                } else {
                    serviceCallbackMap.compute(key,
                            (k, v) -> {
                                List<Callback> list = v == null ? new ArrayList<>() : v;
                                list.add(new Callback(method, node, namedService));
                                return list;
                            });
                }
            }

            ServiceDeregistered deregisterAnnotation = method.getAnnotation(ServiceDeregistered.class);
            if (deregisterAnnotation != null
                    && Modifier.isPublic(method.getModifiers())
                    && (parameterCount == 1 || namedService)) {

                Class<?> parameterType = method.getParameterTypes()[0];
                RegistrationKey key = new RegistrationKey(
                        parameterType,
                        deregisterAnnotation.value().isEmpty() ? parameterType.getCanonicalName() : deregisterAnnotation.value());

                if (namedService) {
//                    serviceDeregisterWithNameCallbacks.add(new Callback(method, node, true));
                    serviceDeregisterWithNameCallbacks.compute(parameterType,
                            (k, v) -> {
                                List<Callback> list = v == null ? new ArrayList<>() : v;
                                list.add(new Callback(method, node, namedService));
                                return list;
                            });
                } else {
                    serviceDeregisterCallbackMap.compute(key,
                            (k, v) -> {
                                List<Callback> list = v == null ? new ArrayList<>() : v;
                                list.add(new Callback(method, node, false));
                                return list;
                            });
                }

            }
        }
    }

    @Data
    @Accessors(chain = true, fluent = true)
    @AllArgsConstructor
    @NoArgsConstructor
    private static class RegistrationKey {
        Class<?> serviceClass;
        String serviceName;
    }

    @Data
    @Accessors(chain = true, fluent = true)
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Callback {
        Method method;
        Object node;
        boolean namedService;

        @SneakyThrows
        void invoke(Object service, String name) {
            if (namedService) {
                method.invoke(node, service, name);
            } else {
                method.invoke(node, service);
            }
        }
    }
}
