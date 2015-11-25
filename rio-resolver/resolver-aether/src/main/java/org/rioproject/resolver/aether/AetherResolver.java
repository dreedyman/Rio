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
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
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
    private final Map<ResolutionRequest, Future<String[]>> resolvingMap = new ConcurrentHashMap<ResolutionRequest, Future<String[]>>();
    private final ExecutorService resolverExecutor;
    private final List<RemoteRepository> cachedRemoteRepositories = new ArrayList<RemoteRepository>();
    private final FlatDirectoryReader flatDirectoryReader = new FlatDirectoryWorkspaceReader();
    private static final Logger logger = LoggerFactory.getLogger(AetherResolver.class.getName());

    public AetherResolver() {
        resolverExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setDaemon(true);
                return thread;
            }
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
        } catch (InterruptedException e) {
            throw new ResolverException(String.format("While trying to resolve %s", artifact), e);
        } catch (ExecutionException e) {
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
        ResolutionRequest request = new ResolutionRequest(artifact, repositories);
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
        } catch (InterruptedException e) {
            throw new ResolverException(String.format("While trying to resolve %s", artifact), e);
        } catch (ExecutionException e) {
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
            throw new ResolverException(String.format("Error locating %s: %s", artifact, e.getLocalizedMessage()));
        } catch (MalformedURLException e) {
            throw new ResolverException(String.format("Error creating URL for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        } catch (SettingsBuildingException e) {
            throw new ResolverException(String.format("Error loading settings for resolved artifact %s: %s",
                                                      artifact, e.getLocalizedMessage()));
        }
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettableResolver setRemoteRepositories(Collection<RemoteRepository> repositories) {
        service.setConfiguredRepositories(transformRemoteRepository(repositories.toArray(new RemoteRepository[repositories.size()])));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override public SettableResolver setFlatDirectories(Collection<File> directories) {
        flatDirectoryReader.addDirectories(directories);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<RemoteRepository> getRemoteRepositories() {
        List<org.eclipse.aether.repository.RemoteRepository> repos = service.getRemoteRepositories();
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();

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

    protected List<org.eclipse.aether.repository.RemoteRepository> transformRemoteRepository(RemoteRepository[] repositories) {
        if(repositories==null)
            throw new IllegalArgumentException("repositories must not be null");
        List<org.eclipse.aether.repository.RemoteRepository> remoteRepositories =
            new ArrayList<org.eclipse.aether.repository.RemoteRepository>();
        for(RemoteRepository rr : repositories) {
            RepositoryPolicy releasePolicy = new RepositoryPolicy(true,
                                                                  rr.getReleaseUpdatePolicy(),
                                                                  rr.getReleaseChecksumPolicy());
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true,
                                                                   rr.getSnapshotUpdatePolicy(),
                                                                   rr.getSnapshotChecksumPolicy());

            org.eclipse.aether.repository.RemoteRepository.Builder repoBuilder =
                new org.eclipse.aether.repository.RemoteRepository.Builder(rr.getId(), "default", rr.getUrl());
            repoBuilder.setSnapshotPolicy(snapshotPolicy);
            repoBuilder.setReleasePolicy(releasePolicy);

            remoteRepositories.add(repoBuilder.build());
        }
        return remoteRepositories;
    }

    protected String[] produceClassPathFromResolutionResult(ResolutionResult result) {
        List<String> classPath = new ArrayList<String>();
        for (ArtifactResult artifactResult : result.getArtifactResults()) {
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
        String newLine = "";
        if(resolvedList.length()==0)
            resolvedList.append("  <No artifacts resolved>");
        else
            newLine = "\n";
        logger.debug(String.format("Artifact resolution for %s:%s", result.getArtifact(), newLine+resolvedList.toString()));
    }

    protected RemoteRepository transformAetherRemoteRepository(org.eclipse.aether.repository.RemoteRepository r) {
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
            artifactLength = artifactResult.getArtifact().toString().length()>artifactLength?
                             artifactResult.getArtifact().toString().length():artifactLength;
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
        private ResolutionRequest request;

        private ResolvingRequestTask(ResolutionRequest request) {
            this.request = request;
        }

        public String[] call() throws ResolverException {
            String[] classPath = null;
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
            } catch (SettingsBuildingException e) {
                throw new ResolverException(String.format("Error reading local Maven configuration: %s",
                                                          e.getLocalizedMessage()), e);
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
                List<ArtifactResult> artifactResults = new ArrayList<ArtifactResult>();
                artifactResults.addAll(e.getResult().getArtifactResults());
                int resolvedLocally = e.getResult().getCollectExceptions().size();
                for(Exception collectException : e.getResult().getCollectExceptions()) {
                    if(collectException instanceof ArtifactDescriptorException) {
                        ArtifactDescriptorException ade = (ArtifactDescriptorException)collectException;
                        ArtifactResult artifactResult = flatDirectoryReader.findArtifact(ade.getResult()
                                                                                             .getArtifact());
                        if (artifactResult != null) {
                            resolvedLocally--;
                            artifactResults.add(artifactResult);
                        }
                    }
                }
                if(resolvedLocally==0) {
                    ResolutionResult result = new ResolutionResult(new DefaultArtifact(request.getArtifact()), artifactResults);
                    classPath = produceClassPathFromResolutionResult(result);
                } else {
                    throw new ResolverException(String.format("Could not download all transitive dependencies: %s",
                                                              e.getLocalizedMessage()));
                }
            }
            return classPath;
        }
    }

    class ResolutionRequest {
        private String artifact;
        private  RemoteRepository[] repositories;
        private AtomicInteger counter = new AtomicInteger(0);

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
