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
package org.rioproject.test.simple;

import org.rioproject.bean.PreDestroy;
import org.rioproject.bean.Started;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.exec.ExecDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.StringTokenizer;

public class ForkImpl implements Fork {
    ServiceBeanContext context;
    static Logger logger = LoggerFactory.getLogger(Fork.class.getName());

    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
    }
    
    public boolean verify() {
        if(context==null) {
            logger.error("Cannot verify with a null ServiceBeanContext");
            return false;
        }
        ExecDescriptor exDesc = context.getServiceElement().getExecDescriptor();
        if(exDesc==null) {
            logger.error("Cannot verify with a null ExecDescriptor");
            return false;
        }        
        String[] declaredArgs = toArray(exDesc.getInputArgs());
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtime.getInputArguments();
        logger.info("Runtime JVM Args [" + flatten(jvmArgs) + "]");
        for (String arg : declaredArgs) {
            boolean matched = false;
            for (String jvmArg : jvmArgs) {
                if (arg.equals(jvmArg)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                logger.error("Expected to match ["+arg+"]");
                return false;
            }
        }
        return true;
    }

    @Started
    public void createMarker() throws IOException {
        File marker = getMarkerFile();
        if(!marker.exists() && marker.createNewFile())
            logger.info("Created marker file: "+marker.getAbsolutePath());
    }

    @PreDestroy
    public void shutdown() {
        logger.info("================\nShutting down "+context.getServiceElement().getName()+"\n================");
        File marker = getMarkerFile();
        if(marker.exists() && marker.delete()) {
            String s = marker.getAbsolutePath();
            logger.info("Deleted marker file: "+s);
        }
    }

    public static File getMarkerFile() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        return new File(tmpDir, "testMarker");
    }

    private String[] toArray(String s) {
        StringTokenizer tok = new StringTokenizer(s);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }

    private String flatten(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s).append(" ");
        }
        return sb.toString();
    }
}
