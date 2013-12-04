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
package org.rioproject.resolver.maven2;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.resolver.Artifact;

/**
 * Test creating an Artifact
 */
public class ArtifactCreationTest {
    @Test(expected = IllegalArgumentException.class)
    public void createBadArtifact() {
        new Artifact("http://foo:9000/foo.oar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBadArtifact2() {
        new Artifact("org/foo.foo:bar:2.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createBadArtifact3() throws Exception {
        new Artifact("C:\\dir\\file.jar;C:\\dir\\file2.jar");
    }

    @Test
    public void createGoodArtifact() {
        Artifact a = new Artifact("org.foo:bar:2.0");
        Assert.assertTrue(a.getGroupId().equals("org.foo"));
        Assert.assertTrue(a.getArtifactId().equals("bar"));
        Assert.assertTrue(a.getVersion().equals("2.0"));
    }

    @Test
    public void createGoodArtifactWithType() {
        Artifact a = new Artifact("org.foo:bar:oar:2.0");
        Assert.assertTrue(a.getGroupId().equals("org.foo"));
        Assert.assertTrue(a.getArtifactId().equals("bar"));
        Assert.assertTrue(a.getVersion().equals("2.0"));
        Assert.assertTrue(a.getType().equals("oar"));
    }

    @Test
    public void createGoodArtifactWithClassifier() {
        Artifact a = new Artifact("org.foo:bar:jar:dl:2.0");
        Assert.assertTrue(a.getGroupId().equals("org.foo"));
        Assert.assertTrue(a.getArtifactId().equals("bar"));
        Assert.assertTrue(a.getVersion().equals("2.0"));
        Assert.assertTrue(a.getType().equals("jar"));
        Assert.assertTrue(a.getClassifier().equals("dl"));
    }
}
