package com.telamin.fluxtion.builder.flowfunction;

import com.telamin.fluxtion.builder.generation.context.GenerationContext;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

public class DataFlowBuildServiceTest {

    @Test
    public void missingProviderHasActionableDiagnostic() {
        try {
            DataFlowBuildService.load(contextWithNoProviders());
            fail("Expected missing-provider diagnostic");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("DataFlowBuilder.build()"));
            assertThat(e.getMessage(), containsString("Fluxtion build-engine provider"));
            assertThat(e.getMessage(), containsString("com.telamin.fluxtion:fluxtion-builder"));
            assertThat(e.getMessage(), containsString("fluxtion-runtime only"));
        }
    }

    private static GenerationContext contextWithNoProviders() {
        final ClassLoader noProviderLoader = new URLClassLoader(new URL[0], null);
        return (GenerationContext) Proxy.newProxyInstance(
                GenerationContext.class.getClassLoader(),
                new Class<?>[]{GenerationContext.class},
                (proxy, method, args) -> {
                    if ("getClassLoader".equals(method.getName())) {
                        return noProviderLoader;
                    }
                    if ("toString".equals(method.getName())) {
                        return "no-provider-test-context";
                    }
                    return null;
                });
    }
}
