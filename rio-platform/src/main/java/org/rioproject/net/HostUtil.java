/*
 * Copyright to the original author or authors
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
package org.rioproject.net;

/**
 * Utility for getting host name and address.
 */
public class HostUtil {

    private HostUtil() {
    }

    /**
     * Return the local host address using
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>
     *
     * @return The local host address
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getHostAddress() throws java.net.UnknownHostException {
        return java.net.InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * Return the local host address for a passed in host using
     * {@link java.net.InetAddress#getByName(String)}
     *
     * @param name The name of the host to return
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddress(String name) throws java.net.UnknownHostException {
        return java.net.InetAddress.getByName(name).getHostAddress();
    }

    /**
     * Return the local host address based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the default host address obtained from
     * {@link java.net.InetAddress#getLocalHost()}
     *
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddressFromProperty(String property) throws java.net.UnknownHostException {
        String host = getHostAddress();
        String value = System.getProperty(property);
        if(value != null) {
            host = java.net.InetAddress.getByName(value).getHostAddress();
        }
        return(host);
    }

    /**
     * Return the local host name
     * <code>java.net.InetAddress.getLocalHost().getHostName()</code>
     *
     * @return The local host name
     * @throws java.net.UnknownHostException if no hostname for the local
     * host could be found.
     */
    public static String getHostName() throws java.net.UnknownHostException {
        return java.net.InetAddress.getLocalHost().getHostName();
    }
}
