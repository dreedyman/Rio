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
package org.rioproject.logging.jul;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Formats log entries, optionally using ANSI coloring.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class RioLogFormatter extends Formatter {
    Date date = new Date();
    private final static String format = "{0,date,yyyy.MM.dd HH:mm:ss,SSS}";
    private MessageFormat formatter;
    private Object args[] = new Object[1];
    private String lineSeparator = System.getProperty("line.separator");
    private boolean includePackageNames;
    private boolean colorize;
    private String levelFormat;
    private final Level[] levels = new Level[]{Level.SEVERE, Level.WARNING, Level.INFO,
                                               Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST};
    private final Map<Level, StringColorizer.Color> levelColorMap = new HashMap<Level, StringColorizer.Color>();

    public RioLogFormatter() {
        super();
        int longest = 0;
        for(Level l : levels) {
            if(l.getLocalizedName().length()>longest)
                longest = l.getLocalizedName().length();
        }
        levelFormat = "%-"+longest+"s";

        /* load options from the logging properties file */
        includePackageNames = hasDeclaredSupportFor(getClass().getName() + ".includePackageNames");
        colorize = !System.getProperty("os.name").startsWith("Win") &&
                   hasDeclaredSupportFor(getClass().getName() + ".colorize");
        /* If we have colorization support, determine if we have mapping colors */
        setColorization(colorize);
    }

    void setColorization(boolean colorize) {
        this.colorize = colorize;
        if(colorize) {
            for(Level l : levels) {
                String property = LogManager.getLogManager().getProperty(getClass().getName()+"."+l.getName());
                if(property!=null) {
                    StringColorizer.Color color = StringColorizer.getColor(property);
                    if(color!=null) {
                        levelColorMap.put(l, color);
                    }
                }
            }
        }
    }

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(levelFormat, record.getLevel().getLocalizedName()));
        sb.append(" ");

        // Minimize memory allocations here.
        date.setTime(record.getMillis());
        args[0] = date;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");

       
        if (record.getSourceClassName() != null) {
            String name = record.getSourceClassName();
            if(!includePackageNames) {
                int ndx = record.getSourceClassName().lastIndexOf(".");
                if (ndx > 0)
                    name = record.getSourceClassName().substring(ndx + 1, record.getSourceClassName().length());
                else
                    name = record.getSourceClassName();
            }
            sb.append(name);
        } else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append(".");
            sb.append(record.getSourceMethodName());
        }

        String message = formatMessage(record);

        sb.append(" - ");
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                /* */
            }
        }
        String formattedRecord;
        if(colorize) {
            StringColorizer.Color color = levelColorMap.get(record.getLevel());
            if(color!=null)
                formattedRecord = StringColorizer.colorize(sb.toString(), color);
            else
                formattedRecord = sb.toString();
        } else {
            formattedRecord = sb.toString();
        }
        return formattedRecord;
    }

    private boolean hasDeclaredSupportFor(String property) {
        String s = LogManager.getLogManager().getProperty(property);
        return !(s == null || s.trim().length() == 0) && s.equals("true");
    }
}
