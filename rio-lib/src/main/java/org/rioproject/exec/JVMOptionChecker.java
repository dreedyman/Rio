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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility to replace JVM input args.
 */
public class JVMOptionChecker {
    static final Logger logger = LoggerFactory.getLogger(JVMOptionChecker.class);

    public static String getJVMInputArgs(String userArgs) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        List<String> inputArgs = new ArrayList<String>();
        /* RuntimeMXBean.getInputArguments() will strip the "" from the command to run
         * if the hot spot option -XX:OnOutOfMemoryError is provided. We need to skip
         * across this, looking for the next option that begins with a - */
        boolean dealWithOnOutOfMemoryError = false;
        for(String s : runtime.getInputArguments()) {
            if(s.startsWith("-XX:OnOutOfMemoryError")) {
                dealWithOnOutOfMemoryError = true;
            } else {
                if(dealWithOnOutOfMemoryError && s.startsWith("-"))
                    dealWithOnOutOfMemoryError = false;
            }
            if(!dealWithOnOutOfMemoryError) {
                inputArgs.add(s);
            }
        }
        if(userArgs==null) {
            return flatten(inputArgs);
        }

        StringTokenizer tok = new StringTokenizer(userArgs);
        String[] args = new String[tok.countTokens()];
        int i = 0;
        while (tok.hasMoreTokens()) {
            args[i] = tok.nextToken();
            i++;
        }

        if(logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Runtime args: ");
            i = 0;
            for (String s : inputArgs) {
                if (i > 0)
                    sb.append(" ");
                sb.append(s);
                i++;
            }
            sb.append("\n");
            sb.append("User options: ");
            sb.append(userArgs);
            logger.debug(sb.toString());
        }
        
        for (String userArg : args) {
            boolean add = true;
            String argValue = getPart(userArg);
            for (String runtimeArg : inputArgs) {
                String runtimeArgValue = getPart(runtimeArg);
                if (argValue.equals(runtimeArgValue)) {
                    add = false;
                    int ndx = inputArgs.indexOf(runtimeArg);
                    logger.debug("Replacing runtime arg [{}] (resolved as: {}) with user arg [{}] (resolved as: {}) at index [{}]",
                                 runtimeArg, runtimeArgValue, userArg, argValue, ndx);
                    inputArgs.set(ndx, userArg);
                }
            }
            if (add) {
                if(logger.isDebugEnabled())
                    logger.debug("Adding user arg [{}]", userArg);
                inputArgs.add(userArg);
            }
        }
        return flatten(inputArgs);
    }

    private static String getPart(String s) {
        String[] parts = s.split("=");
        String part = parts[0];
        if (part.startsWith("-Xms"))
            part = "-Xms";
        if (part.startsWith("-Xmx"))
            part = "-Xmx";
        if (part.startsWith("-Xmn"))
            part = "-Xmn";
        return part;
    }

    private static String flatten(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s).append(" ");
        }
        return sb.toString();
    }
}
