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
package org.rioproject.start;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Helper for management of logging system
 *
 * @author Dennis Reedy
 */
public final class LogManagementHelper {
    private static final org.slf4j.Logger stdOutLogger = LoggerFactory.getLogger("std.out");
    private static final org.slf4j.Logger stdErrLogger = LoggerFactory.getLogger("std.err");

    private LogManagementHelper() {}

    /**
     * Installs a {@code ServiceLogEventHandler} based on the logging system being used.
     */
    public static void setup() {
        //redirectIfNecessary();
        try {
            Class<?> cl = Class.forName("org.rioproject.logging.ServiceLogEventHandlerHelper");
            Method addServiceLogEventHandler = cl.getMethod("addServiceLogEventHandler");
            addServiceLogEventHandler.invoke(null);
        } catch(Exception e) {
            stdErrLogger.warn("Unable to add ServiceLogEventHandler {}: {}", e.getClass().getName(), e.getMessage());
        }
    }

    /**
     * Checks if the underlying logging system configuration has been changed from default
     * startup settings.
     */
    public static void checkConfigurationReset() {
        String config = System.getProperty("logback.configurationFile");
        if(config==null)
            return;
        try {
            Class<?> loggerFactory = Class.forName("org.slf4j.LoggerFactory");
            Method getILoggerFactory = loggerFactory.getMethod("getILoggerFactory");
            Object loggerContext = getILoggerFactory.invoke(null);

            Class<?> configurationWatchListUtil = Class.forName("ch.qos.logback.core.joran.util.ConfigurationWatchListUtil");
            URL main = (URL) getMethod("getMainWatchURL", configurationWatchListUtil).invoke(null, loggerContext);
            File currentConfigFile = null;
            if(main!=null) {
                currentConfigFile = new File(main.toURI());
            }

            File configFile = new File(config);
            if (currentConfigFile == null ||
                !currentConfigFile.getAbsolutePath().equals(configFile.getAbsolutePath())) {
                Method reset = loggerContext.getClass().getMethod("reset");
                reset.invoke(loggerContext);

                if(configFile.getName().endsWith(".xml")) {
                    Class<?> joranConfigurator = Class.forName("ch.qos.logback.classic.joran.JoranConfigurator");
                    Object configurator = joranConfigurator.newInstance();
                    getMethod("setContext", joranConfigurator).invoke(configurator, loggerContext);
                    joranConfigurator.getMethod("doConfigure", File.class).invoke(configurator, configFile);
                } else {
                    Class<?> gafferConfigurator = Class.forName("ch.qos.logback.classic.gaffer.GafferConfigurator");
                    Constructor<?> constructor = gafferConfigurator.getConstructor(loggerContext.getClass());
                    Object configurator = constructor.newInstance(loggerContext);
                    gafferConfigurator.getMethod("run", File.class).invoke(configurator, configFile);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Method getMethod(String name, Class<?> cl) {
        for(Method m : cl.getMethods()) {
            if(m.getName().equals(name)) {
                return m;
            }
        }
        throw new NoSuchMethodError("Could not find "+name+" in "+cl.getName());
    }

    static void redirectIfNecessary() {
        /* If we have been exec'd by Rio (such as a service that has been declared to be forked,
         * stdout and stderr have already been redirected */
        if(System.getenv("RIO_EXEC")==null && System.console()==null) {
            redirectToLogger();
        }
    }

    static void redirectToLogger(){
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
}
