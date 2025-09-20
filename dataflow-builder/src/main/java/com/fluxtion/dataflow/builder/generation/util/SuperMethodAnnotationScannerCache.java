/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.fluxtion.dataflow.builder.generation.util;

import com.fluxtion.dataflow.runtime.flowfunction.Tuple;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.fluxtion.dataflow.builder.generation.util.SuperMethodAnnotationScanner.getAnnotations;


public class SuperMethodAnnotationScannerCache {

    private  Map<Tuple<Method, Class<?>>, Boolean> annotationHierarchyMap = new HashMap<>();

    public SuperMethodAnnotationScannerCache reset(){
        annotationHierarchyMap.clear();
        return this;
    }

    public <A extends Annotation> boolean annotationInHierarchy(Method m, Class<A> t) {
        Tuple<Method, Class<?>> key = Tuple.build(m, t);
        if (annotationHierarchyMap.containsKey(key)) {
            return annotationHierarchyMap.get(key);
        } else {
            boolean hasAnnotation = getAnnotations(m, t).size() > 0;
            annotationHierarchyMap.put(key, hasAnnotation);
            return hasAnnotation;
        }
    }
}
