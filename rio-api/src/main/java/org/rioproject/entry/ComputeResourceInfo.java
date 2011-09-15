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
package org.rioproject.entry;

import net.jini.entry.AbstractEntry;
import net.jini.lookup.entry.ServiceControlled;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The ComputeResourceInfo class defines attributes that relate to system properties of
 * a compute resource (the appliance), namely :
 * <ul>
 * <li>The host name and address
 * <li>Java Virtual Machine vendor & version
 * <li>The operating system name & version
 * <li>The hardware architecture of the compute resource
 * </ul>
 *
 * @author Dennis Reedy
 */
public class ComputeResourceInfo extends AbstractEntry implements ServiceControlled {
    static final long serialVersionUID = 1L;
    public String osName;
    public String osVersion;
    public String arch;
    public String jvmVendor;
    public String jvmVersion;
    public String hostName;
    public String hostAddress;

    /**
     * This initializes all attributes, using 
     * <code>java.net.InetAddress.getLocalHost()</code> as the
     * address for the compute resource.
     *
     * @throws UnknownHostException If the local host cannot be obtained
     */
    public void initialize() throws UnknownHostException {
        initialize(InetAddress.getLocalHost());
    }
    
    /**
     * This initializes all attributes.
     * 
     * @param address The InetAddress of the compute resource
     */
    public void initialize(InetAddress address) {
        this.osName = System.getProperty("os.name");
        this.osVersion = System.getProperty("os.version");
        this.arch = System.getProperty("os.arch");
        this.jvmVendor = System.getProperty("java.vendor");
        this.jvmVersion = System.getProperty("java.version");        
        this.hostName = address.getHostName();
        this.hostAddress = address.getHostAddress();        
    }

}

