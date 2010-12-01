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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Undeploys a Rio OpString
 *
 * @goal undeploy
 *
 * @description Undeploys a Rio OpString
 *
 * @requiresProject true
 */
public class UndeployMojo extends AbstractRioMojo {
    /**
     * OpString to undeploy.
     * @parameter
     * @required
     */
    private String opstring;

    /**
     * Optional group name to set. 
     * @parameter
     * expression="${rio.groups}"
     */
    private String group;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!project.isExecutionRoot()) {
            getLog().debug("Project not the execution root, do not execute");
            return;
        }
        File f = new File(opstring);
        if(!f.exists()) {
            getLog().debug("The opstring ["+opstring+"] does not exist");
            return;
        }
        String groups = "";
        if(group!=null)
            groups="groups="+group;

        ExecHelper.doExec(getRioCommand()+" undeploy "+opstring+" "+groups);
    }
}
