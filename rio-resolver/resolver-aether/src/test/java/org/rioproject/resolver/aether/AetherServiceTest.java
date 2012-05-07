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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the AetherService
 */
public class AetherServiceTest {
    @Test
    public void testGetClasspath() throws Exception {
        ResolutionResult result = AetherService.getDefaultInstance().resolve("org.apache.maven",
                                                                             "maven-settings-builder",
                                                                             "3.0.3");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getArtifactResults().size()>0);
    }
}
