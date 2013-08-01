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
package org.rioproject.watch;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jmx.GenericMBeanInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The WatchInjector provides support for declarative Watch management, by
 * taking a {@link org.rioproject.watch.WatchDescriptor}  and creating
 * {@link org.rioproject.watch.SamplingWatch} instances which are then registered for
 * an instantiated service.
 *
 * <p>If the watch already exists in the bean's
 * {@link org.rioproject.watch.WatchRegistry}, the watch will not be added.
 *
 * <p>The bean must declare a Java bean getter method that has as it's return
 * type one of the following:
 *
 * <ul>
 * <li>Supported primitive types: int.class, long.class, float.class, double.class
 * <li>Supported types: Integer.class, Long.class, Lloat.class, Double.class
 * </ul>
 *
 * The read property return type is verified prior to watch creation.
 * For example, given a bean called Foo, which declares a method called
 * <tt>getCount()</tt> :
 *
 * <pre>
 * public class Foo  {
 *     ...
 *     public long getCount() {
 *         return(value);
 *     }
 *     ...
 * }
 * </pre>
 *
 * Finally, in the OperationalString, Watch declaration is accomplished using
 * the embedded the &lt;Monitor&gt; element, for example:
 * <pre>
 * &lt;SLA ID="backlog" Low="100" High="500"&gt;
 *     &lt;PolicyHandler type="scaling" max="10"
 *                       lowerDampener="3000" upperDampener="3000"/&gt;
 *     <b>&lt;Monitor name="entryCounter" property="count" period="10000"/&gt;</b>
 * &lt;/SLA>
 * </pre>
 *
 * This declaration creates a {@link org.rioproject.watch.SamplingWatch} with a name
 * of <tt>entryCounter</tt>, which adds the value returned from the
 * <tt>getCount()</tt> method every 10 seconds.
 *
 * @author Dennis Reedy
 */
public class WatchInjector {
    private Object impl;
    private PropertyDescriptor[] pds;
    private ServiceBeanContext context;
    /** Collection of created Watch objects */
    private final List<Watch> createdWatches = new ArrayList<Watch>();
    private final List<Thread> mbeanCheckThreads = new ArrayList<Thread>();
    static final String COMPONENT = "org.rioproject.watch.WatchInjector";
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    public WatchInjector(Object impl, ServiceBeanContext context)
        throws IntrospectionException {
        if(impl ==null)
            throw new IllegalArgumentException("impl is null");
        if(context == null)
            throw new IllegalArgumentException("context is null");
        this.impl = impl;
        this.context = context;
        Class aClass = impl.getClass();
        BeanInfo bi = Introspector.getBeanInfo(aClass);
        pds = bi.getPropertyDescriptors();
    }

    /**
     * Add a WatchDescriptor, creating the Watch
     *
     * @param wDesc The WatchDescriptor to add, must not be null
     *
     * @return The created Watch, or <code>null</code> if it could not be
     * created. If the watch already exists, the current Watch wil be returned
     * @throws Exception if there are problems getting the configuration from
     * the context
     */
    public Watch inject(WatchDescriptor wDesc) throws Exception {
        if(wDesc ==null)
            throw new IllegalArgumentException("wDesc is null");

        Watch watch = checkForExistingWatch(wDesc.getName());
        if(watch!=null)
            return(watch);

        if(wDesc.getObjectName()!=null) {
            watch = createWatch(wDesc, impl, context.getConfiguration());
            if(watch!=null) {
                context.getWatchRegistry().register(watch);
                createdWatches.add(watch);
            }
        } else {
            boolean propertyMatch = false;
            String propertyName = wDesc.getProperty();
            for (PropertyDescriptor pd : pds) {
                String propName = pd.getName();
                if (propName.equals(propertyName)) {
                    propertyMatch = true;
                    Method accessor = pd.getReadMethod();
                    if (accessor != null) {
                        watch = createWatch(wDesc, impl, context.getConfiguration());
                        if(watch!=null) {
                            context.getWatchRegistry().register(watch);
                            createdWatches.add(watch);
                        }
                    } else {
                        logger.warn("WatchDescriptor [{}], with declared propertyName [{}], " +
                                    "matched, no readMethod found on target object [{}]",
                                    wDesc.toString(), propertyName, impl.getClass().getName());
                    }
                }
            }

            if(!propertyMatch) {
                logger.warn("WatchDescriptor [{}], with declared propertyName [{}] not found on target object [{}]",
                            wDesc.toString(), propertyName, impl.getClass().getName());
            }
        }
        if(watch!=null)
            ((SamplingWatch)watch).start();
        return(watch);
    }

