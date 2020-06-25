/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Utility to assist in deriving Rio home if not set.
 *
 * @author Dennis Reedy
 */
public class RioHome {
    static final Logger logger = LoggerFactory.getLogger(RioHome.class);

    /**
     * Get the location of Rio home if it is not set. If the location
     * is not set (by querying the {@code rio.home} system property,
     * defaulting to the {@code RIO_HOME} environment variable), attempt
     * to get the location as determined by the parent directory of the
     * jar the {@code RioHome} class is loaded from. If Rio home can be
     * derived, the {@code rio.home} system property will be set.
     *
     * @return The location of Rio home directory, or {@code null} if it cannot be derived.
     */
    public static String get() {
        String rioHome = System.getProperty("rio.home", System.getenv("RIO_HOME"));
        if(rioHome==null) {
            Class clazz = RioHome.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();

            logger.debug("classPath: {}", classPath);
            /* Make sure we are loaded from a JAR */
            if (classPath.startsWith("jar:file:")) {
                String path = classPath.substring("jar:file:".length(), classPath.lastIndexOf("!"));
                logger.debug("path: {}", path);
                File jar = new File(path);
                File directory = jar.getParentFile().getParentFile();
                logger.debug("directory: {}, exists? {}", directory.getPath(), directory.exists());
                rioHome = directory.exists() ? directory.getPath() : null;
            }
            if(rioHome!=null) {
                logger.info("Derived and set \"rio.home\" to {}", rioHome);
                System.setProperty("rio.home", rioHome);
            }
        }
        return rioHome;
    }
}
