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
package org.rioproject.impl.container;

import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.entry.Host;
import org.rioproject.admin.ServiceBeanControl;
import org.rioproject.admin.ServiceBeanControlException;
import org.rioproject.config.Constants;
import org.rioproject.entry.OperationalStringEntry;
import org.rioproject.impl.client.JiniClient;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.impl.servicebean.ServiceBeanActivation;
import org.rioproject.servicebean.ServiceBeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The ServiceAdvertiser is a utility to help advertise a service with configured attributes.
 */
public class ServiceAdvertiser {
    static Logger logger = LoggerFactory.getLogger(ServiceAdvertiser.class.getName());

    /**
     * Advertise a ServiceBean
     *
     * @param serviceProxy Proxy to the service
     * @param context The ServiceBeanContext
     * @throws ServiceBeanControlException If the service bean cannot be advertised
     */
    public static void advertise(final Object serviceProxy,
                                 final ServiceBeanContext context,
                                 final boolean forked) throws ServiceBeanControlException {
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
        List<LookupLocator> lookupLocators = new ArrayList<LookupLocator>();
        if(context.getServiceElement().getServiceBeanConfig().getLocators()!=null) {
            Collections.addAll(lookupLocators, context.getServiceElement().getServiceBeanConfig().getLocators());
        }
        String globalLocators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME);
        if(globalLocators!=null) {
            try {
                Collections.addAll(lookupLocators, JiniClient.parseLocators(globalLocators));
            } catch (MalformedURLException e) {
                logger.warn("Configured LookupLocators [{}] are malformed", globalLocators, e);
            }
        }
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
                } else if (adminObject instanceof JoinAdmin) {
                    Entry[] configuredAttributes = getConfiguredAttributes(componentName,
                                                                           config,
                                                                           serviceName,
                                                                           context.getExportCodebase());
                    JoinAdmin joinAdmin = (JoinAdmin) adminObject;
                    ArrayList<Entry> addList = new ArrayList<Entry>();
                    logger.trace("OperationalString {}", opStringName);
                    /* Try and add an OperationalStringEntry */
                    if (opStringName != null && opStringName.length() > 0) {
                        Entry opStringEntry = loadEntry("org.rioproject.entry.OperationalStringEntry",
                                                        joinAdmin, opStringName, proxyCL);
                        if (opStringEntry != null) {
                            addList.add(opStringEntry);
                            logger.debug("Added OperationalStringEntry [{}] for {}",
                                         ((OperationalStringEntry)opStringEntry).name, serviceName);
                        } else {
                            logger.warn("Unable to obtain the OperationalStringEntry for {}", serviceName);
                        }
                    } else {
                        logger.trace("OperationalString name is {}", (opStringName == null ? "[null]" : "[empty string]"));
                    }

                    /* Next, try and add the net.jini.lookup.entry.Host */
                    Entry hostEntry = loadEntry("net.jini.lookup.entry.Host", joinAdmin, hostAddress, proxyCL);
                    if (hostEntry != null) {
                        addList.add(hostEntry);
                        logger.debug("Added Host [{}] for {}", ((Host)hostEntry).hostName, serviceName);
                    } else {
                        logger.warn("Unable to obtain the Host entry for {}", serviceName);
                    }

                    /* Process the net.jini.lookup.entry.Name attribute */
                    try {
                        Class<?> nameClass = proxyCL.loadClass("net.jini.lookup.entry.Name");
                        Constructor cons = nameClass.getConstructor(String.class);
                        Entry name = (Entry) cons.newInstance(serviceName);
                        boolean add = true;
                        /* Check if the service already has a Name, if it does
                         * ensure it is not the same name as the one this
                         * utility is prepared to add */
                        Entry[] attributes = joinAdmin.getLookupAttributes();
                        for (Entry attribute : attributes) {
                            if (attribute.getClass().getName().equals(nameClass.getName())) {
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
                        logger.warn("Name not found, cannot add a Name Entry", e);
                    }

                    /* If running forked, add JMX connection entries */
                    if(logger.isTraceEnabled())
                        logger.trace("Service: {}, forked? {}", serviceName, forked);
                    if(forked) {
                        Collections.addAll(addList, JMXUtil.getJMXConnectionEntries());
                    }

                    addList.addAll(context.getServiceBeanConfig().getAdditionalEntries());

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
                    if (logger.isTraceEnabled()) {
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
                        logger.trace("Setting groups [{}] using JoinAdmin.setLookupGroups", buff.toString());
                    }
                    joinAdmin.setLookupGroups(groups);
                    if (!lookupLocators.isEmpty()) {
                        if (logger.isTraceEnabled()) {
                            StringBuilder buff = new StringBuilder();
                            for (LookupLocator lookupLocator : lookupLocators) {
                                if (buff.length()>0 )
                                    buff.append(",");
                                buff.append(lookupLocator.toString());
                            }
                            logger.trace("Setting locators [{}] using JoinAdmin.setLookupLocators", buff.toString());
                        }
                        joinAdmin.setLookupLocators(lookupLocators.toArray(new LookupLocator[lookupLocators.size()]));
                    }
                } else {
                    logger.error("Admin must implement JoinAdmin or ServiceBeanControl to be properly advertised");
                }

            } else {
                throw new ServiceBeanControlException(String.format("Unable to obtain mechanism to advertise [%s]", serviceName));
            }
        } catch (ServiceBeanControlException e) {
            /* If we throw a ServiceBeanControlException above, just rethrow it */
            throw e;
        } catch (Throwable t) {
            logger.warn("Advertising ServiceBean, [{}: {}]", t.getClass().getName(), t.getLocalizedMessage());
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
            logger.warn("Unable to obtain configuration for service [{}]", context.getServiceElement().getName(), e);
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
                logger.trace("Obtained [{}] serviceUI declarations for [{}] using component [{}]",
                             serviceUIs.length, serviceName, serviceBeanComponent);
                attrList.addAll(Arrays.asList(serviceUIs));
            } catch (ConfigurationException e) {
                logger.warn("Getting ServiceUIs for [{}]", serviceName, e);
            }
            /* 2. Get any additional attributes */
            try {
                logger.trace("Getting {}.initialAttributes", serviceBeanComponent);
                Entry[] initialAttributes = (Entry[])config.getEntry(serviceBeanComponent,
                                                                     "initialAttributes",
                                                                     Entry[].class,
                                                                     new Entry[0]);
                logger.trace("Obtained [{}] initialAttribute declarations for [{}] using component [{}]",
                             initialAttributes.length, serviceName, serviceBeanComponent);
                attrList.addAll(Arrays.asList(initialAttributes));
            } catch (ConfigurationException e) {
                logger.warn("Getting initialAttributes for [{}]", serviceName, e);
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
            Class<?> entryClass = loader.loadClass(entryClassName);
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
            logger.warn("{} not found, cannot add {}", entryClassName, entryClassName.toLowerCase(), e);
        }
        return(entry);
    }


    /*
     * Add Attributes using the JoinAdmin
     */
    private static void addAttributes(Entry[] attrs, JoinAdmin joinAdmin) {
        try {
            StringBuilder builder = new StringBuilder();
            for(Entry a : attrs) {
                if(builder.length()>0) {
                    builder.append(", ");
                }
                builder.append("[").append(a).append("]");
            }
            if(logger.isTraceEnabled())
                logger.trace("Adding {}", builder.toString());
            joinAdmin.addLookupAttributes(attrs);
        } catch (Exception e) {
            logger.warn("Unable to add Entry attributes", e);
        }
    }
}
