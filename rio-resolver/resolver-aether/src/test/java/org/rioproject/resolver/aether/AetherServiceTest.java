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