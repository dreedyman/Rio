/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.rmi;

import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;
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
    private static final int DEFAULT_RETRY_COUNT = 50;
    private static final String COMPONENT = "org.rioproject.rmi";
    private static final Logger logger = LoggerFactory.getLogger(COMPONENT);
    
    /**
     * Get the RMI Registry
     *
     * @return The RMI Registry
     */
    public static Registry getRegistry() throws UnknownHostException, RemoteException {
        int port = System.getProperty(Constants.REGISTRY_PORT) == null
                   ? DEFAULT_PORT : Integer.parseInt(System.getProperty(Constants.REGISTRY_PORT));
        return getRegistry(port);
    }

    /**
     * Get the RMI Registry.
     *
     * @param port The port to use.
     *
     * @return The RMI Registry
     */
    public static Registry getRegistry(int port) throws UnknownHostException, RemoteException {
        Registry registry;
        String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        if (System.getProperty("javax.net.ssl.keyStore") != null) {
            registry =  LocateRegistry.getRegistry(address, port, new SslRMIClientSocketFactory());
        } else {
            registry =  LocateRegistry.getRegistry(address, port);
        }
        return registry;
    }

    /**
     * Create a RMI Registry has been started for the VM, if not start it.
     *
     * @return The port the RMI Registry was created on, or -1 if the
     * RMIRegistry could not be created
     */
    public static Registry createRegistry() {
        int registryPort;
        Registry registry = null;
        synchronized(RegistryUtil.class) {
            int[] portRange = getRegistryPortRange();
            if (portRange!=null) {
                try {
                    PortRangeRMIServerSocketFactory socketFactory =
                        new PortRangeRMIServerSocketFactory(portRange[0], portRange[1]);
                    ServerSocket s = socketFactory.createServerSocket(0);
                    registryPort = socketFactory.getLastPort();
                    s.close();
                    registry = createRegistry(registryPort);
                    System.setProperty(Constants.REGISTRY_PORT, Integer.toString(registryPort));
                } catch (IOException e) {
                    if (logger.isTraceEnabled())
                        logger.trace("Failed to create RMI Registry for port range "
                                             + portRange[0] + "-" + portRange[1]);
                }
            } else {
                registryPort = DEFAULT_PORT;
                int originalPort = registryPort;
                for(int i = 0; i < DEFAULT_RETRY_COUNT; i++) {
                    try {
                        registry = createRegistry(registryPort);
                        break;
                    } catch(IOException e1) {
                        if (logger.isTraceEnabled())
                            logger.trace("Failed to create RMI Registry using port [{}], increment port and try again",
                                         registryPort);
                    }
                    registryPort++;
                }
                if (registry == null) {
                    logger.warn("Unable to create RMI Registry using ports {} through {}", originalPort, registryPort);
                } else {
                    System.setProperty(Constants.REGISTRY_PORT, Integer.toString(registryPort));
                    if (logger.isDebugEnabled())
                        logger.debug("Created RMI Registry on port={}", System.getProperty(Constants.REGISTRY_PORT));
                }
            }
        }
        return registry;
    }

    private static Registry createRegistry(int registryPort) throws RemoteException {
        Registry registry;
        if (System.getProperty("javax.net.ssl.keyStore") != null) {
            registry = LocateRegistry.createRegistry(registryPort,
                                                     new SslRMIClientSocketFactory(),
                                                     new SslRMIServerSocketFactory());
        } else {
            registry = LocateRegistry.createRegistry(registryPort);
        }
        logger.info("Created Registry on port: {}", registryPort);
        System.setProperty(Constants.REGISTRY_PORT, Integer.toString(registryPort));
        return registry;
    }

    /**
     * Get the registryPortRange property.
     *
     * @return The port range as an integer array, where the first element is the
     * range start, the second the range end. If there is no declared port range, return null.
     */
    private static int[] getRegistryPortRange() {
        String registryPortRange = System.getProperty(Constants.PORT_RANGE);
        int[] portRange = null;
        if (registryPortRange!=null) {
            try {
                String[] range = registryPortRange.split("-");
                portRange = new int[2];
                portRange[0] = Integer.parseInt(range[0]);
                portRange[1] = Integer.parseInt(range[1]);
            } catch (Exception e) {
                portRange = null;
                logger.warn("Illegal range value specified, continue using default registryPort settings.", e);
            }
        }
        return portRange;
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
