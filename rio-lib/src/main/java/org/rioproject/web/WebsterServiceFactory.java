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
package org.rioproject.web;

import com.sun.jini.start.ServiceDescriptor;
import net.jini.config.EmptyConfiguration;
import org.rioproject.config.DynamicConfiguration;
import org.rioproject.start.descriptor.ConfigurationServiceDescriptor;
import org.rioproject.start.descriptor.RioServiceDescriptor;
import org.rioproject.util.RioHome;
import org.rioproject.start.util.ServiceDescriptorUtil;

import java.io.File;

import static org.rioproject.start.descriptor.RioServiceDescriptor.*;

/**
 * Creates either a Webster or a Jetty webster.
 */
public class WebsterServiceFactory {

    public static WebsterService createWebster(int port, String... roots) throws Exception {
        return createWebster(port, null, roots);
    }

    public static WebsterService createWebster(int port, String putDir, String[] roots) throws Exception {
        DynamicConfiguration config = new DynamicConfiguration();
        config.setEntry("org.rioproject.tools.webster", "port", int.class, port);
        config.setEntry("org.rioproject.tools.webster", "roots", String[].class, roots);
        if (putDir != null)
            config.setEntry("org.rioproject.tools.webster", "putDir", String.class, putDir);

        String rioHome = System.getProperty("rio.home", RioHome.get());
        if (rioHome == null)
            throw new RuntimeException("rio.home property not declared or derivable");

        String websterClass = "org.rioproject.tools.webster.Webster";
        String webster = rioHome
                + File.separator + "lib" + File.separator
                + ServiceDescriptorUtil.createVersionedJar("webster");
        ServiceDescriptor serviceDescriptor = new ConfigurationServiceDescriptor(webster, websterClass, config);
        RioServiceDescriptor.Created created = (Created) serviceDescriptor.create(EmptyConfiguration.INSTANCE);
        return (WebsterService) created.impl;
    }

    public static WebsterService createJetty(int port, String... roots) throws Exception {
        return createJetty(port, null, false, roots);
    }

    public static WebsterService createJetty(int port, String putDir, String[] roots) throws Exception {
        return createJetty(port, putDir, false, roots);
    }

    public static WebsterService createJetty(int port, String putDir, boolean secure, String... roots) throws Exception {
        ServiceDescriptor serviceDescriptor =
                ServiceDescriptorUtil.getJetty(Integer.toString(port), roots, putDir, secure);
        RioServiceDescriptor.Created created = (Created) serviceDescriptor.create(EmptyConfiguration.INSTANCE);
        return (WebsterService) created.impl;
    }


}
