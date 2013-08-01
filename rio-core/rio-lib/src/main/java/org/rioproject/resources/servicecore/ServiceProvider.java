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
package org.rioproject.resources.servicecore;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.core.event.EventRegistration;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.id.Uuid;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.event.EventDescriptor;
import org.rioproject.event.EventHandler;
import org.rioproject.event.EventProducer;
import org.rioproject.watch.WatchDataSource;
import org.rioproject.watch.WatchRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * The ServiceProvider is an abstract class that provides the infrastructure
 * required to make a Jini service. This class needs to be extended by Service
 * developers. All ServiceProvider instances can be an Event Producer. An Event
 * Producer is a service that has a zero-to-many dependency between objects such
 * that when it's state changes all it's dependants are notified. All
 * ServiceProvider instances provide support for the Rio Watchable framework.
 * The Rio Watchable framework provides a mechanism to collect and analyze
 * programmer-defined metrics defined in local and distributed applications
 * <p>
 * The ServiceProvider supports the following configuration entries; where each
 * configuration entry name is associated with the component name <span *=""
 * style="font-family: monospace;">org.rioproject.resources.servicecore </span>
 * <br>
 * &nbsp; <br>
 * </p>
 * <ul>
 * <li><span style="font-weight: bold;">eventListenerPreparer </span> <table
 * cellpadding="2" *="" cellspacing="2" border="0" style="text-align: left;
 * width: 100%;"> <tbody>
 * <tr>
 * <td style="vertical-align: top; text-align: right;">Type:</td>
 * <td style="vertical-align: top;">ProxyPreparer</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right;">Default:</td>
 * <td style="vertical-align: top;">new BasicProxyPreparer() <code></code>
 * <br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right;">Description:</td>
 * <td style="vertical-align: top;">Specifies the proxy preparer to use for
 * <code>RemoteEventListener</code> s registered with this service in calls to
 * <code>ServiceProvider.register(org.rioproject.event.EventDescriptor,&nbsp;
 net.jini.core.event.RemoteEventListener,
 java.rmi.MarshalledObject, long)</code>.
 * During event notification, this service calls the <code>notify</code>
 * method on <code>RemoteEventListener</code> instances returned from this
 * preparer. This entry is obtained at service start and restart.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public abstract class ServiceProvider implements Service {
    /**
     * The eventTable associates an EventHandler to an EventDescriptor for the
     * ServiceProvider. Event registration requests for events this
     * ServiceProvider has advertised will consult the eventTable to determine
     * the correct EventHandler to use in order to return an event registration
     */
    protected Map<Long, EventHandler> eventTable;
    /**
     * The watchRegistry is used to fetch WatchDataSource instances that have
     * been created and registered in this ServiceProvider
     */
    protected WatchRegistry watchRegistry;
    /** The Configuration for the ServiceProvider */
    private Configuration config;
    /** Preparer for received remote event listeners */
    private ProxyPreparer listenerPreparer = new BasicProxyPreparer();
    /** Name for use accessing Configuration elements and getting a Logger */
    private static final String COMPONENT = "org.rioproject.resources.servicecore";
    /** A Logger */
    static final Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Set the Configuration for the ServiceProvider
     * 
     * @param config The Configuration
     */
    public void setConfiguration(Configuration config) {
        this.config = config;
        if(config != null) {
            try {
                listenerPreparer =(ProxyPreparer)Config.getNonNullEntry(config,
                                                                        COMPONENT,
                                                                        "eventListenerPreparer",
                                                                        ProxyPreparer.class,
                                                                        listenerPreparer);
            } catch(Exception e) {
                logger.warn("Getting eventListenerPreparer", e);
            }
        }
    }

    /**
     * Get the Configuration for the ServiceProvider
     * 
     * @return The Configuration
     */
    public Configuration getConfiguration() {
        return (config);
    }

    /**
     * @see org.rioproject.event.EventProducer#register
     */
    public EventRegistration register(EventDescriptor descriptor,
                                      RemoteEventListener listener,
                                      MarshalledObject handback,
                                      long duration)
    throws LeaseDeniedException, UnknownEventException, RemoteException {

        if(descriptor == null)
            throw new IllegalArgumentException("descriptor is null");
        if(descriptor.eventID == null)
            throw new UnknownEventException("Event ID is null");
        EventHandler eHandler = eventTable.get(descriptor.eventID);
        if(eHandler == null)
            throw new UnknownEventException("Unknown event ID "+descriptor.eventID);

        /* Prepare the RemoteEventListener */
        RemoteEventListener preparedListener = (RemoteEventListener)listenerPreparer.prepareProxy(listener);
        if(logger.isDebugEnabled())
            logger.debug("Register listener {} for Event {}", preparedListener.toString(), descriptor.toString());
        Object o = getServiceProxy();
        if(!(o instanceof EventProducer)) {
            String reason = "Proxy returned from getServiceProxy() does " +
                           "not implement "+EventProducer.class.getName();
            logger.warn(reason);
            throw new ClassCastException(reason);
        }
        
        return (eHandler.register(o, preparedListener, handback, duration));
    }

    /**
     * Left for concrete implementations of this class to implement
     */
    public abstract Object getAdmin();

    /**
     * Left for concrete implementations of this class to implement
     */
    public abstract void destroy();

    /**
     * Concrete implementations must define a mechanism to retrieve the proxy to
     * represent the service on the network
     *
     * @return A proxy suitable for communication to the service
     */
    public abstract Object getServiceProxy();

    /**
     * Return the Uuid for the Service
     *
     * @return The Uuid
     */
    public abstract Uuid getUuid();

    /**
     * @see org.rioproject.watch.Watchable#fetch()
     */
    public WatchDataSource[] fetch() {
        WatchDataSource[] wds = new WatchDataSource[0];
        if(watchRegistry != null) {
            wds = watchRegistry.fetch();
        } else {
            logger.warn("WatchRegistry is null");
        }
        return(wds);
    }

    /**
     @see org.rioproject.watch.Watchable#fetch(String)
     */
    public WatchDataSource fetch(String id) {
        WatchDataSource wds = null;
        if(watchRegistry!=null) {
            wds = watchRegistry.fetch(id);
        } else {
            logger.warn("WatchRegistry is null");
        }
        return(wds);
    }    

    /**
     * Set the event table.
     *
     * @param eventTable The event table
     */
    public void setEventTable(Map<Long, EventHandler> eventTable) {
        this.eventTable = eventTable;
    }

    /**
     * Get the event table.
     *
     * @return The event table
     */
    public Map<Long, EventHandler> getEventTable() {
        return eventTable;
    }

    /**
     * Get the WatchDataSourceRegistry
     *
     * @return The WatchRegistry
     */
    public WatchRegistry getWatchRegistry() {
        return (watchRegistry);
    }

    /**
     * Set the WatchDataSourceRegistry
     *
     * @param watchRegistry The WatchRegistry
     */
    public void setWatchRegistry(WatchRegistry watchRegistry) {
        this.watchRegistry = watchRegistry;
    }

}
