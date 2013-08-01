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
package org.rioproject.resources.client;

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.*;
import net.jini.lookup.entry.Name;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * The JiniClient class is a helper class that Jini clients (or something that
 * wants to act like a Jini client) uses to create DiscoveryManagement instances
 * to discover services
 *
 * @author Dennis Reedy
 */
public class JiniClient {
    public static final String GROUPS_PROPERTY_NAME = org.rioproject.config.Constants.GROUPS_PROPERTY_NAME;
    public static final String LOCATOR_PROPERTY_NAME = org.rioproject.config.Constants.LOCATOR_PROPERTY_NAME;
    private DiscoveryManagement discoverer = null;
    public Listener listener = null;
    private List<ServiceRegistrar> regArray;
    private boolean createdDiscoverer = false;
    private static Logger logger = LoggerFactory.getLogger(JiniClient.class);

    /**
     * Create an instance of a JiniClient <br>
     * 
     * @throws Exception if any errors occur
     */
    public JiniClient() throws Exception {
        this(null);
    }

    /**
     * Create an instance of a JiniClient providing a DiscoveryManagement
     * reference <br>
     *
     * @param dm The DiscoveryManagement instance to use. If the
     * DiscoveryManagement object is null, JiniClient will look for 2
     * system properties to construct a DiscoveryManagement object:
     * <ul>
     * <li>org.rioproject.groups: a comma separated list of groups to use. If this
     * property is not found LookupDiscoveryGroups will be set to
     * LookupDiscovery.NO_GROUPS. Additionally, the value of "all" will be set
     * to LookupDiscovery.ALL_GROUPS
     * <li>org.rioproject.locators: a comma separated list of
     * LookupLocator formatted URLs
     * </ul>
     * 
     * @throws Exception if any errors occur
     */
    public JiniClient(DiscoveryManagement dm) throws Exception {
        regArray = Collections.synchronizedList(new ArrayList<ServiceRegistrar>());
        listener = new Listener();
        if(dm != null) {
            discoverer = dm;
            dm.addDiscoveryListener(listener);
        } else {
            String[] groups =
                parseGroups(System.getProperty(GROUPS_PROPERTY_NAME));
            LookupLocator[] locators = parseLocators(System.getProperty(
                LOCATOR_PROPERTY_NAME));
            if(logger.isDebugEnabled())
                logger.debug("Starting discovery process...");
            discoverer = new LookupDiscoveryManager(groups, locators, listener);
            createdDiscoverer = true;
        }
    }

    /**
     * Parse a comma or space delimited string of group names
     *
     * @param groupNames The string of group names to parse
     *
     * @return A string array of parsed group names as follows:
     * <ul>
     * <li>If the group names parameter is null, return
     * {@link net.jini.discovery.LookupDiscovery#NO_GROUPS}.
     * <li>If the groupNames value is "all", return
     * {@link net.jini.discovery.LookupDiscovery#ALL_GROUPS}.
     * <li> If one of the groupNames value is "public", replace
     * "public" with an empty string "".
     * <li>Otherwise add the groupName value as a element in the string array
     */
    public static String[] parseGroups(String groupNames) {
        String[] groups;
        if(groupNames == null) {
            groups = LookupDiscovery.NO_GROUPS;
            if(logger.isDebugEnabled())
                logger.debug("Set groups to NO_GROUPS");
        } else {
            if(logger.isDebugEnabled())
                logger.debug("Set groups to [" + groupNames + "]");
            StringTokenizer st = new StringTokenizer(groupNames, " \t\n\r\f,");
            groups = new String[st.countTokens()];
            if(groups.length == 1) {
                groups[0] = st.nextToken();
                if(groups[0].equals("all")) {
                    groups = LookupDiscovery.ALL_GROUPS;
                } else {
                    if(groups[0].equals("public"))
                        groups[0] = "";
                }
            } else {
                for(int i = 0; st.hasMoreTokens(); i++) {
                    String g = st.nextToken();
                    if(g.equals("public"))
                        g = "";
                    groups[i] = g;
                }
            }
        }
        return groups;
    }

    /**
     * Utility to return a formatted string of discovery attributes
     */
    public static String getDiscoveryAttributes(ServiceBeanContext context) {
        String[] g = context.getServiceBeanConfig().getGroups();
        StringBuilder buff = new StringBuilder();
        if(g!= LookupDiscovery.ALL_GROUPS) {
            for(int i=0; i<g.length; i++) {
                if(i>0)
                    buff.append(", ");
                buff.append(g[i]);
            }
        } else {
            buff.append("ALL_GROUPS");
        }
        if(context.getServiceBeanConfig().getLocators()!=null) {
            for(LookupLocator locator : context.getServiceBeanConfig().getLocators()) {
                if(buff.length()>0)
                    buff.append(", ");
                buff.append(locator.toString());
            }
        }
        return buff.toString();
    }

