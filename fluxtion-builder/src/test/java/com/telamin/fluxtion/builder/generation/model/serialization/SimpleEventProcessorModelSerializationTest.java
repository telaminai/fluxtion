package com.telamin.fluxtion.builder.generation.model.serialization;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.builder.generation.model.EventProcessorModel;
import com.telamin.fluxtion.builder.generation.model.SimpleEventProcessorModel;
import com.telamin.fluxtion.builder.generation.model.SourceCbMethodHandle;
import com.telamin.fluxtion.builder.generation.model.SourceField;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessor;
import com.telamin.fluxtion.builder.generation.target.InMemoryEventProcessorBuilder;
import com.telamin.fluxtion.builder.filter.FilterDescription;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verifies that the generator-facing EventProcessorModel view is identical after Java serialization
 * round-trip. This ensures Velocity/JavaSource generation will be identical for serialized vs
 * in-memory models without changing the generators.
 */
public class SimpleEventProcessorModelSerializationTest {

    @Test
    public void roundTripSerializationPreservesGeneratorView() throws Exception {
        // Build a tiny graph using the DSL to exercise callbacks, fields, and filters
        InMemoryEventProcessor ep = InMemoryEventProcessorBuilder.interpreted(c -> {
            // simple stream that exercises lifecycle + event handling paths
            DataFlowBuilder
                    .subscribe(String.class)
                    .map(String::length)
                    .id("len")
                    .console("len=%s");
        }, false);

        // Reflect the underlying SimpleEventProcessorModel from the in-memory EP
        SimpleEventProcessorModel original = extractModel(ep);
        // Sanity: ensure meta-model is populated
        MatcherAssert.assertThat(original.getNodeFields().isEmpty(), Matchers.is(false));

        // Round-trip via Java serialization
        SimpleEventProcessorModel clone = roundTrip(original);

        // Compare generator-visible views used by JavaSourceGenerator/Velocity
        assertModelsEquivalent(original, clone);
    }

    private static SimpleEventProcessorModel extractModel(InMemoryEventProcessor ep) throws Exception {
        java.lang.reflect.Field f = InMemoryEventProcessor.class.getDeclaredField("simpleEventProcessorModel");
        f.setAccessible(true);
        return (SimpleEventProcessorModel) f.get(ep);
    }

