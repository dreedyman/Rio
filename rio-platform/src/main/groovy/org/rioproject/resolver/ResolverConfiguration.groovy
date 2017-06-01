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
package org.rioproject.resolver

import org.rioproject.util.RioHome
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Load a Groovy configuration for the {@link org.rioproject.resolver.Resolver}. The
 * configuration file is loaded from with the location pointed to by the
 * "org.rioproject.resolver.config" system property, or the default of
 * ${rio.home}/config/resolverConfig.groovy
 *
 * @author Dennis Reedy
 */
class ResolverConfiguration {
    public static final String RESOLVER_CONFIG = "org.rioproject.resolver.config"
    public static final String RESOLVER_JAR = "org.rioproject.resolver.jar"
    private final URL resolverConfig
    static final Logger logger = LoggerFactory.getLogger(ResolverConfiguration.class)

    ResolverConfiguration() {
        if(System.properties[RESOLVER_CONFIG]==null) {
            resolverConfig = createFromFile("${RioHome.get()}/config/resolverConfig.groovy")
        } else {
            String configRef = System.getProperty(RESOLVER_CONFIG)
            if(configRef.contains("!/")) {
                int ndx = configRef.lastIndexOf("/")
                String resource = configRef.substring(ndx+1)
                logger.debug("Loading [{}] from context classloader {}",
                             resource, Thread.currentThread().getContextClassLoader())
                resolverConfig = Thread.currentThread().getContextClassLoader().getResource(resource)
            } else {
                resolverConfig = createFromFile(configRef)
            }
        }
        if(resolverConfig==null)
            logger.warn("The resolver configuration was not loaded {}, using empty configuration",
                        System.properties[RESOLVER_CONFIG])
        else
            logger.debug("Using resolver configuration ${resolverConfig.toExternalForm()}")
    }

    String getResolverJar() {
        String resolverJar = System.properties[RESOLVER_JAR]
        if(resolverJar==null && resolverConfig!=null) {
            def config = new ConfigSlurper().parse(resolverConfig)
            resolverJar = config.resolver.jar
        }
        return resolverJar
    }

    List<RemoteRepository> getRemoteRepositories() {
        def repositories = []
        if(resolverConfig!=null) {
            def config = new ConfigSlurper().parse(resolverConfig)
            config.resolver.repositories.remote.each { id, url ->
                RemoteRepository remoteRepository = new RemoteRepository()
                remoteRepository.setId(id as String)
                remoteRepository.setUrl(url as String)
                repositories << remoteRepository
            }
        }
        return repositories
    }

    List<File> getFlatDirectories() {
        def repositories = []
        if(resolverConfig!=null) {
            def config = new ConfigSlurper().parse(resolverConfig)
            config.resolver.repositories.flatDirs.each { repo ->
                repositories << repo
            }
        }
        return repositories

    }

    private static URL createFromFile(String filename) {
        URL url = null
        File configFileRef = new File(filename)
        if(configFileRef.exists())
            url = configFileRef.toURI().toURL()
        return url
    }
}