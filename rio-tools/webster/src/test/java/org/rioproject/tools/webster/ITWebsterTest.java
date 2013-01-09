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
package org.rioproject.tools.webster;

import com.sun.jini.start.NonActivatableServiceDescriptor;
import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.EmptyConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.rioproject.util.ServiceDescriptorUtil;

import java.io.*;

public class ITWebsterTest {

    @Test
    public void createWebsterFromServiceDescriptor() {
        Throwable t = null;
        ServiceDescriptor desc = null;
        Assert.assertNotNull(System.getProperty("java.security.policy"));
        String webster = System.getProperty("WEBSTER_JAR");
        Assert.assertNotNull(webster);
        try {
            desc = ServiceDescriptorUtil.getWebster(System.getProperty("java.security.policy"),
                                                    "0",
                                                    new String[]{System.getProperty("user.dir")},
                                                    false,
                                                    webster);
        } catch (IOException e) {
            t = e;
            e.printStackTrace();
        }
        Assert.assertNull(t);
        Assert.assertNotNull(desc);
        Webster w = getWebster(desc);
        Assert.assertNotNull(w);
    }

    @Test
    public void createWebsterFromServiceDescriptorWithPortRange() {
        Throwable t = null;
        ServiceDescriptor desc = null;
        Assert.assertNotNull(System.getProperty("java.security.policy"));
        String webster = System.getProperty("WEBSTER_JAR");
        Assert.assertNotNull(webster);
        try {
            desc = ServiceDescriptorUtil.getWebster(System.getProperty("java.security.policy"),
                                                    "10000-10005",
                                                    new String[]{System.getProperty("user.dir")},
                                                    false,
                                                    webster);
        } catch (IOException e) {
            t = e;
            e.printStackTrace();
        }
        Assert.assertNull(t);
        Assert.assertNotNull(desc);
        Webster w = getWebster(desc);
        Assert.assertNotNull(w);
        int port = w.getPort();
        Assert.assertTrue("Port " + port + " should be >= 10000", port >= 10000);
        Assert.assertTrue("Port " + port + " should be <= 10005", port <= 10005);
    }

    private Webster getWebster(ServiceDescriptor desc) {
        Webster w = null;
        try {
            NonActivatableServiceDescriptor.Created created =
                (NonActivatableServiceDescriptor.Created) desc.create(EmptyConfiguration.INSTANCE);
            w = (Webster) created.impl;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return w;
    }

}
