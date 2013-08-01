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
package org.rioproject.jsb;

import net.jini.config.ConfigurationException;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.EventHandler;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.jmx.MBeanServerFactory;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLA;
import org.rioproject.sla.SLAPolicyHandler;
import org.rioproject.sla.SLAPolicyHandlerFactory;
import org.rioproject.sla.SLAThresholdEventAdapter;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.watch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.beans.IntrospectionException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The ServiceBeanSLAManager manages service-specific SLAs, and as needed uses
 * the {@link org.rioproject.watch.WatchInjector} to create declarative watches
 *
 * @author Dennis Reedy
 */
public class ServiceBeanSLAManager {
    private Object impl;
    private Object proxy;
    private ServiceBeanContext context;
    private EventHandler slaEventHandler;
    /** List of SLAPolicyHandler instances that have been created */
    private final List<SLAPolicyHandler> slaPolicyHandlers =
        Collections.synchronizedList(new ArrayList<SLAPolicyHandler>());
    /* A WatchInjector */
    private WatchInjector watchInjector;
    /** Table of ThresholdManager instances to MeasurableCapability
     * registrations */
    private final Map<ThresholdManager, MeasurableCapability> thresholdManagerReg = new HashMap<ThresholdManager, MeasurableCapability>();
    /**
     * Transforms SLAThresholdEvents into JMX Notifications for SLAs that are
     * system related to those the service has declared
     */
    private SLAThresholdEventAdapter slaAdapter;
    /* Monitors thread deadlocks in forked vms */
    static final String COMPONENT = ServiceBeanSLAManager.class.getName();
    static final Logger logger = LoggerFactory.getLogger(ServiceBeanSLAManager.class);

    public ServiceBeanSLAManager(final Object impl,
                                 final Object proxy,
                                 final ServiceBeanContext context,
                                 final EventHandler slaEventHandler) throws IntrospectionException {
        if(impl == null)
            throw new IllegalArgumentException("impl is null");
        if(proxy == null)
            throw new IllegalArgumentException("proxy is null");
        if(context == null)
            throw new IllegalArgumentException("context is null");
        this.impl = impl;
        this.proxy = proxy;
        this.context = context;
        this.slaEventHandler = slaEventHandler;
        watchInjector = new WatchInjector(impl, context);
    }

    /**
     * Terminate the ServiceBeanSLAManager, cleaning up pending resources
     */
    public void terminate() {
        impl = null;
        proxy = null;
        context = null;
        slaEventHandler = null;
        if(watchInjector!=null) {
            watchInjector.terminate();
            watchInjector = null;
        }
        for(Map.Entry<ThresholdManager, MeasurableCapability> entry : thresholdManagerReg.entrySet())
            entry.getKey().clear();
        thresholdManagerReg.clear();
        /* Disconnect all SLAPolicyHandler instances */
        for (SLAPolicyHandler slap : slaPolicyHandlers) {
            if(slap.getThresholdManager()!=null)
                slap.getThresholdManager().clear();
            slap.disconnect();
        }
    }

