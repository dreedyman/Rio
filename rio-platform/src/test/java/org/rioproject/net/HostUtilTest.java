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
package org.rioproject.net;

import junit.framework.Assert;
import org.junit.Test;

import java.net.InetAddress;

/**
 * @author Dennis Reedy
 */
public class HostUtilTest {
    @Test
    public void testGetInetAddress() throws Exception {
        InetAddress inetAddress = HostUtil.getInetAddress();
        Assert.assertNotNull(inetAddress);
    }

    @Test
    public void testGetHostAddress() throws Exception {
        String address = HostUtil.getHostAddress(null);
        Assert.assertNotNull(address);
    }

    @Test
    public void testGetHostAddressFromProperty() throws Exception {
        String address = HostUtil.getHostAddressFromProperty("foo");
        Assert.assertNotNull(address);
    }

    @Test
    public void testGetInetAddressFromProperty() throws Exception {
        InetAddress inetAddress = HostUtil.getInetAddressFromProperty("foo");
        Assert.assertNotNull(inetAddress);
    }
}
