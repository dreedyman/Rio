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
package org.rioproject.impl.config;


import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.util.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility for accessing configuration arguments.
 *
 * @author Dennis Reedy
 */
public class ConfigHelper {
    public static final String CLASSPATH_RESOURCE="classpath:";
    public static final String FILE_RESOURCE="file:";
    private static Logger logger = LoggerFactory.getLogger(ConfigHelper.class);

    /**
     * Get configuration arguments to create a
     * {@link net.jini.config.Configuration} object
     *
     * @param service The {@code ServiceElement} containing details
     * to create a {@link net.jini.config.Configuration}
     *
     * @return
     * <ul>
     * <li>If the first parameter of the enclosed configuration args parameters is an {@link Artifact},
     * resolve the {@code Artifact}, and returned the resolved file.
     * <li>If the first parameter of the enclosed configuration args parameters is a "-",
     * return the configuration args intact.
     * <li>If the first element of the enclosed configuration args parameter begins with
     * <tt>classpath:</tt>, load the {@link java.net.URL} of the configuration
     * file as a resource using the current context classloader, and return the
     * {@link java.net.URL#toString()} of the loaded resource. Only the first
     * element of the args parameter is used to construct the configuration
     * argument.
     * <li>If the first element of the enclosed configuration args parameter begins with
     * <tt>file:</tt>, return the canonical path of the file. If the parameter
     * does not include a leading {@link java.io.File#separator}, the file
     * location will be considered relative to the current working directory.
     * If there are any system property elements defined in the args element,
     * they will be translated to the corresponding system properties of the
     * running JVM.
     * <li>If the first element of the enclosed configuration args parameter ends with .config or
     * .groovy, return the args parameter intact. If there are any system
     * property elements defined in the args element, they will be translated
     * to the corresponding system properties of the running JVM.
     * <li>Otherwise, create a temporary file containing the contents of the
     * args value and return the canonical path of the created file as the
     * configuration argument
     * </ul>
     *
     * @throws IllegalArgumentException if the service parameter is null
     * @throws IOException if there are errors accessing the file system
     */
    public static String[] getConfigArgs(final ServiceElement service) throws IOException {
        return getConfigArgs(service, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Get configuration arguments to create a
     * {@link net.jini.config.Configuration} object
     *
     * @param service The {@code ServiceElement} containing details
     * to create a {@link net.jini.config.Configuration}
     * @param cl The ClassLoader to use if resources need to be loaded.
     *
     * @return
     * <ul>
     * <li>If the first parameter of the enclosed configuration args parameters is an {@link Artifact},
     * resolve the {@code Artifact}, and returned the resolved file.
     * <li>If the first parameter of the enclosed configuration args parameters is a "-",
     * return the configuration args intact.
     * <li>If the first element of the enclosed configuration args parameter begins with
     * <tt>classpath:</tt>, load the {@link java.net.URL} of the configuration
     * file as a resource using the current context classloader, and return the
     * {@link java.net.URL#toString()} of the loaded resource. Only the first
     * element of the args parameter is used to construct the configuration
     * argument.
     * <li>If the first element of the enclosed configuration args parameter begins with
     * <tt>file:</tt>, return the canonical path of the file. If the parameter
     * does not include a leading {@link java.io.File#separator}, the file
     * location will be considered relative to the current working directory.
     * If there are any system property elements defined in the args element,
     * they will be translated to the corresponding system properties of the
     * running JVM.
     * <li>If the first element of the enclosed configuration args parameter ends with .config or
     * .groovy, return the args parameter intact. If there are any system
     * property elements defined in the args element, they will be translated
     * to the corresponding system properties of the running JVM.
     * <li>Otherwise, create a temporary file containing the contents of the
     * args value and return the canonical path of the created file as the
     * configuration argument
     * </ul>
     *
     * @throws IllegalArgumentException if the service parameter is null, or if a
     * ClassLoader is not provided
     * @throws IOException if there are errors accessing the file system
     */
    public static String[] getConfigArgs(final ServiceElement service, final ClassLoader cl) throws IOException {
        if(service==null)
            throw new IllegalArgumentException("service cannot be null");
        String[] args = service.getServiceBeanConfig().getConfigArgs();
        if(cl==null)
            throw new IllegalArgumentException("You must provide a ClassLoader");
        if (args[0].equals("-"))
            return args;
        if(Artifact.isArtifact(args[0])) {
            logger.debug("Loading configuration {} as an artifact", args[0]);
            URL configLocation = null;
            try {
                Resolver resolver = ResolverHelper.getResolver();
                Artifact artifact = new Artifact(args[0]);
                configLocation = resolver.getLocation(artifact.getGAV(),
                                                      artifact.getType(),
                                                      service.getRemoteRepositories());
                return new String[]{new File(configLocation.toURI()).getPath()};
            } catch(ResolverException e) {
                throw new IOException("Could not resolve "+args[0], e);
            } catch (URISyntaxException e) {
                throw new IOException("Could load resolved configuration from "+configLocation.toExternalForm(), e);
            }
        }
        if(args[0].startsWith(CLASSPATH_RESOURCE)) {
            logger.debug("Loading configuration {} as a classpath resource", args[0]);
            String classPathArgs = args[0].substring(CLASSPATH_RESOURCE.length(), args[0].length());
            List<String> list = new ArrayList<String>();
            for(String s : classPathArgs.split(",")) {
                URL url = cl.getResource(s);
                if(url==null) {
                    throw new IOException("Unable to load ["+s+"] " +
                                          "configuration file as a classpath " +
                                          "resource using classloader ["+cl+"]");
                }
                list.add(url.toString());
            }
            return list.toArray(new String[list.size()]);
        }
        if(args[0].startsWith(FILE_RESOURCE) ||
           args[0].endsWith(".config") || args[0].endsWith(".groovy")) {
            logger.debug("Loading configuration {} as a file resource", args[0]);
            String fileArg;
            if(args[0].startsWith(FILE_RESOURCE)) {
                if(args[0].startsWith(FILE_RESOURCE+"//")) {
                    String s = FILE_RESOURCE+"//";
                    fileArg = args[0].substring(s.length(), args[0].length());
                } else {
                    fileArg = args[0].substring(FILE_RESOURCE.length(),
                                                args[0].length());
                }
            } else {
                fileArg = args[0];
            }
            String fileArgs = PropertyHelper.expandProperties(fileArg,
                                                              PropertyHelper.PARSETIME);
            List<String> list = new ArrayList<String>();

            for(String fileName : fileArgs.split(",")) {
                if(fileName.startsWith("http:") || fileName.startsWith("https:")) {
                    list.add(fileName);
                    continue;
                }
                File f = new File(fileName);
                if(!f.isAbsolute()) {
                    fileName = System.getProperty("user.dir")+File.separator+fileName;
                    f = new File(fileName);
                }

                if(!f.exists())
                    throw new FileNotFoundException("Unable to load declared configuration " +
                                                    "file ["+fileArg+"], resolved " +
                                                    "as ["+fileName+"]. File does not exist");
                list.add(fileName);
            }
            return list.toArray(new String[list.size()]);
        }

        if(isGroovy(args)) {
            return args;
        } else {
            String extension = ".config";
            File tmp = File.createTempFile("tmp", extension);
            tmp.deleteOnExit();
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(tmp));
                for (String arg : args) {
                    out.write(arg);
                }
            } finally {
                if (out != null)
                    out.close();
            }
            return (new String[]{tmp.getCanonicalPath()});
        }
    }

    private static boolean isGroovy(final String... args) {
        StringBuilder buff = new StringBuilder();
        for(String s : args)
            buff.append(s);
        boolean isGroovy = false;
        StringTokenizer tok = new StringTokenizer(buff.toString(), "\n");
        while(tok.hasMoreTokens()) {
            String line = tok.nextToken();
            if(line.contains("{")) {
                if(line.contains("class"))
                    isGroovy = true;
                break;
            }
        }
        return isGroovy;
    }
}
