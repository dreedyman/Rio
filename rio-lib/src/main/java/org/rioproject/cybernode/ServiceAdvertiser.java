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
package org.rioproject.cybernode;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.LookupDiscovery;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.admin.ServiceBeanControlException;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jsb.ServiceBeanActivation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ServiceAdvertiser is a utility to help advertise a service with configured attributes.
 */
public class ServiceAdvertiser {
    static Logger logger = Logger.getLogger(ServiceAdvertiser.class.getName());

    /**
     * Advertise a ServiceBean
     *
     * @param serviceProxy Proxy to the service
     * @param context The ServiceBeanContext
     * @throws ServiceBeanControlException If the service bean cannot be advertised
     */
    public static void advertise(Object serviceProxy, ServiceBeanContext context) throws ServiceBeanControlException {
        if (serviceProxy == null)
            throw new IllegalArgumentException("serviceProxy is null");
        if (context == null)
            throw new IllegalArgumentException("context is null");

        String hostAddress = context.getComputeResourceManager().getComputeResource().getAddress().getHostAddress();
        Configuration config;
        try {
            config = context.getConfiguration();
        } catch (ConfigurationException e) {
            throw new ServiceBeanControlException("Unable to obtain configuration for service " +
                                                  "[" + context.getServiceElement().getName() + "]",
                                                  e);
        }
        String serviceName = context.getServiceElement().getName();
        String opStringName = context.getServiceElement().getOperationalStringName();
        String[] groups = context.getServiceElement().getServiceBeanConfig().getGroups();
        LookupLocator[] locators = context.getServiceElement().getServiceBeanConfig().getLocators();
        String componentName = getServiceComponentName(context);

        final Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();
        try {
            ClassLoader proxyCL = serviceProxy.getClass().getClassLoader();
            currentThread.setContextClassLoader(proxyCL);
            if (serviceProxy instanceof Administrable) {
                Administrable admin = (Administrable) serviceProxy;
                Object adminObject = admin.getAdmin();

                if (adminObject instanceof ServiceBeanControl) {
                    ServiceBeanControl controller = (ServiceBeanControl) adminObject;
                    controller.advertise();
                    /* Additional attributes are ignored here, they are obtained
                     * by the ServiceBeanAdmin.advertise() method */
                    /*
                    if(attrs.length>0) {
                        JoinAdmin joinAdmin = (JoinAdmin)adminObject;
                        addAttributes(attrs, joinAdmin);
                    }
                    */
                } else if (adminObject instanceof JoinAdmin) {
                    Entry[] configuredAttributes = getConfiguredAttributes(componentName,
                                                                           config,
                                                                           serviceName,
                                                                           context.getExportCodebase());
                    JoinAdmin joinAdmin = (JoinAdmin) adminObject;
                    ArrayList<Entry> addList = new ArrayList<Entry>();
                    /* Try and add an OperationalStringEntry */
                    if (opStringName != null && opStringName.length() > 0) {
                        Entry opStringEntry =
                            loadEntry("org.rioproject.entry.OperationalStringEntry", joinAdmin, opStringName, proxyCL);
                        if (opStringEntry != null)
                            addList.add(opStringEntry);

                        Entry hostEntry = loadEntry("net.jini.lookup.entry.Host", joinAdmin, hostAddress, proxyCL);
                        if (hostEntry != null)
                            addList.add(hostEntry);
                    } else {
                        if (logger.isLoggable(Level.FINEST)) {
                            String s = (opStringName == null ? "[null]" : "[empty string]");
                            logger.finest("OperationalString name is " + s);
                        }
                    }
                    /* Process the net.jini.lookup.entry.Name attribute */
                    try {
                        Class nameClass = proxyCL.loadClass("net.jini.lookup.entry.Name");
                        Constructor cons = nameClass.getConstructor(String.class);
                        Entry name = (Entry) cons.newInstance(serviceName);
                        boolean add = true;
                        /* Check if the service already has a Name, if it does
                         * ensure it is not the same name as the one this
                         * utility is prepared to add */
                        Entry[] attributes = joinAdmin.getLookupAttributes();
                        for (Entry attribute : attributes) {
                            if (attribute.getClass().getName().equals(
                                                                         nameClass.getName())) {
                                Field n = attribute.getClass().getDeclaredField("name");
                                String value = (String) n.get(attribute);
                                if (value.equals(serviceName))
                                    add = false;
                                break;
                            }
                        }
                        if (add)
                            addList.add(name);

                    } catch (Exception e) {
                        if (logger.isLoggable(Level.FINEST))
                            logger.log(Level.FINEST, "Name not found, cannot add a Name Entry", e);
                        else
                            logger.warning("Name not found, cannot add a Name Entry");
                    }

                    /* If any additional attributes (including the
                     * OperationalString and Name entry already processed) are
                     * passed in, include them as well */
                    addList.addAll(Arrays.asList(configuredAttributes));

                    /* If we have Entry objects to add, add them */
                    if (!addList.isEmpty()) {
                        Entry[] adds = addList.toArray(new Entry[addList.size()]);
                        addAttributes(adds, joinAdmin);
                    }
                    /* Apply groups to the JoinAdmin */
                    if (groups == null || groups.length == 0)
                        groups = LookupDiscovery.NO_GROUPS;
                    if (groups != null && groups.length > 0) {
                        if (groups.length == 1 && groups[0].equals("all")) {
                            groups = LookupDiscovery.ALL_GROUPS;
                        } else {
                            for (int i = 0; i < groups.length; i++) {
                                if (groups[i].equals("public"))
                                    groups[i] = "";
                            }
                        }
                    }
                    if (logger.isLoggable(Level.FINEST)) {
                        StringBuilder buff = new StringBuilder();
                        if (groups == null || groups.length == 0) {
                            buff.append("LookupDiscovery.NO_GROUPS");
                        } else {
                            for (int i = 0; i < groups.length; i++) {
                                if (i > 0)
                                    buff.append(",");
                                buff.append(groups[i]);
                            }
                        }
                        logger.finest("Setting groups [" + buff.toString() + "] using JoinAdmin.setLookupGroups");
                    }
                    joinAdmin.setLookupGroups(groups);
                    if ((locators != null) && (locators.length > 0)) {
                        if (logger.isLoggable(Level.FINEST)) {
                            StringBuilder buff = new StringBuilder();
                            for (int i = 0; i < locators.length; i++) {
                                if (i > 0)
                                    buff.append(",");
                                buff.append(locators[i].toString());
                            }
                            logger.finest("Setting locators [" +
                                          buff.toString() +
                                          "] using JoinAdmin.setLookupLocators");
                        }
                        joinAdmin.setLookupLocators(locators);
                    }
                } else {
                    logger.log(Level.SEVERE,
                               "Admin must implement JoinAdmin or ServiceBeanControl to be properly advertised");
                }

            } else {
                throw new ServiceBeanControlException("Unable to obtain mechanism to advertise [" + serviceName + "]");
            }
        } catch (ServiceBeanControlException e) {
            /* If we throw a ServiceBeanControlException above, just rethrow it */
            throw e;
        } catch (Throwable t) {
            logger.warning("Advertising ServiceBean, [" + t.getClass().getName() + ":" + t.getLocalizedMessage() + "]");
            throw new ServiceBeanControlException("advertise", t);
        } finally {
            currentThread.setContextClassLoader(currentClassLoader);
        }
    }

