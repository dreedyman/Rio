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
package org.rioproject.test.simple;

import org.rioproject.log.ServiceLogEventHandler;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Simple service that logs.
 */
public class LoggingSimpleImpl implements Simple {
    Logger logger = Logger.getLogger(LoggingSimpleImpl.class.getName());
    
    public String hello(String message)  {
        if(System.getProperty("org.rioproject.serviceBeanExec")!=null) {
            for(Handler h : Logger.getLogger("").getHandlers()) {
                if(h instanceof ServiceLogEventHandler) {
                    ServiceLogEventHandler s = (ServiceLogEventHandler)h;
                    s.addPublishableLogger(logger.getName());
                    s.setPublishOnLevel(Level.INFO.toString());
                    logger.info("Set to "+Level.INFO);
                    break;
                }
            }
        }
        if(message==null) {
            logger.severe("Passed a null message parameter");
        }
        return(processMessage(message));
    }

    private String processMessage(String message) {
        String response = null;
        try {
            int length = message.length();
            response = "You sent a message that was "+length+" long";
        } catch(Throwable t) {
            logger.log(Level.WARNING, "Caught while processing message", t);
        }
        return response;
    }
}
