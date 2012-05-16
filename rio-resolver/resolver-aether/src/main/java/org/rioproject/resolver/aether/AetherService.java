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

import org.apache.maven.repository.internal.*;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.rioproject.resolver.aether.filters.ClassifierFilter;
import org.rioproject.resolver.aether.filters.ExcludePlatformFilter;
import org.rioproject.resolver.aether.util.ConsoleRepositoryListener;
import org.rioproject.resolver.aether.util.ConsoleTransferListener;
import org.rioproject.resolver.aether.util.SettingsUtil;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.impl.*;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.*;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.artifact.SubArtifact;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use Maven 3's Aether API for Maven dependency resolution.
 *
 * @author Dennis Reedy
 */
public final class AetherService {
    private final RepositorySystemSession repositorySystemSession;
    private final RepositorySystem repositorySystem;
    private Settings effectiveSettings;
    private String dependencyFilterScope;
    private final Collection<DependencyFilter> dependencyFilters =
        Collections.synchronizedCollection(new ArrayList<DependencyFilter>());
    private static final Logger logger = Logger.getLogger(AetherService.class.getName());

    private AetherService(final RepositorySystem repositorySystem, final WorkspaceReader workspaceReader) throws SettingsBuildingException {
        this.repositorySystem = repositorySystem;
        this.effectiveSettings = SettingsUtil.getSettings();
        this.repositorySystemSession = newSession(repositorySystem,
                                                  workspaceReader,
                                                  SettingsUtil.getLocalRepositoryLocation(effectiveSettings));
    }

    public static AetherService getDefaultInstance() {
        try {
            RepositorySystem repositorySystem = newRepositorySystem();
            return new AetherService(repositorySystem, null);
        } catch (SettingsBuildingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static AetherService getInstance(final WorkspaceReader workspaceReader) {
        try {
            RepositorySystem repositorySystem = newRepositorySystem();
            return new AetherService(repositorySystem, workspaceReader);
        } catch (SettingsBuildingException e) {
            throw new IllegalStateException(e);
        }
    }

    public RepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    public void setDependencyFilterScope(final String dependencyFilterScope) {
        this.dependencyFilterScope = dependencyFilterScope;
    }

    public void addDependencyFilter(final DependencyFilter filter) {
        dependencyFilters.add(filter);
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(final RepositorySystem system,
                                               final WorkspaceReader workspaceReader,
                                               final String repositoryLocation)
        throws SettingsBuildingException {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());
        if(workspaceReader!=null)
            session.setWorkspaceReader(workspaceReader);
        LocalRepository localRepository = new LocalRepository(repositoryLocation);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepository));
        return session;
    }

