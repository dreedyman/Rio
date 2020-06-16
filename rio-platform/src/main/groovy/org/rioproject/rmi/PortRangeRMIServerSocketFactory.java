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
package org.rioproject.rmi;

import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

/**
 * A {@link javax.net.ServerSocketFactory} that allocates {@link java.net.ServerSocket}s within a port range.
 *
 * <p>NOTE: The IANA recommends the range 49152-65535, as indicated by the following document:
 * http://www.iana.org/assignments/port-numbers.
 *
 * @author Dennis Reedy
 */
public class PortRangeRMIServerSocketFactory implements RMIServerSocketFactory {
    private final PortRangeServerSocketFactory serverSocketFactory;
    /**
     * Creates a {@link java.rmi.server.RMIServerSocketFactory} that allocates
     * {@link java.net.ServerSocket}s within the given bounds (both inclusive).
     *
     * @param start The range to start from (inclusive)
     * @param end The end of the range (inclusive)
     *
     *
     * @throws IllegalArgumentException is either bound is not between
     * 0 and 65535, or if <code>end</code> is &lt; than <code>low</code>.
     */
    public PortRangeRMIServerSocketFactory(int start, int end) {
        serverSocketFactory = new PortRangeServerSocketFactory(start, end);
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return serverSocketFactory.createServerSocket(port);
    }

    public int getLastPort() {
        return serverSocketFactory.getLastPort();
    }
}
