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
package org.rioproject.impl.config;

import org.junit.Assert;
import net.jini.config.Configuration;
import org.junit.Test;
import org.rioproject.config.GroovyConfig;
import org.rioproject.opstring.ServiceElement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * Test the {@link org.rioproject.impl.config.ConfigHelper} class.
 *
 * @author Dennis Reedy
 */
public class ConfigHelperTest {

    @Test(expected = IllegalArgumentException.class)
    public void testGetConfigArgsNoCLassLoader() throws IOException {
        ConfigHelper.getConfigArgs(new ServiceElement(), null);
    }

    @Test
    public void testUsingConfig() throws IOException {
        ServiceElement service = new ServiceElement();
        service.getServiceBeanConfig().setConfigArgs(getConfig());
        String[] args = ConfigHelper.getConfigArgs(service);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.length);
        File file = new File(args[0]);
        Assert.assertTrue(file.exists());
        Configuration configuration = new GroovyConfig(args, Thread.currentThread().getContextClassLoader());
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testUsingGroovyConfig() throws IOException {
        ServiceElement service = new ServiceElement();
        service.getServiceBeanConfig().setConfigArgs(getGroovyConfig());
        String[] args = ConfigHelper.getConfigArgs(service);
        Assert.assertNotNull(args);
        Assert.assertEquals(1, args.length);
        File file = new File(args[0]);
        Assert.assertFalse(file.exists());
        Configuration configuration = new GroovyConfig(args, Thread.currentThread().getContextClassLoader());
        Assert.assertNotNull(configuration);
    }

    @Test(expected = FileNotFoundException.class)
    public void testUsingMissingGroovyFile() throws IOException {
        ServiceElement service = new ServiceElement();
        service.getServiceBeanConfig().setConfigArgs("file:foo");
        String[] args = ConfigHelper.getConfigArgs(service);
        Assert.assertNotNull(args);
        Configuration configuration = new GroovyConfig(args, Thread.currentThread().getContextClassLoader());
        Assert.assertNotNull(configuration);
    }



    private String getConfig() {
        return
        "import net.jini.jrmp.JrmpExporter;\n" +
        "com.sun.jini.outrigger {\n" +
        "    serverExporter = new JrmpExporter();\n" +
        "    maxUnexportDelay = 0;\n" +
        "}";
    }

    private String getGroovyConfig() {
        return
            "@org.rioproject.config.Component('org.rioproject.eventcollector.service')\n" +
            "class PersistentEventManagerConfig {\n" +
            "    File getPersistentDirectoryRoot() {\n" +
            "        String userDir = System.getProperty(\"user.dir\")\n" +
            "        return new File(userDir, \"target/events\")\n" +
            "     }\n" +
            "}";
    }
}
