/*
 * Copyright 2012 the original author or authors
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
package org.rioproject.url.artifact;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Test the {@code ArtifactURLConfiguration}
 *
 * @author Dennis Reedy
 */
public class ArtifactURLConfigurationTest {
    @Test
    public void testArtifactConfigurationSimple() {
        ArtifactURLConfiguration config = new ArtifactURLConfiguration("org.foo:bar:1.0");
        Assert.assertEquals(config.getArtifact(), "org.foo:bar:1.0");

    }

    @Test
    public void testArtifactConfigurationWithRepository() {
        ArtifactURLConfiguration config = new ArtifactURLConfiguration("org.foo:bar:1.0;http://blah:8080");
        Assert.assertEquals(1, config.getRepositories().length);
        Assert.assertEquals("http://blah:8080", config.getRepositories()[0].getUrl());
    }

    @Test
    public void testArtifactConfigurationWithRepositoryId() {
        ArtifactURLConfiguration config = new ArtifactURLConfiguration("org.foo:bar:1.0;http://blah:8080@baz");
        Assert.assertEquals(1, config.getRepositories().length);
        Assert.assertEquals("baz", config.getRepositories()[0].getId());
    }
}
