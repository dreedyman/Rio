/*
 * Copyright 2008 the original author or authors.
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

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * Allows the instrumentation class to be accessed and checks for log redirection if running without a console.
 *
 * @author Dennis Reedy
 */
public class AgentHook {
    private static final org.slf4j.Logger stdOutLogger = LoggerFactory.getLogger("std.out");
    private static final org.slf4j.Logger stdErrLogger = LoggerFactory.getLogger("std.err");
    static Instrumentation instrumentation;

    public static Instrumentation getInstrumentation() {
        return(instrumentation);
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        redirectIfNecessary();
        try {
            Class cl = Class.forName("org.rioproject.logging.ServiceLogEventHandlerHelper");
            @SuppressWarnings("unchecked")
            Method addServiceLogEventHandler = cl.getMethod("addServiceLogEventHandler");
            addServiceLogEventHandler.invoke(null);
        } catch(Exception e) {
            stdErrLogger.warn("Unable to add ServiceLogEventHandler {}: {}", e.getClass().getName(), e.getMessage());
        }
    }

    static void redirectIfNecessary() {
        /* If we have been exec'd by Rio (such as a service that has been declared to be forked,
         * stdout and stderr have already been redirected */
        if(System.getenv("RIO_EXEC")==null && System.console()==null) {
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
}