    private static SimpleEventProcessorModel roundTrip(SimpleEventProcessorModel model) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(model);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (SimpleEventProcessorModel) ois.readObject();
        }
    }

    private static void assertModelsEquivalent(EventProcessorModel a, EventProcessorModel b) {
        // lifecycle lists
        assertCbListEq(a.getInitialiseMethods(), b.getInitialiseMethods());
        assertCbListEq(a.getStartMethods(), b.getStartMethods());
        assertCbListEq(a.getStartCompleteMethods(), b.getStartCompleteMethods());
        assertCbListEq(a.getStopMethods(), b.getStopMethods());
        assertCbListEq(a.getBatchPauseMethods(), b.getBatchPauseMethods());
        assertCbListEq(a.getEventEndMethods(), b.getEventEndMethods());
        assertCbListEq(a.getBatchEndMethods(), b.getBatchEndMethods());
        assertCbListEq(a.getTearDownMethods(), b.getTearDownMethods());

        // dispatch-related
        assertCbListEq(a.getAllPostEventCallBacks(), b.getAllPostEventCallBacks());
        assertCbListEq(a.getTriggerOnlyCallBacks(), b.getTriggerOnlyCallBacks());
        assertDispatchMapEq(a.getDispatchMap(), b.getDispatchMap());
        assertDispatchMapEq(a.getPostDispatchMap(), b.getPostDispatchMap());
        assertDispatchMapEq(a.getHandlerOnlyDispatchMap(), b.getHandlerOnlyDispatchMap());

        // fields
        assertFieldListEq(a.getNodeFields(), b.getNodeFields());
        assertFieldListEq(a.getTopologicallySortedNodeFields(), b.getTopologicallySortedNodeFields());
        assertFieldListEq(a.getNodeRegistrationListenerFields(), b.getNodeRegistrationListenerFields());

        // flags and filter descriptions
        MatcherAssert.assertThat(a.isDispatchOnlyVersion(), Matchers.is(b.isDispatchOnlyVersion()));
        MatcherAssert.assertThat(mapList(a.getFilterDescriptionList()), Matchers.is(mapList(b.getFilterDescriptionList())));

        // forked triggers
        MatcherAssert.assertThat(sorted(a.getForkedTriggerInstances()), Matchers.is(sorted(b.getForkedTriggerInstances())));

        // dirty map keys (values are complex, compare keys only)
        MatcherAssert.assertThat(sorted(a.getDirtyFieldMap().keySet()), Matchers.is(sorted(b.getDirtyFieldMap().keySet())));
    }

    private static <T extends SourceCbMethodHandle> void assertCbListEq(List<T> l1, List<T> l2) {
        MatcherAssert.assertThat(mapCbList(l1), Matchers.is(mapCbList(l2)));
    }

    private static <T extends SourceField> void assertFieldListEq(List<T> l1, List<T> l2) {
        MatcherAssert.assertThat(mapFieldList(l1), Matchers.is(mapFieldList(l2)));
    }

    private static <T extends SourceCbMethodHandle> List<String> mapCbList(List<T> list) {
        return list.stream()
                .map(cb -> String.join("|",
                        safe(cb.getMethodTarget()),
                        safe(cb.getMethodName()),
                        Integer.toString(cb.getParameterCount()),
                        className(cb.getReturnType()),
                        safe(cb.getVariableName()),
                        className(cb.getParameterClass()),
                        Boolean.toString(cb.isEventHandler()),
                        Boolean.toString(cb.isExportedHandler()),
                        Boolean.toString(cb.isPostEventHandler()),
                        Boolean.toString(cb.isInvertedDirtyHandler()),
                        Boolean.toString(cb.isGuardedParent()),
                        Boolean.toString(cb.isNoPropagateEventHandler()),
                        Boolean.toString(cb.isForkExecution())
                ))
                .sorted()
                .collect(Collectors.toList());
    }

    private static <T extends SourceField> List<String> mapFieldList(List<T> list) {
        return list.stream()
                .map(f -> String.join("|",
                        safe(f.getName()),
                        safe(f.getFqn()),
                        Boolean.toString(f.isPublicAccess()),
                        className(f.getFieldClass()),
                        Boolean.toString(f.isAuditor()),
                        Boolean.toString(f.isAuditInvocations()),
                        Boolean.toString(f.isGeneric())
                ))
                .sorted()
                .collect(Collectors.toList());
    }

    private static <T extends SourceCbMethodHandle> void assertDispatchMapEq(Map<Class<?>, Map<FilterDescription, List<T>>> m1,
                                                                             Map<Class<?>, Map<FilterDescription, List<T>>> m2) {
        List<String> a = flattenDispatch(m1);
        List<String> b = flattenDispatch(m2);
        MatcherAssert.assertThat(a, Matchers.is(b));
    }

    private static <T extends SourceCbMethodHandle> List<String> flattenDispatch(Map<Class<?>, Map<FilterDescription, List<T>>> m) {
        List<String> out = new ArrayList<>();
        List<Class<?>> keys = new ArrayList<>(m.keySet());
        keys.sort(Comparator.comparing(Class::getName));
        for (Class<?> k : keys) {
            List<FilterDescription> filters = new ArrayList<>(m.get(k).keySet());
            filters.sort(Comparator.comparing(SimpleEventProcessorModelSerializationTest::filterKey));
            for (FilterDescription fd : filters) {
                List<String> mappedCbs = mapCbList(m.get(k).getOrDefault(fd, Collections.emptyList()));
                out.add(k.getName() + "->" + filterKey(fd) + "->" + mappedCbs);
            }
        }
        return out;
    }

    private static String filterKey(FilterDescription fd) {
        if (fd == null) return "null";
        String cls = fd.getEventClass() == null ? "null" : fd.getEventClass().getName();
        return cls + ":" + (fd.isIntFilter() ? Integer.toString(fd.getValue()) : "-") + ":" + Objects.toString(fd.getStringValue(), "-");
    }

    private static List<String> mapList(List<FilterDescription> list) {
        return list.stream().map(SimpleEventProcessorModelSerializationTest::filterKey).sorted().collect(Collectors.toList());
    }

    private static <T> List<T> sorted(Collection<T> c) {
        return c.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String className(Class<?> c) { return c == null ? "" : c.getName(); }
}