    /**
     * Get the configuration component name from a ServiceBeanContext.
     *
     * @param context The ServiceBeanContext to use.
     * @return The configuration component name.
     */
    private static String getServiceComponentName(ServiceBeanContext context) {
        String serviceBeanComponent;
        String className = null;
        if (context.getServiceElement().getComponentBundle() == null) {
            serviceBeanComponent = (String) context.getInitParameter(ServiceBeanActivation.BOOT_CONFIG_COMPONENT);
        } else {
            if (context.getServiceElement().getComponentBundle() != null)
                className = context.getServiceElement().getComponentBundle().getClassName();
            if (className == null)
                className = context.getServiceElement().getExportBundles()[0].getClassName();

            if (className.indexOf(".") > 0) {
                int index = className.lastIndexOf(".");
                serviceBeanComponent = className.substring(0, index);
            } else {
                serviceBeanComponent = className;
            }
        }
        return serviceBeanComponent;
    }

    /**
     * Get configuration defined attributes
     *
     * @param context The ServiceBeanContext. Must not be null.
     *
     * @return An array of configured Entry attributes from the ServiceBeanContext
     */
    public static Entry[] getConfiguredAttributes(ServiceBeanContext context) {
        Configuration config;
        try {
            config = context.getConfiguration();
        } catch (ConfigurationException e) {
            logger.log(Level.WARNING,
                       "Unable to obtain configuration for service [" + context.getServiceElement().getName() + "]",
                       e);
            return new Entry[0];
        }
        ArrayList<Entry> attrList = new ArrayList<Entry>();
        Entry[] configuredAttributes = getConfiguredAttributes(getServiceComponentName(context),
                                                               config,
                                                               context.getServiceElement().getName(),
                                                               context.getExportCodebase());
        Collections.addAll(attrList, configuredAttributes);
        return(attrList.toArray(new Entry[attrList.size()]));
    }


