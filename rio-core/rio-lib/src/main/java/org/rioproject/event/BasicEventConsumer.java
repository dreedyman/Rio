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
package org.rioproject.event;

import com.sun.jini.config.Config;
import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.config.Configuration;
import net.jini.config.EmptyConfiguration;
import net.jini.core.entry.Entry;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.export.Exporter;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.config.ExporterConfig;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.util.TimeConstants;
import org.rioproject.util.TimeUtil;
import org.rioproject.watch.StopWatch;
import org.rioproject.watch.Watch;
import org.rioproject.watch.WatchDataSourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The BasicEventConsumer is a helper class that manages the
 * discovery of {@link EventProducer} instances that provide support for
 * user defined events. The BasicEventConsumer is to be used as a
 * local (within a JVM) utility, managing the discovery, event registration and
 * leasing of {@link net.jini.core.event.EventRegistration} objects on behalf of
 * client(s). In this manner, clients wishing to easily register (subscribe) for the
 * notification of an event in the distributed system need not be overly
 * concerned with the underlying semantics and management of event
 * registrations, leases and events.
 *
 * @author Dennis Reedy
 */
public class BasicEventConsumer implements EventConsumer, ServerProxyTrust  {
    /** The remote ref (e.g. stub or dynamic proxy) for the BasicEventConsumer */
    private EventConsumer eventConsumer;
    /** The Exporter for the BasicEventConsumer */
    private Exporter exporter;
    /** The proxyPreparer for EventRegistration */
    private ProxyPreparer eventLeasePreparer;
    protected final List<RemoteServiceEventListener> eventSubscribers =
        Collections.synchronizedList(new ArrayList<RemoteServiceEventListener>());
    protected final Hashtable<ServiceID, EventLeaseManager> leaseTable = new Hashtable<ServiceID, EventLeaseManager>();
    protected final Map<Long, EventRegistration> eventRegistrationTable = new Hashtable<Long, EventRegistration>();
    protected EventDescriptor edTemplate;
    protected int received = 0;
    protected long sktime, ektime;
    protected MarshalledObject handback = null;
    /** Default Lease duration is 5 minutes */
    protected static final long DEFAULT_LEASE_DURATION = TimeConstants.FIVE_MINUTES;
    protected long leaseDuration = DEFAULT_LEASE_DURATION;
    protected StopWatch responseWatch = null;
    protected WatchDataSourceRegistry watchRegistry = null;
    /** Number of retries to attempt to connect to an EventProducer */
    private int connectRetries;
    private static final int DEFAULT_CONNECT_RETRY_COUNT = 3;
    /** How long to wait between retries */
    private long retryWait;
    private static final long DEFAULT_RETRY_WAIT = TimeConstants.ONE_SECOND;
    public static final String RESPONSE_WATCH = "Response Time";
    static final String COMPONENT = "org.rioproject.event";
    /** The Logger */
    static Logger logger = LoggerFactory.getLogger(BasicEventConsumer.class);
    private Configuration config;
    private final ExecutorService service;

    /**
     * Create a BasicEventConsumer with an EventDescriptor
     *
     * @param edTemplate The EventDescriptor template
     *
     * @throws Exception If the BasicEventConsumer cannot be created
     */
    public BasicEventConsumer(final EventDescriptor edTemplate) throws Exception {
        this(edTemplate, null, null, null);
    }

    /**
     * Create a BasicEventConsumer with a RemoteServiceEventListener
     *
     * @param listener The RemoteServiceEventListener
     *
     * @throws Exception If the BasicEventConsumer cannot be created
     */
    public BasicEventConsumer(final RemoteServiceEventListener listener) throws Exception {
        this(null, listener, null, null);
    }

    /**
     * Create a BasicEventConsumer with an EventDescriptor and a
     * RemoteServiceEventListener
     *
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     *
     * @throws Exception If the BasicEventConsumer cannot be created
     */
    public BasicEventConsumer(final EventDescriptor edTemplate, final RemoteServiceEventListener listener)
        throws Exception {
        this(edTemplate, listener, null, null);
    }

