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
package org.rioproject.test.utils;

import java.util.logging.*;

/**
 * Utility for setting loggers.
 */
public class LogUtil {

    public static void setLogger(String loggerName, Level level) {
        Logger l = LogManager.getLogManager().getLogger(loggerName);
        if (l == null) {
            l = Logger.getLogger(loggerName);
            l.setUseParentHandlers(true);
        }
        l.setLevel(level);
        if (l.getHandlers().length == 0) {
            Handler h = new ConsoleHandler();
            h.setLevel(level);
            l.addHandler(h);
        } else {
            for (Handler h : l.getHandlers()) {
                h.setLevel(level);
            }
        }
    }
}
