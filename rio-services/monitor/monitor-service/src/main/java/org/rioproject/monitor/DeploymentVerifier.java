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
package org.rioproject.monitor;

import org.rioproject.config.Constants;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.*;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to verify that either jars declared in an OperationalString can be served, or if the
 * OperationalString is configured to use artifacts, that required artifacts have been resolved
 */
public class DeploymentVerifier {
    static Logger logger = Logger.getLogger(DeploymentVerifier.class.getName());

    public void verifyDeploymentRequest(DeployRequest request) throws ResolverException, IOException {
        for(OperationalString o : request.getOperationalStrings()) {
            verifyOperationalString(o, request.getRepositories());
        }
    }

    public void verifyOperationalString(OperationalString opString, RemoteRepository[] repositories)
        throws ResolverException, IOException {
        Resolver resolver = ResolverHelper.getResolver();
        for(ServiceElement service : opString.getServices()) {
            verifyOperationalStringService(service, resolver, repositories);
        }
        for(OperationalString nested : opString.getNestedOperationalStrings())
            verifyOperationalString(nested, repositories);
    }

    void verifyOperationalStringService(ServiceElement service, Resolver resolver, RemoteRepository[] repositories)
        throws IOException, ResolverException {
        /* Check the component bundle for deployment as an artifact, easier check this way */
        if(service.getComponentBundle().getArtifact()!=null) {
            resolveOperationalStringService(service, resolver, repositories);
        } else {
            OpStringUtil.checkCodebase(service, System.getProperty(Constants.CODESERVER));
        }
    }

    void resolveOperationalStringService(ServiceElement service, Resolver resolver, RemoteRepository[] repositories)
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
                if(sb1.length()>0)
                    sb1.append(", ");
                else
                    sb1.append("\n");
                sb1.append(export.getCodebase()).append(jar);
            }
        }
        if(didResolve)
            service.setRemoteRepositories(resolver.getRemoteRepositories());
        sb.append(sb1.toString());
        if (logger.isLoggable(Level.INFO)) {
            logger.info(service.getName()+" derived classpath for loading artifact "+sb.toString());
        }
    }

    void resolve(ClassBundle bundle, Resolver resolver, RemoteRepository[] repositories) throws ResolverException {
        if(logger.isLoggable(Level.FINE))
            logger.fine("Artifact: "+bundle.getArtifact()+", resolver: "+resolver.getClass().getName());
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
            if(logger.isLoggable(Level.FINE))
                logger.fine("Artifact: "+bundle.getArtifact()+", resolved jars "+jars);
            bundle.setCodebase("file://");
            bundle.setJARs(jars.toArray(new String[jars.size()]));
        }
    }
}