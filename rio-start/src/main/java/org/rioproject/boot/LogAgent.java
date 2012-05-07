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
import org.rioproject.logging.WrappedLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.logging.*;

/**
 * Check that the Java logging configuration has a console available. If a console is not available, remove
 * any {@link java.util.logging.ConsoleHandler}s, and set the system out and system error to the name
 * of the service log file.
 * <p/>
 * <p>The resulting {@code service}.out and {@code service}.err wil be stored in the location declared by the
 * {@code RIO_LOG_DIR} system property.
 * </p>
 *
 * @author Dennis Reedy
 */
public final class LogAgent {
    private static final WrappedLogger logger = WrappedLogger.getLogger(AgentHook.class.getName());
    private LogAgent() {
    }

    public static void redirectIfNecessary() {
        String lockFileName = null;
        if (System.console() == null) {
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
            StringBuilder builder = new StringBuilder();
            builder.append("\nThere is no console support, removed ConsoleHandler\n");

            String logDir = System.getProperty("RIO_LOG_DIR");
            if (logDir != null && lockFileName!=null) {
                File lockFile = new File(lockFileName);
                int ndx = lockFile.getName().indexOf(".lck");
                String fileName = lockFile.getName().substring(0, ndx);
                File rioLogDir = new File(logDir);
                File serviceOutput = new File(rioLogDir, fileName + ".out");
                File serviceError = new File(rioLogDir, fileName + ".err");
                redirect(serviceOutput, true);
                redirect(serviceError, false);
                builder.append("System out has been redirected to ").append(serviceOutput.getPath()).append("\n");
                builder.append("System err has been redirected to ").append(serviceError.getPath()).append("\n");
            }
            logger.info(builder.toString());
        }
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
            logger.log(Level.WARNING, e, "Redirecting %s to %s", type, file.getPath());
        }
    }
}