    /**
     * Add a WatchDescriptor, creating the Watch
     *
     * @param wDesc The WatchDescriptor to add, must not be null
     * @param bean The target bean to poll data from
     * @param accessor Method to get data from, must not be null
     *
     * @return The created Watch, or <code>null</code> if it could not be
     * created. If the watch already exists, the current Watch wil be returned
     * @throws Exception if there are problems getting the configuration from
     * the context
     */
    public Watch inject(WatchDescriptor wDesc,
                        Object bean,
                        Method accessor) throws Exception {
        if(wDesc ==null)
            throw new IllegalArgumentException("wDesc is null");
        if(accessor ==null)
            throw new IllegalArgumentException("accessor is null");

        Watch watch = checkForExistingWatch(wDesc.getName());
        if(watch!=null)
            return(watch);

        watch = createWatch(wDesc, bean, accessor, context.getConfiguration());
        if(watch!=null) {
            context.getWatchRegistry().register(watch);
            createdWatches.add(watch);
        }

        if(watch!=null)
            ((SamplingWatch)watch).start();

        return(watch);
    }

    /**
     * Check to see if a Watch of the provided name exists
     *
     * @param watchName The Watch name to check
     *
     * @return The matching Watch or null if not found
     */
    private Watch checkForExistingWatch(String watchName) {
        if(context.getWatchRegistry().findWatch(watchName)!=null) {
            if(logger.isDebugEnabled())
                logger.debug("WatchDescriptor for Watch [{}], found on target object's WatchRegistry [{}]",
                             watchName, impl.getClass().getName()+"]");
            /* return the first matching Watch, which in generally every
             * case is exactly what is required */
            return(context.getWatchRegistry().findWatch(watchName));
        }
        return null;
    }

    public void terminate() {
        impl = null;
        context = null;
        pds = null;        
        createdWatches.clear();
        Thread[] threads =
            mbeanCheckThreads.toArray(new Thread[mbeanCheckThreads.size()]);
        for(Thread t : threads) {
            if(t.isAlive())
                t.interrupt();
        }
    }

    protected Watch createWatch(WatchDescriptor wDesc,
                                Object bean,
                                Configuration config) throws Exception {
        return createWatch(wDesc, bean, null, config);
    }

    protected Watch createWatch(WatchDescriptor wDesc,
                                Object bean,
                                Method accessor,
                                Configuration config) throws Exception {
        SamplingWatch watch = null;
        if(wDesc.getObjectName()!=null) {
            if(wDesc.getMBeanServerConnection()==null) {
                logger.warn("Cannot create Watch to monitor " +
                            "MBean [{}] without " +
                            "an MBeanServerConnection. You are using Java " +
                            "version [{}], " +
                            "that does either not support the JMX Attach API " +
                            "or the MBeanServerConnection could not be " +
                            "obtained for the external service.",
                            wDesc.getObjectName(), System.getProperty("java.version"));
                return null;
            }
            Thread t = new MBeanVerification(wDesc, config);
            t.start();
            mbeanCheckThreads.add(t);
        } else {
            watch = new SamplingWatch(wDesc.getName(), config);
            watch.setBean(bean);
            watch.setAccessor(accessor);
            watch.setProperty(wDesc.getProperty());
            watch.setPeriod(wDesc.getPeriod());            
        }
        return(watch);
    }

    /**
     * Get all created Watch instances
     *
     * @return All watches that have been created and registered
     * by this utility. If there are no watches created and registered by this
     * utility a zero-length array is returned. A new array is allocated each
     * time
     */
    private Watch[] getWatches() {
        Watch[] watches = new Watch[createdWatches.size()];
        for(int i=0; i< watches.length; i++)
            watches[i] = createdWatches.get(i);
        return(watches);
    }

    /**
     * Get all created Watch names
     *
     * @return The names of all watches that have been created and registered
     * by this utility. If there are no watches created and registered by
     * this utility a zero-length array is returned. A new array is
     * allocated each time
     */
    public String[] getWatchNames() {
        String[] names = new String[createdWatches.size()];
        for(int i = 0; i < names.length; i++)
            names[i] = (createdWatches.get(i)).getId();
        return (names);
    }

    /**
     * If this utility created a Watch with the provided name, return the Watch.
     * Otherwise return null
     *
     * @param name Te name to look for
     *
     * @return A Watch instance or null if not created
     */
    private Watch getWatch(String name) {
        Watch watch = null;
        Watch[] watches = getWatches();
        for (Watch watche : watches) {
            if (watche.getId().equals(name)) {
                watch = watche;
                break;
            }
        }
        return(watch);
    }

