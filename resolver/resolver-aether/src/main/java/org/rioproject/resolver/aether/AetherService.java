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

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.rioproject.resolver.aether.filters.ClassifierFilter;
import org.rioproject.resolver.aether.filters.ExcludePlatformFilter;
import org.rioproject.resolver.aether.util.ConsoleRepositoryListener;
import org.rioproject.resolver.aether.util.ConsoleTransferListener;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
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

/**
 * Use Maven 3's Aether API for Maven dependency resolution.
 */
public class AetherService {
    private final RepositorySystemSession repositorySystemSession;
    private final RepositorySystem repositorySystem;
    private Settings effectiveSettings;
    private String dependencyFilterScope;
    private final Collection<DependencyFilter> dependencyFilters =
        Collections.synchronizedCollection(new ArrayList<DependencyFilter>());

    private AetherService(RepositorySystem repositorySystem) throws SettingsBuildingException {
        this.repositorySystem = repositorySystem;
        this.effectiveSettings = getSettings();
        this.repositorySystemSession = newSession(repositorySystem, getLocalRepositoryLocation());
    }

    public static AetherService getDefaultInstance() {
        try {
            RepositorySystem repositorySystem = newRepositorySystem();
            return new AetherService(repositorySystem);
        } catch (SettingsBuildingException e) {
            throw new IllegalStateException(e);
        }
    }

    public RepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    public void setDependencyFilterScope(String dependencyFilterScope) {
        this.dependencyFilterScope = dependencyFilterScope;
    }

    public void addDependencyFilter(DependencyFilter filter) {
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

    private RepositorySystemSession newSession(RepositorySystem system, String repositoryLocation) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());
        LocalRepository localRepository = new LocalRepository(repositoryLocation);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepository));
        return session;
    }

    /**
     * Determine the local repository path, honoring any custom setting in the user's maven settings.xml.
     * Defaults to <code>${user.home}/.m2/repository</code> (which is the maven default).
     *
     * @return The location of he local repository
     */
    public String getLocalRepositoryLocation() {
        String localRepositoryLocation = effectiveSettings.getLocalRepository();
        if (localRepositoryLocation == null) {

            localRepositoryLocation = System.getProperty("user.home") +
                                      File.separator +
                                      ".m2" +
                                      File.separator +
                                      "repository";
        }
        return localRepositoryLocation;
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
    public ResolutionResult resolve(String groupId, String artifactId, String version)
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
    public ResolutionResult resolve(String groupId, String artifactId, String extension, String version)
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
    public ResolutionResult resolve(String groupId,
                                    String artifactId,
                                    String extension,
                                    String classifier,
                                    String version) throws DependencyCollectionException,
                                                           DependencyResolutionException,
                                                           SettingsBuildingException {

        return resolve(groupId, artifactId, extension, classifier, version, getRemoteRepositories());
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
    public ResolutionResult resolve(String groupId,
                                    String artifactId,
                                    String extension,
                                    String classifier,
                                    String version,
                                    List<RemoteRepository> repositories) throws DependencyCollectionException,
                                                                                DependencyResolutionException,
                                                                                SettingsBuildingException {

        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        Dependency dependency = new Dependency(artifact, JavaScopes.COMPILE);
        //if(repositories==null)
            repositories = getRemoteRepositories();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositories);

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
     * @param artifactFile The file for the project's artifact, may be {@code null}.
     *
     * @throws InstallationException if the requested installation is unsuccessful
     */
    public void install(String groupId, String artifactId, String version, File pomFile, File artifactFile)
        throws InstallationException {

        InstallRequest installRequest = new InstallRequest();

        if(artifactFile!=null) {
            Artifact jarArtifact = new DefaultArtifact(groupId, artifactId, "", "jar", version);
            jarArtifact = jarArtifact.setFile(artifactFile);

            Artifact pomArtifact = new SubArtifact(jarArtifact, "", "pom");
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest.addArtifact(jarArtifact).addArtifact(pomArtifact);
        } else {
            Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);
            pomArtifact = pomArtifact.setFile(pomFile);
            installRequest.addArtifact(pomArtifact);
        }
        repositorySystem.install(repositorySystemSession, installRequest);
    }

    protected DependencyFilter getDependencyFilter(Artifact a) {
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
    public URL getLocation(String artifactCoordinates, String artifactExt) throws ArtifactResolutionException,
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
    public URL getLocation(String artifactCoordinates,
                           String artifactExt,
                           List<RemoteRepository> repositories) throws ArtifactResolutionException,
                                                                       MalformedURLException {
        DefaultArtifact a = new DefaultArtifact(artifactCoordinates);
        String extension = artifactExt==null? "jar":artifactExt;
        ArtifactRequest artifactRequest = new ArtifactRequest();
        DefaultArtifact artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), extension, a.getVersion());
        artifactRequest.setArtifact(artifact);
        if(repositories==null)
            repositories = getRemoteRepositories();
        artifactRequest.setRepositories(repositories);
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
        return artifactResult.getArtifact().getFile().toURI().toURL();
    }

    private Settings getSettings() throws SettingsBuildingException {
        DefaultSettingsBuilder defaultSettingsBuilder = new DefaultSettingsBuilder();
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        File userSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        request.setUserSettingsFile(userSettingsFile);
        defaultSettingsBuilder.setSettingsWriter(new DefaultSettingsWriter());
        defaultSettingsBuilder.setSettingsReader(new DefaultSettingsReader());
        defaultSettingsBuilder.setSettingsValidator(new DefaultSettingsValidator());
        SettingsBuildingResult build = defaultSettingsBuilder.build(request);
        return build.getEffectiveSettings();
    }

    List<RemoteRepository> getRemoteRepositories() {
        List<String> activeProfiles = effectiveSettings.getActiveProfiles();
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        //boolean haveRio = false;
        for(String activeProfile : activeProfiles) {
            for(Profile profile : effectiveSettings.getProfiles()) {
                if(profile.getId().equals(activeProfile)) {
                    for(org.apache.maven.settings.Repository r : profile.getRepositories()) {
                        /*if(r.getId().equals("rio") || r.getUrl().equals("http://www.rio-project.org/maven2/"))
                            haveRio = true;*/
                        RemoteRepository remoteRepository = new RemoteRepository(r.getId(), "default", r.getUrl());
                        RepositoryPolicy snapShotPolicy = new RepositoryPolicy(r.getSnapshots().isEnabled(),
                                                                               r.getSnapshots().getUpdatePolicy(),
                                                                               r.getSnapshots().getChecksumPolicy());
                        RepositoryPolicy releasesPolicy = new RepositoryPolicy(r.getReleases().isEnabled(),
                                                                               r.getReleases().getUpdatePolicy(),
                                                                               r.getReleases().getChecksumPolicy());
                        remoteRepository.setPolicy(true, snapShotPolicy);
                        remoteRepository.setPolicy(false, releasesPolicy);
                        repositories.add(remoteRepository);
                    }
                    break;
                }
            }
        }
        RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
        List<Mirror> mirrors = effectiveSettings.getMirrors();
        for (Mirror mirror : mirrors) {
            if (mirror.getMirrorOf().equals("*") || mirror.getMirrorOf().equals("central")) {
                System.out.println("[AetherService] Using mirror for central: " + mirror.getUrl());
                central = new RemoteRepository("central", "default", mirror.getUrl());
            }
        }
        repositories.add(central);
        /*if(!haveRio)
            repositories.add(new RemoteRepository("rio", "default", "http://www.rio-project.org/maven2/"));*/
        return repositories;
    }

}
