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
package org.rioproject.rmi;

import java.net.UnknownHostException;
import net.jini.jeri.*;
import net.jini.jeri.tcp.TcpEndpoint;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.Assert;
import org.junit.Test;
import org.rioproject.net.PortRangeServerSocketFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the PortRangeServerSocketFactory class
 */
public class ITPortRangeServerSocketFactoryTest {
    @Test
    public void verifyBadPortRangeThrowsIllegalArgumentException() {
        Throwable t = null;
        try {
            new PortRangeServerSocketFactory(-1, 0);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNotNull(t);
        Assert.assertTrue(t instanceof IllegalArgumentException);
        t = null;
        try {
            new PortRangeServerSocketFactory(-1, 0);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNotNull(t);
        Assert.assertTrue(t instanceof IllegalArgumentException);

        t = null;
        try {
            new PortRangeServerSocketFactory(-1, 0);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNotNull(t);
        Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    @Test
    public void verifyPortRangeEquality() {
        Throwable t = null;
        PortRangeServerSocketFactory pr1 = null;
        try {
            pr1 = new PortRangeServerSocketFactory(0, 100);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);

        PortRangeServerSocketFactory pr2 = null;
        t = null;
        try {
            pr2 = new PortRangeServerSocketFactory(0, 100);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertEquals(pr1, pr2);
        Assert.assertEquals(pr2, pr1);
        Assert.assertEquals(pr1, pr1);
        Assert.assertEquals(pr2, pr2);
    }

    @Test
    public void verifyPortRangeHashCode() {
        Throwable t = null;
        PortRangeServerSocketFactory pr1 = null;
        try {
            pr1 = new PortRangeServerSocketFactory(0, 100);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);

        PortRangeServerSocketFactory pr2 = null;
        t = null;
        try {
            pr2 = new PortRangeServerSocketFactory(0, 1000);
        } catch (Exception e) {
            t = e;
        }
        Assert.assertNull(t);
        Assert.assertTrue(pr1.hashCode() != pr2.hashCode());
    }

    @Test
    public void verifyServerSocketsWithinRange() {
        PortRangeServerSocketFactory range = new PortRangeServerSocketFactory(49152, 49155);
        List<ServerSocket> serverSockets = new ArrayList<>();
        Throwable t = null;
        while(t==null) {
            try {
                ServerSocket s = range.createServerSocket(0);
                serverSockets.add(s);
            } catch(Exception e) {
                t = e;
            }
        }
        Assert.assertNotNull(t);
        //Assert.assertTrue("Should have 4 ServerSockets, have "+serverSockets.size(), serverSockets.size()==4);
        for (ServerSocket s : serverSockets) {
            int port = s.getLocalPort();
            Assert.assertTrue("Port "+port+" should be >= "+range.getStart(), port >= range.getStart());
            Assert.assertTrue("Port "+port+" should be <= "+range.getEnd(), port <= range.getEnd());
        }
    }

    @Test
    public void createPortRangeWithStartRangeOnly() {
        PortRangeServerSocketFactory range = new PortRangeServerSocketFactory(65530);
        Assert.assertEquals(range.getEnd(), PortRangeServerSocketFactory.RANGE_END);
        List<ServerSocket> serverSockets = new ArrayList<>();
        Throwable t = null;
        while (t == null) {
            try {
                ServerSocket s = range.createServerSocket(0);
                serverSockets.add(s);
            } catch(Exception e) {
                t = e;
            }
        }
        Assert.assertNotNull(t);
        Assert.assertEquals("Should have 6 ServerSockets", 6, serverSockets.size());
        for (ServerSocket s : serverSockets) {
            int port = s.getLocalPort();
            Assert.assertTrue("Port "+port+" should be >= "+range.getStart(), port >= range.getStart());
            Assert.assertTrue("Port "+port+" should be <= "+range.getEnd(), port <= range.getEnd());
        }
    }

    @Test
    public void createBasicJeriExporter() throws UnknownHostException {
        Throwable t = null;
        PortRangeServerSocketFactory range = null;
        List<Endpoint> endPoints = new ArrayList<>();
        String host = getHostAddressFromProperty();
        for(int i=0; i<500; i++) {
            try {
                range = new PortRangeServerSocketFactory(10000, 10500);
                TcpServerEndpoint tcpEndPoint = TcpServerEndpoint.getInstance(host,
                                                                              0,
                                                                              null, //SocketFactory
                                                                              range);
                endPoints.add(new BasicJeriExporter(tcpEndPoint, new BasicILFactory())
                                      .getServerEndpoint().enumerateListenEndpoints(new EndpointContext()));

            } catch (Exception e) {
                t = e;
                e.printStackTrace();
                break;
            }
        }
        Assert.assertNull(t);
        Assert.assertNotNull(range);
        Assert.assertEquals(500, endPoints.size());
        for(Endpoint e : endPoints) {
            Assert.assertTrue(e instanceof TcpEndpoint);
            int port = ((TcpEndpoint)e).getPort();
            System.out.print(port+" ");
            Assert.assertTrue("Port "+port+" should be >= "+range.getStart(), port >= range.getStart());
        }
    }

	String getHostAddress() throws java.net.UnknownHostException {
	    return java.net.InetAddress.getLocalHost().getHostAddress();
	}

    /**
     * Return the local host address based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the default host address obtained from
     * {@link java.net.InetAddress#getLocalHost()}
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
	String getHostAddressFromProperty() throws java.net.UnknownHostException {
        String host = getHostAddress();
        String value = System.getProperty("java.rmi.server.hostname");
        if (value != null) {
            host = java.net.InetAddress.getByName(value).getHostAddress();
        }
        return host;
    }

    static class EndpointContext implements ServerEndpoint.ListenContext {
        List<ServerEndpoint.ListenEndpoint> endpoints = new ArrayList<>();

        @Override
        public ServerEndpoint.ListenCookie addListenEndpoint(ServerEndpoint.ListenEndpoint lep) throws IOException {
            endpoints.add(lep);
            return lep.listen(new Dispatcher()).getCookie();
        }
    }


    private static class Dispatcher implements RequestDispatcher {
        public void dispatch(InboundRequest request) {
            /*try {
           Thread.sleep(5000);
           } catch (InterruptedException e) {
           }*/
        }
    }
}
