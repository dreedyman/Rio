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

import org.slf4j.LoggerFactory;

import java.io.PrintStream;

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
    private static final org.slf4j.Logger stdOutLogger = LoggerFactory.getLogger("std.out");
    private static final org.slf4j.Logger stdErrLogger = LoggerFactory.getLogger("std.err");

    private LogAgent() {
    }

    public static void redirectIfNecessary() {
        if(System.console()==null) {
            redirectToLogger();
        }
    }

    public static void redirectToLogger(){
        System.setOut(new PrintStream(System.out){
            public void print(String s){
                stdOutLogger.info(s);
            }
        });
        System.setErr(new PrintStream(System.err){
            public void print(String s){
                stdErrLogger.error(s);
            }
        });
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
