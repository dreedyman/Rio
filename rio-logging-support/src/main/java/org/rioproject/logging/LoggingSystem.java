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

/**
 * Determine the logging system that is loaded.
 *
 * @author Dennis Reedy
 */
public final class LoggingSystem {
    private LoggingSystem() {
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