    /**
     * Modify an injected Watch
     *
     * @param wDesc The WatchDescriptor to modify, must not be null
     *
     * @throws ConfigurationException if there are errors reading the
     * configuration
     */
    public void modify(WatchDescriptor wDesc) throws ConfigurationException {
        if(wDesc == null)
            throw new IllegalArgumentException("wDesc is null");
        Watch watch = getWatch(wDesc.getName());
        if(watch == null) {
            if(logger.isDebugEnabled())
                logger.debug("Unable to modify Watch [{}], not created by the WatchInjector", wDesc.getName());
            return;
        }
        SamplingWatch sWatch = (SamplingWatch)watch;
        if(wDesc.getPeriod()!=sWatch.getPeriod()) {
            sWatch.setPeriod(wDesc.getPeriod());
        } else if(!(wDesc.getProperty().equals(sWatch.getProperty()))) {
            sWatch.setProperty(wDesc.getProperty());
        }

    }

    /**
     * Unregister and remove an injected Watch
     *
     * @param wDesc The WatchDescriptor to remove, must not be null
     */
    public void remove(WatchDescriptor wDesc) {
        if(wDesc == null)
            throw new IllegalArgumentException("wDesc is null");
        Watch watch = getWatch(wDesc.getName());
        if(watch==null) {
            logger.warn("Unable to remove Watch [{}], not created by the WatchInjector", wDesc.getName());
            return;
        }
        if(createdWatches.remove(watch))
            context.getWatchRegistry().deregister(watch);
    }

    /**
     * Unregister and remove an injected Watch
     *
     * @param name The Watch name (id) to remove, must not be null
     */
    public void remove(String name) {
        if(name == null)
            throw new IllegalArgumentException("name is null");
        Watch watch = getWatch(name);
        if(watch==null) {
            logger.warn("Unable to remove Watch [{}], not created by the WatchInjector", name);
            return;
        }
        if(createdWatches.remove(watch))
            context.getWatchRegistry().deregister(watch);
    }


    class MBeanVerification extends Thread {
        WatchDescriptor wDesc;
        ObjectName oName;
        Configuration config;
        boolean alive = true;

        MBeanVerification(WatchDescriptor wDesc, Configuration config) {
            this.wDesc = wDesc;
            this.config = config;
        }

        @Override
        public void interrupt() {
            alive = false;
            super.interrupt();
        }

        public void run() {
            try {
                boolean proceed = false;
                oName = new ObjectName(wDesc.getObjectName());
                int iterations = 0;
                while(alive) {
                    proceed = wDesc.getMBeanServerConnection().isRegistered(oName);
                    if(!proceed) {
                        try {
                            if(iterations++%5==0)
                                logger.info("Waiting for MBean [{}] to become available before injecting " +
                                            "watch and associated SLA monitoring", wDesc.getObjectName());
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if(proceed) {
                    createAndRegisterWatch();
                    logger.info("MBean [{}] is available, injecting watch [{}] and associated SLA monitoring",
                                wDesc.getObjectName(), wDesc.getName());
                }
            } catch (MalformedObjectNameException e) {
                logger.warn("The value ["+wDesc.getObjectName()+"] cannot be " +
                            "used to create an ObjectName. Please verify the " +
                            "format of the value and retry. The service will " +
                            "continue to execute, but the monitor you have " +
                            "requested to be created to observe this MBean can " +
                            "not be created",
                            e);
            } catch (IOException e) {
                logger.warn("A connection exception has occurred communicating " +
                           "to the attached MBeanServer for the exec'd service. " +
                           "The service will continue to execute, " +
                           "but the monitor you have requested to be created to " +
                           "observe this MBean can not be created",
                           e);
            } finally {
                mbeanCheckThreads.remove(Thread.currentThread());
            }
        }

        void createAndRegisterWatch() {
            SamplingWatch watch = new SamplingWatch(wDesc.getName(), config);
            GenericMBeanInvoker mbi = new GenericMBeanInvoker();
            mbi.setObjectName(oName);
            mbi.setAttribute(wDesc.getAttribute());
            mbi.setMBeanServerConnection(wDesc.getMBeanServerConnection());
            watch.setBean(mbi);
            watch.setProperty(GenericMBeanInvoker.GETTER);
            watch.setPeriod(wDesc.getPeriod());
            watch.start();
            context.getWatchRegistry().register(watch);
            createdWatches.add(watch);
        }
    }

}
