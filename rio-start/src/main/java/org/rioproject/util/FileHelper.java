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
package org.rioproject.util;

import java.io.File;

/**
 * Utility for looking for a file in a directory or getting the version from a file name.
 *
 * @author Dennis Reedy
 */
public class FileHelper {

    /**
     * Find a file starting with the {@code baseName}.
     *
     * @param dir The directory the file is located in.
     * @param baseName The name the sought after file starts with.
     *
     * @return The {@code File} or {@code null}
     *
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public static File find(File dir, String baseName) {
        if(dir==null)
            throw new IllegalArgumentException("directory can not be null");
        if(baseName==null)
            throw new IllegalArgumentException("baseName can not be null");
        File found = null;
        File[] files = dir.listFiles();
        if(files!=null) {
            for(File file : files) {
                if(file.getName().startsWith(baseName)) {
                    found = file;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Get the version from the jar name (name-version.jar)
     *
     * @param name The name of the jar
     *
     * @return The version or {@code null} the the name does not match the pattern
     *
     * @throws IllegalArgumentException if the {@code name} argument is {@code null}
     */
    public static String getJarVersion(String name) {
        if(name==null)
            throw new IllegalArgumentException("name can not be null");
        int startIndex = name.lastIndexOf("-");
        int lastIndex = name.lastIndexOf(".");
        if(startIndex==-1 || lastIndex==-1)
            return null;
        return name.substring(startIndex+1, lastIndex);
    }
}
