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
package org.rioproject.sla;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import org.rioproject.config.Constants;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.EventHandler;
import org.rioproject.net.HostUtil;
import org.rioproject.watch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A SLAPolicyHandler handles thresholds for a ThresholdWatch, registering to a
 * ThresholdManager. If a threshold is crossed (breached or cleared), the
 * SLAPolicyHandler will fire a SLAThresholdEvent using an EventHandler provided
 *
 * The SLAPolicyHandler should be extended to provide logic on how to process
 * specific policy (if-then-else logic) on how to manage SLA Thresholds
 * produced by ThresholdWatch instantiations
 *
 * @author Dennis Reedy
 */
public class SLAPolicyHandler implements SettableThresholdListener {
    /** The SLA */
    private SLA mySLA;    
    /** Event source object */
    protected Object eventSource;
    /** ServiceBeanContext for the ServiceBean */
    protected ServiceBeanContext context;
    /** EventHandler for SLAThresholdEvent dispatching */
    private EventHandler eventHandler;
    /** The ThresholdManager the SLA is for */
    protected ThresholdManager thresholdManager;
    /** Flag which indicates the SLAPolicyHandler has initialized */
    protected boolean initialized=false;
    private String name;
    /** Collection of SLAPolicyEventListener */
    private final List<SLAPolicyEventListener> listeners =
        new ArrayList<SLAPolicyEventListener>();
    /** The description of the SLA Handler */
    private String description = "Default Policy Handler";
    /** Host address of the compute resource */
    private String hostAddress;
    private final BlockingQueue<SLAThresholdEvent> eventQ =
        new LinkedBlockingQueue<SLAThresholdEvent>();
    private ExecutorService executor;
    /** A Logger for this component */
    static Logger logger = LoggerFactory.getLogger(SLAPolicyHandler.class);

