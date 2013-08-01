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
package org.rioproject.core.jsb;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.entry.Entry;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.associations.AssociationManagement;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.watch.WatchRegistry;

import java.io.IOException;

/**
 * Specifies the semantics for retrieving the context of a ServiceBean. A
 * ServiceBeanContext provides a ServiceBean with necessary context required to
 * obtain information about it's environment, attributes, ServiceBeanManager and
 * ComputeResourceManager
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanContext {
    /**
     * Get the export codebase used to load ServiceBean download JARs
     * 
     * @return The codebase identifies the codebase of the export JARs for the
     * ServiceBean. The returned value is suitable for use in creating an HTTP
     * protocol {@link java.net.URL}
     */
    String getExportCodebase();

    /**
     * The ServiceBeanManager provides a mechanism for the ServiceBean to obtain
     * a DiscardManager, request it's ServiceElement be updated to
     * OperationalStringManager instance(s) and obtain system resources.
     * 
     * @return The {@link org.rioproject.core.jsb.ServiceBeanManager} for the 
     * ServiceBean
     */
    ServiceBeanManager getServiceBeanManager();

    /**
     * The ComputeResourceManager provides a mechanism for the ServiceBean to
     * obtain the ComputeResource object and acquire information about the
     * environment and attributes of the ComputeResource.
     * 
     * @return The {@link org.rioproject.core.jsb.ComputeResourceManager} for the 
     * ServiceBean
     */
    ComputeResourceManager getComputeResourceManager();

    /**
     * Get the ServiceBean {@link net.jini.config.Configuration} object
     * 
     * @return The {@link net.jini.config.Configuration} object for 
     * a ServiceBean. A new {@link net.jini.config.Configuration} object will
     * be returned each time this method is invoked
     *
     * @throws ConfigurationException if there are problems creating the
     * Configuration
     */
    Configuration getConfiguration() throws ConfigurationException;

    /**
     * Returns an {@link java.lang.Object} containing the value of the named 
     * initialization parameter, or null if the parameter does not exist. <br>
     * 
     * @param name A {@link java.lang.String} containing the name of the parameter 
     * whose value is requested 
     * @return The {@link java.lang.Object} corresponding to the value of the 
     * parameter requested, or null if the parameter does not exist. 
     */
    Object getInitParameter(String name);

    /**
     * Get the names (keys) for all initialization parameters
     * 
     * @return An {@link java.util.Iterator} of the names of the ServiceBean's 
     * initialization parameters, or an empty {@link java.util.Iterator}
     * if the ServiceBean has no initialization parameters. 
     * A new {@link java.util.Iterator} is returned each time this method is 
     * called
     */
    Iterable<String> getInitParameterNames();

    /**
     * Get the ServiceElement for the ServiceBean
     * 
     * @return The {@link org.rioproject.opstring.ServiceElement} object
     */
    ServiceElement getServiceElement();

    /**
     * Get the ServiceBeanConfig for the ServiceBean
     * 
     * @return The {@link org.rioproject.opstring.ServiceBeanConfig} object
     */
    ServiceBeanConfig getServiceBeanConfig();

    /**
     * Get the DiscoveryManagement object based on declared discovery attributes
     * 
     * @return The {@link net.jini.discovery.DiscoveryManagement} object for the 
     * ServiceBean
     *
     * @throws IOException If there are problems acquiring a
     * DiscoveryManagement instance
     */
    DiscoveryManagement getDiscoveryManagement() throws IOException;
    
    /**
     * Get the AssociationManagement object for the ServiceBean
     * 
     * @return The {@link org.rioproject.associations.AssociationManagement} object for the
     * ServiceBean
     */
    AssociationManagement getAssociationManagement();

    /**
     * Register an event handler. This associates an EventHandler to an
     * EventDescriptor for the ServiceBean.
     *
     * @param descriptor The EventDescriptor for the event
     * @param handler The associated EventHandler
     */
    void registerEventHandler(EventDescriptor descriptor, EventHandler handler);

    /**
     * Add an attribute to the collection of attributes used to describe the
     * ServiceBean. Attributes added to the ServiceBeanContext will be accessed
     * when the ServiceBean is being advertised for the first time.
     *
     * @param attribute Entry to add
     */
    void addAttribute(Entry attribute);

    /**
     * Get the {@link org.rioproject.watch.WatchRegistry} for the ServiceBean.
     *
     * @return The {@link org.rioproject.watch.WatchRegistry} object for the
     * ServiceBean
     */
    WatchRegistry getWatchRegistry();

}
