package org.rioproject.system.capability.connectivity;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.deploy.SystemComponent;

/**
 * Test matching
 *
 * @author Dennis Reedy
 */
public class TCPConnectivityTest {
    @Test
    public void testDoesNotSupportHostAddress() throws Exception {
        SystemComponent requirement = new SystemComponent(TCPConnectivity.ID);
        requirement.put(TCPConnectivity.HOST_ADDRESS, "127.0.0.1");
        TCPConnectivity tcpConnectivity = new TCPConnectivity();
        tcpConnectivity.define(TCPConnectivity.HOST_ADDRESS, "10.0.1.1");
        Assert.assertFalse(tcpConnectivity.supports(requirement));
        System.out.println(tcpConnectivity);
        System.out.println(requirement);
    }

    @Test
    public void testSupportsHostAddress() throws Exception {
        SystemComponent requirement = new SystemComponent(TCPConnectivity.ID);
        requirement.put(TCPConnectivity.HOST_ADDRESS, "127.0.0.1");
        TCPConnectivity tcpConnectivity = new TCPConnectivity();
        tcpConnectivity.define(TCPConnectivity.HOST_ADDRESS, "127.0.0.1");
        Assert.assertTrue(tcpConnectivity.supports(requirement));
    }

    @Test
    public void testSupportsHostName() throws Exception {
        SystemComponent requirement = new SystemComponent(TCPConnectivity.ID);
        requirement.put(TCPConnectivity.HOST_NAME, "mixed.case.name.net");
        TCPConnectivity tcpConnectivity = new TCPConnectivity();
        tcpConnectivity.define(TCPConnectivity.HOST_NAME, "MiXed.Case.Name.Net");
        Assert.assertTrue(tcpConnectivity.supports(requirement));
    }

    @Test
    public void testDoesNotSupportHostName() throws Exception {
        SystemComponent requirement = new SystemComponent(TCPConnectivity.ID);
        requirement.put(TCPConnectivity.HOST_NAME, "some.name.net");
        TCPConnectivity tcpConnectivity = new TCPConnectivity();
        tcpConnectivity.define(TCPConnectivity.HOST_NAME, "some.other.name.net");
        Assert.assertFalse(tcpConnectivity.supports(requirement));
    }
}
