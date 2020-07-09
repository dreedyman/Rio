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
package org.rioproject.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility for getting host name and address.
 */
public final class HostUtil {
    private static final AtomicReference<InetAddress> firstNonIpV4LoopbackAddress = new AtomicReference<>();
    private static final AtomicReference<InetAddress> firstNonIpV6LoopbackAddress = new AtomicReference<>();
    private static final Logger logger = LoggerFactory.getLogger(HostUtil.class);

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
            if (logger.isDebugEnabled())
                logger.debug("Getting first non loopback address");
            address = getFirstNonLoopbackAddress(true, false);
            if(logger.isDebugEnabled())
                logger.debug("Address return as: {}", address);
        } catch (SocketException e) {
            logger.warn("Problem getting InetAddress", e);
        }
        if (address == null) {
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
        InetAddress inetAddress;
        String value = System.getProperty(property);
        if (value != null) {
            inetAddress = InetAddress.getByName(value);
        } else {
            inetAddress = getInetAddress();
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
        if (preferIpv4 && preferIPv6) {
            throw new IllegalArgumentException("Cannot have both ipv4 and ipv6");
        }
        if (!preferIpv4 && !preferIPv6) {
            throw new IllegalArgumentException("Must have either ipv4 or ipv6");
        }
        AtomicReference<InetAddress> ref = preferIpv4 ? firstNonIpV4LoopbackAddress : firstNonIpV6LoopbackAddress;
        if (ref.get() == null) {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface i = en.nextElement();
                if (logger.isDebugEnabled()) {
                    logger.debug("Checking NetworkInterface: {}",
                                 i);
                }
                for (Enumeration<InetAddress> en2 = i.getInetAddresses(); en2.hasMoreElements(); ) {
                    InetAddress addr = en2.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        if (addr instanceof Inet4Address && preferIpv4) {
                            ref.set(addr);
                            return ref.get();
                        }
                        if (addr instanceof Inet6Address && preferIPv6) {
                            ref.set(addr);
                            return ref.get();

                        }
                    }
                }
            }
        }
        return ref.get();
    }

}
