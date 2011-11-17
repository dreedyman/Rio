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

import java.io.File;
import java.lang.reflect.Field;
import java.net.*;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.*;

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
public class ResolvingLoader extends RMIClassLoaderSpi {
    private final Map<String, String> artifactToCodebase = new HashMap<String, String>();
    private static final Resolver resolver;
    static {
        try {
            resolver = ResolverHelper.getResolver();
            ResolverHelper.setLogging(resolver, false);
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }
    }
    private static final RMIClassLoaderSpi loader = RMIClassLoader.getDefaultProviderInstance();

    @Override
    public Class<?> loadClass(String codebase,
                              String name,
                              ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        codebase = resolveCodebase(codebase);
        return loader.loadClass(codebase, name, defaultLoader);
    }

    @Override
    public Class<?> loadProxyClass(String codebase,
                                   String[] interfaces,
                                   ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        codebase = resolveCodebase(codebase);
        return loader.loadProxyClass(codebase, interfaces, defaultLoader);
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        codebase = resolveCodebase(codebase);
        return loader.getClassLoader(codebase);
    }

    @Override
    public String getClassAnnotation(Class<?> aClass) {
        if(aClass.getClassLoader() instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader)aClass.getClassLoader()).getURLs();
            if(urls.length>0 && urls[0].getProtocol().equals("artifact")) {
                StringBuilder builder = new StringBuilder();
                for(URL u : urls) {
                    if(builder.length()>0)
                        builder.append(" ");
                    builder.append(u.toExternalForm());
                }
                return resolveCodebase(builder.toString());
            }
        }
        return loader.getClassAnnotation(aClass);
    }

    public static void release(ClassLoader serviceLoader) {
        try {
            Field loaderTable = sun.rmi.server.LoaderHandler.class.getDeclaredField("loaderTable");
            loaderTable.setAccessible(true);
            HashMap loaderTableMap = (HashMap)loaderTable.get(null);
            findAndRemove(serviceLoader, loaderTableMap);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private String resolveCodebase(String codebase) {
        String adaptedCodebase;
        if(codebase!=null && codebase.startsWith("artifact:")) {
            synchronized (artifactToCodebase) {
                adaptedCodebase = artifactToCodebase.get(codebase);
            }
            if(adaptedCodebase==null) {
                try {
                    StringBuilder builder = new StringBuilder();
                    String[] codebaseParts = codebase.split(" ");
                    for(String codebasePart : codebaseParts) {
                        String path =  codebasePart.substring(codebase.indexOf(":")+1);
                        ArtifactURLConfiguration artifactURLConfiguration = new ArtifactURLConfiguration(path);
                        String[] cp = resolver.getClassPathFor(artifactURLConfiguration.getArtifact(),
                                                               artifactURLConfiguration.getRepositories());
                        for(String s : cp) {
                            if(builder.length()>0)
                                builder.append(" ");
                            builder.append(new File(s).toURI().toURL().toExternalForm());
                        }
                    }
                    adaptedCodebase = builder.toString();
                    synchronized (artifactToCodebase) {
                        artifactToCodebase.put(codebase, adaptedCodebase);
                    }
                } catch (ResolverException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            adaptedCodebase = codebase;
        }
        return adaptedCodebase;
    }

    private static void findAndRemove(ClassLoader loader, Map loaderTable) {
        //Map<ClassLoader, Object> toRemove = new HashMap<ClassLoader, Object>();
        for(Object o : loaderTable.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            Object key = entry.getKey();
            try {
                Field parentField = key.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                ClassLoader toCheck = (ClassLoader) parentField.get(key);
                if (isDescendantOf(toCheck, loader)) {
                    parentField.set(key, null);
                    //toRemove.put(toCheck, key);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        //for(Map.Entry<ClassLoader, Object> entry : toRemove.entrySet()) {
            //System.out.println("===> REMOVED "+entry.getKey());
            //loaderTable.remove(entry.getValue());
            //System.err.println("LoaderEntry: " + loaderEntry);
            //loaderEntry.removed = true;
        //}
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
