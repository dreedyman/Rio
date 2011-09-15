/*
 * Copyright 2011 to the original author or authors.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.resolver.aether.AetherResolver;
import org.sonatype.aether.installation.InstallationException;

import java.io.IOException;

/**
 * Test including test dependencies
 */
public class ResolverTestDependenciesTest {

    @Before
    public void cleanAndInstall() throws InstallationException, IOException {
        Util.cleanProjectFromRepository();
        Util.verifyAndInstall();
    }

    @Test
    public void resolveWithoutTestDependenciesIncluded() throws ResolverException {
        AetherResolver r = new AetherResolver();
        String[] cp = r.getClassPathFor("org.rioproject.resolver.test.project:project-service:2.0");
        Assert.assertTrue("We should have 2 items in the classpath, we got: " + cp.length, cp.length == 2);
    }

    @Test
    public void resolveWithTestDependenciesIncluded() throws ResolverException {
        ProjectModuleResolver r = new ProjectModuleResolver();
        String[] cp = r.getClassPathFor("org.rioproject.resolver.test.project:project-service:2.0");
        Assert.assertTrue("We should have 3 items in the classpath, we got: " + cp.length, cp.length == 3);
    }
}
