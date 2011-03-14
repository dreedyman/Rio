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

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import net.jini.config.Configuration;
import org.rioproject.boot.BootUtil;
import org.rioproject.rmi.RegistryUtil;
import org.rioproject.config.Constants;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides JMX connection utilities.
 *
 * @author Dennis Reedy
 */
public class JMXConnectionUtil {
    static final String COMPONENT = "org.rioproject.jmx";
    static final Logger logger = Logger.getLogger(COMPONENT);

    /**
     * Create a {@link javax.management.remote.JMXConnectorServer}, bound to
     * the RMI Registry created by an infrastructure service (Cybernode or
     * Monitor).
     *
     * <p>If <tt>JMXConnectorServer</tt> has been created, return immediately.
     * If the <tt>JMXConnectorServer</tt> needs to be created, it will be
     * bound to the RMI Registry, and set the
     * <tt>org.rioproject.jmxServiceURL</tt> system property.
     *
     * <p>This utility uses the {@link org.rioproject.rmi.RegistryUtil} class to
     * obtain the port to access the RMI Registry.
     *
     * <h4>Configuration</h4>
     * This method supports the following configuration entries;
     * where each configuration entry name is associated with the component name
     * <tt>org.rioproject.jmx</tt>.
     * <ul>
     * <li><span
     * style="font-weight: bold; font-family: courier new,courier,monospace;
     * color: rgb(0, 0, 0);">hostAddress</span>
     * <table style="text-align: left; width: 100%;" border="0"
     * cellspacing="2" cellpadding="2">
     * <tbody>
     * <tr>
     * <td
     * style="vertical-align: top; text-align: right; font-weight: bold;">Type:<br>
     * </td>
     * <td style="vertical-align: top; font-family: monospace;">String<br>
     * </td>
     * </tr>
     * <tr>
     * <td
     * style="vertical-align: top; text-align: right; font-weight: bold;">Default:<br>
     * </td>
     * <td style="vertical-align: top;">If the<span
     * style="font-weight: bold;"> </span><span
     * style="font-family: monospace;">java.rmi.server.hostname</span>
     * property
     * is set,<span style="font-weight: bold;"> </span>use this as the
     * default, otherwise use the value returned by {@link
     * java.net.InetAddress#getLocalHost()}<span style="font-weight: bold;"><br>
     * </span></td>
     * </tr>
     * </tbody>
     * </table>
     * </li>
     * </ul>
     *
     * @param config Configuration object to use
     *
     * @throws Exception If there are errors reading the configuration, or
     * creating the {@link javax.management.remote.JMXConnectorServer}
     */
    @SuppressWarnings("unchecked")
    public static void createJMXConnection(Configuration config) throws Exception {
        if(System.getProperty(Constants.JMX_SERVICE_URL)!=null)
            return;
        RegistryUtil.checkRegistry(config);
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        int registryPort = Integer.parseInt(sPort);

        if(registryPort==0) {
            logger.severe("RMI Registry property ["+Constants.REGISTRY_PORT+"] " +
                          "not found, unable to create MBeanServer");
            throw new Exception("Unable to create the JMXConnectorServer");
        }

        String defaultAddress =
            BootUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);

        String hostAddress = (String) config.getEntry(COMPONENT,
                                                      "hostAddress",
                                                      String.class,
                                                      defaultAddress);

        MBeanServer mbs = MBeanServerFactory.getMBeanServer();
        /*String jmxServiceURL = "service:jmx:rmi:///jndi/rmi://"+
                               hostAddress+":"+registryPort+
                               "/jmxrmi";
*/
         JMXServiceURL jmxServiceURL =
                new JMXServiceURL("service:jmx:rmi://"+hostAddress+
                                  ":"+registryPort+"/jndi/rmi://"+hostAddress+":"+registryPort+"/jmxrmi");

        if(logger.isLoggable(Level.INFO))
            logger.info("JMXServiceURL="+jmxServiceURL);

