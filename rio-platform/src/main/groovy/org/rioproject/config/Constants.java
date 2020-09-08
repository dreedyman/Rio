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
package org.rioproject.config;

/**
 * System constants
 *
 * @author Dennis Reedy
 */
public interface Constants {
    String BASE_COMPONENT = "org.rioproject";
    @Deprecated
    String CODESERVER = BASE_COMPONENT+".webster";
    /**
     * System property set indicating the address and port of the Webster
     * instance created by this utility, in the form of :
     * <pre>http://address:port</pre>
     */
    String WEBSTER = BASE_COMPONENT+".webster";
    /**
     * System property set indicating the roots that webster is serving
     */
    String WEBSTER_ROOTS = WEBSTER+".roots";
    /**
     * System property that points to a keystore
     */
    String KEYSTORE = BASE_COMPONENT+".keystore";
    /**
     * System property set when an RMI Registry is started
     */
    String REGISTRY_PORT = BASE_COMPONENT+".registryPort";
    /**
     * System property for the cybernode's process ID
     */
    String PROCESS_ID = BASE_COMPONENT+".processID";
    /**
     * System property set to indicate to use a specific MBeanServer. If not
     * set then the Platform MBeanServer is to be used
     */
    String JMX_MBEANSERVER = BASE_COMPONENT+".jmxMBeanServer";
    /**
     * System property set when the JMX Platform MBeanServer is set to the
     * RMI Registry
     */
    String JMX_SERVICE_URL = BASE_COMPONENT+".jmxServiceURL";
    /**
     * System property set when to indicate a comma separated list of group
     * names to use
     */
    String GROUPS_PROPERTY_NAME = BASE_COMPONENT+".groups";
    /**
     * System property set when to indicate a comma separated list of lookup
     * locators
     */
    String LOCATOR_PROPERTY_NAME = BASE_COMPONENT+".locators";
    /**
     * System property set when to indicate a comma separated list of lookup
     * locators
     */
    String ENV_PROPERTY_NAME = BASE_COMPONENT+".env";
    /**
     * System property to set and check to determine address to bind to or
     * address bound to
     */
    String RMI_HOST_ADDRESS = "java.rmi.server.hostname";
    /**
     * System property name for ServiceBeanExecutor name binding
     */
    String SERVICE_BEAN_EXEC_NAME = BASE_COMPONENT+".serviceBeanExec";
    /**
     * Property to indicate that a service bean is being started
     */
    String STARTING = BASE_COMPONENT+".starting-bean";
    /**
     * Property to indicate the directory the test framework has been started
     * in.
     */
    String RIO_TEST_EXEC_DIR = BASE_COMPONENT+".test.exec.dir";
    /**
     * Property to indicate that the Resolver should not include Rio artifacts
     * in the resolution of a classpath. If this property is set to &quot;false&quot;,
     * the Resolver will not include the Rio platform (and it's dependencies) in
     * generated classpaths.
     */
    @SuppressWarnings("unused")
    String RESOLVER_PRUNE_PLATFORM = BASE_COMPONENT+".resolver.prune.platform";
    /**
     * Property to indicate the jar(s) to use when creating the class loader to load an implementation
     * of the {@link org.rioproject.resolver.Resolver}
     */
    String RESOLVER_JAR = BASE_COMPONENT+".resolver.jar";
    /**
     * Property to indicate that Rio should limit the creation of utilities that
     * create sockets (like net.jini.export.Exporter) to limit socket ports
     * to within the specified port range. The port range is specified as &quot;-&quot; delimited
     * string, <tt>startRange-endRange</tt>, where <tt>startRange</tt> and <tt>endRange</tt>
     * are inclusive
     */
    String PORT_RANGE = BASE_COMPONENT+".portRange";
    /**
     * Default name of the OperationalString name for core-services
     */
    String CORE_OPSTRING="System-Core";
    /**
     * Property to use for the starter configuration for forking a service
     */
    String START_SERVICE_BEAN_EXEC_CONFIG = BASE_COMPONENT+".start-service-bean-exec";
}
