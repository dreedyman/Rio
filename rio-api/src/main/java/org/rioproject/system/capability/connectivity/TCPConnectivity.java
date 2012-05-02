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
package org.rioproject.system.capability.connectivity;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * The <code>TCPConnectivity</code> object provides definition for TCP/IP
 * networks
 *
 * @author Dennis Reedy
 */
public class TCPConnectivity extends ConnectivityCapability {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "TCP/IP Connectivity";
    /** Number of NetworkInterfaces */
    public static final String IPv6_NIC = "IPv6 NICs";
    /** Number of NetworkInterfaces */
    public static final String IPv4_NIC = "IPv4 NICs";

    /** 
     * Create a TCPConnectivity 
     */
    public TCPConnectivity() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a TCPConnectivity with a description
     *
     * @param description A description
     */
    public TCPConnectivity(String description) {
        super(description);
        int ipv4Nics = 0;
        int ipv6Nics = 0;
        StringBuilder ipv4buff = new StringBuilder();
        StringBuilder ipv6buff = new StringBuilder();
        try {
            for(Enumeration en = NetworkInterface.getNetworkInterfaces();
                en.hasMoreElements();) {
                NetworkInterface nic = (NetworkInterface)en.nextElement();
                for(Enumeration en1=nic.getInetAddresses(); en1.hasMoreElements();) {
                    InetAddress addr = (InetAddress)en1.nextElement();                    
                    if(addr instanceof Inet6Address) {
                        if(ipv6Nics>0)
                            ipv6buff.append(", ");
                        ipv6buff.append(nic.getName());
                        ipv6Nics++;
                    } else {
                        if(ipv4Nics>0)
                            ipv4buff.append(", ");
                        ipv4buff.append(nic.getName());
                        ipv4Nics++;
                    }
                }
            }
            define(IPv4_NIC, ipv4buff.toString());
            if(ipv6Nics>0)
                define(IPv6_NIC, ipv6buff.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Getting NetworkInterfaces", e);
        }
    }
}