    /**
     * Add a new SLA.
     *
     * @param slas Array of SLA instances to add
     */
    public void addSLAs(final SLA[] slas) {
        for (SLA sla : slas) {
            String identifier = sla.getIdentifier();
            /* Get the WatchDescriptors from the SLA. If there are no
             * WatchDescriptors found, use the SLA's ID. Othwerwise, use the
             * first WatchDescriptor name as the identifier
             */
            WatchDescriptor[] wds = sla.getWatchDescriptors();
            if (wds.length > 0) {
                identifier = wds[0].getName();
            }
            SLAPolicyHandler handler = null;
            ServiceElement elem = context.getServiceElement();
            /* Check if the SLA matches a MeasurableCapability.  */
            MeasurableCapability mCap = getMeasurableCapability(identifier);
            if (mCap != null) {
                logger.trace("[{}] SLA [{}] correlates to a MeasurableCapability", elem.getName(), identifier);
                try {
                    /* Load the SLA PolicyHandler and set attributes */
                    handler = createSLAPolicyHandler(sla, null);

                    ThresholdManager tMgr = new BoundedThresholdManager(mCap.getId());
                    tMgr.setThresholdValues(sla);
                    handler.setThresholdManager(tMgr);
                    mCap.addSecondaryThresholdManager(tMgr);
                    thresholdManagerReg.put(tMgr, mCap);
                    logger.trace("[{}] SLA ID [{}], associated to MeasurableCapability={}, SLAPolicyHandler={}",
                                 elem.getName(), identifier, mCap.getClass().getName(), handler.getClass().getName());
                } catch (Exception e) {
                    logger.warn("Creating SLAPolicyHandler for system SLA [{}]", sla.getIdentifier(), e);
                }

            /* Check if the SLA matches the ThreadDeadlockMonitor. */
            } else if(identifier.equals(ThreadDeadlockMonitor.ID)) {
                WatchDescriptor wDesc = ServiceElementUtil.getWatchDescriptor(elem, ThreadDeadlockMonitor.ID);
                if(wDesc==null)
                    wDesc = ThreadDeadlockMonitor.getWatchDescriptor();

                /* If the service is not forked and running in it's own VM,
                 * we currently do not allow service specific thread deadlock
                 * monitoring. There is one Cybernode-based
                 * ThreadDeadlockMonitor that will send out notifications */
                if(wDesc.getMBeanServerConnection()==null && !runningForked()) {
                    logger.warn("Thread deadlock detection is provided at the process level, not " +
                                "enabled on a service-by-service approach within a Cybernode. The SLA declaration for " +
                                "the [{}] service will be ignored. Note that thread deadlock " +
                                "detection has been enabled by the Cybernode.", elem.getName());
                    return;
                }
                if(wDesc.getPeriod()<1000) {
                    logger.info("Thread deadlock monitoring has been disabled " +
                                "for service [{}]. The configured thread deadlock check time was " +
                                "[{}]. To enable thread deadlock monitoring, the thread deadlock " +
                                "check time must be >= 1000 milliseconds.", elem.getName(), wDesc.getPeriod());
                    return;
                }
                logger.info("Setting Thread deadlock detection: {}", sla);
                try {
                    ClassLoader loader = impl.getClass().getClassLoader();
                    /* Load the SLA PolicyHandler and set attributes */
                    handler = createSLAPolicyHandler(sla, loader);
                    Method getThreadDeadlockCalculable =
                        ThreadDeadlockMonitor.class.getMethod("getThreadDeadlockCalculable");
                    ThreadDeadlockMonitor threadDeadlockMonitor = new ThreadDeadlockMonitor();
                    if(wDesc.getMBeanServerConnection()!=null) {
                        ThreadMXBean threadMXBean =
                            JMXUtil.getPlatformMXBeanProxy(wDesc.getMBeanServerConnection(),
                                                           ManagementFactory.THREAD_MXBEAN_NAME,
                                                           ThreadMXBean.class);
                        threadDeadlockMonitor.setThreadMXBean(threadMXBean);
                    }
                    watchInjector.inject(wDesc, threadDeadlockMonitor, getThreadDeadlockCalculable);

                } catch (Exception e) {
                    logger.warn("Creating SLAPolicyHandler for SLA [{}]", sla.getIdentifier(), e);
                }
            } else {
                try {
                    handler = createSLAPolicyHandler(sla, impl.getClass().getClassLoader());
                    /* Inject watch if necessary */
                    for (WatchDescriptor wd : wds) {
                        try {
                            watchInjector.inject(wd);
                        } catch (ConfigurationException e) {
                            logger.warn("Injecting Watch [{}] for SLA [{}]",  wd.getName(), sla.getIdentifier(), e);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Creating SLAPolicyHandler for SLA [{}]", sla.getIdentifier(), e);
                }
            }
            if (handler != null) {
                logger.trace("[{}] Adding SLA [{}] to the Watch Registry for subsequent association",
                             context.getServiceElement().getName(), identifier);
                context.getWatchRegistry().addThresholdListener(identifier, handler);
            } else {
                logger.error("[{}] Could not addSLA [{}] to the Watch Registry for subsequent association, handler is null",
                             context.getServiceElement().getName(), identifier);
            }
        }
    }

    /**
     * Update SLAs
     *
     * @param slas Array of SLAs to update
     */
    public void updateSLAs(final SLA[] slas) {
        /* Create a representation of the current Collection so we
         * can determine if any SLAPolicyHandlers are no longer needed */
        ArrayList<SLAPolicyHandler> toDiscardList = new ArrayList<SLAPolicyHandler>(slaPolicyHandlers);

        /* List for new SLAs, that is SLAs that do not have an ID equal to
         * a current Watch */
        ArrayList<SLA> toAddList = new ArrayList<SLA>();
        for (SLA sla : slas) {
            SLAPolicyHandler slap = getSLAPolicyHandler(sla);
            if (slap == null) {
                toAddList.add(sla);
            } else {
                toDiscardList.remove(slap);
                if (SLAPolicyHandlerFactory.slaPolicyHandlerChanged(sla, slap)) {
                    if(logger.isTraceEnabled()) {
                        StringBuilder b = new StringBuilder();
                        b.append("The SLAPolicyHandler for [");
                        b.append(sla.getIdentifier());
                        b.append("] has changed. ");
                        b.append("Configured SLAPolicyHandler=[");
                        b.append(sla.getSlaPolicyHandler());
                        b.append("], SLAPolicyHandler class=[");
                        b.append(slap.getClass().getName());
                        logger.trace(b.toString());
                    }
                    removeSLAPolicyHandler(slap);
                    toAddList.add(sla);
                } else {
                    if(logger.isTraceEnabled()) {
                        StringBuilder b = new StringBuilder();
                        b.append("Updating the SLAPolicyHandler for [");
                        b.append(sla.getIdentifier());
                        b.append("] with new SLA values: ");
                        b.append(sla);                        
                        logger.trace(b.toString());
                    }
                    slap.setSLA(sla);
                    WatchDescriptor[] wds = sla.getWatchDescriptors();
                    for (WatchDescriptor wd : wds) {
                        try {
                            watchInjector.modify(wd);
                        } catch (ConfigurationException e) {
                            logger.warn("Modifying WatchDescriptor [{}] for SLA [{}]", wd.getName(), sla.getIdentifier(),e);
                        }
                    }
                }
            }
        }
        /* Add any new SLAs */
        addSLAs(toAddList.toArray(new SLA[toAddList.size()]));
        /* Remove uneeded SLAs */
        SLAPolicyHandler[] toDiscard =
            toDiscardList.toArray(
                new SLAPolicyHandler[toDiscardList.size()]);
        for (SLAPolicyHandler d : toDiscard) {
            removeSLAPolicyHandler(d);
        }

        /* Discard uneeded watches */
        discardWatches(slas);
    }

    /**
     * If the service bean has registered to JMX, and if there are declared
     * system SLAs that have been registered and the SLAThresholdEventAdapter is
     * null, create the adapter and register if
     */
    public void createSLAThresholdEventAdapter() {
        if(slaAdapter != null)
            return;
        if(!slaPolicyHandlers.isEmpty()) {
            try {
                ObjectName objectName = JMXUtil.getObjectName(context, "", context.getServiceElement().getName());
                if(MBeanServerFactory.getMBeanServer().isRegistered(objectName)) {
                    slaAdapter =
                        new SLAThresholdEventAdapter(objectName,
                                                     context.getServiceBeanManager().getNotificationBroadcasterSupport());
                }
            } catch(Exception e) {
                logger.debug("Registering SLAThresholdEventAdapter", e);
            }
        }
    }

    /**
     * Create a SLAPolicyHandler using the SLA
     *
     * @param sla The SLA to obtain a SLAPolicyHandler for
     * @param loader The Classloader to use to load the SLAPolicyHandler
     *
     * @return An SLAPolicyHandler implementation ready for use
     *
     * @throws Exception if the SLAPolicyHandler cannot be created
     */
    private SLAPolicyHandler createSLAPolicyHandler(final SLA sla, final ClassLoader loader)
        throws Exception {
        SLAPolicyHandler slappy = SLAPolicyHandlerFactory.create(sla, proxy, slaEventHandler, context, loader);
        logger.trace("[{}] SLA [{}] Created SLAPolicyHandler [{}]",
                     context.getServiceElement().getName(), sla.getIdentifier(), slappy.getClass().getName());
        slaPolicyHandlers.add(slappy);
        return (slappy);
    }

    /*
     * Get all SLAPolicyHandler instances
     *
     * @return Array of SLAPolicyHandler instances. A new array is allocated
     *         each time. If there are no SLAPolicyHandler instances, a
     *         zero-length array is returned
     */
    private SLAPolicyHandler[] getSLAPolicyHandlers() {
        SLAPolicyHandler[] handlers = slaPolicyHandlers.toArray(new SLAPolicyHandler[slaPolicyHandlers.size()]);
        return (handlers);
    }

    /*
     * Get a SLAPolicyHandler instance
     *
     * @param sla The SLA to use, must not be null
     *
     * @return The SLAPolicyHandler instance for the provided SLA or null if
     * not found or the sla parameter is <code>null</code>
     */
    private SLAPolicyHandler getSLAPolicyHandler(final SLA sla) {
        if(sla!=null) {
            SLAPolicyHandler[] slappys = getSLAPolicyHandlers();
            for (SLAPolicyHandler slappy : slappys) {
                if (slappy.getID().equals(sla.getIdentifier()))
                    return (slappy);
            }
        }
        return (null);
    }

    /*
     * Unregister and remove a SLAPolicyHandler
     */
    private void removeSLAPolicyHandler(final SLAPolicyHandler slaPolicyHandler) {
        slaPolicyHandlers.remove(slaPolicyHandler);
        slaPolicyHandler.disconnect();
        context.getWatchRegistry().removeThresholdListener(slaPolicyHandler.getID(), slaPolicyHandler);
        ThresholdManager tMgr = slaPolicyHandler.getThresholdManager();
        MeasurableCapability mCap = thresholdManagerReg.remove(tMgr);
        if(mCap != null) {
            mCap.removeSecondaryThresholdManager(tMgr);
        }
    }

    /*
     * Discard uneeded Watches. This is determined by comparing
     * an array of Watch instances, to the collection of known Watch
     * instances. Discard Watch instances which do not match to
     * Watch identifiers
     */
    private void discardWatches(final SLA[] serviceSLAs) {
        if(watchInjector==null)
            return;
        String[] createdWatches = watchInjector.getWatchNames();
        if(createdWatches.length==0)
            return;
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(createdWatches));
        WatchDescriptor[] configuredWatches = getWatchDescriptors(serviceSLAs);

        for (WatchDescriptor wd : configuredWatches) {
            for (String wName : createdWatches) {
                if (wd.getName().equals(wName)) {
                    list.remove(wName);
                    break;
                }
            }
        }
        String[] removals = list.toArray(new String[list.size()]);
        for (String remove : removals) {
            watchInjector.remove(remove);
        }
    }

    /*
     * Get the WatchDescriptor instances from the SLA configs
     */
    private WatchDescriptor[] getWatchDescriptors(final SLA[] slas) {
        ArrayList<WatchDescriptor> list = new ArrayList<WatchDescriptor>();
        for (SLA sla : slas) {
            WatchDescriptor[] wDesc = sla.getWatchDescriptors();
            list.addAll(Arrays.asList(wDesc));
        }
        return (list.toArray(new WatchDescriptor[list.size()]));
    }

    /*
     * Get the MeasurableCapability for a SLA
     *
     * @return The MeasurableCapability for a SLA. If a matching
     *         MeasurableCapability cannot be found return null
     */
    private MeasurableCapability getMeasurableCapability(final String id) {
        MeasurableCapability[] mCaps = context.getComputeResourceManager().
            getMatchedMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps) {
            if (id.equals(mCap.getId()))
                return (mCap);
        }
        return (null);
    }

    private boolean runningForked() {
        return (System.getProperty(Constants.SERVICE_BEAN_EXEC_NAME)!=null);
    }
}