    /**
     * Resolve an artifact with the specified coordinates.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     *
     * @return A <code>ResolutionResult</code> for the artifact with the specified coordinates.
     *
     * @throws DependencyCollectionException If errors are encountered creating the collection of dependencies
     * @throws DependencyResolutionException If errors are encountered resolving dependencies
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public ResolutionResult resolve(final String groupId, final String artifactId, final String version)
        throws DependencyCollectionException, DependencyResolutionException, SettingsBuildingException {
        return resolve(groupId, artifactId, "jar", version);
    }

    /**
     * Resolve an artifact with the specified coordinates.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param extension The file extension of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     *
     * @return A <code>ResolutionResult</code> for the artifact with the specified coordinates.
     *
     * @throws DependencyCollectionException If errors are encountered creating the collection of dependencies
     * @throws DependencyResolutionException If errors are encountered resolving dependencies
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public ResolutionResult resolve(final String groupId, final String artifactId, final String extension, final String version)
        throws DependencyCollectionException, DependencyResolutionException, SettingsBuildingException {
        return resolve(groupId, artifactId, extension, null, version);

    }

    /**
     * Resolve an artifact with the specified coordinates.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param extension The file extension of the artifact, may be {@code null}.
     * @param classifier The classifier of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     *
     * @return A <code>ResolutionResult</code> for the artifact with the specified coordinates.
     *
     * @throws DependencyCollectionException If errors are encountered creating the collection of dependencies
     * @throws DependencyResolutionException If errors are encountered resolving dependencies
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public ResolutionResult resolve(final String groupId,
                                    final String artifactId,
                                    final String extension,
                                    final String classifier,
                                    final String version) throws DependencyCollectionException,
                                                                 DependencyResolutionException,
                                                                 SettingsBuildingException {
        return resolve(groupId, artifactId, extension, classifier, version, getRemoteRepositories(null));
    }

    /**
     * Resolve an artifact with the specified coordinates.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param extension The file extension of the artifact, may be {@code null}.
     * @param classifier The classifier of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     * @param repositories A collection of repositories to use when resolving the artifact,
     * may be {@code null}. If {@code null}, the repositories will be determined by reading the settings
     *
     * @return A <code>ResolutionResult</code> for the artifact with the specified coordinates.
     *
     * @throws DependencyCollectionException If errors are encountered creating the collection of dependencies
     * @throws DependencyResolutionException If errors are encountered resolving dependencies
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public ResolutionResult resolve(final String groupId,
                                    final String artifactId,
                                    final String extension,
                                    final String classifier,
                                    final String version,
                                    final List<RemoteRepository> repositories) throws DependencyCollectionException,
                                                                                      DependencyResolutionException,
                                                                                      SettingsBuildingException {

        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        Dependency dependency = new Dependency(artifact, JavaScopes.COMPILE);
        List<RemoteRepository> myRepositories = getRemoteRepositories(repositories);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(myRepositories);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, getDependencyFilter(artifact));
        dependencyRequest.setCollectRequest(collectRequest);

        List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(repositorySystemSession,
                                                                                    dependencyRequest).getArtifactResults();
        return new ResolutionResult(artifact, artifactResults);
    }

    /**
     * Installs a JAR and its POM to the local repository.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     * @param pomFile The pom File, can not be {@code null}.
     * @param artifactFile The file for the project's artifact, may be {@code null}. If null, the <i>type</i> of the
     * artifact is determined to be <code>.pom</code>. Otherwise the type is obtained from the
     * <code>artifactFile</code>'s extension
     *
     * @throws InstallationException if the requested installation is unsuccessful
     * @throws IllegalArgumentException if the groupId, artifactId, version or pomFile is null.
     */
    public void install(final String groupId, final String artifactId, final String version, final File pomFile, final File artifactFile)
        throws InstallationException {

        InstallRequest installRequest = new InstallRequest();
        if(artifactFile!=null) {
            String name = artifactFile.getName();
            String type = name.substring(artifactFile.getName().lastIndexOf(".")+1, name.length());
            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "", type, version);
            jarArtifact = jarArtifact.setFile(artifactFile);
            Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest = installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);
        } else {
            Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest = installRequest.addArtifact(pomArtifact);
        }
        repositorySystem.install(repositorySystemSession, installRequest);
    }

    protected DependencyFilter getDependencyFilter(final Artifact a) {
        Collection<DependencyFilter> filters = new ArrayList<DependencyFilter>();
        if(a.getClassifier()!=null && a.getClassifier().equals("dl"))
            filters.add(new ClassifierFilter(a.getClassifier()));
        else
            filters.add(new ExcludePlatformFilter());
        filters.add(DependencyFilterUtils.classpathFilter(dependencyFilterScope==null?
                                                          JavaScopes.COMPILE:dependencyFilterScope));
        for(DependencyFilter filter : dependencyFilters)
            filters.add(filter);
        return DependencyFilterUtils.andFilter(filters);
    }

    /**
     * Determine the location of an artifact given its coordinates.
     *
     * @param artifactCoordinates maven artifact coordinate string
     * @param artifactExt The extension of the artifact. If null, jar is used.
     *
     * @return The location of the artifact
     *
     * @throws ArtifactResolutionException if the artifact cannot be resolved
     * @throws MalformedURLException if the resolved artifact cannot be converted to a URL
     */
    public URL getLocation(final String artifactCoordinates, final String artifactExt) throws ArtifactResolutionException,
                                                                                              MalformedURLException {
        return getLocation(artifactCoordinates, artifactExt, getRemoteRepositories(null));
    }

    /**
     * Determine the location of an artifact given its coordinates.
     *
     * @param artifactCoordinates maven artifact coordinate string
     * @param artifactExt The extension of the artifact. If null, jar is used.
     * @param repositories A collection of repositories to use when resolving the artifact,
     * may be {@code null}. If {@code null}, the repositories will be determined by reading the settings
     *
     * @return The location of the artifact
     *
     * @throws ArtifactResolutionException if the artifact cannot be resolved
     * @throws MalformedURLException if the resolved artifact cannot be converted to a URL
     */
    public URL getLocation(final String artifactCoordinates,
                           final String artifactExt,
                           final List<RemoteRepository> repositories) throws ArtifactResolutionException,
                                                                             MalformedURLException {
        DefaultArtifact a = new DefaultArtifact(artifactCoordinates);
        String extension = artifactExt==null? "jar":artifactExt;
        ArtifactRequest artifactRequest = new ArtifactRequest();
        DefaultArtifact artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), extension, a.getVersion());
        artifactRequest.setArtifact(artifact);
        List<RemoteRepository> myRepositories = getRemoteRepositories(repositories);
        artifactRequest.setRepositories(myRepositories);
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
        return artifactResult.getArtifact().getFile().toURI().toURL();
    }

    public List<RemoteRepository> getRemoteRepositories(final List<RemoteRepository> repositories) {
        List<String> activeProfiles = effectiveSettings.getActiveProfiles();
        List<RemoteRepository> myRepositories = new ArrayList<RemoteRepository>();
        if(repositories!=null)
            myRepositories.addAll(repositories);
        //boolean haveCentral = false;
        for(String activeProfile : activeProfiles) {
            for(Profile profile : effectiveSettings.getProfiles()) {
                if(profile.getId().equals(activeProfile)) {
                    for(org.apache.maven.settings.Repository r : profile.getRepositories()) {
                        if(!alreadyHaveRepository(myRepositories, r.getId())) {
                            RemoteRepository remoteRepository = new RemoteRepository(r.getId(), "default", r.getUrl());
                            RepositoryPolicy snapShotPolicy = createRepositoryPolicy(r.getSnapshots());
                            RepositoryPolicy releasesPolicy = createRepositoryPolicy(r.getReleases());

                            remoteRepository.setPolicy(true, snapShotPolicy);
                            remoteRepository.setPolicy(false, releasesPolicy);
                            myRepositories.add(remoteRepository);
                        }
                    }
                    break;
                }
            }
        }

        if(!myRepositories.isEmpty()) {
            RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
            List<Mirror> mirrors = effectiveSettings.getMirrors();
            for (Mirror mirror : mirrors) {
                if (mirror.getMirrorOf().equals("*") || mirror.getMirrorOf().equals("central")) {
                    if(logger.isLoggable(Level.CONFIG))
                        logger.config(String.format("Using mirror for central: %s", mirror.getUrl()));
                    central = new RemoteRepository("central", "default", mirror.getUrl());
                }
            }
            if(!alreadyHaveRepository(myRepositories, "central"))
                myRepositories.add(central);
        }
        for(Server server : effectiveSettings.getServers()) {
            for(RemoteRepository remoteRepository : myRepositories) {
                if(server.getId().equals(remoteRepository.getId())) {
                    if(server.getUsername()!=null) {
                        Authentication authentication = new Authentication(server.getUsername(),
                                                                           server.getPassword(),
                                                                           server.getPrivateKey(),
                                                                           server.getPassphrase());
                        remoteRepository.setAuthentication(authentication);
                    }
                }
            }
        }
        if(logger.isLoggable(Level.FINE))
            logger.fine(String.format("Repositories %s", myRepositories));
        return myRepositories;
    }

    private RepositoryPolicy createRepositoryPolicy(org.apache.maven.settings.RepositoryPolicy r) {
        boolean enabled = true;
        String updatePolicy = "";
        String checksumPolicy = "";
        if(r!=null) {
            enabled = r.isEnabled();
            checksumPolicy = r.getUpdatePolicy();
            updatePolicy = r.getChecksumPolicy();
        }
        return new RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }

    private boolean alreadyHaveRepository(List<RemoteRepository> repositories, String id) {
        boolean hasRepository = false;
        for(RemoteRepository r : repositories) {
            if(id.equals(r.getId())) {
                hasRepository = true;
                break;
            }
        }
        return hasRepository;
    }
}
