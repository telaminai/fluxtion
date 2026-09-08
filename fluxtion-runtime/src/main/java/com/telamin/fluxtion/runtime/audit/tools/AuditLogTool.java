/*
 * Copyright: © 2025. Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only OR SSPL-1.0
 */
package com.telamin.fluxtion.runtime.audit.tools;

import com.telamin.fluxtion.runtime.audit.BinaryLogReader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads a binary Fluxtion audit log — {@code grep} and {@code tail} for audit trails.
 *
 * <pre>
 * audit-log [options] &lt;file&gt;
 *   --from &lt;millis&gt;        logTime lower bound
 *   --to &lt;millis&gt;          logTime upper bound
 *   --event &lt;glob&gt;         event type
 *   --node &lt;glob&gt;          node name
 *   --key &lt;glob&gt;           property key
 *   --limit &lt;n&gt;            stop after n matching records
 *   --sink text|null       default text
 *   --stats                counts, unresolved ids, unreadable bytes
 * </pre>
 *
 * <p>Output is the same YAML shape the text {@code LogRecord} produces, so it pipes into anything that
 * already reads a Fluxtion audit log.
 *
 * <p><b>{@code --stats} reports the two silent data-loss conditions</b> — ids the dictionary never
 * described, and a tail too damaged to parse. A reader that hides those is worse than one that refuses
 * to run, because the usual reason to open an audit log is that something went wrong.
 */
public final class AuditLogTool {

    /**
     * The default output: the YAML shape the text record produces.
     *
     * <p>The record header is <b>held until the first entry survives the filter</b>. Emitting it
     * eagerly means a {@code --node} pattern matching nothing still prints a header per record, so a
     * filter that excluded everything looks like a log full of empty events. Entry-level filters are
     * the common case, and this is what makes their output readable.
     */
    static final class TextSink implements AuditLogFilter.Sink {
        private final PrintStream out;
        private String pending;
        private boolean anyPrinted;

        TextSink(PrintStream out) {
            this.out = out;
        }

        @Override
        public void record(String eventType, long eventTime, long logTime, long endTime) {
            pending = "eventLogRecord: \n"
                    + "    eventTime: " + eventTime + "\n"
                    + "    logTime: " + logTime + "\n"
                    + "    event: " + eventType + "\n"
                    + "    nodeLogs: ";
        }

        @Override
        public void entry(String node, String key, String value) {
            if (pending != null) {
                if (anyPrinted) {
                    out.println();
                }
                out.println(pending);
                pending = null;
                anyPrinted = true;
            }
            out.println("        - " + node + ": { " + key + ": " + value + "}");
        }

        @Override
        public void close() {
            out.flush();
        }
    }

    /** Counts and discards — for measuring read cost without output in the way. */
    static final class NullSink implements AuditLogFilter.Sink {
        @Override public void record(String e, long a, long b, long c) { }
        @Override public void entry(String node, String key, String value) { }
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) throws IOException {
        String event = null, node = null, key = null, sinkName = "text", file = null;
        long from = Long.MIN_VALUE, to = Long.MAX_VALUE, limit = Long.MAX_VALUE;
        boolean stats = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            try {
                switch (a) {
                    case "--from":  from = Long.parseLong(args[++i]); break;
                    case "--to":    to = Long.parseLong(args[++i]); break;
                    case "--event": event = args[++i]; break;
                    case "--node":  node = args[++i]; break;
                    case "--key":   key = args[++i]; break;
                    case "--limit": limit = Long.parseLong(args[++i]); break;
                    case "--sink":  sinkName = args[++i]; break;
                    case "--stats": stats = true; break;
                    case "-h": case "--help": usage(out); return 0;
                    default:
                        if (a.startsWith("--")) {
                            err.println("unknown option: " + a);
                            usage(err);
                            return 2;
                        }
                        file = a;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                err.println(a + " needs a value");
                return 2;
            } catch (NumberFormatException e) {
                err.println(a + " needs a number, got " + args[i]);
                return 2;
            }
        }
        if (file == null) {
            err.println("no file given");
            usage(err);
            return 2;
        }
        Path path = Paths.get(file);
        if (!Files.isReadable(path)) {
            err.println("cannot read " + file);
            return 2;
        }

        AuditLogFilter.Sink sink = "null".equals(sinkName) ? new NullSink() : new TextSink(out);
        AuditLogFilter filter = new AuditLogFilter(event, node, key, from, to, limit, sink);

        BinaryLogReader.Result result;
        try {
            result = BinaryLogReader.read(path, filter);
        } catch (IOException e) {
            err.println(e.getMessage());
            return 3;
        }
        sink.close();

        String unmatchable = filter.unmatchablePattern();
        if (unmatchable != null) {
            err.println("no name in this log matches " + unmatchable
                    + " — the pattern cannot match anything here, so the empty result is the pattern, "
                    + "not the log");
        }
        if (stats) {
            err.println("records read      : " + result.records);
            err.println("records matched   : " + filter.matchedRecords());
            err.println("entries read      : " + result.entries);
            err.println("entries matched   : " + filter.matchedEntries());
            err.println("dictionary names  : " + result.dictionary.size());
            err.println("unresolved ids    : " + result.unresolvedIds
                    + (result.unresolvedIds > 0 ? "   <- names the log never described" : ""));
            err.println("unreadable bytes  : " + result.truncatedBytes
                    + (result.truncatedBytes > 0 ? "   <- damaged tail, the rest was read" : ""));
        }
        return 0;
    }

    private static void usage(PrintStream out) {
        out.println("audit-log [options] <file>");
        out.println("  --from <millis>   --to <millis>     logTime bounds");
        out.println("  --event <glob>    --node <glob>     --key <glob>");
        out.println("  --limit <n>       --sink text|null  --stats");
    }

    private AuditLogTool() {
    }
}
