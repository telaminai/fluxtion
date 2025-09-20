/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.connector;

import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.annotations.feature.Experimental;
import com.fluxtion.dataflow.runtime.eventfeed.BaseEventFeed;
import com.fluxtion.dataflow.runtime.eventfeed.ReadStrategy;
import com.fluxtion.dataflow.runtime.node.EventSubscription;
import lombok.Getter;
import lombok.extern.java.Log;
import org.agrona.IoUtil;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

@Experimental
@Log
@SuppressWarnings("all")
public class FileEventFeed extends BaseEventFeed<String> {

    @Getter
    private final String filename;
    private InputStream stream;
    private BufferedReader reader = null;
    private char[] buffer;
    private int offset = 0;

    private boolean tail = true;
    private boolean commitRead = true;
    private boolean latestRead = false;

    private long streamOffset;
    private MappedByteBuffer commitPointer;
    private boolean once;

    public FileEventFeed(String filename, String feedName) {
        this(1024, filename, feedName, ReadStrategy.COMMITED);
    }

    public FileEventFeed(String filename, String feedName, boolean broadcast) {
        this(1024, filename, feedName, ReadStrategy.COMMITED, broadcast, false);
    }

    public FileEventFeed(String filename, String feedName, ReadStrategy readStrategy) {
        this(1024, filename, feedName, readStrategy);
    }

    public FileEventFeed(String filename, String feedName, ReadStrategy readStrategy, boolean broadcast) {
        this(1024, filename, feedName, readStrategy, broadcast, false);
    }

    public FileEventFeed(String filename, String feedName, ReadStrategy readStrategy, boolean broadcast, boolean cache) {
        this(1024, filename, feedName, readStrategy, broadcast, cache);
    }

    /* visible for testing */
    public FileEventFeed(int initialBufferSize, String filename, String feedName, ReadStrategy readStrategy) {
        this(initialBufferSize, filename, feedName, readStrategy, false, false);
    }

