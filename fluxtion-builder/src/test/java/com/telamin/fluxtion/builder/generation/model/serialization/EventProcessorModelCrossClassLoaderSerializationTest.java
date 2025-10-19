package com.telamin.fluxtion.builder.generation.model.serialization;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.generation.model.SimpleEventProcessorModel;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessor;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessorBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Verifies that SimpleEventProcessorModel can be serialized and deserialized when the
 * deserializing ClassLoader cannot load the event classes referenced by the model's
 * dispatch maps. The model should not require those classes at deserialization time
 * because it stores class names (String) rather than Class literals in its public API.
 */
public class EventProcessorModelCrossClassLoaderSerializationTest {

    @Test
    public void serializeDeserializeWithRestrictedClassLoader() throws Exception {
        // Build a minimal graph that includes an event type in the dispatch map
        InMemoryEventProcessor ep = InMemoryEventProcessorBuilder.interpreted(c ->
                DataFlowBuilder.subscribe(String.class).console("%s"), false);

        SimpleEventProcessorModel original = extractModel(ep);
        byte[] bytes = toBytes(original);

        // ClassLoader that refuses to load java.lang.String to simulate missing event class
        ClassLoader restricted = new ClassLoader(EventProcessorModelCrossClassLoaderSerializationTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if ("java.lang.String".equals(name)) {
                    throw new ClassNotFoundException("blocked: " + name);
                }
                return super.loadClass(name, resolve);
            }
        };

        // Custom OIS that resolves classes via the restricted loader
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                String name = desc.getName();
                try {
                    return Class.forName(name, false, restricted);
                } catch (ClassNotFoundException e) {
                    return super.resolveClass(desc);
                }
            }
        };

        Object obj = ois.readObject();
        ois.close();
        // If we get here without CNFE, deserialization succeeded even though String was blocked
        MatcherAssert.assertThat(obj, Matchers.instanceOf(SimpleEventProcessorModel.class));
        SimpleEventProcessorModel clone = (SimpleEventProcessorModel) obj;

        // Validate that dispatch map keys are Strings and include the String event type name
        Map<String, ?> dispatchMap = clone.getDispatchMap();
        MatcherAssert.assertThat(dispatchMap.keySet(), Matchers.hasItem("java.lang.String"));

        // Also validate post/handler-only maps are accessible
        clone.getPostDispatchMap();
        clone.getHandlerOnlyDispatchMap();

        // And import classes are exposed as names
        MatcherAssert.assertThat(clone.getImportClasses(), Matchers.everyItem(Matchers.instanceOf(String.class)));
    }

    private static byte[] toBytes(Serializable s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(s);
        }
        return bos.toByteArray();
    }

    private static SimpleEventProcessorModel extractModel(InMemoryEventProcessor ep) throws Exception {
        Field f = InMemoryEventProcessor.class.getDeclaredField("simpleEventProcessorModel");
        f.setAccessible(true);
        return (SimpleEventProcessorModel) f.get(ep);
    }
}
