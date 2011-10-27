/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.config;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.BasicILFactory;
import org.rioproject.net.HostUtil;

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
        
    /**
     * Get an {@link net.jini.export.Exporter} instance from 
     * a {@link net.jini.config.Configuration} using the provided component name 
     * and entry. This method will first establish what the 
     * default Exporter is as follows:
     * <ul>
     * <li>If the  special entry 
     * <tt>org.rioproject.defaultExporter</tt> can be found, the
     * Exporter specified by this entry will be used as the default Exporter.
     * <li>
     * This method will then check if the component and entry provided
     * exists. If the entry can be found in the provided Configuration that Exporter 
     * will be used. If the entry cannot be found, then the defaultExporter will be 
     * used as the Exporter.
     * </ul>
     * @param config The Configuration to obtain the Exporter from
     * @param component The component name
     * @param entry The entry name
     * @param defaultExporter The Exporter to use as a default if the special 
     * entry <tt>org.rioproject.defaultExporter</tt> can not be found
     *
     * @return A suitable Exporter
     *
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static Exporter getExporter(Configuration config, String component, String entry, Exporter defaultExporter)
    throws ConfigurationException {
        
        final Exporter exporter =  (Exporter)config.getEntry(DEFAULT_COMPONENT,
                                                             ENTRY_NAME,
                                                             Exporter.class,
                                                             defaultExporter);
        return((Exporter)Config.getNonNullEntry(config, component, entry, Exporter.class, exporter));
    }

    /**
     * Get an {@link net.jini.export.Exporter} instance from
     * a {@link net.jini.config.Configuration} using the provided component name
     * and entry. This method will first establish what the
     * default Exporter is as follows:
     * <ul>
     * <li>If the  special entry
     * <tt>org.rioproject.defaultExporter</tt> can be found, the
     * Exporter specified by this entry will be used as the default Exporter.
     * If the special <tt>org.rioproject.defaultExporter</tt>  entry cannot be
     * found, the following Exporter will be used:
     * <pre>
     *  String address = HostUtil.getHostAddressFromProperty(&quot;java.rmi.server.hostname&quot;);
     *  exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(address, 0),
     *                                   new BasicILFactory(),
     *                                   false,
     *                                   true);
     * </pre>
     * <li>
     * This method will then check if the component and entry provided
     * exists. If the entry can be found in the provided Configuration that Exporter
     * will be used. If the entry cannot be found, then a defaultExporter will be
     * used as the Exporter. 
     * </ul>
     * @param config The Configuration to obtain the Exporter from
     * @param component The component name
     * @param entry The entry name
     *
     * @return A suitable Exporter
     *
     * @throws ConfigurationException If there are errors reading the
     * configuration
     */
    public static Exporter getExporter(Configuration config, String component, String entry)
        throws ConfigurationException {

        Exporter exporter = (Exporter)config.getEntry(component, entry, Exporter.class, null);
        if (exporter != null) {
            return exporter;
        }
        exporter = (Exporter)config.getEntry(DEFAULT_COMPONENT, ENTRY_NAME, Exporter.class, null);
        if (exporter != null) {
            return exporter;
        }
        try {
            String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(address, 0),
                                             new BasicILFactory(),
                                             false,
                                             true);
        } catch (UnknownHostException e) {
            exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory(), false, true);
        }

        return exporter;
    }
}
