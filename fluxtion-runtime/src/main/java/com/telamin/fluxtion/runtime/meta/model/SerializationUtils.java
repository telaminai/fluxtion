/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

import com.telamin.fluxtion.runtime.meta.dto.*;

import java.io.*;

/**
 * Unified serialization utility supporting both Java Object Serialization and Kryo.
 * <p>
 * The format is selected by {@link SerializationFormat}. When {@code KRYO} is selected
 * but Kryo is not on the classpath, the utility falls back to {@code JAVA} with a
 * logged warning.
 * <p>
 * Kryo serializers are pre-registered for all DTO and model classes to avoid the cost
 * of class-name encoding and reflection-based serializer lookup on every call.
 * <p>
 * Content-Type mapping:
 * <ul>
 *   <li>{@code application/octet-stream} — Java Object Serialization (default)</li>
 *   <li>{@code application/x-kryo} — Kryo binary serialization</li>
 * </ul>
 */
public final class SerializationUtils {

    /** Content-Type for Java Object Serialization */
    public static final String CONTENT_TYPE_JAVA = "application/octet-stream";
    /** Content-Type for Kryo binary serialization */
    public static final String CONTENT_TYPE_KRYO = "application/x-kryo";

    /** System property to select the default serialization format */
    public static final String FORMAT_PROPERTY = "fluxtion.serialization.format";

