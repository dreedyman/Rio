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
package org.rioproject.config;

/**
 * System constants
 *
 * @author Dennis Reedy
 */
public interface Constants {
    static final String BASE_COMPONENT = "org.rioproject";
    /**
     * System property set indicating the address and port of the Webster
     * instance created by this utility, in the form of :
     * <pre>http://address:port</pre>
     */
    static final String CODESERVER = BASE_COMPONENT+".codeserver";
    /**
     * System property set indicating the addresses of repositories the
     * Resolver will use
     */
    static final String REPOSITORIES = BASE_COMPONENT+".resolver.repositories";
    /**
     * System property set when an RMI Registry is started
     */
    static final String REGISTRY_PORT = BASE_COMPONENT+".registryPort";
    /**
     * System property set to indicate to use a specific MBeanServer. If not
     * set then the Platform MBeanServer is to be used
     */
    static final String JMX_MBEANSERVER = BASE_COMPONENT+".jmxMBeanServer";
    /**
     * System property set when the JMX Platform MBeanServer is set to the
     * RMI Registry
     */
    static final String JMX_SERVICE_URL = BASE_COMPONENT+".jmxServiceURL";
    /**
     * System property set when to indicate a comma separated list of group
     * names to use
     */
    static final String GROUPS_PROPERTY_NAME = BASE_COMPONENT+".groups";
    /**
     * System property set when to indicate a comma separated list of lookup
     * locators
     */
    static final String LOCATOR_PROPERTY_NAME = BASE_COMPONENT+".locators";
    /**
     * System property to set and check to determine address to bind to or
     * address bound to
     */
    static final String RMI_HOST_ADDRESS = "java.rmi.server.hostname";
    /**
     * System property name for ServiceBeanExecutor name binding
     */
    static final String SERVICE_BEAN_EXEC_NAME = BASE_COMPONENT+".serviceBeanExec";
    /**
     * Property to indicate that a service bean is being started
     */
    static final String STARTING = BASE_COMPONENT+".starting-bean";
    /**
     * Property to indicate the directory the test framework has been started
     * in.
     */
    static final String RIO_TEST_EXEC_DIR = BASE_COMPONENT+".test.exec.dir";
}