    /**
     * Create a BasicEventConsumer with an EventDescriptor and a
     * RemoteServiceEventListener
     *
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     * @param config Configuration object
     *
     * @throws Exception If the BasicEventConsumer cannot be created
     */
    public BasicEventConsumer(final EventDescriptor edTemplate,
                              final RemoteServiceEventListener listener,
                              final Configuration config) throws Exception {
        this(edTemplate, listener, null, config);
    }

    /**
     * Create a BasicEventConsumer with an EventDescriptor, a
     * RemoteServiceEventListener, and a MarshalledObject to be used as a
     * handback
     *
     * @param edTemplate The EventDescriptor template
     * @param listener The RemoteServiceEventListener
     * @param handback The MarshalledObject to be used as a handback
     * @param config Configuration object
     *
     * @throws Exception If the BasicEventConsumer cannot be created
     */                                  
    public BasicEventConsumer(final EventDescriptor edTemplate,
                              final RemoteServiceEventListener listener,
                              final MarshalledObject handback,
                              final Configuration config) throws Exception {
        ProxyPreparer basicLeasePreparer = new BasicProxyPreparer();

        this.config = config;
        if(this.config == null)
            this.config = EmptyConfiguration.INSTANCE;

        leaseDuration = Config.getLongEntry(this.config,
                                            COMPONENT,
                                            "eventLeaseDuration",
                                            DEFAULT_LEASE_DURATION, //default
                                            1000*60,                // min
                                            Long.MAX_VALUE);        // max

        exporter = ExporterConfig.getExporter(this.config, COMPONENT, "eventConsumerExporter");

        eventLeasePreparer = (ProxyPreparer)this.config.getEntry(COMPONENT,
                                                                 "eventLeasePreparer",
                                                                 ProxyPreparer.class,
                                                                 basicLeasePreparer);
        connectRetries = Config.getIntEntry(this.config,
                                            COMPONENT,
                                            "connectRetries",
                                            DEFAULT_CONNECT_RETRY_COUNT, // default
                                            0,                           // min
                                            5);                          // max

        retryWait = Config.getLongEntry(this.config,
                                        COMPONENT,
                                        "retryWait",
                                        DEFAULT_RETRY_WAIT,  // default
                                        0,                   // min
                                        Long.MAX_VALUE);     // max

        eventConsumer = (EventConsumer)exporter.export(this);
        //refQueue = new ReferenceQueue();
        this.edTemplate = edTemplate;
        if(edTemplate!=null)
            logger.trace("Create BasicEventConsumer for EventDescriptor : {}", edTemplate.toString());
        this.handback = handback;
        if(listener != null)
            eventSubscribers.add(listener);
        service = Executors.newCachedThreadPool();
    }

    /**
     * The terminate method will de-register for event notifications across all
     * discovered EventProducer instances. Invocation of this method will result
     * in the cancellation of any leases involved with event registration and
     * the removal from the event notification pool. This method will also
     * unexport the EventConsumer, removing it from the RMI runtime. This method
     * will also destroy the response time watch if it was created
     */
    public void terminate() {
        if(service!=null)
            service.shutdownNow();
        /* Deregister all listeners */
        RemoteServiceEventListener[] listeners = getListeners();
        for (RemoteServiceEventListener listener : listeners) {
            try {
                deregister(listener);
            } catch (Exception e) {
                logger.warn("Deregistering EventConsumer", e);
            }
        }
        /* Destroy the watch */
        destroyWatch();
        /* If we exported, unexport, use force=true */
        if(eventConsumer != null) {
            try {
                exporter.unexport(true);
            } catch(IllegalStateException e) {
                logger.warn("EventConsumer not unexported", e);
            }
        }
    }

    /**
     * Register a RemoteServiceEventListener to this EventConsumer. If there are
     * no RemoteServiceEventListener instances registered events will be dropped
     * by this BasicEventConsumer. Each registered RemoteServiceEventListener
     * will be notified for each event received.
     *
     * @param listener The RemoteServiceEventListener
     * @return true if the RemoteServiceEventListener has been added
     */
    public boolean register(final RemoteServiceEventListener listener) {
        return (register(listener, null));
    }

