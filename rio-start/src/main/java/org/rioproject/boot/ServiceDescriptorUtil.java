/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.boot;

import com.sun.jini.config.ConfigUtil;
import com.sun.jini.start.NonActivatableServiceDescriptor;
import com.sun.jini.start.ServiceDescriptor;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
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
 * Provides utilities to obtain {@link com.sun.jini.start.ServiceDescriptor} instances
 * for Rio services.
 *
 * @author Dennis Reedy
 */
public class ServiceDescriptorUtil {
    /* Port value obtained from invoking the getStartupPort() method */
    private static int port = 0;

    static int getStartupPort() throws IOException {
        if(port==0) {
            if(System.getProperty(Constants.PORT_RANGE)!=null) {
                port = BootUtil.getPortFromRange(System.getProperty(Constants.PORT_RANGE));
            } else {
                port = BootUtil.getAnonymousPort();
            }
        }
        return(port);
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>.
     *
     * @param policy The security policy file to use
     * @param roots The roots webster should serve
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using an anonymous port. The <tt>webster.jar</tt> file will be
     * loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(String policy, String[] roots) throws IOException {
        return(getWebster(policy, "0", roots));        
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>.
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster.jar</tt> file will be
     * loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(String policy, String sPort, String[] roots) throws IOException {
        return(getWebster(policy, sPort, roots, false));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param debug If true, set the <tt>org.rioproject.tools.debug</tt> property
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster.jar</tt> file will be
     * loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     */
    public static ServiceDescriptor getWebster(String policy,
                                               String sPort,
                                               String[] roots,
                                               boolean debug) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null)
            throw new RuntimeException("RIO_HOME property not declared");
        String webster = rioHome+File.separator+"lib"+File.separator+"webster.jar";
        return(getWebster(policy, sPort, roots, debug, webster));

    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param debug If true, set the <tt>org.rioproject.tools.debug</tt> property
     * @param webster The location an name of the webster jar
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster.jar</tt> file will be
     * loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws IllegalArgumentException If the <tt>RIO_HOME</tt> system property is not set
     */
    public static ServiceDescriptor getWebster(String policy,
                                               String sPort,
                                               String[] roots,
                                               boolean debug,
                                               String webster) throws IOException {
        if(webster==null)
            throw new IllegalArgumentException("webster jar cannot be null");
        String portOptionArg = "-port";
        String portArg;
        if(sPort.contains("-")) {
            portOptionArg = "-portRange";
            portArg = sPort;
        } else {
            try {
                int p = Integer.parseInt(sPort);
                port = p==0?getStartupPort():p;
                portArg = Integer.toString(port);
            } catch(NumberFormatException e) {
                throw new RuntimeException("invalid port ["+sPort+"]");
            }
        }
        String websterRoots = ConfigUtil.concat(roots);
        String websterClass = "org.rioproject.tools.webster.Webster";

        if(debug) {
            System.setProperty("org.rioproject.tools.webster.debug", "1");
        }
        String address =
            BootUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        return(new NonActivatableServiceDescriptor("",
                                                   policy,
                                                   webster,
                                                   websterClass,
                                                   new String[]{portOptionArg,
                                                                portArg,
                                                                "-roots",
                                                                websterRoots,
                                                                "-bindAddress",
                                                                address
                                                   }));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.cybernode.Cybernode</tt>.
     *
     * @param policy The security policy file to use
     * @param cybernodeConfig The configuration file the Cybernode will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Cybernode using an anonymous port. The <tt>cybernode.jar</tt> file
     * will be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getCybernode(String policy, String... cybernodeConfig) throws IOException {
        String cybernodeClasspath = getCybernodeClasspath();
        String cybernodeCodebase = "artifact:org.rioproject.cybernode/cybernode-proxy/"+ RioVersion.VERSION;
        String implClass = "org.rioproject.cybernode.CybernodeImpl";
        return(new RioServiceDescriptor(cybernodeCodebase, policy, cybernodeClasspath, implClass, cybernodeConfig));

    }

    public static String getCybernodeClasspath() {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String[] jars;
        if(System.getProperty("RIO_TEST_ATTACH")!=null) {
            System.setProperty(Constants.RESOLVER_JAR, getProjectResolverLocation(rioHome));
            jars = new String[]{"cybernode-service.jar", "rio-test.jar"};
        } else {
            jars = new String[]{"cybernode-service.jar"};
        }
        return makePath(rioHome+File.separator+"lib", jars);
    }

    public static String getCybernodeCodebase() {
        return "artifact:org.rioproject.cybernode/cybernode-proxy/"+ RioVersion.VERSION;
    }

    static String getProjectResolverLocation(String rioHome) {
        StringBuilder sb = new StringBuilder();
        sb.append(rioHome).append(File.separator)
            .append("lib").append(File.separator)
            .append("resolver").append(File.separator)
            .append("resolver-project.jar");
        return sb.toString();
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.monitor.ProvisionMonitor</tt>.
     *
     * @param policy The security policy file to use
     * @param monitorConfig The configuration options the Monitor will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Monitor using an anonymous port. The <tt>monitor.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getMonitor(String policy, String... monitorConfig) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String[] jars;
        if(System.getProperty("RIO_TEST_ATTACH")!=null) {
            System.setProperty(Constants.RESOLVER_JAR, getProjectResolverLocation(rioHome));
            jars = new String[]{"monitor-service.jar", "rio-test.jar"};
        } else {
            jars = new String[]{"monitor-service.jar"};
        }
        File libDlDir = new File(rioHome+File.separator+"lib-dl");
        install("org.rioproject", "rio-api", RioVersion.VERSION, null, new File(libDlDir, "rio-api.jar"));
        install("org.rioproject.monitor", "monitor-api", RioVersion.VERSION, null, new File(libDlDir, "monitor-api.jar"));
        install("org.rioproject.monitor", "monitor-proxy", RioVersion.VERSION, null, new File(libDlDir, "monitor-proxy.jar"));

        String monitorClasspath = makePath(rioHome+File.separator+"lib", jars);
        String monitorCodebase = "artifact:org.rioproject.monitor/monitor-proxy/"+ RioVersion.VERSION;
        String implClass = "org.rioproject.monitor.ProvisionMonitorImpl";
        return (new RioServiceDescriptor(monitorCodebase, policy, monitorClasspath, implClass, monitorConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Jini Lookup Service (Reggie).
     *
     * @param policy The security policy file to use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * Reggie using an anonymous port. The <tt>reggie.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     * @param lookupConfig The configuration options Reggie will use
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getLookup(String policy, String... lookupConfig) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String reggieClasspath = rioHome+File.separator+"lib"+File.separator+"reggie.jar";
        File pomDir = new File(rioHome+File.separator+"config"+File.separator+"poms");
        File libDlDir = new File(rioHome+File.separator+"lib-dl");
        install("net.jini", "jsk-dl", "2.1", new File(pomDir, "jsk-dl.pom"), new File(libDlDir, "jsk-dl.jar"));
        install("com.sun.jini", "reggie-dl", "2.1", new File(pomDir, "reggie-dl.pom"), new File(libDlDir, "reggie-dl.jar"));
        String reggieCodebase = "artifact:com.sun.jini/reggie-dl/2.1";
        String implClass = "com.sun.jini.reggie.TransientRegistrarImpl";
        return (new RioServiceDescriptor(reggieCodebase, policy, reggieClasspath, implClass, lookupConfig));
    }
    
    protected static String makePath(String dir, String... jars) {
        StringBuilder sb = new StringBuilder();
        for(String jar : jars) {
            if(sb.length()>0)
                sb.append(File.pathSeparator);
            sb.append(dir).append(File.separator).append(jar);
        }
        return sb.toString();
    }

    static void install() throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        Map<Artifact, String> rioArtifactJars = new HashMap<Artifact, String>();
        rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-api:"+RioVersion.VERSION),   "cybernode-api.jar");
        rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-proxy:"+RioVersion.VERSION), "cybernode-proxy.jar");
        rioArtifactJars.put(new Artifact("org.rioproject.cybernode:cybernode-ui:"+RioVersion.VERSION),    "cybernode-ui.jar");
        rioArtifactJars.put(new Artifact("org.rioproject.monitor:monitor-api:"+RioVersion.VERSION),       "monitor-api.jar");
        rioArtifactJars.put(new Artifact("org.rioproject.monitor:monitor-proxy:"+RioVersion.VERSION),     "monitor-proxy.jar");
        rioArtifactJars.put(new Artifact("org.rioproject:rio-api:"+RioVersion.VERSION),                   "rio-api.jar");
        rioArtifactJars.put(new Artifact("org.rioproject.watch-ui:"+RioVersion.VERSION),                  "watch-ui.jar");


        File libDlDir = new File(rioHome+File.separator+"lib-dl");
        for(Map.Entry<Artifact, String> entry : rioArtifactJars.entrySet()) {
            Artifact a = entry.getKey();
            install(a.getGroupId(), a.getArtifactId(), a.getVersion(), null, new File(libDlDir, entry.getValue()));
        }

        File pomDir = new File(rioHome+File.separator+"config"+File.separator+"poms");
        Map<File, Map<Artifact, String>> pomArtifactJars = new HashMap<File, Map<Artifact, String>>();
        pomArtifactJars.put(new File(pomDir, "jmx-lookup.pom"), createMap("net.jini.lookup:jmx-lookup:2.1", "jmx-lookup.jar"));
        pomArtifactJars.put(new File(pomDir, "jsk-dl.pom"),     createMap("net.jini:jsk-dl:2.1",            "jsk-dl.jar"));
        pomArtifactJars.put(new File(pomDir, "reggie-dl.pom"),  createMap("com.sun.jini:reggie-dl:2.1",     "reggie-dl.jar"));
        pomArtifactJars.put(new File(pomDir, "serviceui.pom"),  createMap("net.jini.lookup:serviceui:2.1",  "serviceui.jar"));
        for(Map.Entry<File, Map<Artifact, String>> entry : pomArtifactJars.entrySet()) {
            File pom = entry.getKey();
            for(Map.Entry<Artifact, String> entry2 : entry.getValue().entrySet()) {
                Artifact a = entry2.getKey();
                install(a.getGroupId(), a.getArtifactId(), a.getVersion(), pom, new File(libDlDir, entry2.getValue()));
            }
        }
    }

    private static Map<Artifact, String> createMap(String a, String j) {
        Map<Artifact, String> map = new HashMap<Artifact, String>();
        map.put(new Artifact(a), j);
        return map;
    }

    static void install(String groupId, String artifactId, String version, File pomFile, File artifactFile) throws IOException {
        File localRepository = Repository.getLocalRepository();
        StringBuilder sb = new StringBuilder();
        sb.append(groupId.replaceAll("\\.", File.separator));
        sb.append(File.separator);
        sb.append(artifactId);
        sb.append(File.separator);
        sb.append(version);
        sb.append(File.separator);

        int ndx = artifactFile.getName().lastIndexOf(".");
        String jarName = artifactFile.getName().substring(0, ndx);
        sb.append(jarName).append("-").append(version).append(".jar");
        File jar = new File(localRepository, sb.toString());
        if(jar.exists())
            return;

        /*
         * Look for the pom in the artifact jar. Once we find it, read it in, then write it out as a temp file
         */
        if(pomFile==null) {
            String line;
            List<String> pomListing = new ArrayList<String>();
            JarFile jarFile = new JarFile(artifactFile);
            sb.delete(0, sb.length());
            sb.append("META-INF/maven/").append(groupId).append("/").append(artifactId).append("/").append("pom.xml");
            JarEntry pomEntry = jarFile.getJarEntry(sb.toString());
            if(pomEntry==null) {
                System.err.println("Unable to find jar entry ["+sb.toString()+"], in ["+artifactFile.getPath()+"], " +
                                   "cannot install "+groupId+":"+artifactId+":"+version);
                return;
            }
            InputStream is = jarFile.getInputStream(pomEntry);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while((line = br.readLine())!=null) {
                pomListing.add(line);
            }
            File tempPom = File.createTempFile(groupId+"-"+artifactId, ".pom");
            tempPom.deleteOnExit();
            Writer output = new BufferedWriter(new FileWriter(tempPom));
            try {
                for(String s : pomListing)
                    output.write(s);
            }
            finally {
                is.close();
                output.close();
            }
            pomFile = tempPom;
        }
        try {
            Resolver r = ResolverHelper.getResolver();
            ClassLoader resolverLoader = r.getClass().getClassLoader();
            ClassLoader cCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(resolverLoader);
            try {
                Class aetherService = resolverLoader.loadClass("org.rioproject.resolver.aether.AetherService");
                Method getDefaultInstance = aetherService.getDeclaredMethod("getDefaultInstance");
                Object aetherServiceInstance = getDefaultInstance.invoke(null);
                Method install = aetherService.getDeclaredMethod("install",
                                                                 String.class,
                                                                 String.class,
                                                                 String.class,
                                                                 File.class,
                                                                 File.class);
                install.invoke(aetherServiceInstance, groupId, artifactId, version, pomFile,  artifactFile);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(cCL);
            }
        } catch (ResolverException e) {
            e.printStackTrace();
        }
    }

}
