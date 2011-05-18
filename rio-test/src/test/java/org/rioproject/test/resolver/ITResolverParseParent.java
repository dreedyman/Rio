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
package org.rioproject.test.resolver;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.test.ProjectModuleResolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Test parsing pom with project.parent.version set
 */
public class ITResolverParseParent {
    @Test
    public void testVersionFromProjectParentVersion() throws IOException, ResolverException {
        File projectAPITarget = new File("src/test/resources/project/project-api/target");
        if(!projectAPITarget.exists())
            if(projectAPITarget.mkdirs())
                System.out.println("Created "+projectAPITarget.getPath());
        File projectServiceTarget = new File("src/test/resources/project/project-service/target");
        if(!projectServiceTarget.exists())
            if(projectServiceTarget.mkdirs())
                System.out.println("Created "+projectServiceTarget.getPath());
        File apiArtifact = new File(projectAPITarget, "project-api-2.0.jar");
        if(!apiArtifact.exists())
            createPhonyArtifact(apiArtifact);
        File serviceArtifact = new File(projectServiceTarget, "project-service-2.0.jar");
        if(!serviceArtifact.exists())
            createPhonyArtifact(serviceArtifact);
        File srcPom = new File("src/test/resources/project/project-service/pom.xml");
        Assert.assertTrue(srcPom.exists());
        Resolver r = new ProjectModuleResolver();
        String[] cp = r.getClassPathFor("org.rioproject.resolver.test.project:project-service:2.0");
        Assert.assertTrue("We should have 2 items in the classpath, we got: "+cp.length, cp.length==2);
    }

    private void createPhonyArtifact(File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        BufferedWriter out = new BufferedWriter(writer);
        out.write("phony");
        out.close();
    }
}
