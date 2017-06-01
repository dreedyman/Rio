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

import org.rioproject.util.RioHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

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
 *     <li>If the <code>org.rioproject.resolver.jar</code> system property is not set, the
 *     {@link org.rioproject.resolver.ResolverConfiguration} is used to obtain the resolver jar(s). If the
 *     {@code ResolverConfiguration} cannot be loaded, or does not contain a jar property, the
 *     <code>$RIO_HOME/lib/resolver/resolver-aether.jar</code> will be used
 * </ul>
 * <p>Refer to {@link ResolverHelper#getResolver} for details on determining the class to instantiate.</p>
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public final class ResolverHelper {
    private static URLClassLoader resolverLoader;
    private static final Logger logger = LoggerFactory.getLogger(ResolverHelper.class.getName());
    private static final ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
    static {
        try {
            Resolver resolver = doGetResolver(Thread.currentThread().getContextClassLoader());
            if(resolver!=null)
                resolverLoader = (URLClassLoader) resolver.getClass().getClassLoader();
        } catch (IOException | ResolverException e) {
            logger.warn("Failed getting resolver from context classloader, try loading from rio dist", e);
        }
        if(resolverLoader==null) {
            File resolverJar = new File(getResolverJarFile());
            try {
                resolverLoader = new URLClassLoader(new URL[]{resolverJar.toURI().toURL()},
                                                    Thread.currentThread().getContextClassLoader());
            } catch (MalformedURLException e) {
                logger.error("Creating ClassLoader to load {}", resolverJar.getPath(), e);
            }
        }
    }

    /*
     * Disallow instantiation
     */
    private ResolverHelper() {
    }

    /**
     * Resolve the classpath of the provided artifact, returning an array of {@link URL}s.
     *
     * @param artifact The artifact to resolve
     * @param resolver The {@link Resolver} to use
     * @param repositories The repositories to use for resolution
     *
     * @return The classpath for the artifact
     *
     * @throws ResolverException If there are exceptions resolving the artifact
     */
    public static URL[] resolve(final String artifact, final Resolver resolver, final RemoteRepository[] repositories)
        throws ResolverException {

        if(logger.isDebugEnabled())
            logger.debug("Using Resolver {}", resolver.getClass().getName());
        List<URL> jars = new ArrayList<>();
        if (artifact != null) {
            String[] artifactParts = artifact.split(" ");
            for(String artifactPart : artifactParts) {
                String[] classPath = resolver.getClassPathFor(artifactPart, repositories);
                for (String jar : classPath) {
                    String s = null;
                    File jarFile = new File(jar);
                    if(jarFile.exists()) {
                        s = jarFile.toURI().toString();
                    } else {
                        logger.warn("{} NOT FOUND", jarFile.getPath());
                    }
                    if(s!=null) {
                        URL url;
                        try {
                            url = new URL(handleWindows(s));
                        } catch (MalformedURLException e) {
                            throw new ResolverException("Invalid classpath element: "+s, e);
                        }
                        if(!jars.contains(url))
                            jars.add(url);
                    }
                }
            }
        }
        if(logger.isDebugEnabled())
            logger.debug("Artifact: {}, resolved jars {}", artifact, jars);
        return jars.toArray(new URL[jars.size()]);
    }

    /**
     * Provides a standard means for obtaining Resolver instances, using a
     * configurable provider. The resolver provider can be specified by providing
     * a resource named "META-INF/services/org.rioproject.resolver.Resolver"
     * containing the name of the provider class. If multiple resources with
     * that name are available, then the one used will be the first one returned
     * by ServiceLoader.load.
     *
     * @return An instance of the resolver provider
     *
     * @throws ResolverException if there are problems loading the resource
     */
    public static Resolver getResolver() throws ResolverException {
       return getResolver(resolverLoader);
    }

    private static String getResolverJarFile() {
        String resolverJarFile = resolverConfiguration.getResolverJar();
        if(resolverJarFile!=null && !new File(resolverJarFile).exists()) {
            logger.warn("The configured resolver jar file [{}] does not exist, will attempt to load default resolver",
                        resolverJarFile);
            resolverJarFile = null;
        }
        if(resolverJarFile==null) {
            String resolverJarPrefix = "resolver-aether";
            String rioHome = RioHome.get();
            if (rioHome == null || rioHome.length() == 0) {
                String message = String.format("Unable to determine the location of Rio home, this must be set " +
                                               "in order to load the default %s support", resolverJarPrefix);
                logger.error(message);
                throw new RuntimeException(message);
            }
            File resolverLibDir = new File(rioHome + File.separator + "lib" + File.separator + "resolver");
            if (resolverLibDir.exists() && resolverLibDir.isDirectory()) {
                File[] files = resolverLibDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(resolverJarPrefix)) {
                            resolverJarFile = file.getPath();
                            break;
                        }
                    }
                }
            } else {
                String message = String.format("The resolver lib directory does not exist, tried using: %s",
                                               resolverLibDir.getPath());
                logger.error(message);
                throw new RuntimeException(message);
            }
        }
        if(logger.isDebugEnabled())
            logger.debug("Resolver JAR file: {}", resolverJarFile);
        return resolverJarFile;
    }

    /**
     * Provides a standard means for obtaining Resolver instances, using a
     * configurable provider. The resolver provider can be specified by providing
     * a resource named "META-INF/services/org.rioproject.resolver.Resolver"
     * containing the name of the provider class. If multiple resources with
     * that name are available, then the one used will be the first one returned
     * by ServiceLoader.load.
     *
     * @param cl The class loader to load resources and classes, and to pass when
     * constructing the provider. If null, uses the context class loader.
     *
     * @return An instance of the resolver provider
     *
     * @throws ResolverException if there are problems loading the resource
     */
    public static Resolver getResolver(final ClassLoader cl) throws ResolverException {
        Resolver r;
        ClassLoader resourceLoader = (cl != null) ? cl : Thread.currentThread().getContextClassLoader();
        try {
            r = doGetResolver(resourceLoader);
            if(logger.isDebugEnabled())
                logger.debug("Selected Resolver: {}", (r==null?"No Resolver configuration found":r.getClass().getName()));
            if(r==null) {
                throw new ResolverException("No Resolver configuration found");
            }
            if(r instanceof SettableResolver) {
                SettableResolver settableResolver = (SettableResolver) r;
                settableResolver.setRemoteRepositories(resolverConfiguration.getRemoteRepositories());
                settableResolver.setFlatDirectories(resolverConfiguration.getFlatDirectories());
            }
            if(logger.isDebugEnabled()) {
                StringBuilder message = new StringBuilder();
                for(RemoteRepository rr : r.getRemoteRepositories()) {
                    if(message.length()>0)
                        message.append("\n");
                    message.append(rr);
                }
                if(r.getRemoteRepositories().isEmpty())
                    logger.debug("Configured resolver repositories: 0");
                else
                    logger.debug("Configured resolver repositories: {}\n{}", r.getRemoteRepositories().size(), message.toString());
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
    private static Resolver doGetResolver(final ClassLoader cl) throws IOException, ResolverException {
        Resolver resolver = null;
        ServiceLoader<Resolver> loader =  ServiceLoader.load(Resolver.class, cl);
        if(logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            int num = 0;
            for(Resolver r : loader) {
                if(sb.length()>0)
                    sb.append(", ");
                sb.append(r.getClass().getName());
                num++;
            }            
            logger.debug("Found {} Resolvers: [{}]", num, sb.toString());
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
    public static String handleWindows(final String s) {
        String newString = s;
        if (System.getProperty("os.name").startsWith("Windows")) {
            if(s.startsWith("/"))
                newString = s.substring(1, s.length());
            if(s.startsWith("file:"))
                newString = s.replace('/', '\\');
        }
        return newString;
    }
}
