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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Install Rio jars to local repository
 */
public class Installer {

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
                Class aetherService = resolverLoader.loadClass("org.rioproject.resolver.aether.AetherService");
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
            install(rioParent, new File(pomDir, "rio-main.pom"), null, aetherServiceInstance);

            File libDir = new File(rioHome + File.separator + "lib");

            /* Install Rio Resolver API */
            Artifact resolverAPI = new Artifact("org.rioproject.resolver:resolver-api:" + RioVersion.VERSION);
            install(resolverAPI, null, new File(libDir, "resolver-api.jar"), aetherServiceInstance);

            /* Install client and proxy jars */
            rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-api:" + RioVersion.VERSION), "cybernode-api.jar");
            rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-proxy:" + RioVersion.VERSION), "cybernode-proxy.jar");
            rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-ui:" + RioVersion.VERSION), "cybernode-ui.jar");
            rioArtifactJars.put(new Artifact("org.rioproject.monitor:monitor-api:" + RioVersion.VERSION), "monitor-api.jar");
            rioArtifactJars.put(new Artifact("org.rioproject.gnostic:gnostic-api:" + RioVersion.VERSION), "gnostic-api.jar");
            rioArtifactJars.put(new Artifact("org.rioproject.monitor:monitor-proxy:" + RioVersion.VERSION), "monitor-proxy.jar");
            rioArtifactJars.put(new Artifact("org.rioproject:rio-api:" + RioVersion.VERSION), "rio-api.jar");
            rioArtifactJars.put(new Artifact("org.rioproject:watch-ui:" + RioVersion.VERSION), "watch-ui.jar");

            File libDlDir = new File(rioHome + File.separator + "lib-dl");
            for (Map.Entry<Artifact, String> entry : rioArtifactJars.entrySet()) {
                Artifact a = entry.getKey();
                install(a, null, new File(libDlDir, entry.getValue()), aetherServiceInstance);
            }

            /* Install the Gnostic service and the Gnostic pom */

            Artifact gnosticParent = new Artifact("org.rioproject:gnostic:" + RioVersion.VERSION);
            install(gnosticParent, new File(pomDir, "rio-gnostic.pom"), null, aetherServiceInstance);
            Artifact gnosticService = new Artifact("org.rioproject.gnostic:gnostic-service:" + RioVersion.VERSION);
            install(gnosticService, null, new File(libDir, "gnostic-service.jar"), aetherServiceInstance);
            
            String jiniVersion = "2.1.1";

            /* Install third party jars */
            Artifact jskPlatform = new Artifact("net.jini:jsk-platform:"+jiniVersion);
            Artifact jmxLookup = new Artifact("net.jini.lookup:jmx-lookup:2.1");
            Artifact jskDL = new Artifact("net.jini:jsk-dl:"+jiniVersion);
            Artifact reggieDL = new Artifact("com.sun.jini:reggie-dl:"+jiniVersion);
            Artifact serviceUI = new Artifact("net.jini.lookup:serviceui:"+jiniVersion);

            install(jskPlatform, new File(pomDir, "jsk-platform.pom"), new File(libDir, "jsk-platform.jar"), aetherServiceInstance);
            install(jmxLookup, new File(pomDir, "jmx-lookup.pom"), new File(libDlDir, "jmx-lookup.jar"), aetherServiceInstance);
            install(jskDL, new File(pomDir, "jsk-dl.pom"), new File(libDlDir, "jsk-dl.jar"), aetherServiceInstance);
            install(reggieDL, new File(pomDir, "reggie-dl.pom"), new File(libDlDir, "reggie-dl.jar"), aetherServiceInstance);
            install(serviceUI, new File(pomDir, "serviceui.pom"), new File(libDlDir, "serviceui.jar"), aetherServiceInstance);

        } catch (ResolverException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(cCL);
        }
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
    public static void install(Artifact artifact, File pomFile, File artifactFile, Object aetherService) throws IOException {
        if(artifact==null)
            throw new IllegalArgumentException("artifact must not be null");
        if(artifactFile==null && pomFile==null)
            throw new IllegalArgumentException("if pomFile is not provided, the artifactFile must not be null");
        File localRepository = Repository.getLocalRepository();
        StringBuilder sb = new StringBuilder();
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
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
            sb.append(jarName).append("-").append(version).append(extension);
            File jar = new File(localRepository, sb.toString());
            if (jar.exists())
                return;

            /*
             * Look for the pom in the artifact. Once we find it, read it in, then write it out as a temp file
             */
            if (pomFile == null) {
                String line;
                List<String> pomListing = new ArrayList<String>();
                JarFile jarFile = new JarFile(artifactFile);
                sb.delete(0, sb.length());
                sb.append("META-INF/maven/").append(groupId).append("/").append(artifactId).append("/").append("pom.xml");
                JarEntry pomEntry = jarFile.getJarEntry(sb.toString());
                if (pomEntry == null) {
                    System.err.println("Unable to find jar entry [" + sb.toString() + "], in [" + artifactFile.getPath() + "], " +
                                       "cannot install " + groupId + ":" + artifactId + ":" + version);
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
                pomFile = tempPom;
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
            install.invoke(aetherService, groupId, artifactId, version, pomFile, artifactFile);
        } catch (Exception e) {
            e.getCause().printStackTrace();
        }
    }

    public static void main(String... args) throws IOException {
        Installer.install();
    }
}