    /**
     * Parse a comma or space delimited string of locator urls
     *
     * @param locatorUrls The string of locator urls to parse
     *
     * @return A {@link net.jini.core.discovery.LookupLocator}  array of
     * parsed locator urls. If the locatorUrls parameter is null, return null
     *
     * @throws MalformedURLException If the locatorUrls contains a value that has an illegal format
     */
    public static LookupLocator[] parseLocators(String locatorUrls) throws MalformedURLException {
        LookupLocator[] locators = null;
        if(locatorUrls != null) {
            if(logger.isDebugEnabled())
                logger.debug("Use unicast discovery");
            StringTokenizer st = new StringTokenizer(locatorUrls,
                                                     " \t\n\r\f,");
            List<LookupLocator> list = new LinkedList<LookupLocator>();
            while (st.hasMoreTokens()) {
                String locator = st.nextToken();
                if(!locator.startsWith("jini://"))
                    locator = "jini://"+locator;
                list.add(new LookupLocator(locator));
                if(logger.isDebugEnabled())
                    logger.debug("Add locator : " + locator);
            }
            locators = list.toArray(new LookupLocator[list.size()]);
        }
        return locators;
    }

    /**
     * Create a ServiceTemplate from a ServiceElement
     *
     * @param sElem A ServiceElement
     * @param interfaceClass The interface class use
     *
     * @return A ServiceTemplate which can be used to discover the service
     *
     * @throws IllegalArgumentException if the {@code sElem} or {@code interfaceClass} arguments are {@code null}
     */
    public static ServiceTemplate getServiceTemplate(final ServiceElement sElem, final Class<?> interfaceClass) {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        if(interfaceClass == null)
            throw new IllegalArgumentException("interfaceClass is null");
        ServiceTemplate template;
        if(sElem.getMatchOnName())
            template = new ServiceTemplate(null, new Class[]{interfaceClass}, new Entry[]{new Name(sElem.getName())});
        else
            template = new ServiceTemplate(null, new Class[]{interfaceClass}, null);
        return (template);
    }

    /**
     * Get a DiscoveryManagement instance from service attributes in a
     * ServiceElement
     * 
     * @param sElem A ServiceElement
     * @return A DiscoveryManagement instance based on
     * service discovery attributes
     *
     * @throws IOException If DiscoveryManagement cannot be created
     */
    public static DiscoveryManagement getDiscoveryManagement(ServiceElement sElem) throws IOException {
        if(sElem == null)
            throw new IllegalArgumentException("sElem is null");
        DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
        return (discoPool.getDiscoveryManager(sElem.getOperationalStringName(),
                                              sElem.getServiceBeanConfig().getGroups(),
                                              sElem.getServiceBeanConfig().getLocators()));
    }

