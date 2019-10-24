/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.config;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaMethod;
import org.rioproject.RioVersion;
import org.rioproject.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Parses platform configuration documents
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class PlatformLoader {
    private static final Logger logger = LoggerFactory.getLogger(PlatformLoader.class);
    private static final List<PlatformCapabilityConfig> platformList = new ArrayList<>();

    /**
     * Parse the platform
     *
     * @param directory The directory to search for groovy configuration documents
     *
     * @return An array of PlatformCapabilityConfig objects
     *
     * @throws Exception if there are errors parsing the configuration files
     */
    @SuppressWarnings("unchecked")
    public PlatformCapabilityConfig[] parsePlatform(String directory) throws Exception {
        if(directory == null)
            throw new IllegalArgumentException("directory is null");

        if(platformList.isEmpty()) {
            File dir = new File(directory);
            if (dir.exists()) {
                if (dir.isDirectory()) {
                    if (dir.canRead()) {
                        logger.debug("Looking for platform configurations in {}", dir.getPath());
                        File[] files = dir.listFiles();
                        for (File file : files) {
                            if (file.getName().endsWith("groovy")) {
                                logger.debug("Load and parse platform configuration {}", file.getName());
                                GroovyClassLoader gCL = new GroovyClassLoader(getClass().getClassLoader());
                                Class gClass = gCL.parseClass(file);
                                GroovyObject gO = (GroovyObject) gClass.newInstance();
                                String methodName = null;
                                for (Object o : gO.getMetaClass().getMethods()) {
                                    MetaMethod m = (MetaMethod) o;
                                    if (m.getName().startsWith("getPlatformCapabilityConfig")) {
                                        methodName = m.getName();
                                        break;
                                    }
                                }
                                if (methodName == null) {
                                    logger.error("The {} class does not contain a getPlatformCapabilityConfig() " +
                                                "or getPlatformCapabilityConfigs() method", file.getName());
                                    continue;
                                }
                                Object[] args = {};
                                try {
                                    Object result = gO.invokeMethod(methodName, args);
                                    if (result != null) {
                                        if (result instanceof Collection) {
                                            Collection c = (Collection) result;
                                            for (Object o : c) {
                                                if (!(o instanceof PlatformCapabilityConfig)) {
                                                    logger.error(
                                                        "The {}.{}() method returned a collection of invalid type(s). " +
                                                        "The {} type is not allowed",
                                                        file.getName(),
                                                        methodName,
                                                        o.getClass().getName());
                                                    break;
                                                }
                                            }
                                            platformList.addAll(c);
                                        } else if (result instanceof PlatformCapabilityConfig) {
                                            platformList.add((PlatformCapabilityConfig) result);
                                        } else {
                                            logger.error("The {}.{}() returned an unsupported type: {}",
                                                        file.getName(), methodName, result.getClass().getName());
                                        }
                                    }
                                } catch (Exception e) {
                                    Throwable t = e.getCause() == null ? e : e.getCause();
                                    logger.error("The {} class is in error. {}:{}",
                                                file.getName(),
                                                t.getClass(),
                                                t.getMessage());
                                }
                            }
                        }
                    } else {
                        logger.warn("No read permissions for platform directory [{}]", directory);
                    }
                } else {
                    logger.warn("Platform directory [{}] is not a directory", dir);
                }
            } else {
                logger.warn("Platform directory [{}] not found", directory);
            }
        }
        return(platformList.toArray(new PlatformCapabilityConfig[platformList.size()]));
    }

    /**
     * Get the default platform configuration
     *
     * @param rioHome Home directory for Rio, must not be null and must exist
     *
     * @return An array of PlatformCapabilityConfig objects
     *
     * @throws IllegalArgumentException if rioHome is null and not running under
     * the StaticCybernode
     * @throws Exception if the rioHome does not exist
     */
    public PlatformCapabilityConfig[] getDefaultPlatform(String rioHome) throws Exception {
        if(System.getProperty("StaticCybernode")!=null)
            return new PlatformCapabilityConfig[0];
        if(rioHome==null) {
            logger.warn("rio.home is not set, returning empty platform configuration");
            return new PlatformCapabilityConfig[0];
        }
        File rioHomeDir = new File(rioHome);
        if(!rioHomeDir.exists())
            throw new Exception(rioHome+" does not exist");

        File libDl = new File(rioHomeDir, "lib-dl");
        File rioApiJar = new File(libDl, String.format("rio-api-%s.jar", RioVersion.VERSION));
        File rioProxyJar = new File(libDl, String.format("rio-proxy-%s.jar", RioVersion.VERSION));
        File serviceUiJar = FileHelper.find(libDl, "serviceui");
        File rioLibJar = new File(rioHomeDir, "lib"+File.separator+String.format("rio-lib-%s.jar", RioVersion.VERSION));
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(rioLibJar.getAbsolutePath()).append(File.pathSeparator);
        pathBuilder.append(rioProxyJar.getAbsolutePath()).append(File.pathSeparator);
        pathBuilder.append(rioApiJar.getAbsolutePath()).append(File.pathSeparator);
        if(serviceUiJar!=null && serviceUiJar.exists())
            pathBuilder.append(serviceUiJar.getAbsolutePath());
        else
            logger.warn("serviceui jar does not exist in {}", libDl.getPath());

        PlatformCapabilityConfig rioCap = new PlatformCapabilityConfig("Rio",
                                                                       RioVersion.VERSION,
                                                                       pathBuilder.toString());
        rioCap.setCommon("yes");
        rioCap.setPlatformClass("org.rioproject.system.capability.software.RioSupport");

        Collection<PlatformCapabilityConfig> c = new ArrayList<>();
        c.add(rioCap);

        File libDir = new File(rioHomeDir, "lib");
        File jskLibJar = FileHelper.find(libDir, "jsk-lib");
        if (jskLibJar != null) {
            PlatformCapabilityConfig jiniCap = new PlatformCapabilityConfig("Apache River",
                                                                            FileHelper.getJarVersion(jskLibJar.getName()),
                                                                            jskLibJar.getAbsolutePath());
            jiniCap.setCommon("yes");
            c.add(jiniCap);
        } else {
            logger.warn("jsk-lib jar does not exist in {}", libDir.getPath());
        }

        return(c.toArray(new PlatformCapabilityConfig[0]));
    }

}
