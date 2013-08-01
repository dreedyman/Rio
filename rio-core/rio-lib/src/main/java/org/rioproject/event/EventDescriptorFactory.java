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
package org.rioproject.event;

import org.rioproject.loader.ClassAnnotator;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.rioproject.util.StringUtil;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create {@code EventDescriptor}s from a classpath.
 *
 * @author Dennis Reedy
 */
public final class EventDescriptorFactory {

    private EventDescriptorFactory() {
    }

    /**
     * Create an {@code EventDescriptor} from a classpath.
     *
     * @param classpath The classpath to use. If {@code null}, the thread's context classloader will be used.
     *                  If not {@code null}, an {@link URLClassLoader} will be created using the provided classpath,
     *                  delegating to the thread's context classloader.
     * @param classNames A var-arg list of {@link RemoteServiceEvent} class names. Must not be {@code null}
     *
     * @return A {@code List} of {@code EventDescriptor}s.
     *
     * @throws MalformedURLException if the classPath can not be used to create an {@link URL}.
     * @throws ResolverException if the classpath represents an artifact (groupId:artifactId:version), and a
     * {@code Resolver} can not be created.
     * @throws ClassNotFoundException If the {@link RemoteServiceEvent} class can not be found.
     * @throws IllegalArgumentException if the {@code classNames} argument is {@code null}.
     * @throws URISyntaxException if the classpath is an artifact and the {@code URls} cannot be transformed to {@code URI}s
     */
    public static List<EventDescriptor> createEventDescriptors(String classpath,
                                                               String... classNames) throws MalformedURLException,
                                                                                            ResolverException,
                                                                                            ClassNotFoundException,
                                                                                            URISyntaxException {
        if(classNames==null)
            throw new IllegalArgumentException("classNames must not be null");
        if(classNames.length==0)
            throw new IllegalArgumentException("classNames must not be empty");
        final List<EventDescriptor> eventDescriptors = new ArrayList<EventDescriptor>();
        if (classpath != null) {
            ClassAnnotator annotator = null;
            String[] classPath;
            if (Artifact.isArtifact(classpath)) {
                ArtifactURLConfiguration artifactURLConfiguration = new ArtifactURLConfiguration(classpath);
                StringBuilder artifactBuilder = new StringBuilder();
                artifactBuilder.append("artifact:").append(artifactURLConfiguration.getArtifact());
                annotator = new ClassAnnotator(new URL[]{new URL(artifactBuilder.toString())});
                String[] cp = ResolverHelper.getResolver().getClassPathFor(classpath);
                classPath = new String[cp.length];
                for (int i = 0; i < classPath.length; i++) {
                    String s = cp[i].startsWith("file:") ? cp[i] : "file:" + cp[i];
                    classPath[i] = ResolverHelper.handleWindows(s);
                }

            } else {
                classPath = StringUtil.toArray(classpath, " ,");
            }
            URL[] urls = new URL[classPath.length];
            for (int i = 0; i < classPath.length; i++) {
                urls[i] = new URL(classPath[i]);
            }
            URLClassLoader loader;
            if(annotator!=null) {
                loader = new ServiceClassLoader(ServiceClassLoader.getURIs(urls), annotator, Thread.currentThread().getContextClassLoader());
            } else {
                loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            }
            for (String className : classNames) {
                Class<?> cl = loader.loadClass(className);
                eventDescriptors.add(new EventDescriptor(cl, getID(cl)));
            }
        } else {
            for (String className : classNames) {
                Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(className);
                eventDescriptors.add(new EventDescriptor(cl, getID(cl)));
            }
        }
        return eventDescriptors;
    }

    private static Long getID(Class<?> eventClass) {
        Long id;
        try {
            Field field = eventClass.getField("ID");
            id = (Long) field.get(null);
        } catch (Exception e) {
            id = null;
        }
        return id;
    }
}
