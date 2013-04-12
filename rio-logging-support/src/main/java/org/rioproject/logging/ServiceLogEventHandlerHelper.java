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

import org.rioproject.logging.jul.JULServiceLogEventHandler;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Handler;

/**
 * Helper to setup and return instances of a {@link ServiceLogEventHandler}.
 *
 * @author Dennis Reedy
 */
public class ServiceLogEventHandlerHelper {

    /**
     * Add a {@link ServiceLogEventHandler} to the underlying logging system.
     */
    @SuppressWarnings("unused")
    public static void addServiceLogEventHandler() {
        if(!LoggingSystem.usingJUL()) {
            addLogbackAppender();
        } else {
            java.util.logging.Logger.getLogger("").addHandler(new JULServiceLogEventHandler());
        }
    }

    /**
     * Find a {@link ServiceLogEventHandler} in the underlying logging system.
     *
     * @return The first instance of a {@link ServiceLogEventHandler} found in the underlying logging system.
     * If no instance is found, return a {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static ServiceLogEventHandler findInstance() {
        ServiceLogEventHandler handler = null;
        if(LoggingSystem.usingJUL()) {
            for(Handler h : java.util.logging.Logger.getLogger("").getHandlers()) {
                if(h instanceof ServiceLogEventHandler) {
                    handler = (ServiceLogEventHandler)h;
                    break;
                }
            }
        } else {
            /* Reflection is used here because Logback may not be in the classpath. */
            try {
                Class loggerFactory = Class.forName("org.slf4j.LoggerFactory");
                Method getILoggerFactory = loggerFactory.getMethod("getILoggerFactory");
                Object loggerContext = getILoggerFactory.invoke(null);
                Method getLoggerList = loggerContext.getClass().getMethod("getLoggerList");
                for(Object logger : (Iterable)getLoggerList.invoke(loggerContext)) {
                    Method iteratorForAppenders = logger.getClass().getMethod("iteratorForAppenders");
                    for (Iterator index = (Iterator) iteratorForAppenders.invoke(logger); index.hasNext();) {
                        Object appender = index.next();
                        if(appender instanceof ServiceLogEventHandler) {
                            handler = (ServiceLogEventHandler)appender;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return handler;
    }

    @SuppressWarnings("unchecked")
    private static void addLogbackAppender() {
        /* Reflection is used here because Logback may not be in the classpath. */
        try {
            String rioHome = System.getProperty("RIO_HOME", System.getenv("RIO_HOME"));
            File configuration = new File(rioHome, "config/logging/appender-config.xml");
            if(!configuration.exists())
                return;
            Class loggerFactory = Class.forName("org.slf4j.LoggerFactory");
            Method getILoggerFactory = loggerFactory.getMethod("getILoggerFactory");
            Object loggerContext = getILoggerFactory.invoke(null);

            Class configuratorClass = Class.forName("ch.qos.logback.classic.joran.JoranConfigurator");
            Object configurator = configuratorClass.newInstance();
            for(Method method : configuratorClass.getMethods()) {
                if(method.getName().equals("setContext")) {
                    method.invoke(configurator, loggerContext);
                    break;
                }
            }
            Method doConfigure = configuratorClass.getMethod("doConfigure", File.class);
            doConfigure.invoke(configurator, configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
