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
package org.rioproject.resolver.aether;

import org.eclipse.aether.repository.RemoteRepository;
import org.junit.*;
import org.rioproject.resolver.FileUtils;
import org.rioproject.resolver.Utils;
import org.rioproject.resolver.maven2.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the AetherService
 */
@Ignore
public class AetherServiceTest {
    File saveOriginalSettings;

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
    public void testGetClasspath() throws Exception {
        List<RemoteRepository> repos = new ArrayList<RemoteRepository>();
        RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder("central",
                                                                            "default",
                                                                            "http://repo1.maven.org/maven2");
        repos.add(repoBuilder.build());

        ResolutionResult result = AetherService.getDefaultInstance()
                                      .setConfiguredRepositories(repos)
                                      .resolve("junit", "junit", "4.10");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getArtifactResults().size()>0);
    }

    @Test
    public void testMirrors() {
        File testRepo;
        Utils.writeLocalM2RepoSettingsWithMirror();
        testRepo = Repository.getLocalRepository();
        if(testRepo.exists())
            FileUtils.remove(testRepo);

        AetherService aetherService = AetherService.getDefaultInstance();
        List<RemoteRepository> list = aetherService.getRemoteRepositories();
        Assert.assertTrue("Expected at least 1, got "+list.size(), list.size()>0);
        RemoteRepository r = aetherService.getMirrorSelector(list).getMirror(list.get(0));
        Assert.assertTrue("Expected "+Utils.getMirroredURL()+" got "+r.getUrl(), r.getUrl().equals(Utils.getMirroredURL()));
    }
}
