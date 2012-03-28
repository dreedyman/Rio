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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.logging.*;

/**
 * Check that the Java logging configuration has a console available. If a console is not available, remove
 * any {@link java.util.logging.ConsoleHandler}s, and set the system out and system error to the name
 * of the service being started as determined by the {@code org.rioproject.service} system property.
 * <p/>
 * <p>The resulting {@code service}.out and {@code service}.err wil be stored in the location declared by the
 * {@code RIO_LOG_DIR} system property.
 * </p>
 *
 * @author Dennis Reedy
 */
public class LogAgent {
    private static final Logger logger = Logger.getLogger(AgentHook.class.getName());
    private LogAgent() {
    }

    public static void redirectIfNecessary() {
        if (System.console() == null) {
            for (Enumeration<String> e = LogManager.getLogManager().getLoggerNames(); e.hasMoreElements(); ) {
                Logger l = Logger.getLogger(e.nextElement());
                for (Handler h : l.getHandlers()) {
                    if (h instanceof ConsoleHandler) {
                        h.close();
                        l.removeHandler(h);
                    }
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append("\n================================\n");
            builder.append("There is no console support, removed ConsoleHandler\n");

            String logDir = System.getProperty("RIO_LOG_DIR");
            if (logDir != null) {
                File rioLogDir = new File(logDir);
                String service = System.getProperty("org.rioproject.service");
                File serviceOutput = getOutputFile(service, new File(rioLogDir, service + "-0.out"));
                File serviceError = getOutputFile(service, new File(rioLogDir, service + "-0.err"));
                redirect(serviceOutput, "standard output");
                redirect(serviceError, "standard error");
                builder.append("System out has been redirected to ").append(serviceOutput.getPath()).append("\n");
                builder.append("System err has been redirected to ").append(serviceError.getPath()).append("\n");
            }
            builder.append("================================");
            logger.info(builder.toString());
        }
    }

    private static void redirect(final File file, final String type) {
        try {
            System.setOut(new PrintStream(new FileOutputStream(file)));
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Redirecting " + type + " to " + file.getPath(), e);
        }
    }

    private static File getOutputFile(final String service, File f) {
        File logFile;
        if (f.exists()) {
            int i = 1;
            /* Get extension */
            String ext = f.getName();
            ext = ext.substring(ext.lastIndexOf("."));
            File dir = f.getParentFile();
            while (f.exists()) {
                f = new File(dir, service + "-" + (i++) + ext);
            }
            logFile = f;
        } else {
            logFile = f;
        }
        return logFile;
    }
}
