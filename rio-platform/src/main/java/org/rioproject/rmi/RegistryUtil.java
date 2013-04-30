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
package org.rioproject.rmi;

import net.jini.config.ConfigurationException;
import org.rioproject.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Utility for getting/creating the RMI Registry.
 *
 * <h4>Configuration</h4>
 * The RegistryUtil class supports the following configuration entries;
 * where each configuration entry name is associated with the component name
 * <tt>org.rioproject.rmi</tt>.
 *
 * <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace; color: rgb(0, 0, 0);"><a
 name="registryPort_"></a>registryPort</span>
 <table style="text-align: left; width: 100%;" border="0"
 cellspacing="2" cellpadding="2">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:<br>
 </td>
 <td style="vertical-align: top; font-family: monospace;">int</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:<br>
 </td>
 <td style="vertical-align: top;"><span
 style="font-family: monospace;">1099</span><br>
 </td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:<br>
 </td>
 <td style="vertical-align: top;">The port to use when
 creating the RMI Registry. This
 entry will be read at initialization, and based on the value provided
 by the registryRetries property, will be incremented<br>
 </td>
 </tr>
 </tbody>
 </table>
 </li>
 </ul>
 <code></code>
 <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace; color: rgb(0, 0, 0);"><a
 name="registryRetries_"></a>registryRetries</span>
 <table style="text-align: left; width: 100%;" border="0"
 cellspacing="2" cellpadding="2">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:<br>
 </td>
 <td style="vertical-align: top; font-family: monospace;">int</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:<br>
 </td>
 <td style="vertical-align: top;"><span
 style="font-family: monospace;">50</span><br>
 </td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:<br>
 </td>
 <td style="vertical-align: top;">This
 entry will be read at initialization and provides the ability to
 recover from RMI Registry creation failure by incrementing the port the
 RMI Registry instance will accept requests on. The port provided by the
 registryPort property is used as a basis to increment from. If retries
 are needed, the registryPort is incremented by one each time until
 either the RMI Registry is created without exceptions, or the registry
 retries have been exhausted<br>
 </td>
 </tr>
 </tbody>
 </table>
 </li>
 </ul>

 @author Dennis Reedy
 */
public class RegistryUtil {
    public static final int DEFAULT_PORT = 1099;
    public static final int DEFAULT_RETRY_COUNT = 50;
    static final String COMPONENT = "org.rioproject.rmi";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Check if RMI Registry has been started for the VM, if not start it.
     * This method will use the {@link org.rioproject.config.Constants#REGISTRY_PORT}
     * system property to determine if the {@link java.rmi.registry.Registry} has
     * been created.
     *
     * <p>If the RMI Registry is created, this method will also set the
     * @link org.rioproject.config.Constants#REGISTRY_PORT} system property
     *
     * @return The port the RMI Registry was created on, or -1 if the
     * RMIRegistry could not be created
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static int checkRegistry() throws ConfigurationException {
        int port;
        synchronized(RegistryUtil.class) {
            if(System.getProperty(Constants.REGISTRY_PORT)==null) {
                port = getRegistry();
                if(port>0)
                    System.setProperty(Constants.REGISTRY_PORT, Integer.toString(port));
            } else {
                port = Integer.parseInt(System.getProperty(Constants.REGISTRY_PORT));
            }
        }
        return port;
    }

    /**
     * Check if RMI Registry has been started for the VM, if not start it.
     *
     * @return The port the RMI Registry was created on, or -1 if the
     * RMIRegistry could not be created
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static int getRegistry() throws ConfigurationException {
        int registryPort;
        synchronized(RegistryUtil.class) {
            int[] portRange = getRegistryPortRange();
            if(portRange!=null) {
                try {
                    PortRangeRMIServerSocketFactory socketFactory =
                        new PortRangeRMIServerSocketFactory(portRange[0], portRange[1]);
                    ServerSocket s = socketFactory.createServerSocket(0);
                    registryPort = socketFactory.getLastPort();
                    s.close();
                    LocateRegistry.createRegistry(registryPort);
                } catch (IOException e) {
                    if(logger.isTraceEnabled())
                            logger.trace("Failed to create RMI Registry for port range {}-{}"+
                                          portRange[0], portRange[1]);
                    registryPort = -1;
                }
            } else {
                registryPort = DEFAULT_PORT;
                int originalPort = registryPort;
                int registryRetries = DEFAULT_RETRY_COUNT;
                Registry registry = null;
                for(int i = 0; i < registryRetries; i++) {
                    try {
                        registry = LocateRegistry.createRegistry(registryPort);
                        break;
                    } catch(RemoteException e1) {
                        if(logger.isTraceEnabled())
                            logger.trace("Failed to create RMI Registry using port [{}], increment port and try again",
                                         registryPort);
                    }
                    registryPort++;
                }
                if(registry==null) {
                    logger.warn("Unable to create RMI Registry using ports {} through {}", originalPort, registryPort);
                    registryPort = -1;
                } else {
                    if(logger.isDebugEnabled())
                        logger.debug("Created RMI Registry on port={}", System.getProperty(Constants.REGISTRY_PORT));
                }
            }
        }

        return registryPort;
    }

    /**
     * Get the registryPortRange property.
     *
     * @return The port range as an integer array, where the first element is the
     * range start, the second the range end. If there is no declared port range, return null.
     */
    static int[] getRegistryPortRange() {
        String registryPortRange = System.getProperty(Constants.PORT_RANGE);
        int[] portRange = null;
        if(registryPortRange!=null) {
            try {
                String[] range = registryPortRange.split("-");
                portRange = new int[2];
                portRange[0] = Integer.valueOf(range[0]);
                portRange[1] = Integer.valueOf(range[1]);
            } catch (Exception e) {
                portRange = null;
                logger.warn("Illegal range value specified, continue using default registryPort settings.", e);
            }
        }
        return(portRange);
    }

    /**
     * Get the registryRetries property.
     *
     * @return The number of times to attempt to find an RMI Registry instance
     */
    public static int getRegistryRetries() {
        return DEFAULT_RETRY_COUNT;
    }
}
