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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.resolver.ResolverException;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.SetTestManager;
import org.rioproject.test.TestManager;

/**
 * Tests that you can configure an artifact as the opstring in a test configuration
 */
@RunWith(RioTestRunner.class)
public class LoadArtifactAsOpStringTest {
    @SetTestManager
    static TestManager testManager;

    @Test(expected=ResolverException.class)
    public void verifyConfigurationCanBeLoaded() {
        Assert.assertNotNull("TestManager should not be null", testManager);
        Assert.assertEquals("org.rioproject.test:deploy-oar-test:1.0", testManager.getOpStringToDeploy());
        testManager.deploy();
    }
}
