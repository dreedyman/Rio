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
    static final String COMPONENT = "org.rioproject.boot";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

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
        List<PlatformCapabilityConfig> platformList = new ArrayList<PlatformCapabilityConfig>();
        File dir = new File(directory);
        if(dir.exists()) {
            if(dir.isDirectory()) {
                if(dir.canRead()) {
                    File[] files = dir.listFiles();
                    for (File file : files) {
                        if(file.getName().endsWith("groovy")) {
                            GroovyClassLoader gCL = new GroovyClassLoader(getClass().getClassLoader());
                            Class gClass = gCL.parseClass(file);
                            GroovyObject gO = (GroovyObject)gClass.newInstance();
                            String methodName = null;
                            for(Object o : gO.getMetaClass().getMethods()) {
                                MetaMethod m = (MetaMethod)o;
                                if(m.getName().startsWith("getPlatformCapabilityConfig")) {
                                    methodName = m.getName();
                                    break;
                                }
                            }
                            if(methodName==null) {
                                logger.warn("The {} class does not contain a getPlatformCapabilityConfig() " +
                                            "or getPlatformCapabilityConfigs() method", file.getName());
                                continue;
                            }
                            Object[] args = {};
                            try {
                                Object result = gO.invokeMethod(methodName, args);
                                if(result!=null) {
                                    if(result instanceof Collection) {
                                        Collection c = (Collection)result;
                                        for(Object o : c) {
                                            if(!(o instanceof PlatformCapabilityConfig)) {
                                                logger.warn("The {}.{}() method returned a collection of invalid type(s). " +
                                                            "The {} type is not allowed", file.getName(), methodName, o.getClass().getName());
                                                break;
                                            }
                                        }
                                        platformList.addAll(c);
                                    } else if(result instanceof PlatformCapabilityConfig) {
                                        platformList.add((PlatformCapabilityConfig)result);
                                    } else {
                                        logger.warn("The {}.{}() returned an unsupported type: {}",
                                                    file.getName(), methodName, result.getClass().getName());
                                    }
                                }
                            } catch(Exception e) {
                                Throwable t = e.getCause()==null?e:e.getCause();
                                logger.warn("The {} class is in error. {}:{}", file.getName(), t.getClass(), t.getMessage());
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
            throw new IllegalArgumentException("RIO_HOME cannot be null. You must set it as a system property " +
                                               "or it must be set in your environment");
        }
        File rioHomeDir = new File(rioHome);
        if(!rioHomeDir.exists())
            throw new Exception(rioHome+" does not exist");

        File rioApiJar = new File(rioHomeDir, "lib-dl"+File.separator+String.format("rio-api-%s.jar", RioVersion.VERSION));
        File rioProxyJar = new File(rioHomeDir, "lib-dl"+File.separator+String.format("rio-proxy-%s.jar", RioVersion.VERSION));
        File serviceUiJar = FileHelper.find(new File(rioHomeDir, "lib-dl"), "serviceui");
        File rioLibJar = new File(rioHomeDir, "lib"+File.separator+String.format("rio-lib-%s.jar", RioVersion.VERSION));
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(rioLibJar.getAbsolutePath()).append(File.pathSeparator);
        pathBuilder.append(rioProxyJar.getAbsolutePath()).append(File.pathSeparator);
        pathBuilder.append(rioApiJar.getAbsolutePath()).append(File.pathSeparator);
        pathBuilder.append(serviceUiJar.getAbsolutePath());

        PlatformCapabilityConfig rioCap = new PlatformCapabilityConfig("Rio",
                                                                       RioVersion.VERSION,
                                                                       pathBuilder.toString());
        rioCap.setCommon("yes");
        rioCap.setPlatformClass("org.rioproject.system.capability.software.RioSupport");

        File jskLibJar = FileHelper.find(new File(rioHomeDir, "lib"), "jsk-lib");
        PlatformCapabilityConfig jiniCap = new PlatformCapabilityConfig("Apache River",
                                                                        FileHelper.getJarVersion(jskLibJar.getName()),
                                                                        jskLibJar.getAbsolutePath());
        jiniCap.setCommon("yes");

        Collection<PlatformCapabilityConfig> c = new ArrayList<PlatformCapabilityConfig>();
        c.add(rioCap);
        c.add(jiniCap);
        return(c.toArray(new PlatformCapabilityConfig[c.size()]));
    }

}
