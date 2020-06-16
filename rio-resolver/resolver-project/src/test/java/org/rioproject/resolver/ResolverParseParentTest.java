/*
 * Copyright 2010 to the original author or authors.
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

import org.apache.maven.settings.building.SettingsBuildingException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.eclipse.aether.installation.InstallationException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test parsing pom with project.parent.version set
 */
public class ResolverParseParentTest {
    @Test
    public void testVersionFromProjectParentVersion() throws IOException, ResolverException, InstallationException, SettingsBuildingException {
        ProjectModuleResolver r = Util.verifyAndInstall();
        String[] cp = r.getClassPathFor("org.rioproject.resolver.test.project:project-service:2.0");
        assertEquals("We should have 2 items in the classpath, we got: " + cp.length, 2, cp.length);
    }

    @After
    public void clean() {
        Util.cleanProjectFromRepository();
    }
}
