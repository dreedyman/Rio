/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The {@code WrappedLogger} provides a wrapper around a {@link Logger}, providing ease of use surrounding
 * var-args support for parameterization of log messages.
 *
 * <p>The {@code WrappedLogger} will also first check if the logging level is supported, then perform the
 * logger invocation.</p>
 *
 * @author Dennis Reedy
 */
public class WrappedLogger {
    private final Logger logger;
    private static WrappedLogger instance;
    private static final String THIS_CLASS_NAME = WrappedLogger.class.getName();

    private WrappedLogger(final String name) {
        logger = Logger.getLogger(name);
    }

    private WrappedLogger(final String name, final String resourceBundleName) {
        logger = Logger.getLogger(name, resourceBundleName);
    }

    public static synchronized WrappedLogger getLogger(final String name) {
        if(instance==null)
            instance = new WrappedLogger(name);
        return instance;
    }

    public static synchronized WrappedLogger getLogger(final String name, final String resourceBundleName) {
        if(instance==null)
            instance = new WrappedLogger(name, resourceBundleName);
        return instance;
    }

    public void severe(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.SEVERE)) {
            doLog(createLogRecord(Level.SEVERE, format(formatString, objects)));
        }
    }

    public void warning(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.WARNING)) {
            doLog(createLogRecord(Level.WARNING, format(formatString, objects)));
        }
    }

    public void config(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.CONFIG)) {
            doLog(createLogRecord(Level.CONFIG, format(formatString, objects)));
        }
    }

    public void info(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.INFO)) {
            doLog(createLogRecord(Level.INFO, format(formatString, objects)));
        }
    }

    public void fine(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.FINE)) {
            doLog(createLogRecord(Level.FINE, format(formatString, objects)));
        }
    }

    public void finer(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.FINER)) {
            doLog(createLogRecord(Level.FINER, format(formatString, objects)));
        }
    }

    public void finest(final String formatString, final Object... objects) {
        if(logger.isLoggable(Level.FINEST)) {
            doLog(createLogRecord(Level.FINEST, format(formatString, objects)));
        }
    }

    public void log(final Level level, final Throwable t, final String formatString, final Object... objects) {
        if(logger.isLoggable(level)) {
            doLog(createLogRecord(level, format(formatString, objects), t));
        }
    }

    public void log(final Level level, final String message, final Throwable t) {
        if(logger.isLoggable(level)) {
            doLog(createLogRecord(level, message, t));
        }
    }

    public boolean isLoggable(final Level level) {
        return logger.isLoggable(level);
    }

    public Logger getLogger() {
        return logger;
    }

    private String format(final String formatString, final Object... objects) {
        if(objects.length>0)
            return String.format(formatString, objects);
        else
            return formatString;
    }

    private LogRecord createLogRecord(final Level l, final String s) {
        return new LogRecord(l, s);
    }

    private LogRecord createLogRecord(final Level l, final String s, final Throwable t) {
        LogRecord lr = new LogRecord(l, s);
        lr.setThrown(t);
        return lr;
    }

    private void doLog(final LogRecord lr) {
        StackTraceElement element = null;
        for (StackTraceElement ste : new Throwable().getStackTrace()) {
            if (!THIS_CLASS_NAME.equals(ste.getClassName())) {
                element = ste;
                break;
            }
        }
        if (element != null) {
            lr.setSourceClassName(element.getClassName());
            lr.setSourceMethodName(element.getMethodName());
        }
        try {
            logger.log(lr);
        } catch (Throwable t) {
            /* should not happen, and if it does just eat it. */
        }
    }
}