    /**
     * Register a RemoteServiceEventListener to this EventConsumer with a
     * MarshalledObject handback object. If there are no
     * RemoteServiceEventListener instances registered events will be dropped by
     * this BasicEventConsumer.
     *
     * Each registered RemoteServiceEventListener will be notified for each
     * event received
     *
     * @param listener The RemoteServiceEventListener
     * @param handback The MarshalledObject to be used as a handback
     * @return true if the RemoteServiceEventListener has been added
     */
    public boolean register(final RemoteServiceEventListener listener, final MarshalledObject handback) {
        this.handback = handback;
        boolean added = eventSubscribers.add(listener);
        return (added);
    }

    /**
     * De-registers a registered RemoteServiceEventListener from this
     * EventConsumer
     *
     * @param listener The RemoteServiceEventListener
     * @return true if the RemoteServiceEventListener was removed
     */
    public boolean deregister(final RemoteServiceEventListener listener) {
        return(removeListener(listener));
    }

    /**
     * Create a response time watch for this EventConsumer, which will track the
     * response time for event consumers, measured by how long the response time
     * takes
     *
     * @param watchRegistry The WatchDataSourceRegistry to register the watch
     *
     * @return The Watch
     */
    public Watch createWatch(final WatchDataSourceRegistry watchRegistry) {
        if(watchRegistry == null)
            throw new IllegalArgumentException("watchRegistry is null");
        if(responseWatch!=null)
            return responseWatch;
        responseWatch = new StopWatch(RESPONSE_WATCH, config);
        this.watchRegistry = watchRegistry;
        watchRegistry.register(responseWatch);
        return responseWatch;
    }

    /**
     * Destroys the response time watch. Once this method is called the response
     * time watch will be rendered useless
     */
    public void destroyWatch() {
        if(watchRegistry != null) {
            watchRegistry.deregister(responseWatch);
        }
        if(responseWatch != null) {
            try {
                responseWatch.getWatchDataSource().clear();
                responseWatch.getWatchDataSource().close();
            } catch(RemoteException e) {
                logger.warn("RemoteException Destroying Watch", e);
            }
        }
        responseWatch = null;
    }

    /**
     * Get the response time watch for this EventConsumer
     *
     * @return The response time watch for this EventConsumer
     */
    public Watch getWatch() {
        return (responseWatch);
    }

    /**
     * Given a ServiceItem this method checks to see if the ServiceItem contains
     * a proxy of type EventProducer, performs event registration and ensures the
     * Lease contained in the event registration is managed by a
     * LeaseRenewalManager
     *
     * @param item The ServiceItem
     * @return The {@link net.jini.core.event.EventRegistration}, or null if the
     * service is not an {@link EventProducer} or the {@link EventDescriptor}
     * template the BasicEventConsumer was started with cannot be matched
     *
     * @throws IllegalArgumentException if the item parameter is null
     */
    public EventRegistration register(final ServiceItem item) {
        if(item==null)
            throw new IllegalArgumentException("item is null");

        if(edTemplate==null)
            throw new IllegalStateException("An EventDescriptor template has not " +
                                            "been set when creating the " +
                                            "BasicEventConsumer, this utility " +
                                            "cannot determine the EventDescriptor " +
                                            "to register for. Check your use " +
                                            "of the BasicEventConsumer and " +
                                            "construct it with an EventDescriptor");

        if(!(item.service instanceof EventProducer)) {
            logger.trace("Service is not an EventProducer");
            return (null);
        }

        EventDescriptor eDesc = getDescriptor(item.attributeSets, edTemplate);
        if(eDesc == null) {
            logger.debug("Cannot get EventDescriptor match");
            logger.trace("ServiceItem.service ClassLoader {}, EventDescriptor {}, EventDescriptor ClassLoader {}",
                         item.service.getClass().getClassLoader().toString(),
                         edTemplate.toString(),
                         edTemplate.getClass().getClassLoader().toString());
            return (null);
        }

        EventProducer producer = (EventProducer)item.service;
        return register(producer, eDesc, item.serviceID);
    }

