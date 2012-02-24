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
package org.rioproject.resolver;

import org.rioproject.resolver.maven2.Repository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>A helper that provides utilities for obtaining and working with a
 * {@link org.rioproject.resolver.Resolver}.</p>
 *
 * <p>This utility uses the following approach to load a <code>Resolver</code> instance:</p>
 * <p>The location of the jar that contains an implementation of the <code>Resolver</code> interface is
 *     determined in the following order:</p>
 * <ul>
 *     <li>First checking if the <code>org.rioproject.resolver.jar</code> system property has been
 *     declared. This property should contain the location of the resolver jar(s) needed to instantiate a
 *     <code>Resolver</code>.
 *     <li>If the <code>org.rioproject.resolver.jar</code> system property is not set, the default
 *     <code>$RIO_HOME/lib/resolver/resolver-aether.jar</code> will be used
 * </ul>
 * <p>Refer to {@link ResolverHelper#getResolver} for details on determining the class to instantiate.</p>
 *
 * @author Dennis Reedy
 */
public class ResolverHelper {
    public final static String M2_HOME = Repository.getLocalRepository().getAbsolutePath();
    static final String M2_HOME_URI = Repository.getLocalRepository().toURI().toString();
    private static URLClassLoader resolverLoader;
    static final Logger logger = Logger.getLogger(ResolverHelper.class.getName());
    public static final String RESOLVER_JAR = "org.rioproject.resolver.jar";

    /*
     * Disallow instantiation
     */
    private ResolverHelper() {

    }

    /**
     * Resolve the classpath with the local Maven repository as the codebase
     *
     * @param artifact The artifact to resolve
     * @param resolver The {@link Resolver} to use
     * @param repositories The repositories to use for resolution
     *
     * @return The classpath for the artifact
     *
     * @throws ResolverException If there are exceptions resolving the artifact
     */
    public static String[] resolve(String artifact, Resolver resolver, RemoteRepository[] repositories) throws ResolverException {
        return resolve(artifact, resolver, repositories, M2_HOME_URI);
    }

    /**
     * Resolve the classpath using the provided codebase
     *
     * @param artifact The artifact to resolve
     * @param resolver The {@link Resolver} to use
     * @param repositories The repositories to use for resolution
     * @param codebase The codebase to set for jars that are located
     * in the local Maven repository.
     *
     * @return The classpath for the artifact
     *
     * @throws ResolverException If there are exceptions resolving the artifact
     */
    public static String[] resolve(String artifact,
                                   Resolver resolver,
                                   RemoteRepository[] repositories,
                                   String codebase) throws ResolverException {
        List<String> jars = new ArrayList<String>();
        if (artifact != null) {
            String[] artifactParts = artifact.split(" ");
            for(String artifactPart : artifactParts) {
                String[] classPath = resolver.getClassPathFor(artifactPart, repositories);
                for (String jar : classPath) {
                    String s = null;
                    if(jar.startsWith(M2_HOME)) {
                        String jarPart = jar.substring(M2_HOME.length());
                        if(codebase.endsWith("/") && jarPart.startsWith(File.separator)) {
                            jarPart = jarPart.substring(1, jarPart.length());
                        }
                        if(codebase.startsWith("http:")) {
                            jarPart = handleWindowsHTTP(jarPart);
                        }
                        s = codebase+jarPart;
                    } else {
                        File jarFile = new File(jar);
                        if(jarFile.exists()) {
                            s = jarFile.toURI().toString();
                        } else {
                            logger.warning(jarFile.getPath()+" NOT FOUND");
                        }
                    }
                    if(s!=null) {
                        s = handleWindows(s);
                        if(!jars.contains(s))
                            jars.add(s);
                    }
                }
            }
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine("Artifact: "+artifact+", resolved jars "+jars);
        return jars.toArray(new String[jars.size()]);
    }

    /**
     * Provides a standard means for obtaining Resolver instances, using a
     * configurable provider. The resolver provider can be specified by providing
     * a resource named "META-INF/services/org.rioproject.resolver.Resolver"
     * containing the name of the provider class. If multiple resources with
     * that name are available, then the one used will be the first one returned
     * by ServiceLoader.load. If the resource is not found, the
     * SimpleResolver class is used.
     *
     * @return An instance of the resolver provider
     *
     * @throws ResolverException if there are problems loading the resource
     */
    public static synchronized Resolver getResolver() throws ResolverException {
        File resolverJar = new File(getResolverJarFile());
        if(resolverLoader==null)
            try {
                resolverLoader = new URLClassLoader(new URL[]{resolverJar.toURI().toURL()},
                                                    Thread.currentThread().getContextClassLoader());
            } catch (MalformedURLException e) {
                throw new ResolverException("Creating ClassLoader to load "+resolverJar.getPath(), e);
            }
        return getResolver(resolverLoader);
    }

    private static String getResolverJarFile() {
        String resolverJarFile = System.getProperty(RESOLVER_JAR);
        if(resolverJarFile==null) {
            if(System.getProperty("RIO_HOME")==null)
                throw new RuntimeException("RIO_HOME must be set in order to load the resolver-aether.jar");
            String resolverJarName = "resolver-aether.jar";
            String resolverLibDir = System.getProperty("RIO_HOME")+File.separator+"lib"+File.separator+"resolver";
            resolverJarFile = resolverLibDir+File.separator+resolverJarName;
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine("#######################\n"+resolverJarFile+"\n#######################");
        return resolverJarFile;
    }

    /**
     * Provides a standard means for obtaining Resolver instances, using a
     * configurable provider. The resolver provider can be specified by providing
     * a resource named "META-INF/services/org.rioproject.resolver.Resolver"
     * containing the name of the provider class. If multiple resources with
     * that name are available, then the one used will be the last one returned
     * by ServiceLoader.load.
     *
     * @param cl The class loader to load resources and classes, and to pass when
     * constructing the provider. If null, uses the context class loader.
     *
     * @return An instance of the resolver provider
     *
     * @throws ResolverException if there are problems loading the resource
     */
    public static Resolver getResolver(ClassLoader cl) throws ResolverException {
        Resolver r;
        ClassLoader resourceLoader = (cl != null) ? cl : Thread.currentThread().getContextClassLoader();
        try {
            r = doGetResolver(resourceLoader);
            if(logger.isLoggable(Level.FINE))
                logger.fine("Selected Resolver: " +(r==null?"No Resolver configuration found":r.getClass().getName()));
            if(r==null) {
                throw new ResolverException("No Resolver configuration found");
            }
        } catch (Exception e) {
            if(e instanceof ResolverException)
                throw (ResolverException)e;
            throw new ResolverException("Creating Resolver", e);
        }
        return r;
    }

    /*
     * Returns the Resolver using ServiceLoader.load.
     */
    private static Resolver doGetResolver(ClassLoader cl) throws IOException, ResolverException {
        Resolver resolver = null;
        ServiceLoader<Resolver> loader =  ServiceLoader.load(Resolver.class, cl);
        if(logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            int num = 0;
            for(Resolver r : loader) {
                if(sb.length()>0)
                    sb.append(", ");
                sb.append(r.getClass().getName());
                num++;
            }            
            logger.fine("Found "+num+" Resolvers: ["+sb.toString()+"]");
        }
        for(Resolver r : loader) {
            if(r!=null) {
                resolver = r;
            }
        }
        return resolver;
    }

    /*
     * Convert windows path names if needed
     */
    public static String handleWindows(String s) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            if(s.startsWith("/"))
                s = s.substring(1, s.length());
            if(s.startsWith("file:"))
                s = s.replace('/', '\\');
        }
        return s;
    }

    /*
     * Trim leading '/'
     */
    private static String handleWindowsHTTP(String s) {
        if (System.getProperty("os.name").startsWith("Windows"))
            s = s.replace('\\', '/');
        return s;
    }
}