    public FileEventFeed(int initialBufferSize, String filename, String feedName, ReadStrategy readStrategy, boolean broadcast, boolean cache) {
        super(feedName, cache, readStrategy, broadcast);
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename is null or empty");
        }
        buffer = new char[initialBufferSize];
        this.filename = filename;
    }

    @Override
    public void onStart() {
        ReadStrategy readStrategy = getReadStrategy();
        tail = readStrategy == ReadStrategy.COMMITED | readStrategy == ReadStrategy.EARLIEST | readStrategy == ReadStrategy.LATEST;
        once = !tail;
        commitRead = readStrategy == ReadStrategy.COMMITED;
        latestRead = readStrategy == ReadStrategy.LATEST | readStrategy == ReadStrategy.ONCE_LATEST;
        log.info(() -> "start FileEventSource %s file:%s tail:%b once:%b, commitRead:%b latestRead:%s readStrategy:%s".formatted(roleName(), filename, tail, once, commitRead, latestRead, readStrategy));

        File committedReadFile = new File(filename + ".readPointer");
        if (readStrategy == ReadStrategy.ONCE_EARLIEST | readStrategy == ReadStrategy.EARLIEST) {
            streamOffset = 0;
        } else if (committedReadFile.exists()) {
            commitPointer = IoUtil.mapExistingFile(committedReadFile, "committedReadFile_" + filename);
            streamOffset = commitPointer.getLong(0);
            log.info(() -> "%s reading committedReadFile:%s, streamOffset:%s".formatted(roleName(), committedReadFile.getAbsolutePath(), streamOffset));
        } else if (commitRead) {
            commitPointer = IoUtil.mapNewFile(committedReadFile, 1024);
            streamOffset = 0;
            log.info(() -> "%s creating committedReadFile:%s, streamOffset:%s".formatted(roleName(), committedReadFile.getAbsolutePath(), streamOffset));
        }

        connectReader();
        tail = true;
    }

    @Override
    protected boolean validSubscription(DataFlow subscriber, EventSubscription<?> subscriptionId) {
        boolean validSubscription = subscriptionId.getFeedName().equals(getFeedName());
        log.fine(() -> "feedName:" + getFeedName() + " subscription:" + subscriptionId.getFeedName() + " validSubscription:" + validSubscription);
        return validSubscription;
    }

    @SuppressWarnings("all")
    @Override
    public int doWork() {
        if (!tail) {
            return 0;
        }
        try {
            if (connectReader() == null) {
                return 0;
            }
            log.finest(() -> "doWork FileEventFeed " + roleName());
            String lastReadLine = null;
            int readCount = 0;
            int nread;

            while (reader.ready()) {
                tail = !once;
                nread = reader.read(buffer, offset, buffer.length - offset);
                int _nread = nread;
                log.finest(() -> "Read %s bytes from %s".formatted(_nread, getFilename()));

                if (nread > 0) {
                    offset += nread;
                    String line;
                    do {
                        line = extractLine();
                        if (line != null) {
                            String _line = line;
                            log.finest(() -> "Read:%s -> %s".formatted(filename, _line));
                            readCount++;
                            int _readCount = readCount;
                            log.finest(() -> "Read a line from '%s' count:'%s' line:'%s'".formatted(getFilename(), _readCount, _line));
                            if (latestRead) {
                                lastReadLine = line;
                            } else {
                                _publish(line);
                            }
                        }
                    } while (line != null);

                    if (latestRead & lastReadLine != null & !once) {
                        String trimmedLine = lastReadLine;
                        log.fine(() -> "publish latest:" + trimmedLine);
                        _publish(lastReadLine);
                    }

                    if (lastReadLine == null && offset == buffer.length) {
                        char[] newbuf = new char[buffer.length * 2];
                        System.arraycopy(buffer, 0, newbuf, 0, buffer.length);
//                        log.debug("Increased buffer from {} to {}", buffer.length, newbuf.length);
                        buffer = newbuf;
                    }
                }
            }

            return readCount;

        } catch (IOException e) {
            log.info("IOException in FileEventFeed");
            e.printStackTrace();
            try {
                reader.close();
            } catch (IOException ex) {

            }
            try {
                stream.close();
            } catch (IOException ex) {

            }
            reader = null;
            stream = null;
        }
        return 0;
    }

    @Override
    public void onClose() {
        log.info("Stopping");
        try {
            if (stream != null) {
                stream.close();
                log.info("Closed input stream");
            }
        } catch (IOException e) {
            log.severe("Failed to close FileStreamSourceTask stream: " + e.getMessage());
        } finally {
            if (commitPointer != null) {
                commitPointer.force();
                IoUtil.unmap(commitPointer);
            }
        }
    }

    private Reader connectReader() {
        if (stream == null && filename != null && !filename.isEmpty()) {
            try {
                stream = Files.newInputStream(Paths.get(filename));
                log.fine(() -> "Found previous offset, trying to skip to file offset " + streamOffset);
                long skipLeft = streamOffset;
                while (skipLeft > 0) {
                    try {
                        long skipped = stream.skip(skipLeft);
                        skipLeft -= skipped;
                    } catch (IOException e) {
                        log.severe("Error while trying to seek to previous offset in file: " + filename + " " + e);
                        //TODO log error and stop
                    }
                }
                log.fine(() -> "Skipped to offset " + streamOffset);
                reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                log.info(() -> "Opened %s for reading offset %s".formatted(getFilename(), streamOffset));
            } catch (NoSuchFileException e) {
                log.severe("Couldn't find file " + getFilename() + " for FileStreamSourceTask, sleeping to wait for it to be created");
            } catch (IOException e) {
                log.severe("Error while trying to open file: " + filename + " " + e);
                throw new RuntimeException(e);
            }
        }
        return reader;
    }

    private void _publish(String line) {
        super.publish(line);
        if (commitRead) {
            commitPointer.force();
        }
    }

    private String extractLine() {
        int until = -1, newStart = -1;
        for (int i = 0; i < offset; i++) {
            if (buffer[i] == '\n') {
                until = i;
                newStart = i + 1;
                break;
            } else if (buffer[i] == '\r') {
                // We need to check for \r\n, so we must skip this if we can't check the next char
                if (i + 1 >= offset)
                    return null;

                until = i;
                newStart = (buffer[i + 1] == '\n') ? i + 2 : i + 1;
                break;
            }
        }

        if (until != -1) {
            String result = new String(buffer, 0, until);
            System.arraycopy(buffer, newStart, buffer, 0, buffer.length - newStart);
            offset = offset - newStart;
            streamOffset += newStart;
            if (commitRead) {
                commitPointer.putLong(0, streamOffset);
            }
            return result;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "FileEventFeed{" +
                "name='" + getFeedName() + '\'' +
                "filename='" + filename + '\'' +
                '}';
    }
}