    private static final boolean KRYO_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("com.esotericsoftware.kryo.Kryo");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        KRYO_AVAILABLE = available;
    }

    private SerializationUtils() {
    }

    /**
     * Returns the default format based on the system property {@code fluxtion.serialization.format}.
     * Valid values: {@code java}, {@code kryo}. Defaults to {@code kryo}.
     */
    public static SerializationFormat defaultFormat() {
        String prop = System.getProperty(FORMAT_PROPERTY, "kryo");
        if ("kryo".equalsIgnoreCase(prop) && KRYO_AVAILABLE) {
            return SerializationFormat.KRYO;
        }
        return SerializationFormat.JAVA;
    }

    /**
     * Returns the Content-Type string for the given format.
     */
    public static String contentTypeFor(SerializationFormat format) {
        return format == SerializationFormat.KRYO ? CONTENT_TYPE_KRYO : CONTENT_TYPE_JAVA;
    }

    /**
     * Returns the format for a given Content-Type string.
     */
    public static SerializationFormat formatFor(String contentType) {
        if (contentType != null && contentType.contains("x-kryo")) {
            return SerializationFormat.KRYO;
        }
        return SerializationFormat.JAVA;
    }

    /**
     * Returns true if Kryo is available on the classpath.
     */
    public static boolean isKryoAvailable() {
        return KRYO_AVAILABLE;
    }

    /**
     * Serialize an object using the specified format.
     */
    public static byte[] serialize(Object obj, SerializationFormat format) throws IOException {
        if (format == SerializationFormat.KRYO && KRYO_AVAILABLE) {
            return KryoDelegate.serialize(obj);
        }
        return javaSerialize(obj);
    }

    /**
     * Serialize an object using the default format.
     */
    public static byte[] serialize(Object obj) throws IOException {
        return serialize(obj, defaultFormat());
    }

    /**
     * Deserialize bytes using the specified format.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] bytes, SerializationFormat format) throws IOException, ClassNotFoundException {
        if (format == SerializationFormat.KRYO && KRYO_AVAILABLE) {
            return KryoDelegate.deserialize(bytes);
        }
        return javaDeserialize(bytes);
    }

    /**
     * Deserialize bytes using the specified format and ClassLoader.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeWithLoader(byte[] bytes, SerializationFormat format, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        if (format == SerializationFormat.KRYO && KRYO_AVAILABLE) {
            return KryoDelegate.deserialize(bytes, classLoader);
        }
        return javaDeserialize(bytes, classLoader);
    }

    /**
     * Deserialize bytes using the default format.
     */
    public static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        return deserialize(bytes, defaultFormat());
    }

    /**
     * Deserialize bytes using the default format and ClassLoader.
     */
    public static <T> T deserializeWithLoader(byte[] bytes, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        return deserializeWithLoader(bytes, defaultFormat(), classLoader);
    }

    // --- Java Object Serialization ---

    public static byte[] javaSerialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T javaDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T javaDeserialize(byte[] bytes, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), false, classLoader);
            }
        }) {
            return (T) ois.readObject();
        }
    }

    /**
     * Kryo operations are isolated in a delegate class to avoid ClassNotFoundException
     * when Kryo is not on the classpath.
     */
    static final class KryoDelegate {

        private static final ThreadLocal<com.esotericsoftware.kryo.Kryo> KRYO_POOL =
                ThreadLocal.withInitial(KryoDelegate::createKryo);

        static com.esotericsoftware.kryo.Kryo createKryo() {
            com.esotericsoftware.kryo.Kryo kryo = new com.esotericsoftware.kryo.Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(true);
            // Use Objenesis as fallback for classes without no-arg constructors (e.g., builder-pattern DTOs)
            kryo.setInstantiatorStrategy(new com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy(
                    new org.objenesis.strategy.StdInstantiatorStrategy()));

            // Pre-register DTO classes (meta.dto package)
            kryo.register(TopologicallySortedDependencyGraphDto.class);
            kryo.register(NodeDto.class);
            kryo.register(AnnotatedMethodDto.class);
            kryo.register(AnnotationDescriptorDto.class);
            kryo.register(MethodDescriptor.class);
            kryo.register(ExportFunctionDataDto.class);

            // Pre-register model classes (runtime.model package)
            kryo.register(CbMethodHandle.class);
            kryo.register(CbMethodHandle.CallBackType.class);
            kryo.register(ClassName.class);
            kryo.register(DirtyFlag.class);
            kryo.register(DispatchStrategy.class);
            kryo.register(ExportFunctionData.class);
            kryo.register(Field.class);
            kryo.register(Field.MappedField.class);
            kryo.register(FilterDescription.class);
            kryo.register(RemoteGenerationRequest.class);
            kryo.register(RemoteGenerationResponse.class);
            kryo.register(RemoteSourceFromDtoRequest.class);
            kryo.register(SourceGenConfig.class);

            // Common JDK types used in the object graphs
            kryo.register(java.util.ArrayList.class);
            kryo.register(java.util.HashMap.class);
            kryo.register(java.util.HashSet.class);
            kryo.register(java.util.LinkedHashMap.class);
            kryo.register(java.util.LinkedHashSet.class);
            kryo.register(java.util.TreeMap.class);
            kryo.register(java.util.Collections.emptyList().getClass());
            kryo.register(java.util.Collections.emptyMap().getClass());
            kryo.register(java.util.Collections.emptySet().getClass());
            kryo.register(java.util.Collections.singletonList(null).getClass());

            // Unmodifiable wrappers — serialize the underlying mutable data, deserialize as mutable, then wrap
            registerUnmodifiableListSerializer(kryo);
            registerUnmodifiableMapSerializer(kryo);
            registerUnmodifiableSetSerializer(kryo);

            return kryo;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void registerUnmodifiableListSerializer(com.esotericsoftware.kryo.Kryo kryo) {
            Class<?> cls = java.util.Collections.unmodifiableList(new java.util.ArrayList<>()).getClass();
            kryo.register(cls, new com.esotericsoftware.kryo.Serializer<java.util.List>() {
                @Override
                public void write(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Output out, java.util.List list) {
                    k.writeClassAndObject(out, new java.util.ArrayList<>(list));
                }
                @Override
                public java.util.List read(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Input in, Class<? extends java.util.List> type) {
                    java.util.List mutable = (java.util.List) k.readClassAndObject(in);
                    return java.util.Collections.unmodifiableList(mutable);
                }
            });
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void registerUnmodifiableMapSerializer(com.esotericsoftware.kryo.Kryo kryo) {
            Class<?> cls = java.util.Collections.unmodifiableMap(new java.util.HashMap<>()).getClass();
            kryo.register(cls, new com.esotericsoftware.kryo.Serializer<java.util.Map>() {
                @Override
                public void write(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Output out, java.util.Map map) {
                    k.writeClassAndObject(out, new java.util.HashMap<>(map));
                }
                @Override
                public java.util.Map read(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Input in, Class<? extends java.util.Map> type) {
                    java.util.Map mutable = (java.util.Map) k.readClassAndObject(in);
                    return java.util.Collections.unmodifiableMap(mutable);
                }
            });
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void registerUnmodifiableSetSerializer(com.esotericsoftware.kryo.Kryo kryo) {
            Class<?> cls = java.util.Collections.unmodifiableSet(new java.util.HashSet<>()).getClass();
            kryo.register(cls, new com.esotericsoftware.kryo.Serializer<java.util.Set>() {
                @Override
                public void write(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Output out, java.util.Set set) {
                    k.writeClassAndObject(out, new java.util.HashSet<>(set));
                }
                @Override
                public java.util.Set read(com.esotericsoftware.kryo.Kryo k, com.esotericsoftware.kryo.io.Input in, Class<? extends java.util.Set> type) {
                    java.util.Set mutable = (java.util.Set) k.readClassAndObject(in);
                    return java.util.Collections.unmodifiableSet(mutable);
                }
            });
        }

        static byte[] serialize(Object obj) {
            com.esotericsoftware.kryo.Kryo kryo = KRYO_POOL.get();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(4096, -1);
            kryo.writeClassAndObject(output, obj);
            output.close();
            return output.toBytes();
        }

        @SuppressWarnings("unchecked")
        static <T> T deserialize(byte[] bytes) {
            com.esotericsoftware.kryo.Kryo kryo = KRYO_POOL.get();
            com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(bytes);
            T result = (T) kryo.readClassAndObject(input);
            input.close();
            return result;
        }

        @SuppressWarnings("unchecked")
        static <T> T deserialize(byte[] bytes, ClassLoader classLoader) {
            com.esotericsoftware.kryo.Kryo kryo = KRYO_POOL.get();
            ClassLoader oldLoader = kryo.getClassLoader();
            try {
                kryo.setClassLoader(classLoader);
                com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(bytes);
                T result = (T) kryo.readClassAndObject(input);
                input.close();
                return result;
            } finally {
                kryo.setClassLoader(oldLoader);
            }
        }
    }
}
