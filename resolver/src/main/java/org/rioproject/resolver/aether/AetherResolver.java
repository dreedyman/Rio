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
package org.rioproject.resolver.aether;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.aether.util.ConsoleDependencyGraphDumper;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses Maven 3's native dependency resolution interface, Aether.
 */
public class AetherResolver implements Resolver {
    private AetherService service = AetherService.getDefaultInstance();
    private static Logger logger = Logger.getLogger(AetherResolver.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getClassPathFor(String artifact) throws ResolverException {
        String[] classPath;
        try {
            Artifact a = new Artifact(artifact);
            ResolutionResult result = service.resolve(a.getGroupId(),
                                                      a.getArtifactId(),
                                                      "jar",
                                                      a.getClassifier(),
                                                      a.getVersion());
            classPath = produceClassPathFromResolutionResult(result);
        } catch (RepositoryException e) {
            throw new ResolverException("While trying to resolve "+artifact, e);
        } catch (SettingsBuildingException e) {
            throw new ResolverException("Error reading local Maven configuration", e);
        }
        return classPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getClassPathFor(String artifact, RemoteRepository[] repositories) throws ResolverException {
        String[] classPath;
        List<org.sonatype.aether.repository.RemoteRepository> remoteRepositories =
            transformRemoteRepository(repositories);
        try {
            Artifact a = new Artifact(artifact);
            ResolutionResult result = service.resolve(a.getGroupId(),
                                                      a.getArtifactId(),
                                                      "jar",
                                                      a.getClassifier(),
                                                      a.getVersion(),
                                                      remoteRepositories);
            classPath = produceClassPathFromResolutionResult(result);
        } catch (RepositoryException e) {
            throw new ResolverException("While trying to resolve "+artifact, e);
        } catch (SettingsBuildingException e) {
            throw new ResolverException("Error reading local Maven configuration", e);
        }
        return classPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getLocation(String artifact, String artifactType) throws ResolverException {
        URL location;
        try {
            location = service.getLocation(artifact, artifactType);
        } catch (ArtifactResolutionException e) {
            throw new ResolverException("Error locating "+artifact, e);
        } catch (MalformedURLException e) {
            throw new ResolverException("Error creating URL for resolved artifact "+artifact, e);
        }
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RemoteRepository> getRemoteRepositories() {
        List<org.sonatype.aether.repository.RemoteRepository> repos = service.getRemoteRepositories();
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        for(org.sonatype.aether.repository.RemoteRepository r : repos) {
            RemoteRepository rr = new RemoteRepository();
            rr.setId(r.getId());
            rr.setUrl(r.getUrl());
            rr.setReleaseChecksumPolicy(r.getPolicy(false).getChecksumPolicy());
            rr.setSnapshotChecksumPolicy(r.getPolicy(true).getChecksumPolicy());
            rr.setReleases(r.getPolicy(true).isEnabled());
            rr.setSnapshots(r.getPolicy(false).isEnabled());
            remoteRepositories.add(rr);
        }
        return remoteRepositories;
    }

    protected AetherService getAetherService() {
        return service;
    }

    protected List<org.sonatype.aether.repository.RemoteRepository> transformRemoteRepository(RemoteRepository[] repositories) {
        if(repositories==null)
            throw new IllegalArgumentException("repositories must not be null");
        List<org.sonatype.aether.repository.RemoteRepository> remoteRepositories =
            new ArrayList<org.sonatype.aether.repository.RemoteRepository>();
        for(RemoteRepository rr : repositories) {
            remoteRepositories.add(new org.sonatype.aether.repository.RemoteRepository(rr.getId(),
                                                                                       "default",
                                                                                       rr.getUrl()));
        }
        return remoteRepositories;
    }

    protected String[] produceClassPathFromResolutionResult(ResolutionResult result) {
        if(logger.isLoggable(Level.FINE)) {
            ConsoleDependencyGraphDumper graphDumper = new ConsoleDependencyGraphDumper();
            graphDumper.setFilter(service.getDependencyFilter(result.getArtifact()));
            result.getRoot().accept(graphDumper);
        }
        List<String> classPath = new ArrayList<String>();
        for (ArtifactResult artifactResult : result.getArtifactResults()) {
            classPath.add(artifactResult.getArtifact().getFile().getAbsolutePath());
        }
        logResolutionResult(result);
        return classPath.toArray(new String[classPath.size()]);
    }

    protected void logResolutionResult(ResolutionResult result) {
        StringBuilder resolvedList = new StringBuilder();
        int artifactLength = getMaxArtifactStringLength(result.getArtifactResults());
        for (ArtifactResult artifactResult : result.getArtifactResults() ) {
            if(resolvedList.length()>0)
                resolvedList.append("\n");
            resolvedList.append("  ").append(String.format("%-"+artifactLength+"s", artifactResult.getArtifact()));
            resolvedList.append(" resolved to ").append(artifactResult.getArtifact().getFile());
        }
        System.err.println("[INFO] Artifact resolution for "+result.getArtifact()+": \n"+resolvedList);
    }

    private int getMaxArtifactStringLength(List<ArtifactResult> artifactResults) {
        int artifactLength = 0;
        for (ArtifactResult artifactResult : artifactResults ) {
            artifactLength = artifactResult.getArtifact().toString().length()>artifactLength?
                             artifactResult.getArtifact().toString().length():artifactLength;
        }
        return artifactLength;
    }

}
