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
    public static final String RESOLVER_JAR = "org.rioproject.resolver.jar";
    private final resolverConfig
    static final Logger logger = LoggerFactory.getLogger(ResolverConfiguration.class)

    ResolverConfiguration() {
        if(System.properties[RESOLVER_CONFIG]==null) {
            resolverConfig = new File("${RioHome.get()}/config/resolverConfig.groovy")
        } else {
            resolverConfig = new File(System.properties[RESOLVER_CONFIG])
        }
        if(!resolverConfig.exists())
            logger.warn("The resolver configuration file does not exist {}, will", resolverConfig.path)
        else
            logger.info("Using resolver configuration ${resolverConfig}")
    }

    String getResolverJar() {
        String resolverJar = System.properties[RESOLVER_JAR]
        if(resolverJar==null && resolverConfig.exists()) {
            def config = new ConfigSlurper().parse(resolverConfig.toURI().toURL())
            resolverJar = config.resolver.jar
        }
        return resolverJar
    }

    List<RemoteRepository> getRemoteRepositories() {
        def repositories = []
        if(resolverConfig.exists()) {
            def config = new ConfigSlurper().parse(resolverConfig.toURI().toURL())
            config.resolver.repositories.each { id, url ->
                RemoteRepository remoteRepository = new RemoteRepository()
                remoteRepository.id = id
                remoteRepository.url = url
                repositories << remoteRepository
            }
        }
        return repositories
    }
}