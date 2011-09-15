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

import net.jini.jeri.*;
import net.jini.jeri.tcp.TcpEndpoint;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertTrue(pr1.equals(pr2));
        Assert.assertTrue(pr2.equals(pr1));
        Assert.assertTrue(pr1.equals(pr1));
        Assert.assertTrue(pr2.equals(pr2));
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
        List<ServerSocket> serverSockets = new ArrayList<ServerSocket>();
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
        Assert.assertTrue("Should have 4 ServerSockets", serverSockets.size()==4);
        for (ServerSocket s : serverSockets) {
            int port = s.getLocalPort();
            Assert.assertTrue("Port "+port+" should be >= "+range.getStart(), port >= range.getStart());
            Assert.assertTrue("Port "+port+" should be <= "+range.getEnd(), port <= range.getEnd());
        }
    }

    @Test
    public void createPortRangeWithStartRangeOnly() {
        PortRangeServerSocketFactory range = new PortRangeServerSocketFactory(65530);
        Assert.assertTrue(range.getEnd()==PortRangeServerSocketFactory.RANGE_END);
        List<ServerSocket> serverSockets = new ArrayList<ServerSocket>();
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
        Assert.assertTrue("Should have 6 ServerSockets", serverSockets.size()==6);
        for (ServerSocket s : serverSockets) {
            int port = s.getLocalPort();
            Assert.assertTrue("Port "+port+" should be >= "+range.getStart(), port >= range.getStart());
            Assert.assertTrue("Port "+port+" should be <= "+range.getEnd(), port <= range.getEnd());
        }
    }

    @Test
    public void createBasicJeriExporter() {
        Throwable t = null;
        PortRangeServerSocketFactory range = null;
        List<Endpoint> endPoints = new ArrayList<Endpoint>();
        for(int i=0; i<500; i++) {
            try {
                String host = getHostAddressFromProperty("java.rmi.server.hostname");
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
        Assert.assertTrue(endPoints.size()==500);
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
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
	String getHostAddressFromProperty(String property) throws java.net.UnknownHostException {
        String host = getHostAddress();
        String value = System.getProperty(property);
        if(value != null) {
            host = java.net.InetAddress.getByName(value).getHostAddress();
        }
        return(host);
    }

    class EndpointContext implements ServerEndpoint.ListenContext {
        List<ServerEndpoint.ListenEndpoint> endpoints = new ArrayList<ServerEndpoint.ListenEndpoint>();

        @Override
        public ServerEndpoint.ListenCookie addListenEndpoint(ServerEndpoint.ListenEndpoint lep) throws IOException {
            endpoints.add(lep);
            return lep.listen(new Dispatcher()).getCookie();
        }
    }


    private class Dispatcher implements RequestDispatcher {
        public void dispatch(InboundRequest request) {
            /*try {
           Thread.sleep(5000);
           } catch (InterruptedException e) {
           }*/
        }
    }
}
