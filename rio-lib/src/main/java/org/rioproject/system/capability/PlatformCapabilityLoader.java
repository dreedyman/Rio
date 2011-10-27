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
package org.rioproject.system.capability;

import org.rioproject.config.PlatformCapabilityConfig;
import org.rioproject.loader.CommonClassLoader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to assist in the loading of a {@link PlatformCapability}
 */
public class PlatformCapabilityLoader {
    /**
     * Get the loadable classpath for the PlatformCapability.
     *
     * @param platformCapability The <code>PlatformCapability</code> to load
     *
     * @return The classpath for loading the PlatformCapability. If the
     * classpath for this PlatformCapability is already loaded by the common
     * class loader, this method will return a zero-length array. Otherwise this
     * method will return the resources required to load this platform
     * capability
     *
     * @throws java.net.MalformedURLException if the classpath cannot be constructed
     */
    public static URL[] getLoadableClassPath(PlatformCapability platformCapability) throws MalformedURLException {
        String[] classPath = platformCapability.getClassPath();
        if(classPath==null)
            return(new URL[0]);
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        CommonClassLoader cCL = CommonClassLoader.getInstance();
        URL[] loadedURLs = null;
        try {
            Thread.currentThread().setContextClassLoader(cCL);
            loadedURLs = cCL.getURLs();
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
        List<URL> loadables = new ArrayList<URL>();
        URL[] urls = PlatformCapabilityConfig.toURLs(classPath);
        for (URL url : urls) {
            boolean loaded = false;
            for (URL loadedURL : loadedURLs) {
                if (url.sameFile(loadedURL)) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded)
                loadables.add(url);
        }
        return(loadables.toArray(new URL[loadables.size()]));
    }
}
