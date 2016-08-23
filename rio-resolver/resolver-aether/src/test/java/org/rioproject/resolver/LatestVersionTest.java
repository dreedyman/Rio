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
package org.rioproject.resolver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.aether.AetherResolver;
import org.rioproject.resolver.maven2.Repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Dennis Reedy
 */
public class LatestVersionTest {
    private File saveOriginalSettings;

    @Before
    public void saveOriginalSettings() throws IOException {
        saveOriginalSettings = Utils.saveM2Settings();
    }

    @After
    public void restoreOriginalSettings() throws IOException {
        if(saveOriginalSettings!=null) {
            FileUtils.copy(saveOriginalSettings, Utils.getM2Settings());
        } else {
            FileUtils.remove(Utils.getM2Settings());
        }
    }

    @Test
    public void testLatestLocation() throws ResolverException, URISyntaxException {
        File testRepo;
        Throwable thrown = null;
        try {
            Utils.writeLocalM2RepoSettings();
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            Resolver r = new AetherResolver();
            URL loc = r.getLocation("org.codehaus.groovy:groovy-all:LATEST", null);
            Assert.assertNotNull(loc);
            Assert.assertTrue(new File(loc.toURI()).exists());
        } finally {
            Assert.assertNull(thrown);
        }
    }

    @Test
    public void testLatestClasspath() throws ResolverException {
        File testRepo;
        Throwable thrown = null;
        try {
            Utils.writeLocalM2RepoSettings();
            testRepo = Repository.getLocalRepository();
            if(testRepo.exists())
                FileUtils.remove(testRepo);
            Resolver r = new AetherResolver();
            String[] cp = r.getClassPathFor("net.jini:jsk-lib:LATEST");
            Assert.assertNotNull(cp);
            Assert.assertTrue(cp.length>0);
            //Assert.assertTrue(groovyJar.exists());
        } finally {
            Assert.assertNull(thrown);
        }
    }
}