    /**
     * Register for notification of event from an {@link EventProducer}.
     *
     * @param eventProducer The EventProducer, must not be null
     * @param eventDesc The EventDescriptor, must not be null
     * @param serviceID The serviceID of the EventProducer to register
     * @return The {@link net.jini.core.event.EventRegistration}, or null if the
     * service is not an {@link EventProducer} or the {@link EventDescriptor}
     * template the BasicEventConsumer was started with cannot be matched
     *
     * @throws IllegalArgumentException if any og the parameters are null
     */
    public EventRegistration register(final EventProducer eventProducer,
                                      final EventDescriptor eventDesc,
                                      final ServiceID serviceID) {
        if(eventProducer==null)
            throw new IllegalArgumentException("eventProducer is null");
        if(eventDesc==null)
            throw new IllegalArgumentException("eventDesc is null");
        if(serviceID==null)
            throw new IllegalArgumentException("serviceID is null");

        if(leaseTable.containsKey(serviceID)) {
            logger.trace("Already registered to EventProducer");
            return eventRegistrationTable.get(eventDesc.eventID);
        }

        EventRegistration eReg = null;
        Lease lease = connect(eventProducer, eventDesc);
        if(lease!=null) {
            EventLeaseManager eventLeaseManager = new EventLeaseManager(eventProducer, lease, eventDesc);
            leaseTable.put(serviceID, eventLeaseManager);
            service.submit(eventLeaseManager);
            eReg = eventRegistrationTable.get(eventDesc.eventID);
        }
        return (eReg);
    }

    /**
     * Connect to the EventProducer and get a Lease
     *
     * @param producer The EventProducer
     * @param eDesc The EventDescriptor
     *
     * @return The Lease for the @link net.jini.core.event.EventRegistration}
     */
    Lease connect(final EventProducer producer, final EventDescriptor eDesc) {
        Lease lease = null;
        for(int i=0; i<connectRetries; i++) {
            try {
                EventRegistration eReg = producer.register(eDesc, eventConsumer, handback, leaseDuration);
                eventRegistrationTable.put(eDesc.eventID, eReg);
                lease = (Lease)eventLeasePreparer.prepareProxy(eReg.getLease());
                long leaseTime = lease.getExpiration() - System.currentTimeMillis();
                if(leaseTime>0) {
                    logger.trace("Event Registration Lease acquired and prepared. Duration={} ({} seconds)",
                                 leaseTime, (leaseTime / 1000));
                    break;
                } else {
                    logger.warn("Invalid Lease time [{}], retry count [{}]", leaseTime, i);
                    try {
                        lease.cancel();
                    } catch(Exception e ) {
                        logger.trace("Cancelling Lease with invalid lease time", e);
                    }
                    if(retryWait>0) {
                        try {
                            Thread.sleep(retryWait);
                        } catch(InterruptedException ie) {
                            /* should not happen */
                        }
                    }
                }

            } catch(Exception e) {
                /* Determine if we should even try to reconnect */
                if(!ThrowableUtil.isRetryable(e)) {
                    logger.warn("Unrecoverable Exception getting EventRegistration for {}, {}: {}",
                                eDesc.toString(), e.getClass().getName(), e.getMessage());
                    logger.trace("Unrecoverable Exception getting EventRegistration for {}", eDesc.toString(), e);
                    break;
                } else {
                    if(retryWait>0) {
                        try {
                            Thread.sleep(retryWait);
                        } catch(InterruptedException ignore) {
                            /* ignore */
                        }
                    }
                }
            }
        }
        return (lease);
    }

    /**
     * Returns the source object of an EventRegistration given an event ID
     *
     * @param eventID The eventID
     * @return If found, returns the source object associated with the
     * eventID, otherwise returns null
     */
    public Object getEventRegistrationSource(final long eventID) {
        EventRegistration eReg = eventRegistrationTable.get(eventID);
        Object source = null;
        if(eReg!=null)
            source = eReg.getSource();
        return (source);
    }