    /**
     * Get configuration defined attributes
     *
     * @param serviceBeanComponent The configuration component to use when accessing the configuration.
     * Must not be null.
     * @param config The Configuration. Must not be null.
     * @param serviceName The name of the service, used for logging. Must not be null.
     * @param exportCodebase The codebase the service is using, may be null
     *
     * @return An array of configured Entry attributes from the ServiceBeanContext
     */
    private static Entry[] getConfiguredAttributes(String serviceBeanComponent,
                                                   Configuration config,
                                                   String serviceName,
                                                   String exportCodebase) {
        ArrayList<Entry> attrList = new ArrayList<Entry>();
        if(serviceBeanComponent!=null) {
            try {
                /* 1. Get any configured ServiceUIs */
                Entry[] serviceUIs = (Entry[])config.getEntry(serviceBeanComponent,
                                                              "serviceUIs",
                                                              Entry[].class,
                                                              new Entry[0],
                                                              exportCodebase == null ?
                                                              Configuration.NO_DATA : exportCodebase);
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("Obtained ["+serviceUIs.length+"] " +
                                  "serviceUI declarations for "+
                                  "["+serviceName+"] "+
                                  "using component ["+serviceBeanComponent+"]");
                attrList.addAll(Arrays.asList(serviceUIs));
            } catch (ConfigurationException e) {
                logger.log(Level.WARNING,
                           "Getting ServiceUIs for ["+serviceName+"]",
                           e);
            }
            /* 2. Get any additional attributes */
            try {
                Entry[] initialAttributes = (Entry[])config.getEntry(serviceBeanComponent,
                                                                     "initialAttributes",
                                                                     Entry[].class,
                                                                     new Entry[0]);
                attrList.addAll(Arrays.asList(initialAttributes));
            } catch (ConfigurationException e) {
                logger.log(Level.WARNING, "Getting initialAttributes for ["+serviceName+"]", e);
            }
        }
        return(attrList.toArray(new Entry[attrList.size()]));
    }

    /*
     * Add an entry
     */
    private static Entry loadEntry(String entryClassName, JoinAdmin joinAdmin, String value, ClassLoader loader) {
        Entry entry = null;
        try {
            boolean add = true;
            Class entryClass = loader.loadClass(entryClassName);
            Constructor cons = entryClass.getConstructor(String.class);
            Entry newEntry = (Entry)cons.newInstance(value);
            /* Check if the service already has the Entry, if it does perform
             * no more work if it does not add the entry*/
            Entry[] attributes = joinAdmin.getLookupAttributes();
            for (Entry attribute : attributes) {
                if (attribute.getClass().getName().equals(
                    entryClass.getName())) {
                    add = false;
                    break;
                }
            }
            if(add)
                entry = newEntry;

        } catch(Exception e) {
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           entryClassName+" not " +
                           "found, "+
                           "cannot add an "+
                           entryClassName,
                           e);
            else
                logger.warning(entryClassName+" not found, "+
                               "cannot add "+entryClassName);
        }
        return(entry);
    }


    /*
     * Add Attributes using the JoinAdmin
     */
    private static void addAttributes(Entry[] attrs, JoinAdmin joinAdmin) {
        try {
            joinAdmin.addLookupAttributes(attrs);
        } catch (Exception e) {
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Unable to add Entry attributes", e);
            else
                logger.warning("Unable to add Entry attributes");
            e.printStackTrace();
        }
    }
}
