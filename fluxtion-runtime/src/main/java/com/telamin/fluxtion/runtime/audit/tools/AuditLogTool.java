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
 *   --declare-unit millis|nanos --out &lt;copy&gt;
 *                          write a copy whose header states the unit (for a file written before
 *                          the header carried one); nothing else is changed, and no other option applies
 * </pre>
 *
 * <p><b>{@code --from}/{@code --to} are milliseconds whatever the file's unit.</b> The filter reads the
 * header first and scales the bounds to the file's unit; a file whose header states no unit, or an
 * undefined one, refuses a time query rather than compare milliseconds to something else.
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
        private long pendingEndTime;
        /** A record whose header was printed and whose endTime line has not been. */
        private boolean recordOpen;

        TextSink(PrintStream out) {
            this.out = out;
        }

        @Override
        public void record(String eventType, long eventTime, long logTime, long endTime) {
            // endTime is emitted after nodeLogs, where the text record puts it - the callback supplied
            // it and this output claimed the text record's shape while dropping it.
            closeRecord();
            pendingEndTime = endTime;
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
                recordOpen = true;
            }
            out.println("        - " + node + ": { " + key + ": " + value + "}");
        }

        /** Prints the deferred {@code endTime} line for a record whose header was printed. */
        private void closeRecord() {
            if (recordOpen) {
                out.println("    endTime: " + pendingEndTime);
                recordOpen = false;
            }
        }

        @Override
        public void close() {
            closeRecord();
            out.flush();
        }
    }

    /**
     * The header's time unit, by name. Every timestamp this tool prints is a bare number; without
     * this line a nanosecond file and a millisecond one are indistinguishable in its output, which is
     * the confusion the header field was added to end.
     */
    static String timeUnitName(int code) {
        switch (code) {
            case com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_MILLIS: return "epoch milliseconds";
            case com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS:  return "epoch nanoseconds";
            case com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_UNSPECIFIED:
                return "unspecified (file predates the unit field) - declare it with --declare-unit";
            default: return "unknown code " + code;
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
        String declareUnit = null, outFile = null;
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
                    case "--declare-unit": declareUnit = args[++i]; break;
                    case "--out": outFile = args[++i]; break;
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

        if (declareUnit != null || outFile != null) {
            return declareUnit(path, declareUnit, outFile, out, err);
        }

        AuditLogFilter.Sink sink = "null".equals(sinkName) ? new NullSink() : new TextSink(out);
        AuditLogFilter filter = new AuditLogFilter(event, node, key, from, to, limit, sink);

        BinaryLogReader.Result result;
        try {
            result = BinaryLogReader.read(path, filter);
        } catch (IOException e) {
            err.println(e.getMessage());
            return 3;
        } catch (IllegalArgumentException refusedQuery) {
            // The filter refused the time query at the header: the bounds are milliseconds and the
            // file's unit cannot honour them. Nothing was printed, and the exit code says so.
            err.println(refusedQuery.getMessage());
            return 2;
        }
        sink.close();

        String unmatchable = filter.unmatchableWithinSelection();
        if (unmatchable != null) {
            // SCOPED TO THE SELECTION, and says so. Entries are only seen for records the event and
            // time filters admitted, so a node present only under another event is not observed - and
            // the old wording, "no name in this log matches", then stated something false.
            err.println("nothing matching " + unmatchable
                    + " appears in the records this query selected — so the empty result is the "
                    + "pattern, not the data. Note the scope: a name used only under another --event, "
                    + "or outside the time range, is not counted here. Widen the other filters to ask "
                    + "whether it exists in the file at all.");
        }
        if (stats) {
            err.println("records read      : " + result.records);
            err.println("records matched   : " + filter.matchedRecords());
            err.println("entries read      : " + result.entries);
            err.println("entries matched   : " + filter.matchedEntries());
            err.println("time unit         : " + timeUnitName(result.timeUnit));
            err.println("dictionary names  : " + result.dictionary.size());
            err.println("unresolved ids    : " + result.unresolvedIds
                    + (result.unresolvedIds > 0 ? "   <- names the log never described" : ""));
            err.println("unreadable bytes  : " + result.truncatedBytes
                    + (result.truncatedBytes > 0 ? "   <- damaged tail, the rest was read" : ""));
        }
        return 0;
    }

    /**
     * Writes a copy of {@code path} whose header states {@code unitName}. Only a file whose header
     * states NO unit is accepted: a stated unit is the producer's claim, and this tool does not
     * overwrite one claim with another. The copy is byte-identical past the header, so the records
     * are the evidence they were; what changes is that the unit now travels with them, where every
     * reader looks, instead of in whoever remembered which runtime wrote the file.
     */
    static int declareUnit(Path path, String unitName, String outFile, PrintStream out, PrintStream err)
            throws IOException {
        if (unitName == null || outFile == null) {
            err.println("--declare-unit needs both a unit (millis|nanos) and --out <copy>");
            return 2;
        }
        int unit;
        switch (unitName) {
            case "millis": unit = com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_MILLIS; break;
            case "nanos":  unit = com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_EPOCH_NANOS; break;
            default:
                err.println("--declare-unit takes millis or nanos, got " + unitName);
                return 2;
        }
        Path target = Paths.get(outFile);
        if (Files.exists(target)) {
            err.println("refusing to overwrite " + outFile);
            return 2;
        }
        byte[] bytes = Files.readAllBytes(path);
        int headerBytes = com.telamin.fluxtion.runtime.audit.BinaryLogFile.HEADER_BYTES;
        byte[] magic = com.telamin.fluxtion.runtime.audit.BinaryLogFile.MAGIC;
        if (bytes.length < headerBytes || !java.util.Arrays.equals(magic, java.util.Arrays.copyOf(bytes, magic.length))) {
            err.println("not an audit log: " + path);
            return 3;
        }
        int existing = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
        if (existing != com.telamin.fluxtion.runtime.audit.BinaryLogFile.TIME_UNIT_UNSPECIFIED) {
            err.println("this file's header already states its unit (" + timeUnitName(existing)
                    + "); --declare-unit only fills in a header that states none");
            return 2;
        }
        bytes[6] = (byte) (unit >>> 8);
        bytes[7] = (byte) unit;
        Files.write(target, bytes);
        out.println("wrote " + target + " declaring " + timeUnitName(unit) + " (" + bytes.length + " bytes)");
        return 0;
    }

    private static void usage(PrintStream out) {
        out.println("audit-log [options] <file>");
        out.println("  --from <millis>   --to <millis>     logTime bounds (scaled to the file's declared unit)");
        out.println("  --event <glob>    --node <glob>     --key <glob>");
        out.println("  --limit <n>       --sink text|null  --stats");
        out.println("  --declare-unit millis|nanos --out <copy>   state the unit of a file whose header has none");
    }

    private AuditLogTool() {
    }
}