    /**
     * Construct a SLAPolicyHandler
     *
     * @param sla The SLA for the SLAPolicyHandler
     */
    public SLAPolicyHandler(final SLA sla) {
        mySLA = sla;
        try {
            hostAddress = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS);
        } catch(UnknownHostException e) {
            logger.error("Getting Host Address", e);
            hostAddress="unknown";
        }
    }

    /**
     * Prepare the SLAPolicyHandler for processing. The method will only
     * set these values if the SLAPolicyHandler has not been initialized before
     *
     * @param eventSource The object to be used as the remote event source
     * @param eventHandler Handler which sends events
     * @param context The ServiceBeanContext
     * 
     * @throws IllegalArgumentException if any of the parameters are null
     */    
    public void initialize(final Object eventSource, final EventHandler eventHandler, final ServiceBeanContext context) {
        if(initialized) {
            logger.trace("[{}] {} [{}] already initialized", getName(), getClass().getName(), getID());
            return;
        }
        if(eventSource==null)
            throw new IllegalArgumentException("source is null");
        if(context==null)
            throw new IllegalArgumentException("context is null");
        setName(context.getServiceElement().getName(),
                context.getServiceElement().getServiceBeanConfig().getInstanceID());
        //this.proxy = proxy;
        this.eventSource = eventSource;
        this.eventHandler = eventHandler;
        this.context = context;
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new SLAThresholdEventTask());
        initialized=true;
    }
    
    /**
     * Get the source property
     * 
     * @return The Object used as the event source
     */
    @SuppressWarnings("unused")
    protected Object getEventSource() {
       return(eventSource);
    }
    
    /**
     * Get the description
     *
     * @return String The descriptive attribute for this SLA Handler
     */
    public String getDescription() {
        return(description);
    }

    @SuppressWarnings("unused")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set or update the SLA
     * 
     * @param sla The SLA
     */
    public void setSLA(final SLA sla) {
        if(sla==null)
            throw new IllegalArgumentException("sla is null");
        mySLA = sla;
        if(thresholdManager!=null)
            thresholdManager.setThresholdValues(mySLA);
    }    

    /**
     * Get the SLA
     *
     * @return The SLA that the SLAPolicyHandler has been constructed with
     */
    public SLA getSLA() {
        return(mySLA);
    }

    /**
     * Get the Configuration object
     *
     * @return The Configuration from the
     * {@link org.rioproject.core.jsb.ServiceBeanContext}. If
     * the <tt>ServiceBeanContext</tt> is null, return an empty configuration 
     *
     * @throws ConfigurationException If the configuration cannot be created
     */
    public Configuration getConfiguration() throws ConfigurationException {
        Configuration config;
        if(context!=null)
            config = context.getConfiguration();
        else
            config = EmptyConfiguration.INSTANCE;

        return(config);
    }

    /**
     * Get the ID of the ThresholdWatch the SLAPolicyHandler is associated to
     *
     * @return The identifier (ID) of the ThresholdWatch the SLAPolicyHandler is associated to
     */
    public String getID() {
        return(mySLA.getIdentifier());
    }

    protected void setName(final String name, final long iID) {
        if(iID > 0) {
            this.name = name+":"+iID;
        } else {
            this.name = name+":XX";
        }
    }

    protected String getName() {
        return (name);
    }

    /**
     * Set the ThresholdManager and connect to the ThresholdManager
     *
     * @param thresholdManager The ThresholdManager to connect to
     */
    public void setThresholdManager(final ThresholdManager thresholdManager) {
        if(thresholdManager==null)
            throw new IllegalArgumentException("thresholdManager is null");
        if(this.thresholdManager!=null &&
           this.thresholdManager.equals(thresholdManager))
            return;
        this.thresholdManager = thresholdManager;
        this.thresholdManager.setThresholdValues(getSLA());
        this.thresholdManager.addThresholdListener(this);
        logger.debug("[{}] {} [{}]: setThresholdManager() {}",
                     getName(), getClass().getName(), getID(), mySLA.toString());
    }

    /**
     * @return Get the ThresholdManager
     */
    public ThresholdManager getThresholdManager() {
        return(thresholdManager);
    }

    /**
     * Disconnect from the ThresholdManager
     */
    public void disconnect() {
        if(executor!=null)
            executor.shutdownNow();
        if(thresholdManager!=null)
            thresholdManager.removeThresholdListener(this);
        eventSource = null;
    }

    /**
     * @see org.rioproject.watch.ThresholdListener#notify
     */
    public void notify(final Calculable calculable, final ThresholdValues thresholdValues, final ThresholdType type) {
        logger.debug("SLAPolicyHandler.notify() : {}, type={} Value={}, High={}, Low={}",
                     calculable.getId(),
                     type.name().toLowerCase(),
                     calculable.getValue(),
                     mySLA.getCurrentHighThreshold(),
                     mySLA.getCurrentLowThreshold());
        if(eventHandler!=null)
            sendSLAThresholdEvent(calculable, thresholdValues, type);
        else
            logger.warn("Unable to send SLAThresholdEvent, eventHandler is null");
    }

    /**
     * Register for SLAPolicyEvent notifications
     *
     * @param listener The SLAPolicyEventListener
     */
    @SuppressWarnings("unused")
    public void registerListener(final SLAPolicyEventListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        synchronized(listeners) {
            if(!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Unregister for SLAPolicyEvent notifications
     *
     * @param listener The SLAPolicyEventListener
     */
    @SuppressWarnings("unused")
    public void unregisterListener(final SLAPolicyEventListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify all registered SLAPolicyEventListener instances
     *
     * @param event The SLAPolicyEvent
     */
    protected void notifyListeners(final SLAPolicyEvent event) {
        synchronized (listeners) {
            for(SLAPolicyEventListener l : listeners)
                l.policyAction(event);
        }
    }

    /**
     * Set up a SLAThresholdEvent and send it
     *
     * @param calculable The current metric
     * @param tValues The current thresholds
     * @param type The type of threshold event, breached or cleared
     */
    protected void sendSLAThresholdEvent(final Calculable calculable,
                                         final ThresholdValues tValues,
                                         final ThresholdType type) {
        try {
            SLAThresholdEvent event = new SLAThresholdEvent(eventSource,
                                                            context.getServiceElement(),
                                                            context.getServiceBeanManager().getServiceBeanInstance(),
                                                            calculable,
                                                            mySLA,
                                                            getDescription(),
                                                            hostAddress,
                                                            type);
            String sType = type.name();
            SLAPolicyEvent localEvent = new SLAPolicyEvent(this, mySLA, "THRESHOLD_"+sType);
            localEvent.setSLAThresholdEvent(event);
            notifyListeners(localEvent);
            
            /* Enqueue the remote notification */
            logger.debug("Enqueue SLAThresholdEvent notification for {}", mySLA);
            eventQ.add(event);
        } catch(Exception e) {
            logger.error("Creating a SLAThresholdEvent", e);
        }
    }

    /**
     * This class is used to notify registered event consumers of a
     * SLAThresholdEvent
     */
    class SLAThresholdEventTask implements Runnable {
       public void run() {
            while (true) {
                try {
                    SLAThresholdEvent event = eventQ.take();
                    eventHandler.fire(event);
                } catch(InterruptedException e) {
                    /* */
                    break;
                } catch(Exception e) {
                    logger.warn("Notifying SLAThresholdEvent consumers", e);
                }
            }
        }
    }
}


