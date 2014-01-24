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
package org.rioproject.install;

import org.rioproject.RioVersion;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.resolver.maven2.Repository;
import org.rioproject.util.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Install Rio jars to local repository
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public final class Installer {
    private static final Logger logger = LoggerFactory.getLogger(Installer.class);

    private Installer() {}

    /**
     * Install Rio jars to the local repository
     *
     * @throws IOException If jars cannot be written to the local filesystem
     * @throws RuntimeException If the <code>RIO_HOME</code> property is not set
     */
    public static void install() throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if (rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        Map<Artifact, String> rioArtifactJars = new HashMap<Artifact, String>();
        ClassLoader cCL = Thread.currentThread().getContextClassLoader();
        try {
            Resolver r = ResolverHelper.getResolver();
            ClassLoader resolverLoader = r.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(resolverLoader);
            Object aetherServiceInstance = null;
            try {
                Class<?> aetherService = resolverLoader.loadClass("org.rioproject.resolver.aether.AetherService");
                Method getDefaultInstance = aetherService.getDeclaredMethod("getDefaultInstance");
                aetherServiceInstance = getDefaultInstance.invoke(null);
            } catch (Exception e) {
                throw new IOException("Could not get an instance of the Resolver", e);
            } finally {
                Thread.currentThread().setContextClassLoader(cCL);
            }
            /* Install Rio main (parent) pom */
            File pomDir = new File(rioHome + File.separator + "config" + File.separator + "poms");
            Artifact rioParent = new Artifact("org.rioproject:main:" + RioVersion.VERSION);
            install(rioParent, FileHelper.find(pomDir, "rio-main"), null, aetherServiceInstance);

            File libDir = new File(rioHome + File.separator + "lib");

            /* Install rio-platform */
            Artifact rioPlatform = new Artifact("org.rioproject:rio-platform:" + RioVersion.VERSION);
            install(rioPlatform, null, FileHelper.find(libDir, "rio-platform"), aetherServiceInstance);

            /* Install Rio Resolver API */
            Artifact resolverAPI = new Artifact("org.rioproject.resolver:resolver-api:" + RioVersion.VERSION);
            install(resolverAPI, null, FileHelper.find(libDir, "resolver-api"), aetherServiceInstance);

            /* Install client and proxy jars */
            formatAndAddToMap("org.rioproject.cybernode:cybernode-api", "cybernode-api", rioArtifactJars);
            formatAndAddToMap("org.rioproject.cybernode:cybernode-proxy", "cybernode-proxy", rioArtifactJars);
            formatAndAddToMap("org.rioproject.cybernode:cybernode-ui", "cybernode-ui", rioArtifactJars);
            formatAndAddToMap("org.rioproject.monitor:monitor-api", "monitor-api", rioArtifactJars);
            formatAndAddToMap("org.rioproject.gnostic:gnostic-api", "gnostic-api", rioArtifactJars);
            formatAndAddToMap("org.rioproject.monitor:monitor-proxy", "monitor-proxy", rioArtifactJars);
            formatAndAddToMap("org.rioproject:rio-api", "rio-api", rioArtifactJars);
            formatAndAddToMap("org.rioproject:rio-proxy", "rio-proxy", rioArtifactJars);
            formatAndAddToMap("org.rioproject:watch-ui", "watch-ui", rioArtifactJars);

            formatAndAddToMap("org.rioproject.event-collector:event-collector-api", "event-collector-api", rioArtifactJars);
            formatAndAddToMap("org.rioproject.event-collector:event-collector-proxy", "event-collector-proxy", rioArtifactJars);

            File libDlDir = new File(rioHome + File.separator + "lib-dl");
            for (Map.Entry<Artifact, String> entry : rioArtifactJars.entrySet()) {
                Artifact a = entry.getKey();
                install(a, null, new File(libDlDir, entry.getValue()), aetherServiceInstance);
            }

            /* Install the Gnostic service and the Gnostic pom */
            Artifact gnosticParent = new Artifact("org.rioproject:gnostic:" + RioVersion.VERSION);
            install(gnosticParent, FileHelper.find(pomDir, "rio-gnostic"), null, aetherServiceInstance);
            Artifact gnosticService = new Artifact("org.rioproject.gnostic:gnostic-service:" + RioVersion.VERSION);
            install(gnosticService, null, FileHelper.find(libDir, "gnostic-service"), aetherServiceInstance);

            File jskPlatformJar = FileHelper.find(libDir, "jsk-platform");
            File jskDLJar = FileHelper.find(libDlDir, "jsk-dl");
            File reggieDLJar = FileHelper.find(libDlDir, "reggie-dl");
            File serviceUIJar = FileHelper.find(libDlDir, "serviceui");

            /* Install third party jars */
            Artifact jskPlatform = createArtifact("net.jini:jsk-platform", jskPlatformJar);
            Artifact jskDL = createArtifact("net.jini:jsk-dl", jskDLJar);
            Artifact reggieDL = createArtifact("org.apache.river:reggie-dl", reggieDLJar);
            Artifact serviceUI = createArtifact("net.jini.lookup:serviceui", serviceUIJar);

            install(jskPlatform, FileHelper.find(pomDir, "jsk-platform"), jskPlatformJar, aetherServiceInstance);
            install(jskDL, FileHelper.find(pomDir, "jsk-dl"), jskDLJar, aetherServiceInstance);
            install(reggieDL, FileHelper.find(pomDir, "reggie-dl"), reggieDLJar, aetherServiceInstance);
            install(serviceUI, FileHelper.find(pomDir, "serviceui"), serviceUIJar, aetherServiceInstance);

        } catch (ResolverException e) {
            logger.error("Could not install artifacts", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cCL);
        }
    }

    private static Artifact createArtifact(String ga, File jar) {
        return new Artifact(String.format("%s:%s", ga, FileHelper.getJarVersion(jar.getName())));
    }

    private static void formatAndAddToMap(String a, String j, Map<Artifact, String> map) {
        Artifact artifact = new Artifact(String.format("%s:%s", a, RioVersion.VERSION));
        String jarName = String.format("%s-%s.jar", j, RioVersion.VERSION);
        map.put(artifact, jarName);
    }

    /**
     * Install an artifact
     *
     * @param artifact The artifact, must not be null.
     * @param pomFile The pomFile for the artifact. If null, the pom file will be looked for in the
     * <code>META-INF/maven/</code><i>artifactId</i> directory of the <code>artifactFile</code>
     * @param artifactFile The artifact jar, must not be null if the pomFile is not provided
     * @param aetherService The AetherService instance
     *
     * @throws IOException If jars cannot be written to the local filesystem
     * @throws IllegalArgumentException If groupId, artifactId, version or artifactFile are <code>null</code>
     */
    public static void install(final Artifact artifact,
                               final File pomFile,
                               final File artifactFile,
                               final Object aetherService) throws IOException {
        if(artifact==null)
            throw new IllegalArgumentException("artifact must not be null");
        if(artifactFile==null && pomFile==null)
            throw new IllegalArgumentException("if pomFile is not provided, the artifactFile must not be null");
        File localRepository = Repository.getLocalRepository();
        StringBuilder sb = new StringBuilder();
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
        File workingPomFile = pomFile;
        sb.append(groupId.replace(".", File.separator));
        sb.append(File.separator);
        sb.append(artifactId);
        sb.append(File.separator);
        sb.append(version);
        sb.append(File.separator);

        if(artifactFile!=null) {
            int ndx = artifactFile.getName().lastIndexOf(".");
            String jarName = artifactFile.getName().substring(0, ndx);
            String extension = artifactFile.getName().substring(ndx, artifactFile.getName().length());
            sb.append(jarName).append(extension);
            File jar = new File(localRepository, sb.toString());
            if (jar.exists())
                return;

            /*
             * Look for the pom in the artifact. Once we find it, read it in, then write it out as a temp file
             */
            if (workingPomFile == null) {
                String line;
                List<String> pomListing = new ArrayList<String>();
                JarFile jarFile = new JarFile(artifactFile);
                sb.delete(0, sb.length());
                sb.append("META-INF/maven/").append(groupId).append("/").append(artifactId).append("/").append("pom.xml");
                JarEntry pomEntry = jarFile.getJarEntry(sb.toString());
                if (pomEntry == null) {
                    logger.error("Unable to find jar entry [{}], in [{}], cannot install {}:{}:{}",
                                 sb.toString(), artifactFile.getPath(), groupId, artifactId, version);
                    return;
                }
                InputStream is = jarFile.getInputStream(pomEntry);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    pomListing.add(line);
                }
                File tempPom = File.createTempFile(groupId + "-" + artifactId, ".pom");
                tempPom.deleteOnExit();
                Writer output = new BufferedWriter(new FileWriter(tempPom));
                try {
                    for (String s : pomListing)
                        output.write(s);
                } finally {
                    is.close();
                    output.close();
                }
                workingPomFile = tempPom;
            }
        }

        if(artifactFile==null) {
            sb.append(artifactId).append("-").append(version).append(".pom");
            File targetPom = new File(localRepository, sb.toString());
            if(targetPom.exists())
                return;
        }

        try {
            Method install = aetherService.getClass().getDeclaredMethod("install",
                                                                        String.class,
                                                                        String.class,
                                                                        String.class,
                                                                        File.class,
                                                                        File.class);
            install.invoke(aetherService, groupId, artifactId, version, workingPomFile, artifactFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) throws IOException {
        Installer.install();
    }
}
