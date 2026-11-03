/*
 * Copyright: © 2026. Gregory Higgins
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit;

import com.telamin.fluxtion.runtime.describe.ProcessorDescriptor;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The first document of an audit stream: which processor wrote it, the shape of the graph it ran, and which
 * host run it belongs to. It lets a reader pair a log with the graphml and generated source that produced it,
 * and separate one run from the next.
 *
 * <p>Written once per stream, not per event: {@link EventLogManager} emits it before the first record it
 * publishes after any sink change. A text stream carries it as a YAML document,
 * <pre>
 * auditStreamHeader:
 *     schema: 1
 *     processor: com.acme.TradeProcessor
 *     sourceFingerprint: 5f3c...
 *     toolchainVersion: 1.0.84
 *     runtimeVersion: 1.1.4
 *     startedAt: 2026-11-03T09:15:02.118Z
 *     host: {serverId: pricing-1, runId: 2c41..., group: pricing, name: quoter, generation: 1}
 * </pre>
 * and a binary stream carries the same text in a {@link BinaryLogFile#FRAME_STREAM_HEADER} frame.
 *
 * <p><b>What the key proves.</b> {@code sourceFingerprint} is a digest of the graph's SHAPE: node names, classes,
 * callbacks and edges. Equal fingerprints mean the same graph shape, never the same build: method bodies and
 * configured values are not in it. It is {@code null} for a processor with no descriptor, or whose descriptor
 * records none (an interpreted processor). {@code toolchainVersion} and {@code runtimeVersion} are information,
 * not part of the key. The {@code host} map is whatever the hosting server supplied through
 * {@link EventLogManager#setHeaderContext(Map)}; it is empty when no host supplied one.
 */
public final class AuditStreamHeader {

    /** The version of this document's shape. A reader gates on it; new keys are additive within a schema. */
    public static final int SCHEMA = 1;
    /** The document key a reader recognises the header by, in the text format. */
    public static final String DOCUMENT_KEY = "auditStreamHeader";

    private static final String RUNTIME_VERSION = readRuntimeVersion();

    private final String processor;
    private final String sourceFingerprint;
    private final String toolchainVersion;
    private final String runtimeVersion;
    private final String startedAt;
    private final Map<String, String> host;

