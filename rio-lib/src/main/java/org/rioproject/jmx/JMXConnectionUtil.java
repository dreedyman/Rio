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

import com.sun.tools.attach.VirtualMachine;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.rioproject.rmi.RegistryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.*;
import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * Provides JMX connection utilities.
 *
 * @author Dennis Reedy
 */
public class JMXConnectionUtil {
    static final Logger logger = LoggerFactory.getLogger(JMXConnectionUtil.class);

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
     * @throws Exception If there are errors reading the configuration, or
     * creating the {@link javax.management.remote.JMXConnectorServer}
     */
    public static void createJMXConnection() throws Exception {
        if(System.getProperty(Constants.JMX_SERVICE_URL)!=null)
            return;
        RegistryUtil.checkRegistry();
        String sPort = System.getProperty(Constants.REGISTRY_PORT, "0");
        int registryPort = Integer.parseInt(sPort);

        if(registryPort==0) {
            logger.error("RMI Registry property [{}] not found, unable to create MBeanServer", Constants.REGISTRY_PORT);
            throw new Exception("Unable to create the JMXConnectorServer");
        }

        MBeanServer mbs = MBeanServerFactory.getMBeanServer();
        String hostAddress = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi://"+hostAddress+":"+registryPort+
                                                        "/jndi/rmi://"+hostAddress+":"+registryPort+"/jmxrmi");
        System.setProperty(Constants.JMX_SERVICE_URL, jmxServiceURL.toString());
        if(logger.isInfoEnabled())
            logger.info("JMXServiceURL={}", jmxServiceURL);
        JMXConnectorServer jmxConn = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null, mbs);
        jmxConn.start();
        if(logger.isDebugEnabled())
            logger.debug("JMX Platform MBeanServer exported with RMI Connector");

    }

    /**
     * Get the agentID of the Platform MBeanServer
     *
     * @return The agentID of the Platform MBeanServer
     *
     * @throws Exception if the agent ID cannot be found
     */
    public static String getPlatformMBeanServerAgentId() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final String SERVER_DELEGATE = "JMImplementation:type=MBeanServerDelegate";
        final String MBEAN_SERVER_ID_KEY = "MBeanServerId";
        ObjectName delegateObjName = new ObjectName(SERVER_DELEGATE);
        return (String) mbs.getAttribute(delegateObjName, MBEAN_SERVER_ID_KEY );
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
     * @throws Exception if the following occurs:
     * <ul>
     *     <li>MBeanServerConnection cannot be created</li>
     *     <li>If the underlying provider either does not exist, or if the provider attempts to attach to a
     *     Java virtual machine with which it is not compatible.</li>
     *     <li>If an agent fails to initialize in the target Java virtual machine.</li>
     *     <li>If an agent cannot be loaded into the target Java virtual machine.</li>
     * </ul>
     */
    public static MBeanServerConnection attach(final String id) throws Exception {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.info("The JMX Attach APIs require Java 6 or above. You are running Java {}", jvmVersion);
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
}
