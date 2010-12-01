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

/**
 * Starts a Provision Monitor
 *
 * @goal start-monitor
 *
 * @description Starts a Provision Monitor
 *
 */
public class MonitorMojo extends AbstractRioMojo {
    /**
     * Provide an override
     *
     * @parameter
     * default-value=""
     */
    private String overrides;

    public void execute() throws MojoExecutionException {
        if(!project.isExecutionRoot()) {
            getLog().debug("Project not the execution root, do not execute");
            return;
        }
        overrides = overrides==null?"":overrides;
        ExecHelper.doExec(getRioCommand()+" start monitor "+overrides, false);
    }
}
