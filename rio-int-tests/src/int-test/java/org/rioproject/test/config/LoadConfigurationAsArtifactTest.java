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

import org.junit.Assert;
import net.jini.config.Configuration;
import org.junit.Test;
import org.rioproject.config.GroovyConfig;
import org.rioproject.impl.config.ConfigHelper;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.maven2.Repository;

import java.io.File;
import java.io.IOException;

/**
 * @author Dennis Reedy
 */
public class LoadConfigurationAsArtifactTest {
    @Test
    public void testUsingArtifact() throws IOException {
        File repo = Repository.getLocalRepository();
        try {
            File artifactDir = new File(repo, "rio-test/something/something/darkside/1.0");
            if (!artifactDir.exists()) {
                artifactDir.mkdirs();
            }
            FileUtils.copy(new File("src/int-test/resources/darkside-1.0.config"),
                           new File(artifactDir, "darkside-1.0.config"));
            ServiceElement service = new ServiceElement();
            service.getServiceBeanConfig().setConfigArgs("rio-test.something.something:darkside:config:1.0");
            String[] args = ConfigHelper.getConfigArgs(service);
            Assert.assertNotNull(args);
            Assert.assertEquals(1, args.length);
            Configuration configuration = new GroovyConfig(args, Thread.currentThread().getContextClassLoader());
            Assert.assertNotNull(configuration);
        } finally {
            FileUtils.remove(new File(repo, "rio-test"));
        }
    }
}
