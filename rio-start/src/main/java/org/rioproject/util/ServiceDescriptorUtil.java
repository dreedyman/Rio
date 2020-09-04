/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.util;

import com.sun.jini.config.ConfigUtil;
import com.sun.jini.start.ServiceDescriptor;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
import org.rioproject.config.DynamicConfiguration;
import org.rioproject.net.HostUtil;
import org.rioproject.start.ConfigurationServiceDescriptor;
import org.rioproject.start.RioServiceDescriptor;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides utilities to obtain {@link com.sun.jini.start.ServiceDescriptor} instances
 * for Rio services.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public final class ServiceDescriptorUtil {
    /* Port value obtained from invoking the getStartupPort() method */
    private static int port = 0;

    /*
     * Cannot instantiate this utility
     */
    private ServiceDescriptorUtil() {
    }

    static int getStartupPort() throws IOException {
        if(port==0) {
            if(System.getProperty(Constants.PORT_RANGE)!=null) {
                port = PortUtil.getPortFromRange(System.getProperty(Constants.PORT_RANGE));
            } else {
                port = PortUtil.getAnonymousPort();
            }
        }
        return(port);
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>.
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param putDirectory The directory name that Webster will enable put file support
     *
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots,
                                               final String putDirectory) throws IOException {
        String[] options = null;
        if(putDirectory!=null) {
            options = new String[2];
            options[0] = "-putDirectory";
            options[1] = putDirectory;
        }
        return(getWebster(policy, sPort, roots, options, false));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>.
     *
     * @param policy The security policy file to use
     * @param roots The roots webster should serve
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using an anonymous port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(final String policy, final String[] roots) throws IOException {
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
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots) throws IOException {
        return(getWebster(policy, sPort, roots, false));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>.
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param options Options for Webster to use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots,
                                               final String[] options) throws IOException {
        return(getWebster(policy, sPort, roots, options, false));
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
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots,
                                               final boolean debug) throws IOException {
        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome==null)
            throw new RuntimeException("rio.home property not declared or derivable");
        String webster = rioHome+File.separator+"lib"+File.separator+createVersionedJar("webster");
        return(getWebster(policy, sPort, roots, null, debug, webster));

    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param options Options for Webster to use
     * @param debug If true, set the <tt>org.rioproject.tools.debug</tt> property
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots,
                                               final String[] options,
                                               final boolean debug) throws IOException {
        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome==null)
            throw new RuntimeException("rio.home property not declared or derivable");
        String webster = rioHome+File.separator+"lib"+File.separator+createVersionedJar("webster");
        return(getWebster(policy, sPort, roots, options, debug, webster));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.tools.webster.Webster</tt>
     *
     * @param policy The security policy file to use
     * @param sPort The port webster should use
     * @param roots The roots webster should serve
     * @param options Options for Webster to use
     * @param debug If true, set the <tt>org.rioproject.tools.debug</tt> property
     * @param webster The location an name of the webster jar
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * webster using a specified port. The <tt>webster-${rio-version}.jar</tt> file will be
     * loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws IllegalArgumentException If the<tt>rio.home</tt> system property is not set
     */
    public static ServiceDescriptor getWebster(final String policy,
                                               final String sPort,
                                               final String[] roots,
                                               final String[] options,
                                               final boolean debug,
                                               final String webster) throws IOException {
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
        String address = HostUtil.getHostAddressFromProperty("java.rmi.server.hostname");
        List<String> optionsList = new ArrayList<>();
        if(options!=null) {
            Collections.addAll(optionsList, options);
        }
        if(!optionsList.contains("-roots")) {
            optionsList.add("-roots");
            optionsList.add(websterRoots);
        }
        if(!optionsList.contains("-bindAddress")) {
            optionsList.add("-bindAddress");
            optionsList.add(address);
        }
        optionsList.add(portOptionArg);
        optionsList.add(portArg);
        return new RioServiceDescriptor("",
                                        policy,
                                        webster,
                                        websterClass,
                                        optionsList.toArray(new String[optionsList.size()]));
    }

    public static ServiceDescriptor getJetty(final String sPort,
                                             final String[] roots) {
        return getJetty(sPort, roots, null, 0, 0, true);

    }

    public static ServiceDescriptor getJetty(final String sPort,
                                             final String[] roots, boolean secure) {
        return getJetty(sPort, roots, null, 0, 0, secure);

    }

    public static ServiceDescriptor getJetty(final String sPort,
                                             final String[] roots,
                                             final String putDir) {
        return getJetty(sPort, roots, putDir, 0, 0, true);

    }

    public static ServiceDescriptor getJetty(final String sPort,
                                             final String[] roots,
                                             final String putDir,
                                             final boolean secure) {
        return getJetty(sPort, roots, putDir, 0, 0, secure);

    }

    public static ServiceDescriptor getJetty(final String sPort,
                                             final String[] roots,
                                             final String putDir,
                                             final int minThreads,
                                             final int maxThreads,
                                             final boolean secure) {

        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome == null) {
            throw new RuntimeException("rio.home property not declared or derivable");
        }
        String webster = rioHome + File.separator + "lib" + File.separator + createVersionedJar("webster");

        DynamicConfiguration config = new DynamicConfiguration();
        config.setEntry("org.rioproject.tools.jetty",
                        "port",
                        int.class,
                        Integer.parseInt(sPort));
        config.setEntry("org.rioproject.tools.jetty",
                        "roots",
                        String[].class,
                        roots);
        config.setEntry("org.rioproject.tools.jetty",
                        "secure",
                        Boolean.class,
                        secure);
        if(putDir!=null)
            config.setEntry("org.rioproject.tools.jetty",
                            "putDir",
                            String.class,
                            putDir);
        if(minThreads > 0)
            config.setEntry("org.rioproject.tools.jetty",
                            "minThreads",
                            int.class,
                            minThreads);
        if(maxThreads > 0)
            config.setEntry("org.rioproject.tools.jetty",
                            "maxThreads",
                            int.class,
                            maxThreads);

        String jettyClass = "org.rioproject.tools.jetty.Jetty";

        return new ConfigurationServiceDescriptor(webster, jettyClass, config);
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.cybernode.Cybernode</tt>.
     *
     * @param policy The security policy file to use
     * @param cybernodeConfig The configuration file the Cybernode will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Cybernode using an anonymous port. The <tt>cybernode-service-${rio-version}.jar</tt> file
     * will be loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getCybernode(final String policy, final String... cybernodeConfig) throws IOException {
        String cybernodeClasspath = getCybernodeClasspath();
        String cybernodeCodebase = getCybernodeCodebase();
        String implClass = "org.rioproject.cybernode.service.CybernodeImpl";
        return(new RioServiceDescriptor(cybernodeCodebase, policy, cybernodeClasspath, implClass, cybernodeConfig));
    }

    public static String getCybernodeClasspath() throws IOException {
        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome == null)
            throw new RuntimeException("rio.home property not declared or derivable");
        List<String> jarList = new ArrayList<String>();
        jarList.add(createVersionedJar("cybernode-service"));
        if(System.getProperty("rio.test.attach")!=null) {
            System.setProperty(Constants.RESOLVER_JAR, getProjectResolverLocation(rioHome));
            jarList.add(createVersionedJar("rio-int-test"));
        }
        StringBuilder classPath = new StringBuilder();
        classPath.append(makePath(rioHome+File.separator+"lib", jarList.toArray(new String[jarList.size()])));
        return classPath.toString();
    }

    public static String getCybernodeCodebase() throws UnknownHostException {
        return createAnnotatedArtifactURL(String.format("artifact:org.rioproject.cybernode/cybernode-proxy/%s",
                                                        RioVersion.VERSION));
    }

    static String getProjectResolverLocation(final String rioHome) {
        StringBuilder sb = new StringBuilder();
        sb.append(rioHome).append(File.separator)
            .append("lib").append(File.separator)
            .append("resolver").append(File.separator)
            .append(createVersionedJar("resolver-project"));
        return sb.toString();
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.monitor.ProvisionMonitor</tt>.
     *
     * @param policy The security policy file to use
     * @param monitorConfig The configuration options the Monitor will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Monitor using an anonymous port. The <tt>monitor-service-${rio-version}.jar</tt> file will
     * be loaded from <tt>rio.home/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getMonitor(final String policy, final String... monitorConfig) throws IOException {
        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome == null)
            throw new RuntimeException("rio.home property not declared or derivable");
        List<String> jarList = new ArrayList<String>();
        jarList.add(createVersionedJar("monitor-service"));
        if(System.getProperty("rio.test.attach")!=null) {
            System.setProperty(Constants.RESOLVER_JAR, getProjectResolverLocation(rioHome));
            jarList.add(createVersionedJar("rio-int-test"));
        }
        StringBuilder classPath = new StringBuilder();
        classPath.append(makePath(rioHome+File.separator+"lib", jarList.toArray(new String[jarList.size()])));
        String implClass = "org.rioproject.monitor.service.ProvisionMonitorImpl";
        String monitorCodebase =
            createAnnotatedArtifactURL(String.format("artifact:org.rioproject.monitor/monitor-proxy/%s",
                                                     RioVersion.VERSION));
        return (new RioServiceDescriptor(monitorCodebase, policy, classPath.toString(), implClass, monitorConfig));
    }

    static String createAnnotatedArtifactURL(final String artifact) throws UnknownHostException {
        return String.format("%s;https://%s:%s",
                             artifact,
                             HostUtil.getHostAddressFromProperty("java.rmi.server.hostname"),
                             port);
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Jini Lookup Service (Reggie).
     *
     * @param policy The security policy file to use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * Reggie using an anonymous port. The <tt>reggie-${river-version}.jar</tt> file will
     * be loaded from <tt>rio.home/lib</tt>
     * @param lookupConfig The configuration options Reggie will use
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the<tt>rio.home</tt> system property is not
     * set
     */
    public static ServiceDescriptor getLookup(final String policy, final String... lookupConfig) throws IOException {
        String rioHome = System.getProperty("rio.home", RioHome.get());
        if(rioHome == null)
            throw new RuntimeException("rio.home property not declared or derivable");
        String reggieClasspath = FileHelper.find(new File(rioHome, "lib"), "reggie").getPath();
        File reggieDL = FileHelper.find(new File(rioHome, "lib-dl"), "reggie-dl");
        //String reggieCodebase = "artifact:org.apache.river/reggie-dl/"+ FileHelper.getJarVersion(reggieDL.getName());
        String reggieCodebase = createAnnotatedArtifactURL(String.format("artifact:org.apache.river/reggie-dl/%s",
                                                                         FileHelper.getJarVersion(reggieDL.getName())));
        String implClass = "com.sun.jini.reggie.TransientRegistrarImpl";
        return (new RioServiceDescriptor(reggieCodebase, policy, reggieClasspath, implClass, lookupConfig));
    }

    /**
     * Check if the default InetAddress to use is a loopback address
     *
     * @throws UnknownHostException If the host cannot be resolved
     */
    public static void checkForLoopback() throws UnknownHostException {
        InetAddress address = HostUtil.getInetAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        if(address.isLoopbackAddress()) {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("*******************************************************************************\n");
            builder.append("* The network interface to be used has a loopback address of ");
            builder.append(address.getHostAddress()).append(".\n");
            builder.append("* You may encounter issues communicating to services outside of your machine.\n");
            builder.append("*******************************************************************************\n");
            LoggerFactory.getLogger("org.rioproject").warn(builder.toString());
        }
    }

    protected static String makePath(final String dir, final String... jars) {
        StringBuilder sb = new StringBuilder();
        for(String jar : jars) {
            if(sb.length()>0)
                sb.append(File.pathSeparator);
            sb.append(dir).append(File.separator).append(jar);
        }
        return sb.toString();
    }

    private static String createVersionedJar(String name) {
        return String.format("%s-%s.jar", name, RioVersion.VERSION);
    }

}
