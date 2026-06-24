// BROWSER-BUNDLE HELPER (no upstream counterpart).
//
// Hand-written replacement for the build-config lambda body that previously
// lived in FlowDataSupplier.build():
//
//   c -> {
//       for (Object node : nodeList) c.addNode(node);
//       publisNodeMap.forEach(c::addPublicNode);
//   }
//
// The forEach call captures another bound method reference (c::addPublicNode)
// — a second invokedynamic site nested inside the first. Both compile to
// JAR-shipped Lambda$N classes that CheerpJ Java 8 mode fails to re-resolve
// on subsequent builds.
//
// This class:
//   - is a real top-level class file (no invokedynamic for its own body)
//   - replaces forEach with a plain for-each on entrySet (no nested lambda)
//   - hand-forges writeReplace() so InMemoryEventProcessorBuilder.interpreted()
//     can resolve getContainingClass() / method() via LambdaReflection. The
//     forged SerializedLambda points implClass at EventProcessorConfig and
//     implMethodName at addNode (a real method on that class).

package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.config.EventProcessorConfig;
import com.telamin.fluxtion.runtime.audit.EventLogControlEvent;
import com.telamin.fluxtion.runtime.partition.LambdaReflection.SerializableConsumer;

import java.io.ObjectStreamException;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.util.List;
import java.util.Map;

final class BrowserBuildConfigConsumer implements SerializableConsumer<EventProcessorConfig> {

    private final List<Object> nodeList;
    private final Map<Object, String> publicNodeMap;
    private final EventLogControlEvent.LogLevel auditLogLevel;

    BrowserBuildConfigConsumer(
            List<Object> nodeList,
            Map<Object, String> publicNodeMap,
            EventLogControlEvent.LogLevel auditLogLevel) {
        this.nodeList = nodeList;
        this.publicNodeMap = publicNodeMap;
        this.auditLogLevel = auditLogLevel;
    }

    @Override
    public void accept(EventProcessorConfig c) {
        for (Object node : nodeList) {
            c.addNode(node);
        }
        for (Map.Entry<Object, String> e : publicNodeMap.entrySet()) {
            c.addPublicNode(e.getKey(), e.getValue());
        }
        if (auditLogLevel != null) {
            c.addEventAudit(auditLogLevel);
        }
    }

    private Object writeReplace() throws ObjectStreamException {
        return new SerializedLambda(
                BrowserBuildConfigConsumer.class,
                "com/telamin/fluxtion/runtime/partition/LambdaReflection$SerializableConsumer",
                "accept",
                "(Ljava/lang/Object;)V",
                MethodHandleInfo.REF_invokeVirtual,
                "com/telamin/fluxtion/builder/generation/config/EventProcessorConfig",
                "addNode",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                "(Ljava/lang/Object;)V",
                new Object[0]
        );
    }
}
