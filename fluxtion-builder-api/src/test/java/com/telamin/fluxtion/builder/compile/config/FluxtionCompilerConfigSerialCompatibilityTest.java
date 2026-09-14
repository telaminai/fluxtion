package com.telamin.fluxtion.builder.compile.config;

import org.junit.Test;

import java.io.ObjectStreamClass;

import static org.junit.Assert.assertEquals;

/**
 * The released serialVersionUID must not move.
 *
 * <p>This class is public and {@code Serializable} and shipped without a declared UID, so the JVM
 * computed one from its shape. Adding fields changed it, and an instance written by 1.0.13 then failed
 * to load with {@code InvalidClassException} — found by review, not by any test here, because nothing
 * asserted the value.
 *
 * <p>The UID below was recovered with {@code serialver} against the released 1.0.13 artifact. Pinning it
 * makes field additions compatible again; changing it breaks every instance ever written. This test is
 * the thing that notices.
 */
public class FluxtionCompilerConfigSerialCompatibilityTest {

    /** As computed by 1.0.13, which had no declared UID. */
    private static final long RELEASED_1_0_13_UID = 8518484796223925653L;

    @Test
    public void theReleasedSerialVersionUidIsPinned() {
        ObjectStreamClass descriptor = ObjectStreamClass.lookup(FluxtionCompilerConfig.class);
        assertEquals(
                "FluxtionCompilerConfig's serialVersionUID has moved. Every instance serialized by a "
                        + "released client now fails to deserialize with InvalidClassException. Restore "
                        + RELEASED_1_0_13_UID + "L rather than changing this test.",
                RELEASED_1_0_13_UID, descriptor.getSerialVersionUID());
    }
}
