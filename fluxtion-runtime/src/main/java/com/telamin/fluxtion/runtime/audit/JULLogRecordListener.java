/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.runtime.audit;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * Process {@link LogRecord}'s and publishes using java commons logging either to:
 * <ul>
 * <li>Console {@link ConsoleHandler}
 * </li>File - using {@link FileHandler}
 * </ul>
 *
 * @author greg
 */
public class JULLogRecordListener implements LogRecordListener {

    private static Logger logger = Logger.getLogger("fluxtion.eventLog");
    private static Level level = Level.INFO;

    {
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    public JULLogRecordListener() {
        ConsoleHandler console = new ConsoleHandler();
        console.setFormatter(new FormatterImpl());
        logger.addHandler(console);
    }

    public JULLogRecordListener(File file) throws IOException {
        FileHandler fileHandler = new FileHandler(file.getCanonicalPath());
        fileHandler.setFormatter(new FormatterImpl());
        logger.addHandler(fileHandler);
    }

    @Override
    public void processLogRecord(LogRecord logRecord) {
        logger.log(level, logRecord.toString() + "\n---\n");
    }

    private static class FormatterImpl extends Formatter {

        @Override
        public String format(java.util.logging.LogRecord record) {
            return record.getMessage();
        }
    }

}
