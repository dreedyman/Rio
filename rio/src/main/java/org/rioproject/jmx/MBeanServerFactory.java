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

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Utility for obtaining the MBeanServer.
 *
 * @author Dennis Reedy
 */
public class MBeanServerFactory {

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
            try {
                for (Object o : 
                    javax.management.MBeanServerFactory.findMBeanServer(
                        Constants.JMX_MBEANSERVER)) {
                    server = (MBeanServer) o;
                    if (server != null)
                        break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if(server==null) {
            server = ManagementFactory.getPlatformMBeanServer();
        }
        return(server);
    }
}
