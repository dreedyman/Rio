/*
 * Copyright 2011 the original author or authors
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

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

/**
 * A {@link javax.net.ServerSocketFactory} that allocates {@link java.net.ServerSocket}s within a port range.
 *
 * <p>NOTE: The IANA recommends the range 49152-65535, as indicated by the following document:
 * http://www.iana.org/assignments/port-numbers.
 *
 * @author Dennis Reedy
 */
public class PortRangeServerSocketFactory extends ServerSocketFactory {
    private final int start;
    private final int end;
    private int lastPort;
    private final static Random random = new Random();
    public static final int RANGE_END = 65535;

    /**
     * Creates a {@link javax.net.ServerSocketFactory} that allocates
     * {@link java.net.ServerSocket}s starting from the provided port and extending to 65535
     *
     * @param start The range to start from (inclusive)
     *
     *
     * @throws IllegalArgumentException is not between 0 and 65535.
     */
    public PortRangeServerSocketFactory(int start) {
        this(start, RANGE_END);
    }

    /**
     * Creates a {@link javax.net.ServerSocketFactory} that allocates
     * {@link java.net.ServerSocket}s within the given bounds (both inclusive).
     *
     * @param start The range to start from (inclusive)
     * @param end The end of the range (inclusive)
     *
     *
     * @throws IllegalArgumentException is not between 0 and 65535,
     * or if <code>end</code> is &lt; than <code>low</code>.
     */
    public PortRangeServerSocketFactory(int start, int end) {
        if (start < 0 || end > RANGE_END || start > end) {
            throw new IllegalArgumentException(
                    "illegal port range: [" + start + "," + end + "]");
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getLastPort() {
        return lastPort;
    }

    /**
     * If the port is 0, use the provided range. Otherwise if the port is not 0,
     * test to make sure the port is within range.
     *
     * @param port The port to create.
     *
     * @return A ServerSocket bound to an available port
     *
     * @throws IOException If the port cannot be created.
     * @throws IllegalArgumentException if a non-zero port is provided that is outside of the range
     */
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        if (port == 0) {
            return doGetServerSocket(-1, null);
        } else {
            checkRange(port);
            lastPort = port;
            return new ServerSocket(port);
        }
    }

    /**
     * If the port is 0, use the provided range. Otherwise if the port is not 0,
     * test to make sure the port is within range.
     *
     * @param port The port to create.
     * @param backlog The backlog, how many connections are queued
     *
     * @return A ServerSocket bound to an available port
     *
     * @throws IOException If the port cannot be created.
     * @throws IllegalArgumentException if a non-zero port is provided that is outside of the range
     */
    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        if (port == 0) {
            return doGetServerSocket(backlog, null);
        } else {
            checkRange(port);
            lastPort = port;
            return new ServerSocket(port, backlog);
        }
    }

    /**
     * If the port is 0, use the provided range. Otherwise if the port is not 0,
     * test to make sure the port is within range.
     *
     * @param port The port to create.
     * @param backlog The backlog, how many connections are queued
     * @param inetAddress The network interface address to use
     *
     * @return A ServerSocket bound to an available port
     *
     * @throws IOException If the port cannot be created.
     * @throws IllegalArgumentException if a non-zero port is provided that is outside of the range
     */
    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress inetAddress) throws IOException {
        if (port == 0) {
            return doGetServerSocket(backlog, inetAddress);
        } else {
            checkRange(port);
            lastPort = port;
            return new ServerSocket(port, backlog, inetAddress);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PortRangeServerSocketFactory portRange = (PortRangeServerSocketFactory) o;

        return end == portRange.end && start == portRange.start;

    }

    @Override
    public int hashCode() {
        int result = start;
        result = 31 * result + end;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PortRangeServerSocketFactory {").append(start).append(":").append(end).append("}");
        return sb.toString();
    }

    /**
     * Obtain a {@link java.net.ServerSocket} bound to a port within a specified range
     *
     * @param backlog The backlog to use, or -1 if no backlog is to be used
     * @param addr The InetAddress to bnd the socket to
     *
     * @return A ServerSocket bound to an available port with the PortRange
     *
     * @throws java.io.IOException If the port cannot be created.
     * @throws IllegalArgumentException if the rangeStart is &lt; or equal to 0,
     * or the rangeEnd is &gt; 65536, or if the rangeStart is &gt; the rangeEnd
     */
    private ServerSocket doGetServerSocket(int backlog, InetAddress addr) throws IOException {
        int start = random();
        int p = start;
        do {
            try {
                ServerSocket ss = null;
                if(addr==null) {
                    if(backlog<0) {
                        ss = new ServerSocket(p);
                        lastPort = p;
                    }
                } else  {
                    ss = new ServerSocket(p, backlog, addr);
                    lastPort = p;
                }
                return ss;
            } catch (BindException e) {
                /**/
            }
            p = next(p);
        } while (p != start);

        throw new BindException("No available port within provided range: "+toString());
    }

    /**
     * Check that the port is within the configured range
     * @param port Port to check
     *
     * @throws IllegalArgumentException is the port is not within the configured range
     */
    private void checkRange(int port) {
        if (port < getStart() || port > getEnd()) {
            throw new IllegalArgumentException(
                    "illegal port range: "+port+" not within range [" + getStart() + "," + getEnd() + "]");
        }
    }

    /**
     * Get a random port within the range.
     *
     * @return a random port within the range.
     */
    private int random() {
        return random.nextInt(end - start + 1) + start;
    }

    /**
     * Get the next port within the range.
     *
     * @param port The port to start from
     *
     * @return The next port within the range, wrapping
     * around to the lowest port if necessary.
     */
    private int next(int port) {
        return (port < end ? port + 1 : start);
    }
}