    public AuditStreamHeader(String processor, String sourceFingerprint, String toolchainVersion,
                             String runtimeVersion, String startedAt, Map<String, String> host) {
        this.processor = processor;
        this.sourceFingerprint = sourceFingerprint;
        this.toolchainVersion = toolchainVersion;
        this.runtimeVersion = runtimeVersion;
        this.startedAt = startedAt;
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (host != null) {
            for (Map.Entry<String, String> e : host.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    copy.put(e.getKey(), e.getValue());
                }
            }
        }
        this.host = Collections.unmodifiableMap(copy);
    }

    /**
     * The header for a stream starting now.
     *
     * @param descriptor the processor's descriptor, or {@code null} when none was handed to the manager
     * @param host       the host context, or {@code null}
     */
    public static AuditStreamHeader of(ProcessorDescriptor descriptor, Map<String, String> host) {
        return new AuditStreamHeader(
                descriptor == null ? null : descriptor.name(),
                descriptor == null ? null : descriptor.sourceFingerprint(),
                descriptor == null ? null : descriptor.toolchainVersion(),
                RUNTIME_VERSION,
                Instant.now().toString(),
                host);
    }

    public String processor() {
        return processor;
    }

    public String sourceFingerprint() {
        return sourceFingerprint;
    }

    public String toolchainVersion() {
        return toolchainVersion;
    }

    public String runtimeVersion() {
        return runtimeVersion;
    }

    public String startedAt() {
        return startedAt;
    }

    /** Insertion-ordered and unmodifiable. */
    public Map<String, String> host() {
        return host;
    }

    /**
     * Refuses a host context whose header would exceed {@link BinaryLogFile#MAX_STREAM_HEADER_BYTES}, the limit a
     * binary reader enforces. Applied to text streams too, so the two formats accept the same contexts.
     *
     * @throws IllegalArgumentException naming the size and the limit
     */
    public static void checkSize(Map<String, String> host) {
        // the fixed fields are bounded (class name, 64-hex fingerprint, versions, an instant); the host map is not
        int bytes = new AuditStreamHeader(null, null, null, null, null, host).toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1024;
        if (bytes > BinaryLogFile.MAX_STREAM_HEADER_BYTES) {
            throw new IllegalArgumentException("audit stream header host context is about " + bytes
                    + " bytes; the format allows " + BinaryLogFile.MAX_STREAM_HEADER_BYTES);
        }
    }

    /** The version of the runtime on the classpath, or {@code null} when its build stamp cannot be read. */
    public static String runtimeVersionOnClasspath() {
        return RUNTIME_VERSION;
    }

    /**
     * The text form: one YAML document, laid out like an {@code eventLogRecord} (four-space indent, no
     * document separator; the sink frames documents as it does records).
     */
    public void appendTo(StringBuilder sb) {
        sb.append(DOCUMENT_KEY).append(':');
        sb.append("\n    schema: ").append(SCHEMA);
        sb.append("\n    processor: ");
        scalar(sb, processor);
        sb.append("\n    sourceFingerprint: ");
        scalar(sb, sourceFingerprint);
        sb.append("\n    toolchainVersion: ");
        scalar(sb, toolchainVersion);
        sb.append("\n    runtimeVersion: ");
        scalar(sb, runtimeVersion);
        sb.append("\n    startedAt: ");
        scalar(sb, startedAt);
        sb.append("\n    host: {");
        boolean first = true;
        for (Map.Entry<String, String> e : host.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            scalar(sb, e.getKey());
            sb.append(": ");
            scalar(sb, e.getValue());
        }
        sb.append('}');
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }

    /**
     * A plain scalar only when no YAML 1.1 reader can type it as anything but a string; otherwise single-quoted, with
     * {@code '} doubled. Plain requires: only {@code [A-Za-z0-9._/#$-]}, a first character that is a letter or digit,
     * at least one letter other than {@code e}/{@code E} (so no number, exponent, date or timestamp is plain), no
     * {@code 0x}/{@code 0o}/{@code 0b} prefix, and not a YAML boolean or null word. A version such as {@code 1.1.4}
     * is therefore quoted, and a hex fingerprint is plain.
     *
     * <p><b>Control characters</b> (C0, DEL, C1, U+2028, U+2029) are replaced with U+FFFD: a YAML reader refuses
     * some of them outright, and a header that does not parse is worse than one whose odd character is visibly
     * replaced. The replacement is the stored value; a host should not put control characters in run identity.
     */
    static void scalar(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        String clean = withoutControls(value);
        if (isPlain(clean)) {
            sb.append(clean);
            return;
        }
        sb.append('\'');
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else {
                sb.append(c);
            }
        }
        sb.append('\'');
    }

    private static String withoutControls(String value) {
        StringBuilder out = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029') {
                if (out == null) {
                    out = new StringBuilder(value.length()).append(value, 0, i);
                }
                out.append('\uFFFD');
            } else if (out != null) {
                out.append(c);
            }
        }
        return out == null ? value : out.toString();
    }

    private static boolean isPlain(String value) {
        if (value.isEmpty() || !Character.isLetterOrDigit(value.charAt(0))) {
            return false;
        }
        boolean letter = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean alpha = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            boolean ok = alpha || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-' || c == '/' || c == '#' || c == '$';
            if (!ok) {
                return false;
            }
            if (alpha && c != 'e' && c != 'E') {
                letter = true;
            }
        }
        if (!letter) {
            return false;
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("0x") || lower.startsWith("0o") || lower.startsWith("0b")) {
            return false;
        }
        switch (lower) {
            case "null": case "true": case "false": case "yes": case "no": case "on": case "off":
            case "y": case "n":
                return false;
            default:
                return true;
        }
    }

    private static String readRuntimeVersion() {
        try (InputStream in = AuditStreamHeader.class.getResourceAsStream(
                "/com/telamin/fluxtion/runtime/build.properties")) {
            if (in == null) {
                return null;
            }
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version");
            return v == null || v.isEmpty() || v.startsWith("${") ? null : v.trim();
        } catch (Exception e) {
            return null;
        }
    }
}
