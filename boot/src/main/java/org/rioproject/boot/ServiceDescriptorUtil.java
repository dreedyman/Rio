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
import org.rioproject.config.Constants;

import java.io.File;
import java.io.IOException;

/**
 * Holds static attributes used during the startup of services and provides
 * utilities to obtain {@link com.sun.jini.start.ServiceDescriptor} instances
 * for Rio services
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
     * <tt>org.rioproject.cybernode.Cybernode</tt> using the Webster port
     * created by this utility.
     *
     * @param policy The security policy file to use
     * @param cybernodeConfig The configuration options the Cybernode will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Cybernode using an anonymous port. The <tt>cybernode.jar</tt> file
     * will be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getCybernode(String policy,
                                                 String... cybernodeConfig) throws IOException {
        return(getCybernode(policy, getStartupPort(), cybernodeConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.cybernode.Cybernode</tt>.
     *
     * @param policy The security policy file to use
     * @param port The port to use when constructing the codebase
     * @param cybernodeConfig The configuration options the Cybernode will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Cybernode using an anonymous port. The <tt>cybernode.jar</tt> file
     * will be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getCybernode(String policy,
                                                 int port,
                                                 String... cybernodeConfig) throws IOException {
        return(getCybernode(policy, BootUtil.getHostAddress(), port, cybernodeConfig));

    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.cybernode.Cybernode</tt>.
     *
     * @param policy The security policy file to use
     * @param cybernodeConfig The configuration file the Cybernode will use
     * @param hostAddress The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Cybernode using an anonymous port. The <tt>cybernode.jar</tt> file
     * will be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getCybernode(String policy,
                                                 String hostAddress,
                                                 int port,
                                                 String... cybernodeConfig) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String[] jars;
        if(System.getProperty("RIO_TEST_ATTACH")!=null)
            jars = new String[]{"cybernode-service.jar", "rio-test.jar"};
         else
            jars = new String[]{"cybernode-service.jar"};
        String cybernodeClasspath = makePath(rioHome+File.separator+"lib", jars);
        cybernodeClasspath = cybernodeClasspath+File.pathSeparator+makePath(rioHome+File.separator+"lib-dl",
                                                                            "cybernode-proxy.jar",
                                                                            "cybernode-api.jar");
        String[] dlJars = new String[]{"cybernode-proxy.jar",
                                       "cybernode-api.jar",
                                       "rio-api.jar",
                                       "jmx-lookup.jar",
                                       "jsk-dl.jar",
                                       "rio-lookup-entry.jar",
                                       "resolver-api.jar",
                                       "serviceui.jar"};
        String cybernodeCodebase = BootUtil.getCodebase(dlJars, hostAddress, Integer.toString(port));
        String implClass = "org.rioproject.cybernode.CybernodeImpl";
        return(new RioServiceDescriptor(cybernodeCodebase, policy, cybernodeClasspath, implClass, cybernodeConfig));

    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.monitor.ProvisionMonitor</tt> using the Webster port
     * created by this utility.
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
    public static ServiceDescriptor getMonitor(String policy,
                                               String... monitorConfig) throws IOException {
        return(getMonitor(policy, getStartupPort(), monitorConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.monitor.ProvisionMonitor</tt>.
     *
     * @param policy The security policy file to use
     * @param port The port to use when constructing the codebase
     * @param monitorConfig The configuration options the Monitor will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Monitor using an anonymous port. The <tt>monitor.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getMonitor(String policy,
                                               int port,
                                               String... monitorConfig) throws IOException {
        return(getMonitor(policy, BootUtil.getHostAddress(), port, monitorConfig));

    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * <tt>org.rioproject.monitor.ProvisionMonitor</tt>.
     *
     * @param policy The security policy file to use
     * @param hostAddress The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @param monitorConfig The configuration options the Monitor will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Monitor using an anonymous port. The <tt>monitor.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getMonitor(String policy,
                                               String hostAddress,
                                               int port,
                                               String... monitorConfig) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String[] jars;
        if(System.getProperty("RIO_TEST_ATTACH")!=null)
            jars = new String[]{"monitor-service.jar", "rio-test.jar"};
         else
            jars = new String[]{"monitor-service.jar"};
        String monitorClasspath = makePath(rioHome+File.separator+"lib", jars);
        monitorClasspath = monitorClasspath+File.pathSeparator+makePath(rioHome+File.separator+"lib-dl",
                                                                        "monitor-proxy.jar",
                                                                        "monitor-api.jar");
        String[] dlJars = new String[]{"monitor-proxy.jar",
                                       "monitor-api.jar",
                                       "rio-api.jar",
                                       "jmx-lookup.jar",
                                       "jsk-dl.jar",
                                       "rio-lookup-entry.jar",
                                       "resolver-api.jar",
                                       "serviceui.jar"};
        String monitorCodebase = BootUtil.getCodebase(dlJars, hostAddress, Integer.toString(port));

        String implClass = "org.rioproject.monitor.ProvisionMonitorImpl";
        return (new RioServiceDescriptor(monitorCodebase, policy, monitorClasspath, implClass, monitorConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Jini Lookup Service (Reggie), using the Webster port
     * created by this utility.
     *
     * @param policy The security policy file to use
     * @param lookupConfig The configuration file Reggie will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * Reggie using an anonymous port. The <tt>reggie.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getLookup(String policy,
                                              String... lookupConfig) throws IOException {
        return(getLookup(policy, getStartupPort(), lookupConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Jini Lookup Service, Reggie.
     *
     * @param policy The security policy file to use
     * @param lookupConfig The configuration options Reggie will use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * @param port The port to use when constructing the codebase
     * Reggie using an anonymous port. The <tt>reggie.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getLookup(String policy,
                                              int port,
                                              String... lookupConfig) throws IOException {
        return(getLookup(policy,  BootUtil.getHostAddress(), port, lookupConfig));
    }

    /**
     * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
     * the Jini Lookup Service (Reggie), using the Webster port
     * created by this utility.
     *
     * @param policy The security policy file to use
     * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
     * @param hostAddress The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * Reggie using an anonymous port. The <tt>reggie.jar</tt> file will
     * be loaded from <tt>RIO_HOME/lib</tt>
     * @param lookupConfig The configuration options Reggie will use
     *
     * @throws IOException If there are problems getting the anonymous port
     * @throws RuntimeException If the <tt>RIO_HOME</tt> system property is not
     * set
     */
    public static ServiceDescriptor getLookup(String policy,
                                              String hostAddress,
                                              int port,
                                              String... lookupConfig) throws IOException {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME property not declared");
        String reggieClasspath = rioHome+File.separator+"lib"+File.separator+"reggie.jar";
        String[] dlJars = new String[]{"reggie-dl.jar", "jsk-dl.jar"};
        //String reggieCodebase = makeCodebaseFilePath(rioHome + File.separator + "lib-dl", dlJars);
        String reggieCodebase = BootUtil.getCodebase(dlJars, hostAddress, Integer.toString(port));
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

    protected static String makeCodebaseFilePath(String dir, String... jars) {
        StringBuilder sb = new StringBuilder();
        for(String jar : jars) {
            StringBuilder pathBuilder = new StringBuilder();
            if(sb.length()>0)
                sb.append(" ");
            pathBuilder.append(dir).append(File.separator).append(jar);
            sb.append(new File(pathBuilder.toString()).toURI().toString());
        }
        return sb.toString();
    }
}
