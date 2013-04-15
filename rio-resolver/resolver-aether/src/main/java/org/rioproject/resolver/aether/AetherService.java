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

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManagerFactory;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.*;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.artifact.SubArtifact;
import org.sonatype.aether.util.filter.DependencyFilterUtils;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    private static final Logger logger = LoggerFactory.getLogger(AetherService.class);

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
            return new AetherService(repositorySystem, new LocalRepositoryWorkspaceReader());
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

        /*locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);*/

        locator.setService(LocalRepositoryManagerFactory.class, SimpleLocalRepositoryManagerFactory.class);
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
        return resolve(groupId, artifactId, "jar", null, version);
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
        return resolve(groupId, artifactId, extension, classifier, version, null);
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
        List<RemoteRepository> myRepositories;
        if(repositories==null || repositories.isEmpty())
            myRepositories = getRemoteRepositories();
        else
            myRepositories = repositories;

        setMirrorSelector(myRepositories);
        applyAuthentication(myRepositories);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(myRepositories);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, getDependencyFilter(artifact));
        dependencyRequest.setCollectRequest(collectRequest);

        try {
            List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(repositorySystemSession,
                                                                                        dependencyRequest).getArtifactResults();
            return new ResolutionResult(artifact, artifactResults);
        } catch(NullPointerException e) {
            /* catch, log and rethrow */
            logger.error("Trying to resolve {}, make sure that all parent poms are resolveable", artifact, e);
            throw e;
        }

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
        install(groupId, artifactId, version, null, pomFile, artifactFile);
    }

    /**
     * Installs a JAR and its POM to the local repository.
     *
     * @param groupId The group identifier of the artifact, may be {@code null}.
     * @param artifactId The artifact identifier of the artifact, may be {@code null}.
     * @param version The version of the artifact, may be {@code null}.
     * @param classifier The classifier of the artifact, may be {@code null}.
     * @param pomFile The pom File, can not be {@code null}.
     * @param artifactFile The file for the project's artifact, may be {@code null}. If null, the <i>type</i> of the
     * artifact is determined to be <code>.pom</code>. Otherwise the type is obtained from the
     * <code>artifactFile</code>'s extension
     *
     * @throws InstallationException if the requested installation is unsuccessful
     * @throws IllegalArgumentException if the groupId, artifactId, version or pomFile is null.
     */
    public void install(final String groupId, final String artifactId, final String version, final String classifier, final File pomFile, final File artifactFile)
        throws InstallationException {

        InstallRequest installRequest = new InstallRequest();
        if(artifactFile!=null) {
            String name = artifactFile.getName();
            String type = name.substring(artifactFile.getName().lastIndexOf(".")+1, name.length());
            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
            jarArtifact = jarArtifact.setFile(artifactFile);
            Artifact pomArtifact = new SubArtifact(jarArtifact, classifier, "pom");
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest = installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);
        } else {
            Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, classifier, "pom", version);
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest = installRequest.addArtifact(pomArtifact);
        }
        repositorySystem.install(repositorySystemSession, installRequest);
    }

    @SuppressWarnings("unused")
    public void deploy(final String groupId,
                       final String artifactId,
                       final String version,
                       final File artifactFile,
                       final File pomFile,
                       final String repositoryId,
                       final String repositoryURL) throws DeploymentException {

        deploy(groupId, artifactId, version, null, artifactFile, pomFile, repositoryId, repositoryURL);
    }

    @SuppressWarnings("unused")
    public void deploy(final String groupId,
                       final String artifactId,
                       final String version,
                       final String classifier,
                       final File artifactFile,
                       final File pomFile,
                       final String repositoryId,
                       final String repositoryURL) throws DeploymentException {

        DeployRequest deployRequest = new DeployRequest();
        if(artifactFile!=null) {
            String name = artifactFile.getName();
            String type = name.substring(artifactFile.getName().lastIndexOf(".")+1, name.length());
            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, classifier, type, version);
            jarArtifact = jarArtifact.setFile(artifactFile);
            Artifact pomArtifact = new SubArtifact(jarArtifact, classifier, "pom");
            pomArtifact = pomArtifact.setFile(pomFile);
            deployRequest = deployRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);
        } else {
            Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, classifier, "pom", version);
            pomArtifact = pomArtifact.setFile(pomFile);
            deployRequest = deployRequest.addArtifact(pomArtifact);
        }

        RemoteRepository repository = new RemoteRepository(repositoryId, "default", repositoryURL );
        setMirrorSelector(asList(repository));
        applyAuthentication(asList(repository));
        deployRequest.setRepository(repository);

        repositorySystem.deploy(repositorySystemSession, deployRequest);
    }

    /**
     * Get the {@code DependencyFilter} for an artifact
     *
     * @param a The artifact
     *
     * @return The {@code DependencyFilter} to use
     */
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
        return getLocation(artifactCoordinates, artifactExt, getRemoteRepositories());
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
        List<RemoteRepository> myRepositories;
        if(repositories==null || repositories.isEmpty())
            myRepositories = getRemoteRepositories();
        else
            myRepositories = repositories;

        if(logger.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            if(myRepositories!=null && myRepositories.size()>0) {
                for(RemoteRepository r : myRepositories) {
                    if(builder.length()>0)
                        builder.append(", ");
                    builder.append(r.getUrl());
                }
            } else {
                builder.append("<no provided repositories>");
            }
            logger.debug("Get location of {} using repositories {}", artifactCoordinates, builder.toString());
        }
        setMirrorSelector(myRepositories);
        applyAuthentication(myRepositories);

        DefaultArtifact a = new DefaultArtifact(artifactCoordinates);
        String extension = artifactExt==null? "jar":artifactExt;
        ArtifactRequest artifactRequest = new ArtifactRequest();
        DefaultArtifact artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), extension, a.getVersion());
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(myRepositories);
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
        return artifactResult.getArtifact().getFile().toURI().toURL();
    }

    /**
     * Get the {@code RemoteRepository} instances that are configured.
     *
     * @return An immutable {@code List} of {@code RemoteRepository} instances.
     */
    public List<RemoteRepository> getRemoteRepositories() {
        List<String> activeProfiles = effectiveSettings.getActiveProfiles();
        List<RemoteRepository> myRepositories = new ArrayList<RemoteRepository>();
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

        if(!alreadyHaveRepository(myRepositories, "central")) {
            RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
            myRepositories.add(central);
        }
        return Collections.unmodifiableList(myRepositories);
    }

    /**
     * Apply and authentication required for the provided repositories
     *
     * @param repositories The {@code List} of repositories
     */
    private void applyAuthentication(final List<RemoteRepository> repositories) {
        for(Server server : effectiveSettings.getServers()) {
            for(RemoteRepository remoteRepository : repositories) {
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
    }

    /**
     * Set the {@code MirrorSelector}  to the session if any mirrors have been declared
     *
     * @param repositories The {@code List} of repositories
     */
    private void setMirrorSelector(final List<RemoteRepository> repositories) {
        MirrorSelector mirrorSelector = getMirrorSelector(repositories);
        ((MavenRepositorySystemSession)repositorySystemSession).setMirrorSelector(mirrorSelector);
    }

    /**
     * Get the {@code MirrorSelector} to use.
     *
     * NOTE: This method is package-protected for testing purposes.
     *
     * @param repositories The {@code List} of repositories
     *
     * @return The {@code MirrorSelector} to use.
     */
    MirrorSelector getMirrorSelector(final List<RemoteRepository> repositories) {
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        List<Mirror> mirrors = effectiveSettings.getMirrors();
        if(!mirrors.isEmpty()) {

            List<RemoteRepository> repositoryMirrors = new ArrayList<RemoteRepository>();
            for (Mirror mirror : mirrors) {
                mirrorSelector.add(mirror.getId(),
                                   mirror.getUrl(),
                                   "default",
                                   false,
                                   mirror.getMirrorOf(),
                                   "*");
                repositoryMirrors.add(new RemoteRepository(mirror.getId(), "default", mirror.getUrl()));
            }

            for (RemoteRepository mirror : repositoryMirrors) {
                List<RemoteRepository> mirroredRepositories = new ArrayList<RemoteRepository>();
                for(RemoteRepository r : repositories) {
                    if(mirrorSelector.getMirror(r)!=null) {
                        mirroredRepositories.add(r);
                        r.setUrl(mirror.getUrl());
                    }
                }
                mirror.setMirroredRepositories(mirroredRepositories);
            }
        }
        return mirrorSelector;
    }

    /**
     * Utility to create a {@code RepositoryPolicy} from a {@code org.apache.maven.settings.RepositoryPolicy}
     *
     * @param r The org.apache.maven.settings.RepositoryPolicy to transform
     *
     * @return A transformed org.apache.maven.settings.RepositoryPolicy
     */
    private RepositoryPolicy createRepositoryPolicy(org.apache.maven.settings.RepositoryPolicy r) {
        boolean enabled = true;
        String updatePolicy = "";
        String checksumPolicy = "";
        if(r!=null) {
            enabled = r.isEnabled();
            checksumPolicy = r.getChecksumPolicy();
            updatePolicy = r.getUpdatePolicy();
        }
        return new RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }

    /**
     * Check if there is already a {@code RemoteRepository} in the {@code List} that has the same {@code id}.
     *
     * @param repositories {@code List} of
     * @param id The id to match
     *
     * @return {@code true} if the id is found, {@code false} otherwise.
     */
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

    /**
     * Utility to create a {@code List} of {@code RemoteRepository} from a single {@code RemoteRepository}.
     *
     * @param r the {@code RemoteRepository} to use
     *
     * @return A {@code List} of {@code RemoteRepository}
     */
    private List<RemoteRepository> asList(final RemoteRepository r) {
        List<RemoteRepository> list = new ArrayList<RemoteRepository>();
        list.add(r);
        return list;
    }
}
