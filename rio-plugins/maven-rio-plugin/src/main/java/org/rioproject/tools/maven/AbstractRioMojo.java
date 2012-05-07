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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Map;

public abstract class AbstractRioMojo extends AbstractMojo {

    /**
     * The environment configuration Map.
     *
     * @parameter
     */
    private Map<String, String> environment;

    /**
     * The timeout for deployment/undeployment.
     *
     * @parameter
     */
    private long timeout = 30000l;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    protected String getEnvironmentValue(String name) {
        String home = null;
        // first try configuration
        if (environment != null) {
            home = environment.get(name);
        }
        // fall back to existing system environment
        if (home == null) {
            home = System.getenv().get(name);
        }
        return home;        
    }
    
    protected String getRioHome() throws MojoExecutionException {
        String home = getEnvironmentValue("rio.home");
        if (home == null) {
            home = getEnvironmentValue("RIO_HOME");
        }
        if(home==null)
            throw new MojoExecutionException("RIO_HOME is not declared");
        if(home.length()==0)
            throw new MojoExecutionException("RIO_HOME is not declared correctly, empty value");
        if (!home.endsWith(File.separator))
            home = home + File.separator;
        return home;
    }

    protected String getRioCommand() throws MojoExecutionException {
        String rioHome = getRioHome();
        getLog().debug("Rio location: ${rioHome}");
        String ext = "";
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
            ext = ".cmd";
        return rioHome+"bin"+File.separator+"rio"+ext;
    }

}
