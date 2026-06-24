/*
 * Copyright: © 2026. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.meta.model;

/**
 * Serialization format for the remote generation wire protocol.
 * <ul>
 *   <li>{@link #JAVA} — standard Java Object Serialization ({@code application/octet-stream})</li>
 *   <li>{@link #KRYO} — Kryo binary serialization ({@code application/x-kryo})</li>
 * </ul>
 */
public enum SerializationFormat {
    /** Standard Java Object Serialization — always available, no extra dependencies */
    JAVA,
    /** Kryo binary serialization — smaller, faster, requires Kryo on the classpath */
    KRYO
}