        Map env = System.getProperties();
        JMXConnectorServer jmxConn =
            JMXConnectorServerFactory.newJMXConnectorServer(
                //new JMXServiceURL(jmxServiceURL),
                jmxServiceURL,
                env,
                //null,
                mbs);
        if(jmxConn != null) {
            jmxConn.start();
            System.setProperty(Constants.JMX_SERVICE_URL, jmxServiceURL.toString());
            if(logger.isLoggable(Level.CONFIG))
                logger.config(
                    "JMX Platform MBeanServer exported with RMI Connector");
        } else {
            throw new Exception("Unable to create the JMXConnectorServer");
        }
    }

    /**
     * Get a string that can be used as input for a
     * {@link javax.management.remote.JMXServiceURL}
     *
     * @param port The port the MBeanServer has been created on
     * @param hostAddress The host address the MBeanServer is running on
     *
     * @return A formatted string that can be used as input to create a
     * {@link javax.management.remote.JMXServiceURL}
     */
    public static String getJMXServiceURL(int port, String hostAddress) {
        if(hostAddress==null)
            throw new IllegalArgumentException("hostAddress is null");
        return "service:jmx:rmi:///jndi/rmi://"+hostAddress+":"+port+"/jmxrmi";
    }

    /**
     * Using the <a
     * href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
     * JMX Attach API </a>, connect to a local Java Virtual Machine.
     *
     * <p>This utility requires Java 6 or greater.
     *
     * @param id The identifier used to connect to the Java Virtual Machine
     * @return An MBeanServerConnection to the platform MBeanServer of the
     * Java Virtual Machine identified, or null if the connection cannot be
     * created.
     *
     * @throws IOException if the MBeanServerConnection cannot be created.
     * @throws AttachNotSupportedException if the underlying provider either does not exist,
     * or if the provider attempts to attach to a Java virtual machine with which it is not compatible.
     * @throws AgentInitializationException thrown when an agent fails to initialize in the
     * target Java virtual machine.
     * @throws AgentLoadException when an agent cannot be loaded into the target Java virtual machine.
     */
    public static MBeanServerConnection attach(String id) throws IOException,
                                                                 AttachNotSupportedException,
                                                                 AgentInitializationException,
                                                                 AgentLoadException {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.info("The JMX Attach APIs require Java 6 or above. " +
                        "You are running Java "+jvmVersion);
            return null;
        }

        VirtualMachine vm = VirtualMachine.attach(id);
        String connectorAddr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (connectorAddr == null) {
            String agent = vm.getSystemProperties().getProperty("java.home")+File.separator+"lib"+File.separator+
                           "management-agent.jar";
            vm.loadAgent(agent);
            connectorAddr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        }
        MBeanServerConnection mbs = null;
        if(connectorAddr!=null) {
            JMXServiceURL serviceURL = new JMXServiceURL(connectorAddr);
            JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
            mbs = connector.getMBeanServerConnection();
        }
        return mbs;
    }

    /**
     * Using the <a
     * href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
     * JMX Attach API </a>, list the available local Java Virtual Machines.
     *
     * <p>This utility requires Java 6 or greater.
     *
     * @return A String array of Java Virtual Machine IDs.
     */
    public static String[] listIDs() {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.info("The JMX Attach APIs require Java 6 or above. " +
                        "You are running Java "+jvmVersion);
            return new String[0];
        }
        List<String> vmList = new ArrayList<String>();
        try {
            Class vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method list = vmClass.getMethod("list");
            List vmDescriptors = (List)list.invoke(null);
            for (Object vmDesc : vmDescriptors) {
                Method id = vmDesc.getClass().getMethod("id");
                vmList.add((String)id.invoke(vmDesc));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not obtain list of VMs", e);
        }
        return vmList.toArray(new String[vmList.size()]);
	}

    /**
     * Using the <a
     * href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
     * JMX Attach API </a>, list the available local Java Virtual Machines.
     *
     * <p>This utility requires Java 6 or greater.
     *
     * @return A String array of Java Virtual Machine IDs followed by the
     * displayName of each discovered <tt>VirtualMachine</tt>
     */
    public static String[] listManagedVMs() {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.info("The JMX Attach APIs require Java 6 or above. " +
                        "You are running Java "+jvmVersion);
            return new String[0];
        }
        List<String> vmList = new ArrayList<String>();
        try {
            Class vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method list = vmClass.getMethod("list");
            List vmDescriptors = (List)list.invoke(null);
            for (Object vmDesc : vmDescriptors) {
                Method displayName = vmDesc.getClass().getMethod("displayName");
                Method id = vmDesc.getClass().getMethod("id");
                vmList.add(id.invoke(vmDesc)+" "+displayName.invoke(vmDesc));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not obtain list of VMs", e);
        }
        return vmList.toArray(new String[vmList.size()]);
	}
}
