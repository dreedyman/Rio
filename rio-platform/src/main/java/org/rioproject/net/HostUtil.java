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

import java.net.*;
import java.util.Enumeration;

/**
 * Utility for getting host name and address.
 */
public final class HostUtil {

    private HostUtil() {
    }

    /**
     * Return the local host address
     *
     * @return The first non-loopback address. If there are no non-loopback addresses,
     * return the default host address obtained from {@link java.net.InetAddress#getLocalHost()}.
     *
     * @throws UnknownHostException if no IP address for the local host could be found.
     */
    public static InetAddress getInetAddress() throws UnknownHostException {
        InetAddress address = null;
        try {
            address = getFirstNonLoopbackAddress(false, false);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if(address==null) {
            address = InetAddress.getLocalHost();
        }
        return address;
    }

    /**
     * Return the local host address for a passed in host using
     * {@link java.net.InetAddress#getByName(String)}
     *
     * @param name The name of the host to return
     *
     * @return The local host address
     *
     * @throws UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddress(final String name) throws UnknownHostException {
        return InetAddress.getByName(name).getHostAddress();
    }

    /**
     * Return the local host address based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the first non-loopback address. If there are no non-loopback addresses,
     * return the default host address obtained from {@link java.net.InetAddress#getLocalHost()}.
     *
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws UnknownHostException if no IP address for the host name could be found.
     */
    public static String getHostAddressFromProperty(final String property) throws UnknownHostException {
        return getInetAddressFromProperty(property).getHostAddress();
    }

    /**
     * Return the {@code InetAddress} based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the first non-loopback address. If there are no non-loopback addresses,
     * return the default host address obtained from {@link java.net.InetAddress#getLocalHost()}.
     *
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws UnknownHostException if no IP address for the host name could be found.
     */
    public static InetAddress getInetAddressFromProperty(final String property) throws UnknownHostException {
        InetAddress inetAddress = getInetAddress();
        String value = System.getProperty(property);
        if(value != null) {
            inetAddress = InetAddress.getByName(value);
        }
        return inetAddress;
    }

    /**
     * Get the first non-loopback address.
     *
     * @param preferIpv4 If you want the Internet Protocol version 4 (IPv4) address, this value must be {@code true}.
     * @param preferIPv6 If you want the  Internet Protocol version 6 (IPv6) address, this value must be {@code true}.
     *                   If the {@code preferIpv4} is set to {@code true}, this value is ignored
     * @return The first non loopback address or {@code null} if there are no non-loopback addresses.
     * @throws SocketException If an I/O error occurs.
     */
    public static InetAddress getFirstNonLoopbackAddress(boolean preferIpv4, boolean preferIPv6) throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for (Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements();) {
                InetAddress addr = (InetAddress) en2.nextElement();
                if (!addr.isLoopbackAddress()) {
                    if (addr instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        if(preferIpv4)
                            return addr;
                    }
                    if (addr instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        if(preferIPv6)
                            return addr;
                    } else
                        return addr;
                }
            }
        }
        return null;
    }

}
