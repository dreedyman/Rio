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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.rioproject.resolver.aether.filters.ClassifierFilter;
import org.rioproject.resolver.aether.filters.ExcludePlatformFilter;
import org.rioproject.resolver.aether.util.ConsoleRepositoryListener;
import org.rioproject.resolver.aether.util.ConsoleTransferListener;
import org.rioproject.resolver.aether.util.SettingsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Use Maven 3's Aether API for Maven dependency resolution.
 *
 * @author Dennis Reedy
 */
public final class AetherService {
    private RepositorySystemSession repositorySystemSession;
    private final RepositorySystem repositorySystem;
    private Settings effectiveSettings;
    private String dependencyFilterScope;
    private final WorkspaceReader workspaceReader;
    private final List<RemoteRepository> configuredRepositories = new ArrayList<RemoteRepository>();
    private final Collection<DependencyFilter> dependencyFilters =
        Collections.synchronizedCollection(new ArrayList<DependencyFilter>());
    private static final Logger logger = LoggerFactory.getLogger(AetherService.class);
    static final String CONFIG_PROP_NO_CACHE = "aether.versionResolver.noCache";
    static final String CONFIG_PROP_SESSION_STATE = "aether.updateCheckManager.sessionState";

    private AetherService(final RepositorySystem repositorySystem, final WorkspaceReader workspaceReader) throws SettingsBuildingException {
        this.repositorySystem = repositorySystem;
        this.effectiveSettings = SettingsUtil.getSettings();
        this.workspaceReader = workspaceReader;
        this.repositorySystemSession = getRepositorySystemSession();
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

    public RepositorySystemSession getRepositorySystemSession() throws SettingsBuildingException {
        return newSession(repositorySystem,
                          workspaceReader,
                          SettingsUtil.getLocalRepositoryLocation(effectiveSettings));
    }

    public AetherService setDependencyFilterScope(final String dependencyFilterScope) {
        this.dependencyFilterScope = dependencyFilterScope;
        return this;
    }

    public void addDependencyFilter(final DependencyFilter filter) {
        dependencyFilters.add(filter);
    }

    private static RepositorySystem newRepositorySystem() {
        /*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * pre-populated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService(TransporterFactory.class, FileTransporterFactory.class );
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class );
        /*locator.setService(UpdatePolicyAnalyzer.class, MyUpdatePolicyAnalyzer.class);
        locator.setService(UpdateCheckManager.class, ForceUpdateCheckManager.class);
        locator.setService(LocalRepositoryManagerFactory.class, ConfigurableLocalRepositoryManagerFactory.class);*/

        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(final RepositorySystem system,
                                               final WorkspaceReader workspaceReader,
                                               final String repositoryLocation)
        throws SettingsBuildingException {
        DefaultRepositorySystemSession session = repositorySystemSession==null?
                                                 MavenRepositorySystemUtils.newSession():
                                                 (DefaultRepositorySystemSession) repositorySystemSession;

        /* Do not expect POMs in the repository. */
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(ArtifactDescriptorPolicy.IGNORE_MISSING));

        /* Warn on checksum policy */
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        /* Do not store them. */
        session.setConfigProperty(ConfigurationProperties.PERSISTED_CHECKSUMS, false);

        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_DISABLED));

        session.setConfigProperty(CONFIG_PROP_NO_CACHE, true);
        session.setConfigProperty(CONFIG_PROP_SESSION_STATE, "bypass");


