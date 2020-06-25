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
package org.rioproject.rmi;

import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An <code>RMIClassLoader</code> provider that supports the resolving of artifacts based on the
 * <code>org.rioproject.url.artifact</code> protocol.
 *
 * <p>The <code>ResolvingLoader</code> uses a {@link org.rioproject.resolver.Resolver} to adapt codebases that
 * have <code>artifact:</code> URLs.</p>
 *
 * <p>For each <code>artifact:</code> URL, the artifact is resolved (along with it's dependencies and
 * transitive dependencies), and installed locally. The installed artifact location(s) are then passed to the
 * default <code>RMIClassLoader</code> provider instance, where a class loader is created.</p>
 *
 * @author Dennis Reedy
 */
@SuppressWarnings({"sunapi", "PMD.AvoidThrowingRawExceptionTypes"})
public class ResolvingLoader extends RMIClassLoaderSpi {
    /**
     * A table of artifacts to derived codebases. This improves performance by resolving the classpath once per
     * artifact.
     */
    private final Map<String, String> artifactToCodebase = new ConcurrentHashMap<>();
    private static final Resolver resolver;
    private static final Logger logger = LoggerFactory.getLogger(ResolvingLoader.class.getName());
    static {
        try {
            resolver = ResolverHelper.getResolver();
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }
    }
    private static final RMIClassLoaderSpi loader = RMIClassLoader.getDefaultProviderInstance();

    @Override
    public Class<?> loadClass(final String codebase,
                              final String name,
                              final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if(logger.isTraceEnabled()) {
            logger.trace("Load class {}, with codebase {}, defaultLoader {}",
                         name, codebase, defaultLoader==null?"NULL":defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        Class<?> cl = loader.loadClass(resolvedCodebase, name, defaultLoader);
        if(logger.isTraceEnabled()) {
            logger.trace("Class {} loaded by {}", name, cl.getClassLoader());
        }
        return cl;
    }

    @Override
    public Class<?> loadProxyClass(final String codebase,
                                   final String[] interfaces,
                                   final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if(logger.isTraceEnabled()) {
            logger.trace("Load proxy classes {}, with codebase {}, defaultLoader {}",
                         Arrays.toString(interfaces), codebase, defaultLoader==null?"NULL":defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        Class<?> proxyClass = loader.loadProxyClass(resolvedCodebase, interfaces, defaultLoader);
        if(logger.isTraceEnabled()) {
            logger.trace("Proxy classes {} loaded by {}", Arrays.toString(interfaces), proxyClass.getClassLoader());
        }
        return proxyClass;
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        String resolvedCodebase = resolveCodebase(codebase);
        ClassLoader classLoader = loader.getClassLoader(resolvedCodebase);
        if(logger.isTraceEnabled()) {
            logger.trace("ClassLoader for codebase {}, resolved as {} is {}", codebase, resolvedCodebase, classLoader);
        }
        return classLoader;
    }

    @Override
    public String getClassAnnotation(final Class<?> aClass) {
        String loaderAnnotation = loader.getClassAnnotation(aClass);
        String artifact = null;
        if(loaderAnnotation!=null) {
            for(Map.Entry<String, String> entry : artifactToCodebase.entrySet()) {
                if(entry.getValue().equals(loaderAnnotation)) {
                    artifact = entry.getKey();
                    break;
                }
            }
        }
        String annotation = artifact==null?loaderAnnotation:artifact;
        if(logger.isDebugEnabled())
            logger.debug("Annotation for {} is {}", aClass.getName(), annotation);
        return annotation;
    }

    public static void release(final ClassLoader serviceLoader) {
        try {
            Field loaderTable = sun.rmi.server.LoaderHandler.class.getDeclaredField("loaderTable");
            loaderTable.setAccessible(true);
            HashMap<?,?> loaderTableMap = (HashMap<?,?>)loaderTable.get(null);
            findAndRemove(serviceLoader, loaderTableMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.warn("Failure accessing the loaderTable field", e);
        }
    }

    private String resolveCodebase(final String codebase) {
        String adaptedCodebase;
        if(codebase!=null && codebase.startsWith("artifact:")) {
            adaptedCodebase = artifactToCodebase.get(codebase);
            if(adaptedCodebase==null) {
                try {
                    logger.debug("Resolve {} ", codebase);
                    StringBuilder builder = new StringBuilder();
                    String path =  codebase.substring(codebase.indexOf(":")+1);
                    ArtifactURLConfiguration artifactURLConfiguration = new ArtifactURLConfiguration(path);
                    String[] cp = resolver.getClassPathFor(artifactURLConfiguration.getArtifact(),
                                                           artifactURLConfiguration.getRepositories());
                    for(String s : cp) {
                        if(builder.length()>0)
                            builder.append(" ");
                        builder.append(new File(s).toURI().toURL().toExternalForm());
                    }
                    adaptedCodebase = builder.toString();
                    artifactToCodebase.put(codebase, adaptedCodebase);
                } catch (ResolverException e) {
                    adaptedCodebase = codebase;
                    logger.warn("Unable to resolve {}", codebase);
                } catch (MalformedURLException e) {
                    adaptedCodebase = codebase;
                    logger.warn("The codebase {} is malformed", codebase, e);
                }
            }
        } else {
            adaptedCodebase = codebase;
        }

        return adaptedCodebase;
    }

    private synchronized static void findAndRemove(ClassLoader loader, Map<?,?> loaderTable) {
        for(Object o : loaderTable.entrySet()) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object key = entry.getKey();
            try {
                Field parentField = key.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                ClassLoader toCheck = (ClassLoader) parentField.get(key);
                if (isDescendantOf(toCheck, loader)) {
                    parentField.set(key, null);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.warn("Failure accessing the parent field", e);
            }
        }
    }

    private static boolean isDescendantOf(ClassLoader toCheck, ClassLoader loader) {
        if(toCheck==null)
            return false;
        if(toCheck.equals(loader))
            return true;
        boolean descendantOf = false;
        ClassLoader parent = toCheck.getParent();
        while(parent!=null) {
            if(parent.equals(loader)) {
                descendantOf = true;
                break;
            }
            parent = parent.getParent();
        }
        return descendantOf;
    }
}
