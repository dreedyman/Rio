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
package org.rioproject.boot;

import org.rioproject.logging.FileHandler;
import org.slf4j.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.logging.*;
import java.util.logging.Logger;

/**
 * Check that the Java logging configuration has a console available. If there is no console available, set the
 * system out and system error to the name of the service log file. If Java Util Logging being used, and a console is
 * not available, remove any {@link java.util.logging.ConsoleHandler}s.
 * <p/>
 * <p>The resulting {@code service}.out and {@code service}.err will be stored in the location declared by the
 * {@code RIO_LOG_DIR} system property.
 * </p>
 *
 * @author Dennis Reedy
 */
public final class LogAgent {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LogAgent.class);
    private LogAgent() {
    }

    public static void redirectIfNecessary() {
        if(System.console()==null) {
            String fileName = getFileName();
            StringBuilder builder = new StringBuilder();
            builder.append("\nThere is no console support\n");
            String logDir = System.getProperty("RIO_LOG_DIR");
            File rioLogDir = new File(logDir);
            File serviceOutput = new File(rioLogDir, fileName + ".out");
            File serviceError = new File(rioLogDir, fileName + ".err");
            redirect(serviceOutput, true);
            redirect(serviceError, false);
            builder.append("System out has been redirected to ").append(serviceOutput.getPath()).append("\n");
            builder.append("System err has been redirected to ").append(serviceError.getPath()).append("\n");
            LoggerFactory.getLogger(LogAgent.class).info(builder.toString());

        }
    }

    private static String getFileName() {
        String logDir = System.getProperty("RIO_LOG_DIR");
        String fileName = null;
        if(!usingJUL()) {
            String service = System.getProperty("org.rioproject.service");
            File dir = new File(logDir);
            for(String name : dir.list()) {
                if(name.startsWith(service)) {
                    int ndx = name.indexOf(".log");
                    fileName = name.substring(0, ndx);
                    break;
                }
            }
        } else {
            String lockFileName = null;

            for (Enumeration<String> e = LogManager.getLogManager().getLoggerNames(); e.hasMoreElements(); ) {
                Logger l = Logger.getLogger(e.nextElement());
                for (Handler h : l.getHandlers()) {
                    if (h instanceof ConsoleHandler) {
                        h.close();
                        l.removeHandler(h);
                    }
                    if(h instanceof org.rioproject.logging.FileHandler) {
                        lockFileName = ((FileHandler)h).getLockFileName();
                    }
                }
            }

            if (logDir != null && lockFileName!=null) {
                File lockFile = new File(lockFileName);
                int ndx = lockFile.getName().indexOf(".lck");
                fileName = lockFile.getName().substring(0, ndx);
            }
        }
        return fileName;
    }

    private static void redirect(final File file, boolean isOut) {
        try {
            if(isOut) {
                System.setOut(new PrintStream(new FileOutputStream(file)));
            } else {
                System.setErr(new PrintStream(new FileOutputStream(file)));
            }
        } catch (FileNotFoundException e) {
            String type = isOut?"standard output":"standard error";
            logger.warn("Redirecting {} to {}", type, file.getPath(), e);
        }
    }

    /**
     * Determine if we are using Java Util Logging by trying to load the {@code org.slf4j.impl.JDK14LoggerAdapter}
     * class. The {@code JDK14LoggerAdapter} class is used by SLF4J as a wrapper over the
     * {@link java.util.logging.Logger} class and indicates that SLF4J support for Java Util Logging is in the classpath.
     *
     * @return {@code true} if SLF4J support for Java Util Logging is in the classpath.
     */
    public static boolean usingJUL() {
        boolean usingJUL = false;
        try {
            Class.forName("org.slf4j.impl.JDK14LoggerAdapter");
            usingJUL = true;
        } catch (ClassNotFoundException e) {
            /* We don't care to log anything here */
        }
        return usingJUL;
    }
}
