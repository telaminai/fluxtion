/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.manifest;

import com.telamin.fluxtion.runtime.annotations.OnEventHandler;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * The packaged {@code fluxtion-runtime} jar carries {@code Implementation-Version}. The compiler reads it
 * through {@code OnEventHandler.class.getPackage().getImplementationVersion()} and stamps it into the
 * generated processor's javadoc header as the "api version"; it was "unknown api version" for every
 * release before this entry existed.
 *
 * <p>Runs only against the packaged jar (surefire execution {@code manifest-on-jar} at
 * {@code integration-test}); on {@code target/classes} there is no manifest and the test skips. It lives
 * in its own package on purpose: {@code Package} attributes come from whichever classpath entry first
 * defines the package, and {@code target/test-classes} precedes the jar.
 */
public class ManifestVersionTest {

    @Test
    public void thePackagedJarCarriesTheReactorVersion() {
        String location = OnEventHandler.class.getProtectionDomain().getCodeSource().getLocation().toString();
        Assume.assumeTrue("runs against the packaged jar, not target/classes: " + location, location.endsWith(".jar"));
        String expected = System.getProperty("fluxtion.expected.version");
        Assert.assertNotNull("the pom passes ${project.version} as fluxtion.expected.version", expected);
        Assert.assertEquals("Implementation-Version in " + location,
                expected, OnEventHandler.class.getPackage().getImplementationVersion());
    }
}
