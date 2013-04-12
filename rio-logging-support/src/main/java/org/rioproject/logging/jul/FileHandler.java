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

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.logging.*;

/**
 * An extension of {@link java.util.logging.FileHandler} that sets the pattern based on system properties.
 *
 * <b>Configuration:</b>
 * By default each <tt>FileHandler</tt> is initialized using the following
 * <tt>LogManager</tt> configuration properties.  If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>FileHandler.level specifies the default level for the <tt>Handler</tt>
 *	  (defaults to <tt>Level.ALL</tt>).
 * <li>FileHandler.filter specifies the name of a <tt>Filter</tt> class to use
 *	  (defaults to no <tt>Filter</tt>).
 * <li>FileHandler.formatter specifies the name of a <tt>Formatter</tt> class to use
 *    (defaults to <tt>org.rioproject.log.XMLFormatter</tt>)
 * <li>FileHandler.encoding the name of the character set encoding to use
 *    (defaults to the default platform encoding).
 * <li>FileHandler.limit specifies an approximate maximum amount to write (in bytes)
 * 	  to any one file.  If this is zero, then there is no limit. (Defaults to no limit).
 * <li>FileHandler.count specifies how many output files to cycle through (defaults to 1).
 * <li>FileHandler.pattern specifies a pattern for generating the output file name.  See
 *     below for details. (Defaults to "%h/java%u.log").
 * <li>FileHandler.append specifies whether the FileHandler should append onto
 *     any existing files (defaults to false).
 * </ul>
 * <p>
 */
public class FileHandler extends java.util.logging.FileHandler {
    static Configuration configuration = getConfiguration();

    public FileHandler() throws IOException, SecurityException {
        super(configuration.pattern, configuration.limit, configuration.count, configuration.append);
        setEncoding(configuration.encoding);
    }

    @Override
    public void setFormatter(Formatter formatter) throws SecurityException {
        super.setFormatter(formatter);
        /* if we have a RioLogFormatter, turn of colorization */
        if(formatter instanceof RioLogFormatter) {
            ((RioLogFormatter)formatter).setColorization(false);
        }
    }

    public String getLockFileName() {
        String lockFileName = "";
        try {
            Field getLockFileName = this.getClass().getSuperclass().getDeclaredField("lockFileName");
            getLockFileName.setAccessible(true);
            lockFileName = (String)getLockFileName.get(this);
        } catch (NoSuchFieldException e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Getting lockFileName", e);
        } catch (IllegalAccessException e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Getting lockFileName", e);
        }
        return lockFileName;
    }

    private static Configuration getConfiguration() {
        LogManager manager = LogManager.getLogManager();
        String propertyBase = FileHandler.class.getName();
        String pattern = manager.getProperty(propertyBase+".pattern");
        int limit = getLimitProperty(manager, propertyBase + ".limit", 0);
        if (limit < 0) {
            limit = 0;
        }
        int count = getIntProperty(manager, propertyBase + ".count", 0);
        if (count <= 0) {
            count = 1;
        }
        boolean append = getBooleanProperty(manager, propertyBase + ".append", false);
        String encoding = manager.getProperty(propertyBase+".encoding");
        String logDirectory = System.getProperty("RIO_LOG_DIR");
        String service = System.getProperty("org.rioproject.service");
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name;
        int ndx = name.indexOf("@");
        if(ndx>=1) {
            pid = name.substring(0, ndx);
        }
        if(logDirectory!=null) {
            File dir = new File(logDirectory);
            if(!dir.exists()) {
                dir.mkdirs();
            }
            pattern = replace(pattern, "${RIO_LOG_DIR}", logDirectory);
        }
        if(service!=null)
            pattern = replace(pattern, "${org.rioproject.service}", service);

        pattern = replace(pattern, "%pid", pid);
        pattern = replace(pattern, "%name", name);

        return new Configuration(limit, count, append, pattern, encoding);
    }

    static int getLimitProperty(LogManager manager, String name, int defaultValue) {
        double KB = 1024;
        /** Megabytes */
        double MB = Math.pow(KB, 2);
        /** Gigabytes */
        double GB = Math.pow(KB, 3);

        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.trim();
        if(val.endsWith("M") || val.endsWith("MB")) {
            int ndx = val.indexOf("M");
            val = val.substring(0, ndx);
            int result = getInteger(val, defaultValue);
            return (int) (result*MB);
        }
        if(val.endsWith("G") || val.endsWith("GB")) {
            int ndx = val.indexOf("G");
            val = val.substring(0, ndx);
            int result = getInteger(val, defaultValue);
            return (int) (result*GB);
        }

        return getInteger(val, defaultValue);
    }

    
    static int getIntProperty(LogManager manager, String name, int defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return getInteger(val, defaultValue);
    }
    
    static int getInteger(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }
        
    static boolean getBooleanProperty(LogManager manager, String name, boolean defaultValue) {
        String val = manager.getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.toLowerCase();
        if (val.equals("true") || val.equals("1")) {
            return true;
        } else if (val.equals("false") || val.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    static String replace(String str, String pattern, String replace) {
        if(str==null)
            return "";
        int s = 0;
        int e;
        StringBuilder result = new StringBuilder();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }
    
    private static class Configuration {
        int limit;
        int count;
        boolean append;
        String pattern;
        String encoding;

        private Configuration(int limit, int count, boolean append, String pattern, String encoding) {
            this.limit = limit;
            this.count = count;
            this.append = append;
            this.pattern = pattern;
            this.encoding = encoding;
        }
    }
}
