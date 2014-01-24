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
package org.rioproject.test.config;

import junit.framework.Assert;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import net.jini.id.UuidFactory;
import org.junit.Test;
import org.rioproject.impl.servicebean.DefaultServiceBeanManager;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.impl.container.ServiceContextFactory;
import org.rioproject.impl.opstring.GroovyDSLOpStringParser;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.opstring.OpStringParser;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.tools.webster.Webster;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 *
 * Test loading a configuration as a HTTP resource.
 *
 * @author Dennis Reedy
 */
public class LoadConfigurationAsHttpResourceTest {

    @Test
    public void testLoadingConfigurationAsHttpResource() throws IOException, ConfigurationException {
        Webster webster = new Webster(0, System.getProperty("user.dir")+"/src/test/resources");
        System.setProperty("TEST_PORT", Integer.toString(webster.getPort()));
        OpStringParser opStringParser = new GroovyDSLOpStringParser();
        File file = new File("src/test/resources/opstring/simple_opstring_with_http_config.groovy");
        List<OpString> opStrings = opStringParser.parse(file,   // opstring
                                                        null,     // parent classloader
                                                        null,     // defaultGroups
                                                        null);    // loadPath
        ServiceElement serviceElement = opStrings.get(0).getServices()[0];

        ServiceBeanContext context = new ServiceContextFactory().create(serviceElement,
                                                                    new DefaultServiceBeanManager(serviceElement,
                                                                                   InetAddress.getLocalHost().getHostName(),
                                                                                   InetAddress.getLocalHost().getHostAddress(),
                                                                                   UuidFactory.generate()),
                                                                    new ComputeResource(),
                                                                    EmptyConfiguration.INSTANCE);
        Configuration config = context.getConfiguration();
        String something = (String) config.getEntry("simple", "something", String.class);
        Assert.assertNotNull("Expected something not to be null", something);
        Assert.assertEquals("Expected something", "something", something);
    }

}
