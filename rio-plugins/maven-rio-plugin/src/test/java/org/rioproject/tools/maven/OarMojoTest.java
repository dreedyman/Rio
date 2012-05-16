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
package org.rioproject.tools.maven;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

public class OarMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    //@Test
    public void testMojoLookup() throws Exception {
        OarMojo mojo = getOarMojo("test-project");
        assertNotNull(mojo);
    }

     //@Test
    public void testRepositorySetting() throws Exception {
        OarMojo mojo = getOarMojo("test-project");
        assertNotNull(mojo);
        assertNotNull(mojo.getOpstring());
        MavenProject project = (MavenProject)getVariableValueFromObject( mojo, "project" );
        assertNotNull(project);
        List repositories = project.getRemoteArtifactRepositories();
        assertTrue(repositories.size()==4);
    }

    //@Test
    public void testDependencyParsingForIssue302() throws Exception {
        OarMojo mojo = getOarMojo("test-project");
        assertNotNull(mojo);
        assertNotNull(mojo.getOpstring());
        MavenProject project = (MavenProject)getVariableValueFromObject( mojo, "project" );
        assertNotNull(project);
        injectDependencies(mojo, project);
        Collection deps = mojo.getDependencies();
        assertNotNull(deps);
        Path classpath = new Path(new Project());
        mojo.parseDependenciesAndBuildClasspath(classpath);
        assertTrue(classpath.size()==1);
    }

    public void testEncodeRepository() throws Exception {
        OarMojo mojo = getOarMojo("test-project-1");
        assertNotNull(mojo);
        assertFalse(mojo.getEncodeRepositories());
        mojo.execute();
    }

    private OarMojo getOarMojo(String project) throws Exception {
        File xml = new File(getBasedir(), "src/test/resources/"+project+"/pom.xml");
        return (OarMojo) lookupMojo("oar", xml);
    }

    private void injectDependencies(OarMojo mojo, MavenProject project) throws IllegalAccessException {
        Class mojoClass = mojo.getClass();
        for(Field field : mojoClass.getDeclaredFields()) {
            if(field.getName().equals("dependencies")) {
                field.setAccessible(true);
                field.set(mojo, project.getDependencies());
            }
        }

    }
}
