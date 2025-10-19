/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.compile.serialiser;

import com.google.auto.service.AutoService;
import com.telamin.fluxtion.builder.generation.config.ClassSerializerRegistry;
import com.telamin.fluxtion.builder.generation.serialiser.FieldContext;
import com.telamin.fluxtion.runtime.flowfunction.function.MergeProperty;
import com.telamin.fluxtion.runtime.partition.LambdaReflection;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.function.Function;

@AutoService(ClassSerializerRegistry.class)
public class JavaSerializerRegistry implements ClassSerializerRegistry {

    private final Map<Class<?>, Function<FieldContext, String>> classSerializerMap = new HashMap<>();

    public JavaSerializerRegistry() {
        classSerializerMap.put(String.class, BasicTypeSerializer::stringToSource);
        classSerializerMap.put(Character.class, BasicTypeSerializer::charToSource);
        classSerializerMap.put(char.class, BasicTypeSerializer::charToSource);
        classSerializerMap.put(Long.class, BasicTypeSerializer::longToSource);
        classSerializerMap.put(long.class, BasicTypeSerializer::longToSource);
        classSerializerMap.put(int.class, BasicTypeSerializer::intToSource);
        classSerializerMap.put(Integer.class, BasicTypeSerializer::intToSource);
        classSerializerMap.put(Short.class, BasicTypeSerializer::shortToSource);
        classSerializerMap.put(short.class, BasicTypeSerializer::shortToSource);
        classSerializerMap.put(Byte.class, BasicTypeSerializer::byteToSource);
        classSerializerMap.put(byte.class, BasicTypeSerializer::byteToSource);
        classSerializerMap.put(Double.class, BasicTypeSerializer::doubleToSource);
        classSerializerMap.put(double.class, BasicTypeSerializer::doubleToSource);
        classSerializerMap.put(Float.class, BasicTypeSerializer::floatToSource);
        classSerializerMap.put(float.class, BasicTypeSerializer::floatToSource);
        classSerializerMap.put(Boolean.class, BasicTypeSerializer::booleanToSource);
        classSerializerMap.put(boolean.class, BasicTypeSerializer::booleanToSource);
        classSerializerMap.put(HashMap.class, CollectionSerializer::hashMapToSource);
        classSerializerMap.put(Map.class, CollectionSerializer::mapToSource);
        classSerializerMap.put(List.class, CollectionSerializer::listToSource);
        classSerializerMap.put(ArrayList.class, CollectionSerializer::arrayListToSource);
        classSerializerMap.put(Set.class, CollectionSerializer::setToSource);
        classSerializerMap.put(Duration.class, TimeSerializer::durationToSource);
        classSerializerMap.put(Instant.class, TimeSerializer::instantToSource);
        classSerializerMap.put(LocalDate.class, TimeSerializer::localDateToSource);
        classSerializerMap.put(LocalTime.class, TimeSerializer::localTimeToSource);
        classSerializerMap.put(LocalDateTime.class, TimeSerializer::localDateTimeToSource);
        classSerializerMap.put(Period.class, TimeSerializer::periodToSource);
        classSerializerMap.put(ZoneId.class, TimeSerializer::zoneIdToSource);
        classSerializerMap.put(ZonedDateTime.class, TimeSerializer::zoneDateTimeToSource);
        classSerializerMap.put(Date.class, TimeSerializer::dateToSource);
        classSerializerMap.put(File.class, IoSerializer::fileToSource);
        classSerializerMap.put(URI.class, IoSerializer::uriToSource);
        classSerializerMap.put(URL.class, IoSerializer::urlToSource);
        classSerializerMap.put(InetSocketAddress.class, IoSerializer::inetSocketAddressToSource);
        classSerializerMap.put(SimpleDateFormat.class, FormatSerializer::simpleDataFormatToSource);
        classSerializerMap.put(DateFormat.class, FormatSerializer::simpleDataFormatToSource);
        classSerializerMap.put(DecimalFormat.class, FormatSerializer::decimalFormatToSource);
        classSerializerMap.put(NumberFormat.class, FormatSerializer::decimalFormatToSource);
        classSerializerMap.put(Class.class, MetaSerializer::classToSource);
        classSerializerMap.put(MergeProperty.class, MetaSerializer::mergePropertyToSource);
        classSerializerMap.put(LambdaReflection.MethodReferenceReflection.class, MetaSerializer::methodReferenceToSource);
    }

    @Override
    public String targetLanguage() {
        return "java";
    }

    @Override
    public Map<Class<?>, Function<FieldContext, String>> classSerializerMap() {
        return classSerializerMap;
    }
}