    /**
     * This method handles the cleanup for removing a registration from a
     * EventProducer instance
     *
     * @param serviceID The serviceID of the EventProducer to deregister
     *
     * @throws IllegalArgumentException if the serviceID parameter is null
     */
    public void deregister(final ServiceID serviceID) {
        deregister(serviceID, true);
    }

    /**
     * This method handles the cleanup for removing a registration from a
     * EventProducer instance
     *
     * @param serviceID The serviceID of the EventProducer to deregister
     * @param disconnect Whether to explicitly cancel the lease with the
     * EventProducer, or just let the least time out
     *
     * @throws IllegalArgumentException if the serviceID parameter is null
     */
    public void deregister(final ServiceID serviceID, final boolean disconnect) {
        if(serviceID==null)
            throw new IllegalArgumentException("serviceID is null");
        EventLeaseManager elm = leaseTable.remove(serviceID);
        if(elm != null) {
            try {
                elm.drop(disconnect);
            } catch(Exception e) {
                /* ignore */
            }
        }
    }

    /**
     * Remote event notification. This method is called by an EventProducer to
     * notify the RemoteEventListener of a state change through a RemoteEvent
     *
     * @throws UnknownEventException If the RemoteEvent cannot be downcast to a
     * RemoteServiceEvent
     */
    public void notify(final RemoteEvent rEvent)throws UnknownEventException {
        if(!(rEvent instanceof RemoteServiceEvent))
            throw new UnknownEventException("Unsupported event class");
        RemoteServiceEvent rsEvent = (RemoteServiceEvent)rEvent;
        service.submit(new ClientNotification(rsEvent));
    }

    /**
     * Returns a {@link net.jini.security.TrustVerifier} which can be used to verify
     * that a given proxy to this event consumer can be trusted
     */
    public TrustVerifier getProxyVerifier() {
        return (new BasicProxyTrustVerifier(eventConsumer));
    }

    /**
     * Method to return a matching EventDescriptor from a service's set of
     * attributes <br>
     *
     * @param attrs Array of attributes
     * @param template The EventDescriptor template
     *
     * @return An EventDescriptor if found or null if no match
     */
    protected EventDescriptor getDescriptor(final Entry[] attrs, final EventDescriptor template) {
        EventDescriptor matchedDescriptor = null;
        /* Traverse the attribute collection, for each EventDescriptor
         * attribute, first check if the attribute has a "matches" method. If it
         * does invoke the method to determine 'match-ability' */
        for (Entry attr : attrs) {
            if (attr instanceof EventDescriptor) {
                EventDescriptor ed = (EventDescriptor) attr;
                try {
                    ed.getClass().getMethod("matches", EventDescriptor.class);
                } catch (NoSuchMethodException e) {
                    logger.warn("Rio version mismatch, {} missing matches method", EventDescriptor.class.getName(), e);
                    return (null);
                }
                if (ed.matches(template)) {
                    matchedDescriptor = ed;
                    break;
                }
            }
        }
        logger.trace("Matched [{}] ? {}", template.toString(), (matchedDescriptor==null?"NO":"YES"));
        return (matchedDescriptor);
    }

    /**
     * Convenience method to print statistics for every thousand events sent.
     * This method will only print result if the Logger has it's Level set to
     * FINEST
     */
    protected void printStats() {
        if(!logger.isTraceEnabled())
            return;
        if(received == 0)
            sktime = System.currentTimeMillis();
        int m = received % 1000;
        if(m == 0 && received > 0) {
            ektime = System.currentTimeMillis();
            float tmp = (ektime - sktime) / 1000.f;
            logger.trace(String.format("Recvd [%d], \t[1000/%f]\t[%f/Second]", received, tmp, 1000.f/tmp));
            sktime = System.currentTimeMillis();
        }
    }

    private boolean removeListener(final RemoteServiceEventListener l) {
        boolean removed = eventSubscribers.remove(l);
        if(removed) {
            if(eventSubscribers.isEmpty()) {
                List<ServiceID> keyList = new ArrayList<ServiceID>();
                for(Enumeration<ServiceID> e = leaseTable.keys(); e.hasMoreElements();) {
                    keyList.add(e.nextElement());
                }
                for (ServiceID sid : keyList) {
                    deregister(sid);
                }
            }
        }
        return (removed);
    }

