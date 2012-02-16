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
package org.rioproject.boot;

import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Provides static convenience methods for use in configuration files. This class cannot
 * be instantiated.
 *
 * @author Dennis Reedy
 */
public class BootUtil {
    
    /** This class cannot be instantiated. */
    private BootUtil() {
        throw new AssertionError(
                "org.rioproject.boot.BootUtil cannot be instantiated");
    }
    
    /**
     * Get an anonymous port
     * 
     * @return An anonymous port created by instantiating a 
     * <code>java.net.ServerSocket</code> with a port of 0
     *
     * @throws IOException If an anonymous port cannot be obtained
     */
    public static int getAnonymousPort() throws java.io.IOException {
        java.net.ServerSocket socket = new java.net.ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return(port);
    }

    /**
     * Get a port from a range of ports
     *
     * @param portRange A range of ports. The port range is specified as &quot;-&quot; delimited
     * string, <tt>startRange-endRange</tt>, where <tt>startRange</tt> and <tt>endRange</tt>
     * are inclusive.
     *
     * @return An port created by instantiating a
     * <code>org.rioproject.net.PortRangeServerSocketFactory</code> with provided range
     *
     * @throws IOException If a port cannot be obtained
     * @throws IllegalArgumentException is either bound is not between
     * 0 and 65535, or if <code>end</code> is &lt; than <code>low</code>.
     */
    public static int getPortFromRange(String portRange) throws IOException {
        String[] range = portRange.split("-");
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);
        return getPortFromRange(start, end);
    }

    /**
     * Get a port from a range of ports
     *
     * @param start The range to start from (inclusive)
     * @param end The end of the range (inclusive)
     *
     * @return A port created by instantiating a
     * <code>org.rioproject.net.PortRangeServerSocketFactory</code> with provided range
     *
     * @throws IOException If a port cannot be obtained
     * @throws IllegalArgumentException is either bound is not between
     * 0 and 65535, or if <code>end</code> is &lt; than <code>low</code>.
     */
    public static int getPortFromRange(int start, int end) throws IOException {
        PortRangeServerSocketFactory factory = new PortRangeServerSocketFactory(start, end);
        ServerSocket ss = factory.createServerSocket(0);
        int p = factory.getLastPort();
        ss.close();
        return(p);
    }

}
