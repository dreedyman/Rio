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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Creates process files to indicate local service existence.
 *
 * @author Dennis Reedy
 */
public class ProcFileHelper {

    /**
     * Create a proc file. If the file already exists, simply return it.
     *
     * @return The proc {@code File}
     *
     * @throws IOException If the file cannot be created
     */
    public static synchronized File createProcFile() throws IOException {
        File procDir = getProcDir();
        File procFile = new File(procDir, getProcFileName());
        if(!procFile.exists()) {
            procFile.createNewFile();
            procFile.deleteOnExit();
        }
        return procFile;
    }

    /**
     * Get a proc file that has already been created.
     *
     * @param name The name, must not be {@code null}.
     *
     * @return The proc file
     *
     * @throws IllegalArgumentException if the name is {@code null}
     */
    public static File getProcFile(String name) {
        if(name==null)
            throw new IllegalArgumentException("name is null");
        return new File(getProcDir(), name);
    }

    /**
     * Get the directory proc file are located
     *
     * @return The proc file directory.
     */
    public static File getProcDir() {
        File rioDir = new File(System.getProperty("user.home"), ".rio");
        File procDir = new File(rioDir, "proc");
        if(!procDir.exists())
            procDir.mkdirs();
        return procDir;
    }

    /**
     * Get the proc file name for the JVM
     *
     * @return The proc file name.
     */
    public static String getProcFileName() {
        StringBuilder procNameBuilder = new StringBuilder();
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name;
        int ndx = name.indexOf("@");
        if(ndx>=1) {
            pid = name.substring(0, ndx);
        }
        procNameBuilder.append(pid).append(".proc");
        return procNameBuilder.toString();
    }
}