        LocalRepository localRepo = new LocalRepository(repositoryLocation);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());
        if(workspaceReader!=null)
            session.setWorkspaceReader(workspaceReader);
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

        RepositorySystemSession session = newSession(repositorySystem,
                                                     workspaceReader,
                                                     SettingsUtil.getLocalRepositoryLocation(effectiveSettings));

        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        Dependency dependency = new Dependency(artifact, /*JavaScopes.RUNTIME*/dependencyFilterScope==null?JavaScopes.RUNTIME:dependencyFilterScope);
        List<RemoteRepository> myRepositories;
        if(repositories==null || repositories.isEmpty())
            myRepositories = getRemoteRepositories();
        else
            myRepositories = repositories;

        setMirrorSelector(myRepositories, (DefaultRepositorySystemSession) session);
        List<RemoteRepository> repositoriesToUse = applyAuthentication(myRepositories);

        if(logger.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            for(RemoteRepository r : repositoriesToUse) {
                if(builder.length()>0)
                    builder.append(", ");
                builder.append(r.getUrl());
            }
            logger.debug("Resolve {} using repositories {}", artifact, builder.toString());
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositoriesToUse);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, getDependencyFilter(artifact));
        dependencyRequest.setCollectRequest(collectRequest);

        try {
            List<ArtifactResult> artifactResults = repositorySystem.resolveDependencies(session,
                                                                                        dependencyRequest).getArtifactResults();
            return new ResolutionResult(artifact, artifactResults);
        } catch(NullPointerException e) {
            /* catch and throw a DependencyCollectionException */
            String message = String.format("Trying to resolve %s, failed to read artifact descriptor, " +
                                           "make sure that all parent poms are resolve-able", artifact);
            throw new DependencyCollectionException(new CollectResult(collectRequest), message, e);
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
        throws InstallationException, SettingsBuildingException {
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
        throws InstallationException, SettingsBuildingException {

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
        repositorySystem.install(getRepositorySystemSession(), installRequest);
    }

    @SuppressWarnings("unused")
    public void deploy(final String groupId,
                       final String artifactId,
                       final String version,
                       final File artifactFile,
                       final File pomFile,
                       final String repositoryId,
                       final String repositoryURL) throws DeploymentException, SettingsBuildingException {

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
                       final String repositoryURL) throws DeploymentException, SettingsBuildingException {

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

        RemoteRepository repository = new RemoteRepository.Builder(repositoryId, "default", repositoryURL).build();

        RepositorySystemSession session = getRepositorySystemSession();
        setMirrorSelector(asList(repository) , (DefaultRepositorySystemSession) session);
        List<RemoteRepository> applied = applyAuthentication(asList(repository));
        deployRequest.setRepository(applied.get(0));

        repositorySystem.deploy(session, deployRequest);
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
                                                          JavaScopes.RUNTIME:dependencyFilterScope));
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
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public URL getLocation(final String artifactCoordinates, final String artifactExt) throws ArtifactResolutionException,
                                                                                              MalformedURLException,
                                                                                              SettingsBuildingException {
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
     * @throws SettingsBuildingException If errors are encountered handling settings
     */
    public URL getLocation(final String artifactCoordinates,
                           final String artifactExt,
                           final List<RemoteRepository> repositories) throws ArtifactResolutionException,
                                                                             MalformedURLException,
                                                                             SettingsBuildingException {
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
        RepositorySystemSession session = newSession(repositorySystem,
                                                     workspaceReader,
                                                     SettingsUtil.getLocalRepositoryLocation(effectiveSettings));
        setMirrorSelector(myRepositories, (DefaultRepositorySystemSession) session);
        List<RemoteRepository> repositoriesToUse = applyAuthentication(myRepositories);

        DefaultArtifact a = new DefaultArtifact(artifactCoordinates);
        String extension = artifactExt==null? "jar":artifactExt;
        ArtifactRequest artifactRequest = new ArtifactRequest();
        DefaultArtifact artifact = new DefaultArtifact(a.getGroupId(),
                                                       a.getArtifactId(),
                                                       a.getClassifier(),
                                                       extension,
                                                       a.getVersion());
        artifactRequest.setArtifact(artifact);
        artifactRequest.setRepositories(repositoriesToUse);
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
        return artifactResult.getArtifact().getFile().toURI().toURL();
    }

    public AetherService setConfiguredRepositories(List<RemoteRepository> repositories) {
        configuredRepositories.addAll(repositories);
        return this;
    }

    /**
     * Get the {@code RemoteRepository} instances that are configured.
     *
     * @return An immutable {@code List} of {@code RemoteRepository} instances.
     */
    public List<RemoteRepository> getRemoteRepositories() {
        List<String> activeProfiles = effectiveSettings.getActiveProfiles();
        List<RemoteRepository> myRepositories = new ArrayList<RemoteRepository>();
        myRepositories.addAll(configuredRepositories);
        for(String activeProfile : activeProfiles) {
            for(Profile profile : effectiveSettings.getProfiles()) {
                if(profile.getId().equals(activeProfile)) {
                    for(org.apache.maven.settings.Repository r : profile.getRepositories()) {
                        if(!alreadyHaveRepository(myRepositories, r.getId())) {
                            RepositoryPolicy snapShotPolicy = createRepositoryPolicy(r.getSnapshots());
                            RepositoryPolicy releasesPolicy = createRepositoryPolicy(r.getReleases());
                            RemoteRepository.Builder builder = new RemoteRepository.Builder(r.getId(), "default", r.getUrl());
                            builder.setSnapshotPolicy(snapShotPolicy).setReleasePolicy(releasesPolicy);
                            RemoteRepository remoteRepository = builder.build();
                            myRepositories.add(remoteRepository);
                        }
                    }
                    break;
                }
            }
        }

        /*if(!alreadyHaveRepository(myRepositories, "central")) {
            RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
            myRepositories.add(central);
        }*/
        return Collections.unmodifiableList(myRepositories);
    }

    /**
     * Apply any authentication required for the provided repositories
     *
     * @param repositories The {@code List} of repositories
     *
     * @return A {@code List} of {@code RemoteRepository}s with authentication applied
     */
    private List<RemoteRepository> applyAuthentication(final List<RemoteRepository> repositories) {
        if(effectiveSettings.getServers().isEmpty())
            return repositories;
        Set<RemoteRepository> appliedRepositories = new HashSet<RemoteRepository>();
        for(Server server : effectiveSettings.getServers()) {
            for(RemoteRepository remoteRepository : repositories) {
                if(server.getId().equals(remoteRepository.getId())) {
                    if(server.getUsername()!=null) {
                        Authentication authentication =
                            new AuthenticationBuilder()
                                .addUsername(server.getUsername())
                                .addPassword(server.getPassword())
                                .addPrivateKey(server.getPassword(), server.getPrivateKey()).build();
                        RemoteRepository.Builder builder =
                            new RemoteRepository.Builder(remoteRepository.getId(), "default", remoteRepository.getUrl());
                        builder.setAuthentication(authentication);
                        appliedRepositories.add(builder.build());
                    } else {
                        appliedRepositories.add(remoteRepository);
                    }
                } else {
                    appliedRepositories.add(remoteRepository);
                }
            }
        }
        return new ArrayList<RemoteRepository>(appliedRepositories);
    }

    /**
     * Set the {@code MirrorSelector}  to the session if any mirrors have been declared
     *
     * @param repositories The {@code List} of repositories
     */
    private void setMirrorSelector(final List<RemoteRepository> repositories,
                                   final DefaultRepositorySystemSession session) {
        MirrorSelector mirrorSelector = getMirrorSelector(repositories);
        session.setMirrorSelector(mirrorSelector);
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
                repositoryMirrors.add(new RemoteRepository.Builder(mirror.getId(), "default", mirror.getUrl()).build());
            }

            /*for (RemoteRepository mirror : repositoryMirrors) {
                List<RemoteRepository> mirroredRepositories = new ArrayList<RemoteRepository>();
                for(RemoteRepository r : repositories) {
                    if(mirrorSelector.getMirror(r)!=null) {
                        mirroredRepositories.add(r);
                        r.setUrl(mirror.getUrl());
                    }
                }
                mirror.setMirroredRepositories(mirroredRepositories);
            }*/
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
