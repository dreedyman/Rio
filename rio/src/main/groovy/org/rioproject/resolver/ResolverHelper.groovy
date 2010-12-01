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

import org.rioproject.core.ClassBundle
import org.rioproject.config.maven2.Repository
import java.util.logging.Logger
import java.util.logging.Level

/**
 * Provides utilities for working with a {@link org.rioproject.resolver.Resolver}
 */
class ResolverHelper {
    static String M2_HOME = Repository.getLocalRepository().getAbsolutePath();
    static String M2_HOME_URI = Repository.getLocalRepository().toURI().toString();
    static final Logger logger = Logger.getLogger(ResolverHelper.class.getName())

    /**
     * Resolve the classpath with the local Maven repository as the codebase
     *
     * @param artifact The artifact to resolve
     * @param resolver The {@link Resolver} to use
     * @param repositories The repositories to use for resolution
     * @param install If true, download and install the resolved elements
     */

    def static String[] resolve(String artifact,
                                Resolver resolver,
                                RemoteRepository[] repositories,
                                boolean install) {
        return resolve(artifact, resolver, repositories, (String) M2_HOME_URI, install);
    }

    /**
     * Resolve the classpath using the provided codebase
     *
     * @param artifact The artifact to resolve
     * @param resolver The {@link Resolver} to use
     * @param repositories The repositories to use for resolution
     * @param codebase The codebase to set for jars that are located
     * in the local Maven repository.
     * @param install If true, download and install the resolved elements
     */

    def static String[] resolve(String artifact,
                                Resolver resolver,
                                RemoteRepository[] repositories,
                                String codebase,
                                boolean install) {
        List<String> jars = new ArrayList<String>();
        if (artifact != null) {
            String[] classPath = resolver.getClassPathFor(artifact,
                                                          (RemoteRepository[])repositories,
                                                          install);
            for (String jar : classPath) {
                String s = null
                if(jar.startsWith(M2_HOME)) {
                    String jarPart = jar.substring(M2_HOME.length())
                    if(codebase.endsWith("/") && jarPart.startsWith(File.separator))
                        jarPart = jarPart.substring(1, jarPart.length())
                    if(codebase.startsWith("http:"))
                        jarPart = handleWindowsHTTP(jarPart)
                    s = codebase+jarPart
                } else {
                    File jarFile = new File(jar)
                    if(jarFile.exists())
                        s = jarFile.toURI().toString()
                    else
                        println "[WARNING] ${jarFile.path} NOT FOUND"
                }
                if(s!=null)
                    jars.add(handleWindows(s));
            }
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine "Artifact: ${artifact}, resolved jars ${jars}"
        return jars.toArray(new String[jars.size()])
    }


    /**
     * Resolve the classpath 
     *
     * @param bundle The {@link org.rioproject.core.ClassBundle}
     * @param resolver The {@link Resolver} to use
     * @param pom The pom file
     * @param install If true, download and install the resolved elements
     */
    def static resolve(ClassBundle bundle,
                       Resolver resolver,
                       File pom,
                       boolean install) {
        if(logger.isLoggable(Level.FINE))
            logger.fine "Artifact: ${bundle.getArtifact()}, "+
                        "resolver: ${resolver.getClass().name}, "+
                        "pom: $pom, install: $install"
        if (bundle.getArtifact() != null) {
            String[] classPath = resolver.getClassPathFor(bundle.getArtifact(),
                                                          (File) pom,
                                                          install);
            List<String> jars = new ArrayList<String>();
            for (String jar : classPath) {
                jars.add(handleWindows(jar));
            }
            if(logger.isLoggable(Level.FINE))
                logger.fine "Artifact: ${bundle.getArtifact()}, resolved jars ${jars}"
            bundle.setCodebase("file://")
            bundle.setJARs(jars.toArray(new String[jars.size()]));
        }
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
    def static Resolver getInstance() throws ResolverException {
        return getInstance(null);
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
     * @param cl The class loader to load resources and classes, and to pass when
     * constructing the provider. If null, uses the context class loader.
     *
     * @return An instance of the resolver provider
     *
     * @throws ResolverException if there are problems loading the resource
     */
    def static Resolver getInstance(ClassLoader cl) throws ResolverException {
        Resolver r;
        ClassLoader resourceLoader = (cl != null) ? cl : Thread.currentThread().getContextClassLoader();
        try {
            r = getResolver(resourceLoader);
            if(logger.isLoggable(Level.FINE))
                logger.fine "===> Selected Resolver: " +(r==null?"Use Default":"${r.getClass().name}")
            if(r==null) {
                String defaultResolver = "org.rioproject.resolver.maven2.SimpleResolver";
                Class cls = Class.forName(defaultResolver, true, resourceLoader);
                if (!Resolver.class.isAssignableFrom(cls)) {
                    throw new ResolverException("provider class " + defaultResolver +
                                                " does not implement Resolver");
                } else {
                    r = (Resolver)cls.newInstance();
                }
            }
        } catch (Exception e) {
            throw new ResolverException("Creating Resolver", e);
        }
        return r;
    }

    /*
     * Returns the Resolver using ServiceLoader.load.
     */
    def static Resolver getResolver(ClassLoader cl) throws IOException, ResolverException {
        Resolver resolver = null;
        ServiceLoader<Resolver> loader =  ServiceLoader.load(Resolver.class, cl);
        if(logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder()
            int num = 0
            for(Resolver r : loader) {
                if(sb.length()>0)
                    sb.append(", ")
                sb.append(r.getClass().name)
                num++
            }            
            logger.fine "===> Found ($num) Resolvers: [${sb.toString()}]"
        }
        for(Resolver r : loader) {
            if(r!=null) {
                resolver = r;
                break;
            }
        }
        return resolver;
    }

    /*
     * Convert windows path names if needed
     */
    def static String handleWindows(String s) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            if(s.startsWith("/"))
                s = s.substring(1, s.length())
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
        return s
    }
}
