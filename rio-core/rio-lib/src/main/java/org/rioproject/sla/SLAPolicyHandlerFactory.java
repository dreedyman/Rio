/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.sla;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * A utility to create a SLAPolicyHandler based on a Configuration
 *
 * @author Dennis Reedy
 */
public class SLAPolicyHandlerFactory {
    private static final String COMPONENT="org.rioproject.sla";
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Create a SLAPolicyHandler based on the provided SLA
     * 
     * @param sla The SLA object to create an SLAPolicyHandler instance for
     * @param eventSource The object to be used as the remote event source
     * @param eventHandler Handler which sends events
     * @param context The ServiceBeanContext
     * @param loader A ClassLoader to load resources and classes, and to pass
     * when constructing the SLAPolicyHandler. If null, the context class
     * loader will be used.
     *
     * @return A SLAPolicyHandler created from the SLA
     *
     * @throws Exception If the configuration cannot be used
     */
    public static SLAPolicyHandler create(SLA sla,
                                          Object eventSource,
                                          EventHandler eventHandler,
                                          ServiceBeanContext context,
                                          ClassLoader loader) throws Exception {
        if(sla==null)
            throw new IllegalArgumentException("sla is null");
        logger.trace("Creating SLAPolicyHandler for {}", sla);
        Class slaPolicyHandlerClass = loader.loadClass(sla.getSlaPolicyHandler());
        Constructor cons = slaPolicyHandlerClass.getConstructor(SLA.class);
        SLAPolicyHandler slaPolicyHandler = (SLAPolicyHandler)cons.newInstance(sla);
        /* Initialize the policy handler for processing */
        slaPolicyHandler.initialize(eventSource, eventHandler, context);
        logger.debug("SLAPolicyHandler [{}] created", slaPolicyHandler.getClass().getName());
        return(slaPolicyHandler);
    }  
    
    /**
     * Determine if the provided SLAPolicyHandler is the same as the
     * SLAPolicyHandler class configured as part of the SLA.
     *
     * @param sla The SLA to use
     * @param handler The SLAPolicyHandler
     *
     * @return true if the provided SLAPolicyHandler is the same as the
     * SLAPolicyHandler class configured as part of the SLA
     * 
     */
    public static boolean slaPolicyHandlerChanged(SLA sla, SLAPolicyHandler handler) {
        return !sla.getSlaPolicyHandler().equals(handler.getClass().getName());        
    }
}
