/*
 * Copyright to the original author or authors
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
 * {@link org.rioproject.url.artifact} protocol.
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
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class ResolvingLoader extends RMIClassLoaderSpi {
    /**
     * A table of artifacts to derived codebases. This improves performance by resolving the classpath once per
     * artifact.
     */
    private final Map<String, String> artifactToCodebase = new ConcurrentHashMap<String, String>();
    /**
     * A table of classes to artifact: codebase. This will ensure that if the annotation is requested for a class that
     * has it's classpath resolved from an artifact, that the artifact URL is passed back instead of the resolved
     * (local) classpath.
     */
    private final Map<String, String> classAnnotationMap = new ConcurrentHashMap<String, String>();
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
            logger.trace("codebase: {}, name: {}, defaultLoader: {}",
                         codebase, name, defaultLoader==null?"NULL":defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        if(codebase!=null && codebase.startsWith("artifact:") && classAnnotationMap.get(name)==null) {
            classAnnotationMap.put(name, codebase);
            logger.trace("class: {}, codebase: {}, size now {}", name, codebase, classAnnotationMap.size());
        }
        logger.trace("Load class {} using codebase {}, resolved to {}", name, codebase, resolvedCodebase);
        return loader.loadClass(resolvedCodebase, name, defaultLoader);
    }

    @Override
    public Class<?> loadProxyClass(final String codebase,
                                   final String[] interfaces,
                                   final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if(logger.isTraceEnabled()) {
            logger.trace("codebase: {}, interfaces: {}, defaultLoader: {}",
                         codebase, Arrays.toString(interfaces), defaultLoader==null?"NULL":defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        if(logger.isTraceEnabled()) {
            StringBuilder builder = new StringBuilder();
            for(String s : interfaces) {
                if(builder.length()>0) {
                    builder.append(" ");
                }
                builder.append(s);
            }
            logger.trace("Load proxy classes {} using codebase {}, resolved to {}, defaultLoader: {}",
                         builder.toString(), codebase, resolvedCodebase, defaultLoader);
        }
        return loader.loadProxyClass(resolvedCodebase, interfaces, defaultLoader);
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        if(logger.isTraceEnabled()) {
            logger.trace("codebase: {}", codebase);
        }
        String resolvedCodebase = resolveCodebase(codebase);
        return loader.getClassLoader(resolvedCodebase);
    }

    @Override
    public String getClassAnnotation(final Class<?> aClass) {
        String annotation = classAnnotationMap.get(aClass.getName());
        if(annotation == null)
            annotation = loader.getClassAnnotation(aClass);
        if(logger.isTraceEnabled())
            logger.trace("Getting annotation for {}: {}", aClass.getName(), annotation);
        return annotation;
    }

    public static void release(final ClassLoader serviceLoader) {
        try {
            Field loaderTable = sun.rmi.server.LoaderHandler.class.getDeclaredField("loaderTable");
            loaderTable.setAccessible(true);
            HashMap loaderTableMap = (HashMap)loaderTable.get(null);
            findAndRemove(serviceLoader, loaderTableMap);
        } catch (NoSuchFieldException e) {
            logger.warn("Failure accessing the loaderTable field", e);
        } catch (IllegalAccessException e) {
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
                    logger.warn("Unable to resolve {}", codebase);
                } catch (MalformedURLException e) {
                    logger.warn("The codebase {} is malformed", codebase, e);
                }
            }
        } else {
            adaptedCodebase = codebase;
        }

        return adaptedCodebase;
    }

    private synchronized static void findAndRemove(ClassLoader loader, Map loaderTable) {
        for(Object o : loaderTable.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            try {
                Field parentField = key.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                ClassLoader toCheck = (ClassLoader) parentField.get(key);
                if (isDescendantOf(toCheck, loader)) {
                    parentField.set(key, null);
                }
            } catch (NoSuchFieldException e) {
                logger.warn("Failure accessing the parent field", e);
            } catch (IllegalAccessException e) {
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
