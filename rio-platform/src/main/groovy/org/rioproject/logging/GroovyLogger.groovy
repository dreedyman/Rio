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
package org.rioproject.logging

import java.util.logging.*

/**
 * Simple wrapper for java.util.logging.Logger.
 * This logger can log correct class/method names even if used in Groovy.
 *
 * @author ksky@jggug.org
 * @author Dennis Reedy  (added methods for logger compatibility)
 */
class GroovyLogger {
    private static final EXCLUDE_LIST = [
            GroovyLogger.class.name,
            "groovy.", "org.codehaus.groovy.", "gjdk.groovy.",
            "java.", "java_", "javax.", "sun.",
            "com.google.apphosting.",	// for GAE/J"
    ]
    private final Logger logger

    GroovyLogger(String name) {
        logger = Logger.getLogger(name)
    }

    void log(Level level, String msg) {
        def caller = getCaller()
        logger.logp(level, caller.className, caller.methodName, msg)
    }

    void log(Level level, String msg, Throwable thrown) {
        def caller = getCaller()
        logger.logp(level, caller.className, caller.methodName, msg, thrown)
    }

    boolean isLoggable(Level level) {
        return logger.isLoggable(level)
    }

    private StackTraceElement getCaller() {
        def stack = Thread.currentThread().getStackTrace()
        def caller = stack.find { elem ->
            EXCLUDE_LIST.every {
                !elem.className.startsWith(it)
            }
        }
        return caller
    }

    void severe (String msg) { log(Level.SEVERE,  msg) }
    void warning(String msg) { log(Level.WARNING, msg) }
    void info   (String msg) { log(Level.INFO,    msg) }
    void config (String msg) { log(Level.CONFIG,  msg) }
    void fine   (String msg) { log(Level.FINE,    msg) }
    void finer  (String msg) { log(Level.FINER,   msg) }
    void finest (String msg) { log(Level.FINEST,  msg) }
}