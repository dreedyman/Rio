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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Deploys a Rio OpString
 *
 * @goal deploy
 *
 * @description Deploys a Rio OpString
 *
 * @requiresProject true
 */
public class DeployMojo extends AbstractRioMojo {
    /**
     * OpString to deploy.
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
        StringBuilder repositoryBuilder = new StringBuilder();
        for(Object o : project.getRemoteArtifactRepositories()) {
            try {
                Method getId = o.getClass().getMethod("getId");
                String id = (String) getId.invoke(o);
                if(id.equals("central"))
                    continue;
                Method getUrl = o.getClass().getMethod("getUrl");
                String repositoryUrl = (String) getUrl.invoke(o);
                if(repositoryBuilder.length()>0)
                    repositoryBuilder.append(";");
                repositoryBuilder.append(repositoryUrl);
            } catch(NoSuchMethodException e) {
                throw new MojoExecutionException("Building Repository list", e);
            } catch (InvocationTargetException e) {
                throw new MojoExecutionException("Building Repository list", e);
            } catch (IllegalAccessException e) {
                throw new MojoExecutionException("Building Repository list", e);
            }
        }
        
        if(!project.isExecutionRoot()) {
            getLog().debug("Project not the execution root, do not execute");
            return;
        }
        File f = new File(opstring);
        if(!f.exists()) {
            getLog().info("The opstring ["+opstring+"] does not exist");
            return;
        }
        String groups = "";
        if(group!=null)
            groups="groups="+group;
        if(repositoryBuilder.length()>0) {
            repositoryBuilder.insert(0, " -r=");
        }

        ExecHelper.doExec(getRioCommand()+" deploy -uv -t=30000 "+groups+" "+opstring+repositoryBuilder.toString(),
                          true,
                          true);
    }
}
