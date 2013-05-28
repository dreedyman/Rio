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
package org.rioproject.jmx;

import org.rioproject.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Utility for obtaining the MBeanServer.
 *
 * @author Dennis Reedy
 */
public class MBeanServerFactory {
    private static final Logger logger = LoggerFactory.getLogger(MBeanServerFactory.class);
    /**
     * Get the MBeanServer to use. If the system property
     * {@link org.rioproject.config.Constants#JMX_MBEANSERVER} is set, locate
     * the MBeanServer identified by the property. Otherwise get the platform
     * MBeanServer
     *
     * @return the <code>MBeanServer</code>
     */
    public static MBeanServer getMBeanServer() {
        MBeanServer server = null;
        if(System.getProperty(Constants.JMX_MBEANSERVER)!=null) {
            String agentID = System.getProperty(Constants.JMX_MBEANSERVER);
            try {
                server = javax.management.MBeanServerFactory.findMBeanServer(agentID).get(0);
                logger.info("Obtained MBeanServer using agentID: {}", agentID);
            } catch(Exception e) {
                logger.warn("Could not get MBeanServer from agentId: {}", agentID, e);
            }
        }
        if(server==null) {
            server = ManagementFactory.getPlatformMBeanServer();
        }
        return(server);
    }
}
