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
package org.rioproject.exec;

import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * @author Dennis Reedy
 */
public abstract class AbstractShell implements Shell {
    private final String defaultTemplate;
    private String template;
    static final Logger logger = LoggerFactory.getLogger(AbstractShell.class);

    public AbstractShell(final String defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public void setShellTemplate(final String template) {
        if(template==null)
            throw new IllegalArgumentException("template cannot be null");
        this.template = template;
        logger.info("Set Shell template to: {}", template);
    }

    protected String buildCommandLine(final ExecDescriptor execDescriptor) {
        String commandLine = execDescriptor.getCommandLine();
        if (commandLine == null) {
            throw new IllegalArgumentException("commandLine cannot be null");
        }
        StringBuilder commandLineBuilder = new StringBuilder();
        commandLineBuilder.append(commandLine);

        if (execDescriptor.getInputArgs() != null) {
            commandLineBuilder.append(" ").append(PropertyHelper.expandProperties(execDescriptor.getInputArgs()));
        }
        commandLineBuilder.append(" ").append(getRedirection(execDescriptor));

        return commandLineBuilder.toString();
    }

    protected abstract String getRedirection(ExecDescriptor execDescriptor);

    protected ProcessBuilder createProcessBuilder(final String workingDirectory,
                                                  final ExecDescriptor execDescriptor,
                                                  final String... commandToExec) {
        ProcessBuilder pb = new ProcessBuilder(commandToExec);

        Map<String, String> declaredEnv = execDescriptor.getEnvironment();
        Map<String, String> environment = pb.environment();
        for(Map.Entry<String, String> entry : environment.entrySet()) {
            String value = declaredEnv.get(entry.getKey());
            if(value!=null) {
                String oldValue = entry.getValue();
                String setValue = oldValue+File.pathSeparator+value;
                environment.put(entry.getKey(), setValue);
                logger.info("{} was [{}], now [{}]", entry.getKey(), oldValue, environment.get(entry.getKey()));
            }
        }
        environment.putAll(execDescriptor.getEnvironment());
        logger.trace("Process Builder's environment={}", environment);

        if(workingDirectory!=null) {
            pb = pb.directory(new File(workingDirectory));
            logger.debug("Process Builder's working directory set to [{}]", pb.directory().getPath());
        }

        logger.debug("ProcessBuilder command: {}", pb.command());
        logger.debug("ProcessBuilder environment: {}", pb.environment());

        pb.redirectErrorStream(true);
        return pb;
    }

    protected URL getTemplateURL() throws IOException {
        URL templateURL;
        String templateToUse = template==null?defaultTemplate:template;
        if(templateToUse.startsWith("classpath:")) {
            String s = templateToUse.substring("classpath:".length());
            templateURL = Util.getResource(s);
        } else {
            File templateFile = new File(templateToUse);
            templateURL = templateFile.toURI().toURL();
        }
        return templateURL;
    }

}
