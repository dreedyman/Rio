/*
 * Copyright 2011 the original author or authors.
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
package org.rioproject.monitor.service;

import net.jini.admin.Administrable;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lookup.DiscoveryAdmin;
import org.rioproject.config.Constants;
import org.rioproject.impl.opstring.OpStringUtil;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * This class is used to verify that either jars declared in an OperationalString can be served, or if the
 * OperationalString is configured to use artifacts, that required artifacts have been resolved
 */
public class DeploymentVerifier {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentVerifier.class.getName());
    private final List<RemoteRepository> additionalRepositories = new ArrayList<RemoteRepository>();
    private final DiscoveryManagement discoveryManagement;

    public DeploymentVerifier(final Configuration config, final DiscoveryManagement discoveryManagement) {
        try {
            RemoteRepository[] remoteRepositories = (RemoteRepository[]) config.getEntry("org.rioproject.monitor",
                                                                                         "remoteRepositories",
                                                                                         RemoteRepository[].class,
                                                                                         new RemoteRepository[0]);
            if(remoteRepositories.length>0) {
                Collections.addAll(additionalRepositories, remoteRepositories);
                logger.debug("Configured {} additional repositories", additionalRepositories);
            }
        } catch (ConfigurationException e) {
            logger.warn("Getting RemoteRepositories", e);
        }
        this.discoveryManagement = discoveryManagement;
    }

    public void verifyDeploymentRequest(final DeployRequest request) throws ResolverException, IOException {
        for(OperationalString o : request.getOperationalStrings()) {
            verifyOperationalString(o, request.getRepositories());
        }
    }

    public void verifyOperationalString(final OperationalString opString, final RemoteRepository[] repositories)
        throws ResolverException, IOException {
        Resolver resolver = ResolverHelper.getResolver();
        for(ServiceElement service : opString.getServices()) {
            verifyOperationalStringService(service,
                                           resolver,
                                           mergeRepositories(repositories, service.getRemoteRepositories()));
        }
        for(OperationalString nested : opString.getNestedOperationalStrings())
            verifyOperationalString(nested, repositories);
    }

    public void verifyOperationalStringService(final ServiceElement service,
                                               final Resolver resolver,
                                               final RemoteRepository[] repositories)
        throws IOException, ResolverException {
        /* Check the component bundle for deployment as an artifact, easier check this way */
        if(service.getComponentBundle().getArtifact()!=null) {
            resolveOperationalStringService(service, resolver, repositories);
        } else {
            OpStringUtil.checkCodebase(service, System.getProperty(Constants.WEBSTER));
        }
        ensureGroups(service);
    }

    private void resolveOperationalStringService(final ServiceElement service,
                                                 final Resolver resolver,
                                                 final RemoteRepository[] repositories)
        throws ResolverException {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        boolean didResolve = false;
        for (ClassBundle export : service.getExportBundles()) {
            if(export.getArtifact()!=null) {
                sb.append(" (").append(export.getArtifact()).append("): ");
                resolve(export, resolver, repositories);
                didResolve = true;
            }
            for(String jar : export.getJARNames()) {
                sb1.append("\n");
                sb1.append(export.getCodebase()).append(jar);
            }
        }
        if(didResolve) {
            List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
            remoteRepositories.addAll(additionalRepositories);
            remoteRepositories.addAll(resolver.getRemoteRepositories());
            service.setRemoteRepositories(remoteRepositories);
        }
        sb.append(sb1.toString());
        logger.debug("{} derived classpath for loading artifact {}", service.getName(), sb.toString());
    }

    private void resolve(final ClassBundle bundle,
                         final Resolver resolver,
                         final RemoteRepository[] repositories) throws ResolverException {
        logger.trace("Artifact: {}, resolver: {}", bundle.getArtifact(), resolver.getClass().getName());
        String artifact = bundle.getArtifact();
        if (artifact != null) {
            List<String> jars = new ArrayList<String>();
            String[] artifactParts = artifact.split(" ");
            for(String artifactPart : artifactParts) {
                String[] classPath = resolver.getClassPathFor(artifactPart, repositories);
                for (String jar : classPath) {
                    jar = ResolverHelper.handleWindows(jar);
                    if(!jars.contains(jar))
                        jars.add(jar);
                }
            }
            bundle.setCodebase("file://");
            bundle.setJARs(jars.toArray(new String[jars.size()]));
        }
    }

    RemoteRepository[] mergeRepositories(final RemoteRepository[] r1, final RemoteRepository[] r2) {
        Set<RemoteRepository> remoteRepositories = new HashSet<RemoteRepository>();
        Collections.addAll(remoteRepositories, r1);
        for(RemoteRepository r : r2) {
            remoteRepositories.add(r);
        }
        return remoteRepositories.toArray(new RemoteRepository[remoteRepositories.size()]);
    }

    void ensureGroups(final ServiceElement serviceElement) throws IOException {
        if(serviceElement.getServiceBeanConfig().getGroups()==DiscoveryGroupManagement.ALL_GROUPS) {
            throw new IOException(String.format("Service %s has been declared for ALL_GROUPS", serviceElement.getName()));
        }
        for(ServiceRegistrar registrar : discoveryManagement.getRegistrars()) {
            try {
                List<String> toAdd = new ArrayList<String>();
                DiscoveryAdmin admin = (DiscoveryAdmin) ((Administrable)registrar).getAdmin();
                String[] knownGroups = admin.getMemberGroups();
                for(String serviceGroup : serviceElement.getServiceBeanConfig().getGroups()) {
                    boolean found = false;
                    for(String known : knownGroups) {
                        if(serviceGroup.equals(known)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        toAdd.add(serviceGroup);
                    }
                }
                if(!toAdd.isEmpty()) {
                    admin.addMemberGroups(toAdd.toArray(new String[toAdd.size()]));
                    if(logger.isDebugEnabled()) {
                        logger.debug("Added {} to ServiceRegistrar at {}:{}",
                                     toAdd, registrar.getLocator().getHost(), registrar.getLocator().getPort());
                    }
                }

            } catch (RemoteException e) {
                logger.warn("While trying to ensure groups", e);
            }
        }
    }
}
