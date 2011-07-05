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
import org.rioproject.resolver.maven2.Repository;
import org.sonatype.aether.installation.InstallationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utilities for working with test project
 */
public class Util {

    static void verifyAndInstall() throws InstallationException, IOException {
        File projectPom = new File("src/test/resources/project/pom.xml");
        File projectAPIPom = new File("src/test/resources/project/project-api/pom.xml");
        File projectAPITarget = new File("src/test/resources/project/project-api/target");
        if (!projectAPITarget.exists())
            if (projectAPITarget.mkdirs())
                System.out.println("Created " + projectAPITarget.getPath());
        File projectServicePom = new File("src/test/resources/project/project-service/pom.xml");
        File projectServiceTarget = new File("src/test/resources/project/project-service/target");
        if (!projectServiceTarget.exists())
            if (projectServiceTarget.mkdirs())
                System.out.println("Created " + projectServiceTarget.getPath());
        File apiArtifact = new File(projectAPITarget, "project-api-2.0.jar");
        if (!apiArtifact.exists())
            createPhonyArtifact(apiArtifact);

        File serviceArtifact = new File(projectServiceTarget, "project-service-2.0.jar");
        if (!serviceArtifact.exists())
            createPhonyArtifact(serviceArtifact);
        File srcPom = new File("src/test/resources/project/project-service/pom.xml");
        Assert.assertTrue(srcPom.exists());

        ProjectModuleResolver r = new ProjectModuleResolver();

        r.getAetherService().install("org.rioproject.resolver.test", "project", "2.0", projectPom, null);
        r.getAetherService().install("org.rioproject.resolver.test.project", "project-api", "2.0", projectAPIPom, apiArtifact);
        r.getAetherService().install("org.rioproject.resolver.test.project", "project-service", "2.0", projectServicePom, serviceArtifact);
    }

    public static void cleanProjectFromRepository() {
        File project = new File(Repository.getLocalRepository(), "org/rioproject/resolver/test");
        remove(project);
    }

    private static void createPhonyArtifact(File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        BufferedWriter out = new BufferedWriter(writer);
        out.write("phony");
        out.close();
    }

    static void remove(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory())
                    remove(f);
                else {
                    if (f.delete()) {
                        System.out.println("Removed " + f.getAbsolutePath());
                    } else {
                        if (f.exists())
                            System.out.println("Unable to remove " + f.getAbsolutePath());
                    }
                }
            }
            if (file.delete()) {
                System.out.println("Removed " + file.getAbsolutePath());
            } else {
                if (file.exists())
                    System.out.println("Unable to remove " + file.getAbsolutePath());
            }
        } else {
            if (file.delete()) {
                System.out.println("Removed " + file.getAbsolutePath());
            } else {
                if (file.exists())
                    System.out.println("Unable to remove " + file.getAbsolutePath());
            }
        }
    }
}
