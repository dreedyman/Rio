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
package org.rioproject.impl.config;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.ServerEndpoint;
import net.jini.jeri.tcp.TcpServerEndpoint;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.rioproject.net.PortRangeServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The ExporterConfig is a utility class used to get an 
 * {@link net.jini.export.Exporter} instance from a 
 * {@link net.jini.config.Configuration} defaulting to a special entry configured 
 * for the platform.
 *
 * <p>The <tt>org.rioproject.defaultExporter</tt> allows deployers to configure
 * one Exporter that all services will use.
 *
 * @author Dennis Reedy
 */
public class ExporterConfig {
    public static final String DEFAULT_COMPONENT = "org.rioproject";
    public static final String ENTRY_NAME = "defaultExporter";
    private static final Logger logger = LoggerFactory.getLogger(ExporterConfig.class.getName());

    /**
     * Get an {@link net.jini.export.Exporter} instance from a {@link net.jini.config.Configuration}
     * using the provided component name and entry. This method will first establish what the default {@code Exporter}
     * is as follows:
     * <ul>
     * <li>If the  special entry <tt>org.rioproject.defaultExporter</tt> can be found, the
     * {@code Exporter} specified by this entry will be used as the default Exporter.
     * If the special <tt>org.rioproject.defaultExporter</tt>  entry cannot be
     * found, the following {@code Exporter} will be used:
     * <pre>
     *  InetAddress address = HostUtil.getInetAddressFromProperty(&quot;java.rmi.server.hostname&quot;);
     *  exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(address.getHostAddress(), 0),
     *                                   new BasicILFactory(),
     *                                   false,
     *                                   true);
     * </pre>
     * <li>
     * This method will then check if the component and entry provided exists. If the entry can be found in the
     * provided {@code Configuration} that {@code Exporter} will be used. If the entry cannot be found, then the
     * default {@code Exporter} will be used as the {@code Exporter}.
     * </ul>
     * @param config The Configuration to obtain the Exporter from
     * @param component The component name
     * @param entry The entry name
     *
     * @return A suitable Exporter
     *
     * @throws ConfigurationException If there are errors reading the configuration
     */
    public static Exporter getExporter(Configuration config, String component, String entry) throws ConfigurationException {

        Exporter exporter = (Exporter)config.getEntry(component, entry, Exporter.class, null);
        if(exporter==null) {
            exporter = (Exporter)config.getEntry(DEFAULT_COMPONENT, ENTRY_NAME, Exporter.class, null);
            if(exporter==null) {
                try {
                    exporter = new BasicJeriExporter(getServerEndpoint(), new BasicILFactory(), false, true);
                } catch (UnknownHostException e) {
                    logger.warn("Unable to get host address, defaulting to localhost", e);
                    exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory(), false, true);
                }
            }
        }
        if(logger.isTraceEnabled())
            logger.trace(String.format("Created %s for %s.%s", exporter, component, entry));
        return exporter;
    }

    public static ServerEndpoint getServerEndpoint() throws UnknownHostException {
        InetAddress address = HostUtil.getInetAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        String range = System.getProperty(Constants.PORT_RANGE);
        ServerSocketFactory factory = null;
        if(range!=null) {
            String[] parts = range.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            factory = new PortRangeServerSocketFactory(start, end);
        }
        return TcpServerEndpoint.getInstance(address.getHostAddress(), 0, null, factory);
    }
}