    /**
     * Create a ServiceTemplate from an AssociationDescriptor
     *
     * @param aDesc The AssociationDescriptor
     * @param cl The ClassLoader to use to load the interface class. If null,
     * the threads context classloader will be used
     *
     * @return A ServiceTemplate which can be used to discover the service
     *
     * @throws ClassNotFoundException If the interface class cannot be loaded
     */
    public static ServiceTemplate getServiceTemplate(AssociationDescriptor aDesc, ClassLoader cl)
        throws ClassNotFoundException {
        if(aDesc == null)
            throw new IllegalArgumentException("aDesc is null");
        ServiceTemplate template  ;
        String[] iNames = aDesc.getInterfaceNames();
        Class[] interfaces = new Class[iNames.length];
        ClassLoader loader = cl;
        if(loader==null) {
            final Thread currentThread = Thread.currentThread();
            loader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return (currentThread.getContextClassLoader());
                    }
                });
        }
        for(int i = 0; i < interfaces.length; i++) {
            interfaces[i] = Class.forName(iNames[i], false, loader);
        }

        if(aDesc.matchOnName())
            template = new ServiceTemplate(null, interfaces, new Entry[]{new Name(aDesc.getName())});
        else
            template = new ServiceTemplate(null, interfaces, null);
        return (template);
    }

    /**
     * Get the DiscoveryManagement instance
     * 
     * @return Returns the instance of DiscoveryManagement that was either
     * passed into the constructor, or that was created as a result of null
     * being input to that parameter.
     */
    public DiscoveryManagement getDiscoveryManager() {
        return discoverer;
    }

    /**
     * Add a Locator to discover. The method will construct a new LookupLocator
     * object, set to perform unicast discovery to the given host and port
     * <p>
     * If a <code>DiscoveryManagement</code> is provided to JiniClient and
     * does not implement the DiscoveryLocatorManagement interface, no action is
     * taken
     *
     * @param host The hostname part of the locator
     * @param port The port name part of the locator
     */
    public void addLocator(String host, int port) {
        if(discoverer instanceof DiscoveryLocatorManagement)
            ((DiscoveryLocatorManagement)discoverer).addLocators(new LookupLocator[]{new LookupLocator(host, port)});
    }

    /**
     * Get the array of known locators
     *
     * @return Returns an array consisting of the elements of the managed set of
     * locators; that is, instances of LookupLocator in which each instance
     * corresponds to a specific lookup service to discover. The returned set
     * will include both the set of LookupLocators corresponding to lookup
     * services that have already been discovered as well as the set of those
     * that have not yet been discovered. If the managed set of locators is
     * empty, this method will return the empty array. This method returns a new
     * array upon each invocation.
     * <p>
     * If a <code>DiscoveryManagement</code> is provided to JiniClient and
     * does not implement the DiscoveryLocatorManagement interface, this method
     * will return null
     */
    public LookupLocator[] getLocators() {
        if(discoverer instanceof DiscoveryLocatorManagement) {
            return (((DiscoveryLocatorManagement)discoverer).getLocators());
        }
        return (new LookupLocator[0]);
    }

    /**
     * Deletes a set of locators from the managed set of locators, and discards
     * any already-discovered lookup service that corresponds to a deleted
     * locator.
     * <p>
     * If a <code>DiscoveryManagement</code> is provided to JiniClient and
     * does not implement the DiscoveryLocatorManagement interface, no action is
     * taken
     *
     * @param locators Array of LookupLocator instances to remove
     */
    public void removeLocators(LookupLocator[] locators) {
        if(discoverer instanceof DiscoveryLocatorManagement) {
            ((DiscoveryLocatorManagement)discoverer).removeLocators(locators);
        }
    }

    /**
     * Add a list of groups to be discovered
     * <p>
     * If a <code>DiscoveryManagement</code> is provided to JiniClient and
     * does not implement the DiscoveryGroupManagement interface, no action is
     * taken
     *
     * @param gAdd Array of group names to add
     *
     * @throws IOException if the groups could not be added
     */
    public void addRegistrarGroups(String[] gAdd) throws IOException {
        if(discoverer instanceof DiscoveryGroupManagement)
            ((DiscoveryGroupManagement)discoverer).addGroups(gAdd);
    }

    /**
     * Get the known set of groups
     *
     * @return Returns an array consisting of the elements of the managed set
     * of groups; that is, the names of the groups whose members are the lookup
     * services to discover. If the managed set of groups is empty, this method
     * will return the empty array. If there is no managed set of groups, or
     * network then null is returned; indicating that all groups are to be
     * discovered. If for some reason network errors occur, null is returned.
     */
    public String[] getRegistrarGroups() {
        try {
            return (((DiscoveryGroupManagement)discoverer).getGroups());
        } catch(Exception e) {
            logger.error("Getting Registrar Groups", e);
        }
        return (new String[0]);
    }

    /**
     * Remove a list of groups from discovery management
     *
     * @param gRemove The array of groups to remove
     */
    public void removeRegistrarGroups(String[] gRemove) {
        if(discoverer instanceof DiscoveryGroupManagement)
            ((DiscoveryGroupManagement)discoverer).removeGroups(gRemove);
    }

    /**
     * Stop this JiniClient and terminate discovery management. This method will
     * also terminate all ServiceCache instances that it has created
     */
    public void terminate() {
        if(createdDiscoverer)
            stopDiscoverer();
        regArray.clear();
    }

    void stopDiscoverer() {
        if(listener != null) {
            if(discoverer != null)
                discoverer.removeDiscoveryListener(listener);
            listener = null;
        }
        if(discoverer != null) {
            discoverer.terminate();
            discoverer = null;
        }
    }
    public class Listener implements DiscoveryListener {
        public void discovered(DiscoveryEvent de) {
            try {
                ServiceRegistrar[] registrars = de.getRegistrars();
                for (ServiceRegistrar registrar : registrars) {
                    if (logger.isDebugEnabled() && createdDiscoverer) {
                        LookupLocator lookup = registrar.getLocator();
                        String host = lookup.getHost();
                        logger.debug("Discovered JLS on host [" + host + "]");
                    }
                    regArray.add(registrar);
                }
                synchronized(this) {
                    notifyAll();
                }
            } catch(RemoteException e) {
                logger.error("Discovered ServiceRegistrar", e);
            }
        }

        public void discarded(DiscoveryEvent dEvent) {
            ServiceRegistrar[] registrars = dEvent.getRegistrars();
            for (ServiceRegistrar registrar : registrars) {
                for (ServiceRegistrar r : regArray) {
                    if (registrar.equals(r)) {
                        if (logger.isDebugEnabled()) {
                            try {
                                LookupLocator lookup = registrar.getLocator();
                                String host = lookup.getHost();
                                logger.debug("Discarded JLS on host ["
                                            + host
                                            + "]");
                            } catch (RemoteException e) {
                                logger.error("Getting LookupLocator during discarded notification", e);
                            }
                        }
                        regArray.remove(regArray.indexOf(r));
                        break;
                    }
                }
            }
        }
    }
}
