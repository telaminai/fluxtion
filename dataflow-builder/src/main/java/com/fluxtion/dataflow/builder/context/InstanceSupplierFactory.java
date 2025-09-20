/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.context;

import com.fluxtion.dataflow.builder.generation.context.GenerationContext;
import com.fluxtion.dataflow.runtime.context.DataFlowContext;
import com.fluxtion.dataflow.builder.node.NodeFactory;
import com.fluxtion.dataflow.builder.node.NodeRegistry;
import com.fluxtion.dataflow.runtime.audit.Auditor;
import com.fluxtion.dataflow.runtime.node.InstanceSupplier;
import com.fluxtion.dataflow.runtime.node.InstanceSupplierNode;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class InstanceSupplierFactory implements NodeFactory<InstanceSupplier> {

    private static int count;

    @Override
    public InstanceSupplier<?> createNode(Map<String, Object> config, NodeRegistry registry) {
        final Field field = (Field) config.get(NodeFactory.FIELD_KEY);
        final String instanceName = (String) config.get(NodeFactory.INSTANCE_KEY);
        final boolean hasInstanceQualifier = !instanceName.isEmpty() && instanceName.length() > 0;
        final Type genericFieldType = field.getGenericType();
        final Class<?> rawType;
        if (genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            if (fieldArgTypes[0] instanceof ParameterizedType) {
                rawType = (Class) ((ParameterizedType) fieldArgTypes[0]).getRawType();
            } else {
                rawType = ((Class) fieldArgTypes[0]);
            }
        } else {
            rawType = Object.class;
        }
        final String typeName = "contextService_" + rawType.getSimpleName() + "_" + instanceName + count++;

        return new InstanceSupplierNode<>(
                hasInstanceQualifier ? rawType.getCanonicalName() + "_" + instanceName : rawType.getCanonicalName(),
                true,
                registry.findOrCreateNode(DataFlowContext.class, config, null),
                typeName.replace(".", "_"));
    }

    @Override
    public void preSepGeneration(GenerationContext context, Map<String, Auditor> auditorMap) {
        count = 0;
    }
}
