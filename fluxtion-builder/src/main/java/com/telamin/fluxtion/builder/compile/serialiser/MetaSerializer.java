/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.serialiser;

import com.telamin.fluxtion.builder.generation.model.Field;
import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;
import com.telamin.fluxtion.runtime.flowfunction.FlowFunction;
import com.telamin.fluxtion.runtime.flowfunction.function.MergeProperty;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

import java.io.File;

public interface MetaSerializer {

    static String classToSource(FieldContext<Class<?>> fieldContext) {
        fieldContext.getImportList().add(File.class);
        Class<?> clazz = fieldContext.getInstanceToMap();
        return clazz.getCanonicalName() + ".class";
    }

    static String mergePropertyToSource(FieldContext<MergeProperty<?, ?>> fieldContext) {
        fieldContext.getImportList().add(MergeProperty.class);
        MergeProperty<?, ?> mergeProperty = fieldContext.getInstanceToMap();
        LambdaReflection.SerializableBiConsumer<?, ?> setValue = mergeProperty.getSetValue();
        String containingClass = setValue.getContainingClass().getSimpleName();
        String methodName = setValue.method().getName();
        String lambda = containingClass + "::" + methodName;
        String triggerName = "null";
        //
        FlowFunction<?> trigger = mergeProperty.getTrigger();
        for (Field nodeField : fieldContext.getNodeFields()) {
            if (nodeField.getInstance() == trigger) {
                triggerName = nodeField.getName();
                break;
            }
        }
        return "new MergeProperty<>("
                + triggerName + ", " + lambda + "," + mergeProperty.isTriggering() + "," + mergeProperty.isMandatory() + ")";
    }

    static String methodReferenceToSource(FieldContext<LambdaReflection.MethodReferenceReflection> fieldContext) {
        LambdaReflection.MethodReferenceReflection ref = fieldContext.getInstanceToMap();
        fieldContext.getImportList().add(ref.getContainingClass());
        String sourceString = "";
        boolean foundMatch = false;
        if (ref.isDefaultConstructor()) {
            sourceString = ref.getContainingClass().getSimpleName() + "::new";
        } else if (ref.captured().length > 0) {
            //see if we can find the reference and set the instance
            Object functionInstance = ref.captured()[0];
            for (Field nodeField : fieldContext.getNodeFields()) {
                if (nodeField.getInstance() == functionInstance) {
                    sourceString = nodeField.getName() + "::" + ref.method().getName();
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                sourceString = "new " + ref.getContainingClass().getSimpleName() + "()::" + ref.method().getName();
            }
        } else {
            if (ref.getContainingClass().getTypeParameters().length > 0) {
                String typeParam = "<Object";
                for (int i = 1; i < ref.getContainingClass().getTypeParameters().length; i++) {
                    typeParam += ", Object";
                }
                typeParam += ">";
                sourceString = ref.getContainingClass().getSimpleName() + typeParam + "::" + ref.method().getName();
            } else {
                sourceString = ref.getContainingClass().getSimpleName() + "::" + ref.method().getName();
            }
        }
        return sourceString;
    }
}
