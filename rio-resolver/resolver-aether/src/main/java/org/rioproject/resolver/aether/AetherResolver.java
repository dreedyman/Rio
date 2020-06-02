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
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.*;
import org.rioproject.resolver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Uses Maven 3's native dependency resolution interface, Aether.
 *
 * @author Dennis Reedy
 */
public class AetherResolver implements Resolver, SettableResolver {
    protected AetherService service;
    private final Map<ResolutionRequest, Future<String[]>> resolvingMap = new ConcurrentHashMap<>();
    private final ExecutorService resolverExecutor;
    private final List<RemoteRepository> cachedRemoteRepositories = new ArrayList<>();
    private final FlatDirectoryReader flatDirectoryReader = new FlatDirectoryWorkspaceReader();
    private static final Logger logger = LoggerFactory.getLogger(AetherResolver.class.getName());

    public AetherResolver() {
        resolverExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        service = AetherService.getDefaultInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getClassPathFor(String artifact) throws ResolverException {
        String[] classPath;
        Future<String[]> future;
        ResolutionRequest request = new ResolutionRequest(artifact);
        synchronized (resolvingMap) {
            future = resolvingMap.get(request);
            if(future==null) {
                future = resolverExecutor.submit(new ResolvingRequestTask(request));
                resolvingMap.put(request, future);
                if(logger.isDebugEnabled()) {
                    logger.debug(String.format("Created and set new ResolvingTask for %s", artifact));
                }
            } else {
                request = getResolutionRequest(request);
            }
        }
        request.increment();
        try {
            classPath = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ResolverException(String.format("While trying to resolve %s", artifact), e);
        } finally {
            if(request.decrement()==0) {
                resolvingMap.remove(request);
            }
        }
        return classPath;
    }

    @Override
    public String[] getClassPathFor(String artifact, File pom, boolean download) throws ResolverException {
        return getClassPathFor(artifact);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getClassPathFor(String artifact, RemoteRepository[] repositories) throws ResolverException {
        String[] classPath;
        Future<String[]> future;
        ResolutionRequest request = new ResolutionRequest(artifact,
                                                          repositories);
        synchronized (resolvingMap) {
            future = resolvingMap.get(request);
            if(future==null) {
                future = resolverExecutor.submit(new ResolvingRequestTask(request));
                resolvingMap.put(request, future);
                if(logger.isDebugEnabled()) {
                    logger.debug("Created and set new ResolvingRequestTask for {} with repositories {}",
                                 artifact, repositories);
                }
            } else {
                request = getResolutionRequest(request);
            }
        }
        request.increment();
        try {
            classPath = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ResolverException(String.format("While trying to resolve %s", artifact), e);
        } finally {
            if(request.decrement()==0) {
                resolvingMap.remove(request);
            }
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
            location = getURLFromFlatDirs(artifact, artifactType);
            if(location==null)
                throw new ResolverException(String.format("Error locating %s: %s", artifact, e.getLocalizedMessage()));
        } catch (MalformedURLException e) {
            throw new ResolverException(String.format("Error creating URL for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        } catch (SettingsBuildingException e) {
            throw new ResolverException(String.format("Error loading settings for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        } catch (VersionRangeResolutionException e) {
            throw new ResolverException(String.format("Error resolving latest version for %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        }
        return location;
    }

    private URL getURLFromFlatDirs(String artifact, String artifactType) throws ResolverException {
        DefaultArtifact a = new DefaultArtifact(artifact);
        String extension = artifactType==null? "jar":artifactType;
        DefaultArtifact toResolve = new DefaultArtifact(a.getGroupId(),
                                                        a.getArtifactId(),
                                                        a.getClassifier(),
                                                        extension,
                                                        a.getVersion());
        ArtifactResult result = flatDirectoryReader.findArtifact(toResolve);
        if(result!=null) {
            try {
                return result.getArtifact().getFile().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new ResolverException(String.format("Error creating URL for resolved artifact %s: %s",
                                                          artifact, e.getLocalizedMessage()));
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getLocation(String artifact, String artifactType, RemoteRepository[] repositories) throws ResolverException {
        URL location;
        try {
            List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories =
                transformRemoteRepository(repositories);
            location = service.getLocation(artifact, artifactType, remoteRepositories);
        } catch (ArtifactResolutionException e) {
            location = getURLFromFlatDirs(artifact, artifactType);
            if(location==null)
                throw new ResolverException(String.format("Error locating %s: %s", artifact, e.getLocalizedMessage()));
        } catch (MalformedURLException e) {
            throw new ResolverException(String.format("Error creating URL for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        } catch (SettingsBuildingException e) {
            throw new ResolverException(String.format("Error loading settings for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        } catch (VersionRangeResolutionException e) {
            throw new ResolverException(String.format("Error resolving latest version for %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        }
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettableResolver setRemoteRepositories(Collection<RemoteRepository> repositories) {
        service.setConfiguredRepositories(transformRemoteRepository(repositories.toArray(new RemoteRepository[0])));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override public SettableResolver setFlatDirectories(Collection<File> directories) {
        flatDirectoryReader.addDirectories(directories);
        return this;
    }

    @Override
    public Collection<File> getFlatDirectories() {
        return flatDirectoryReader.getDirectories();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RemoteRepository> getRemoteRepositories() {
        List<org.eclipse.aether.repository.RemoteRepository> repos = service.getRemoteRepositories();
        List<RemoteRepository> remoteRepositories = new ArrayList<>();

        for(org.eclipse.aether.repository.RemoteRepository r : repos)
            remoteRepositories.add(transformAetherRemoteRepository(r));

        for(RemoteRepository rr : cachedRemoteRepositories) {
            if(!remoteRepositories.contains(rr))
                remoteRepositories.add(rr);
        }

        return remoteRepositories;
    }

    public AetherService getAetherService() {
        return service;
    }

    List<org.eclipse.aether.repository.RemoteRepository> transformRemoteRepository(RemoteRepository[] repositories) {
        if(repositories==null)
            throw new IllegalArgumentException("repositories must not be null");
        List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories = new ArrayList<>();
        for(RemoteRepository rr : repositories) {
            RepositoryPolicy releasePolicy = new RepositoryPolicy(true,
                                                                  rr.getReleaseUpdatePolicy(),
                                                                  rr.getReleaseChecksumPolicy());
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true,
                                                                   rr.getSnapshotUpdatePolicy(),
                                                                   rr.getSnapshotChecksumPolicy());

            org.eclipse.aether.repository.RemoteRepository.Builder repoBuilder =
                new org.eclipse.aether.repository.RemoteRepository.Builder(rr.getId(), "default", rr.getUrl());
            remoteRepositories.add(repoBuilder
                                       .setSnapshotPolicy(snapshotPolicy)
                                       .setReleasePolicy(releasePolicy)
                                       .build());
        }
        return remoteRepositories;
    }

    private String[] produceClassPathFromResolutionResult(ResolutionResult result) {
        List<String> classPath = new ArrayList<>();
        for (ArtifactResult artifactResult : result.getArtifactResults()) {
            if(artifactResult.getArtifact()==null) {
                logger.error("Unknown artifact for {}", artifactResult.getRequest().getArtifact());
            }
            if(logger.isDebugEnabled()) {
                if(artifactResult.getArtifact()!=null)
                    logger.debug("Adding classpath for artifact: {}, result: {}",
                                 artifactResult.getArtifact(), artifactResult.getArtifact().getFile());
                else {
                    logger.error("Adding classpath for artifact: {}, no file found", artifactResult);
                }
            }
            classPath.add(artifactResult.getArtifact().getFile().getAbsolutePath());
            ArtifactRepository r = artifactResult.getRepository();
            if(r instanceof org.eclipse.aether.repository.RemoteRepository) {
                RemoteRepository rr = transformAetherRemoteRepository((org.eclipse.aether.repository.RemoteRepository)r);
                if(!cachedRemoteRepositories.contains(rr))
                    cachedRemoteRepositories.add(rr);
            }

        }
        if(logger.isDebugEnabled())
            logResolutionResult(result);
        return classPath.toArray(new String[0]);
    }

    private void logResolutionResult(ResolutionResult result) {
        StringBuilder resolvedList = new StringBuilder();
        int artifactLength = getMaxArtifactStringLength(result.getArtifactResults());
        for (ArtifactResult artifactResult : result.getArtifactResults() ) {
            if(resolvedList.length()>0)
                resolvedList.append("\n");
            resolvedList.append("  ").append(String.format("%-"+artifactLength+"s", artifactResult.getArtifact()));
            resolvedList.append(" resolved to ").append(artifactResult.getArtifact().getFile());
        }
        String newLine = "";
        if(resolvedList.length()==0)
            resolvedList.append("  <No artifacts resolved>");
        else
            newLine = "\n";
        logger.debug(String.format("Artifact resolution for %s:%s", result.getArtifact(), newLine+resolvedList.toString()));
    }

    RemoteRepository transformAetherRemoteRepository(org.eclipse.aether.repository.RemoteRepository r) {
        RemoteRepository rr = new RemoteRepository();
        rr.setId(r.getId());
        rr.setUrl(r.getUrl());
        rr.setReleaseChecksumPolicy(r.getPolicy(false).getChecksumPolicy());
        rr.setReleaseUpdatePolicy(r.getPolicy(false).getUpdatePolicy());
        rr.setSnapshotChecksumPolicy(r.getPolicy(true).getChecksumPolicy());
        rr.setSnapshotUpdatePolicy(r.getPolicy(true).getUpdatePolicy());
        rr.setReleases(r.getPolicy(true).isEnabled());
        rr.setSnapshots(r.getPolicy(false).isEnabled());
        return rr;
    }

    private int getMaxArtifactStringLength(List<ArtifactResult> artifactResults) {
        int artifactLength = 0;
        for (ArtifactResult artifactResult : artifactResults ) {
            artifactLength = Math.max(artifactResult.getArtifact().toString().length(),
                                      artifactLength);
        }
        return artifactLength;
    }

    private ResolutionRequest getResolutionRequest(ResolutionRequest r) {
        ResolutionRequest request = null;
        for(Map.Entry<ResolutionRequest, Future<String[]>> entry : resolvingMap.entrySet()) {
            if(entry.getKey().equals(r)) {
                request = entry.getKey();
                break;
            }
        }
        return request==null?r : request;
    }

    /**
     * Asynchronous task for resolving an artifact
     */
    private class ResolvingRequestTask implements Callable<String[]> {
        private final ResolutionRequest request;

        private ResolvingRequestTask(ResolutionRequest request) {
            this.request = request;
        }

        public String[] call() throws ResolverException {
            String[] classPath;
            List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories = null;
            if(request.getRepositories()!=null) {
                remoteRepositories = transformRemoteRepository(request.getRepositories());
            }
            try {
                ResolutionResult result;
                if(remoteRepositories!=null) {
                    Artifact a = new Artifact(request.getArtifact());
                    result = service.resolve(a.getGroupId(),
                                             a.getArtifactId(),
                                             a.getType(),
                                             a.getClassifier(),
                                             a.getVersion(),
                                             remoteRepositories);
                } else {
                    DefaultArtifact a = new DefaultArtifact(request.getArtifact());
                    result = service.resolve(a.getGroupId(),
                                             a.getArtifactId(),
                                             a.getExtension(),
                                             a.getClassifier(),
                                             a.getVersion());
                }
                classPath = produceClassPathFromResolutionResult(result);
            } catch (DependencyCollectionException e) {
                CollectRequest collectRequest = e.getResult().getRequest();
                ArtifactResult artifactResult = null;
                if(collectRequest.getRoot()!=null && collectRequest.getRoot().getArtifact()!=null)
                    artifactResult = flatDirectoryReader.findArtifact(collectRequest.getRoot().getArtifact());
                if(artifactResult==null) {
                    throw new ResolverException("Encountered bad artifact descriptors, version ranges or " +
                                                "other issues during calculation of the dependency " +
                                                "graph",
                                                e);
                } else {
                    ResolutionResult result = new ResolutionResult()
                                                  .setArtifact(new DefaultArtifact(request.getArtifact()))
                                                  .addArtifactResult(artifactResult);
                    classPath = produceClassPathFromResolutionResult(result);
                }
            } catch (DependencyResolutionException e) {
                List<ArtifactResult> artifactResults = new ArrayList<>();
                Set<org.eclipse.aether.artifact.Artifact> flatDirArtifacts = new HashSet<>();
                for(ArtifactResult result : e.getResult().getArtifactResults()) {
                    if(result.isMissing()) {
                        flatDirArtifacts.add(result.getRequest().getArtifact());
                    } else {
                        artifactResults.add(result);
                    }
                }
                //artifactResults.addAll(e.getResult().getArtifactResults());
                for(Exception collectException : e.getResult().getCollectExceptions()) {
                    if(collectException instanceof ArtifactDescriptorException) {
                        flatDirArtifacts.add(((ArtifactDescriptorException)collectException).getResult().getArtifact());
                    }
                }
                int toResolveLocally =flatDirArtifacts.size();
                if(logger.isDebugEnabled())
                    logger.debug("Try and resolve {} artifacts using configured flatDirs", toResolveLocally);
                for(org.eclipse.aether.artifact.Artifact artifact : flatDirArtifacts) {
                    ArtifactResult artifactResult = flatDirectoryReader.findArtifact(artifact);
                    if (artifactResult != null) {
                        toResolveLocally--;
                        artifactResults.add(artifactResult);
                    }
                }

                if(logger.isDebugEnabled())
                    logger.debug("Number of unresolved artifact after flatDir check: {}", toResolveLocally);
                if(toResolveLocally==0) {
                    ResolutionResult result = new ResolutionResult(new DefaultArtifact(request.getArtifact()), artifactResults);
                    classPath = produceClassPathFromResolutionResult(result);
                } else {
                    throw new ResolverException(String.format("Could not download all transitive dependencies: %s",
                                                              e.getLocalizedMessage()));
                }
            } catch (VersionRangeResolutionException e) {
                throw new ResolverException(String.format("Error resolving latest version: %s",
                                                          e.getLocalizedMessage()), e);
            }
            return classPath;
        }

    }

    static class ResolutionRequest {
        private final String artifact;
        private  RemoteRepository[] repositories;
        private final AtomicInteger counter = new AtomicInteger(0);

        ResolutionRequest(String artifact) {
            this.artifact = artifact;
        }

        ResolutionRequest(String artifact, RemoteRepository[] repositories) {
            this.artifact = artifact;
            this.repositories = repositories;
        }

        String getArtifact() {
            return artifact;
        }

        RemoteRepository[] getRepositories() {
            return repositories;
        }

        void increment() {
            counter.incrementAndGet();
        }

        Integer decrement() {
            return counter.decrementAndGet();
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ResolutionRequest that = (ResolutionRequest) o;
            return artifact.equals(that.artifact) && Arrays.equals(repositories, that.repositories);
        }

        @Override
        public int hashCode() {
            int result = artifact.hashCode();
            result = 31 * result + (repositories != null ? Arrays.hashCode(repositories) : 0);
            return result;
        }
    }

}