    /**
     * Override finalize to ensure we unexport ourselves. This is needed if
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        terminate();
        super.finalize();
    }

    /**
     * Get all registered RemoteServiceEventListeners
     *
     * @return An array of all registered RemoteServiceEventListener objects
     */
    protected RemoteServiceEventListener[] getListeners() {
        return eventSubscribers.toArray(new RemoteServiceEventListener[eventSubscribers.size()]);
    }

    /**
     * Notify client asynchronously
     */
    class ClientNotification implements Runnable {
        RemoteServiceEvent rsEvent;

        ClientNotification(RemoteServiceEvent rsEvent) {
            this.rsEvent = rsEvent;
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            logger.trace("Received RemoteServiceEvent [{}], Number of subscribers : {}",
                         rsEvent.getClass().getName(), eventSubscribers.size());

            RemoteServiceEventListener[] listeners = getListeners();
            for (RemoteServiceEventListener listener : listeners) {
                logger.trace("Notify subscriber [{}]", listener.getClass().getName());
                listener.notify(rsEvent);
                received++;
                printStats();
            }

            if(responseWatch != null) {
                long now = System.currentTimeMillis();
                long elapsed = now - startTime;
                responseWatch.setElapsedTime(elapsed, now);
            }
        }
    }

    /**
     * Use customized Lease renewal to manage leases to EventRegistration
     * leases. This is needed because leases constructed to EventProducer
     * instances may be shorter then 5 minutes. If we use LeaseRenewalManager
     * and the leases are shorter then 5 minutes, then after 5 minutes the lease
     * is allowed to expire.
     */
    class EventLeaseManager implements Runnable {
        private final long leaseTime;
        private boolean keepAlive = true;
        private final EventProducer producer;
        private Lease lease;
        private final EventDescriptor eDesc;

        EventLeaseManager(final EventProducer producer, final Lease lease, final EventDescriptor eDesc) {
            this.producer = producer;
            this.lease = lease;
            this.leaseTime = lease.getExpiration() - System.currentTimeMillis();
            this.eDesc = eDesc;
            logger.trace("Created EventLeaseManager, EventDescriptor={}, Lease Time={} seconds",
                         eDesc.toString(), (leaseTime / 1000));
        }

        void drop(boolean disconnect) {
            keepAlive = false;
            if(disconnect) {
                try {
                    lease.cancel();
                } catch(Exception ignore) {
                    /* ignore */
                }
            }
        }

        public void run() {
            while (keepAlive) {
                try {
                     long waitTillRenew = TimeUtil.computeLeaseRenewalTime(leaseTime);
                    logger.trace("Wait to renew {} lease for {}", eDesc.toString(), TimeUtil.format(waitTillRenew));
                    Thread.sleep(waitTillRenew);
                } catch(InterruptedException ignore) {
                    /* ignore */
                }
                if(lease != null) {
                    try {
                        logger.trace("Renew lease for {}", eDesc.toString());
                        lease.renew(leaseTime);
                    } catch(Exception e) {
                        /* Determine if we should even try to reconnect */
                        if(!ThrowableUtil.isRetryable(e)) {
                            keepAlive = false;
                            logger.warn("EventLeaseManager, Unrecoverable Exception renewing Lease, dropping Lease renewal for {}",
                                        eDesc.toString());
                            logger.trace("Unrecoverable Exception renewing Lease for {}", eDesc.toString(), e);
                        }
                        if(keepAlive) {
                            logger.trace("Attempt to reconnect to producer {} for event {}",
                                         producer.toString(), eDesc.toString());
                            lease = connect(producer, eDesc);
                            if(lease==null) {
                                logger.warn("EventLeaseManager, Unable to obtain Lease, dropping Lease renewal for {}",
                                           eDesc.toString());
                                keepAlive = false;
                            } else {
                                logger.trace("Reconnect succeeded to producer {} for event {}",
                                             producer.toString(), eDesc.toString());
                            }
                        }
                    }
                }
            }
            logger.trace("EventLeaseManager {} leaving", eDesc.toString());
        }
    }
}
