/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.generation.model;

import com.google.common.base.Predicate;
import com.telamin.fluxtion.builder.generation.model.Field.MappedField;
import com.telamin.fluxtion.runtime.annotations.builder.AssignToField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 2024 gregory higgins.
 */
@SuppressWarnings("rawtypes")
class ConstructorMatcherPredicate implements Predicate<Constructor> {

    private final Logger LOGGER = LoggerFactory.getLogger(ConstructorMatcherPredicate.class);
    private final MappedField[] cstrArgList;
    private final HashSet<MappedField> privateFields;
    private final boolean nameAndType;

    public static Predicate<Constructor> matchConstructorNameAndType(MappedField[] cstrArgList, HashSet<MappedField> privateFields) {
        return new ConstructorMatcherPredicate(cstrArgList, privateFields);
    }

    public static Predicate<Constructor> matchConstructorType(MappedField[] cstrArgList, HashSet<MappedField> privateFields) {
        return new ConstructorMatcherPredicate(cstrArgList, privateFields, false);
    }

    public ConstructorMatcherPredicate(MappedField[] cstrArgList, HashSet<MappedField> privateFields) {
        this.cstrArgList = cstrArgList;
        this.privateFields = privateFields;
        this.nameAndType = true;
    }

    public ConstructorMatcherPredicate(MappedField[] cstrArgList, HashSet<MappedField> privateFields, boolean nameAndType) {
        this.cstrArgList = cstrArgList;
        this.privateFields = privateFields;
        this.nameAndType = nameAndType;
    }

    @Override
    public boolean apply(Constructor input) {
        boolean match = cstrArgList[0] != null;
        if (match) {
            LOGGER.debug("already matched constructor, ignoring");
            return false;
        } else {
            LOGGER.debug("unmatched constructor, reset constructorArgs");
            Arrays.fill(cstrArgList, null);
        }
        Parameter[] parameters = input.getParameters();
        int parameterCount = parameters.length;
        if (parameterCount == 0 || parameterCount != privateFields.size()) {
            LOGGER.debug("parameterCount:{} privateFieldsCount:{} mismatch reject constructor", parameterCount, privateFields.size());
        } else {
            //possible match
            int matchCount = 0;
            for (MappedField mappedInstance : privateFields) {
                String varName = mappedInstance.mappedName;
                Class<?> parentClass = mappedInstance.parentClass();
                Class<?> realClass = mappedInstance.realClass();
                LOGGER.debug("match field var:{}, type:{}", varName, parentClass);
                //                            Class<?> parentClass = mappedInstance.collection?List.class:parentInstance.getClass();
                boolean matchOnName = false;
                LOGGER.debug("matching constructor by type and name");
                //match array
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i] == null) {
                        continue;
                    }
                    Parameter parameter = parameters[i];
                    String paramName = parameter.getName();
                    if (parameter.getAnnotation(AssignToField.class) != null) {
                        paramName = parameter.getAnnotation(AssignToField.class).value();
                        LOGGER.debug("assigning parameter name from annotation AssignToField " +
                                "fieldName:'{}' overriding:'{}'", paramName, parameter.getName());
                    }
                    Class<?> parameterType = parameters[i].getType();
                    LOGGER.debug("constructor parameter type:{}, paramName:{}, varName:{}", parameterType, paramName, varName);
                    if (parameterType != null
                            && (parameterType.isAssignableFrom(parentClass) || parameterType.isAssignableFrom(realClass))
                            && paramName.equals(varName)) {
                        matchCount++;
                        parameters[i] = null;
                        cstrArgList[i] = mappedInstance;
                        matchOnName = true;
                        LOGGER.debug("matched constructor arg:{}, by type and name", paramName);
                        break;
                    }
                }
                if (!matchOnName && !nameAndType) {
                    LOGGER.debug("no match, matching constructor by type only");
                    for (int i = 0; i < parameters.length; i++) {
                        if (parameters[i] == null) {
                            continue;
                        }
                        Class<?> parameterType = parameters[i].getType();
                        String paramName = parameters[i].getName();
                        LOGGER.debug("constructor parameter type:{}, paramName:{}, varName:{}", parameterType, paramName, varName);
                        if (parameterType != null && (parameterType.isAssignableFrom(parentClass) || parameterType.isAssignableFrom(realClass))) {
                            matchCount++;
                            parameters[i] = null;
                            cstrArgList[i] = mappedInstance;
                            matchOnName = true;
                            LOGGER.debug("matched constructor arg:{}, by type only", paramName);
                            break;
                        }
                    }
                    if (!matchOnName) {
                        LOGGER.debug("no match for varName:{}", varName);
                        break;
                    }
                }
            }
            if (matchCount == parameterCount) {
                LOGGER.debug("matched constructor:{}", input);
                match = true;
            } else {
                LOGGER.debug("unmatched constructor:{}", input);
                Arrays.fill(cstrArgList, null);
            }
        }
        return match;
    }

    public static List<String> validateNoTypeClash(Set<MappedField> privateFields, Constructor constructor) {
        Set<String> mappedNames = Arrays.stream(constructor.getParameters())
                .filter(p -> p.getAnnotation(AssignToField.class) != null)
                .map(p -> p.getAnnotation(AssignToField.class))
                .map(AssignToField::value)
                .collect(Collectors.toSet());

        Set<MappedField> filteredFields = privateFields.stream()
                .filter(m -> !mappedNames.contains(m.mappedName))
                .collect(Collectors.toSet());

        List<String> output = filteredFields.stream()
                .filter(m -> {
                    Class<?> classToTest = m.parentClass();
                    HashSet<MappedField> setToTest = new HashSet<>(filteredFields);
                    setToTest.remove(m);
                    return setToTest.stream()
                            .map(MappedField::parentClass)
                            .anyMatch(c -> {
                                boolean val = c.isAssignableFrom(classToTest) || classToTest.isAssignableFrom(c);
                                return val;
                            });
                })
                .map(MappedField::getMappedName)
                .collect(Collectors.toList());
        return output;
    }

}
