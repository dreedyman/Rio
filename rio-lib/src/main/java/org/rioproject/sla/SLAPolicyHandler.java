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
package org.rioproject.sla;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.EmptyConfiguration;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.event.EventHandler;
import org.rioproject.watch.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class SLAPolicyHandler implements ThresholdListener {
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
    private static final String description = "Default Policy Handler";
    /** Host address of the compute resource */
    private String hostAddress;
    private final BlockingQueue<SLAThresholdEvent> eventQ =
        new LinkedBlockingQueue<SLAThresholdEvent>();
    private ExecutorService executor;
    /** A Logger for this component */
    static Logger logger = Logger.getLogger("org.rioproject.sla");

    /**
     * Construct a SLAPolicyHandler
     *
     * @param sla The SLA for the SLAPolicyHandler
     */
    public SLAPolicyHandler(SLA sla) {
        mySLA = sla;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e) {
            logger.log(Level.SEVERE, "Getting Host Address", e);
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
    public void initialize(Object eventSource,
                           EventHandler eventHandler,
                           ServiceBeanContext context) {
        if(initialized) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest("["+getName()+"] "+getClass().getName()+
                              " ["+getID()+"] already initialized");
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
    
    /**
     * Set or update the SLA
     * 
     * @param sla The SLA
     */
    public void setSLA(SLA sla) {
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
     * @see org.rioproject.watch.ThresholdListener#getID
     */
    public String getID() {
        return(mySLA.getIdentifier());
    }

    protected void setName(String name, long iID) {
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
     * @see org.rioproject.watch.ThresholdListener#setThresholdManager
     */
    public void setThresholdManager(ThresholdManager thresholdManager) {
        if(thresholdManager==null)
            throw new IllegalArgumentException("thresholdManager is null");
        if(this.thresholdManager!=null &&
           this.thresholdManager.equals(thresholdManager))
            return;
        this.thresholdManager = thresholdManager;
        this.thresholdManager.setThresholdValues(getSLA());
        this.thresholdManager.addThresholdListener(this);
        if(logger.isLoggable(Level.FINER)) {
            logger.finer("["+getName()+"] "+ getClass().getName()+" "+
                        "["+getID()+"]: setThresholdManager() "+
                        mySLA.toString());
        }
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
    public void notify(Calculable calculable, 
                       ThresholdValues thresholdValues, 
                       int type) {
        if(logger.isLoggable(Level.FINE)) {            
            String status = (type == ThresholdEvent.BREACHED?"breached":"cleared");
            logger.fine("SLAPolicyHandler.notify() : "+calculable.getId()+", "+
                        "type="+status+" "+
                        "Value="+calculable.getValue()+", "+
                        "High="+mySLA.getCurrentHighThreshold()+", "+
                        "Low="+mySLA.getCurrentLowThreshold());
        }
        if(eventHandler!=null)
            sendSLAThresholdEvent(calculable, thresholdValues, type);
        else
            logger.warning("Unable to send SLAThresholdEvent, eventHandler is null");
    }

    /**
     * Register for SLAPolicyEvent notifications
     *
     * @param listener The SLAPolicyEventListener
     */
    public void registerListener(SLAPolicyEventListener listener) {
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
    public void unregisterListener(SLAPolicyEventListener listener) {
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
    protected void notifyListeners(SLAPolicyEvent event) {
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
    protected void sendSLAThresholdEvent(Calculable calculable, 
                                         ThresholdValues tValues, 
                                         int type) {
        try {
            double range[] = new double[]{tValues.getCurrentLowThreshold(), 
                                          tValues.getCurrentHighThreshold()};
            SLA sla = new SLA(mySLA.getIdentifier(), range);
            SLAThresholdEvent event =
                new SLAThresholdEvent(eventSource,
                                      context.getServiceElement(),
                                      context.getServiceBeanManager().getServiceBeanInstance(),
                                      calculable,
                                      sla,
                                      getDescription(),
                                      hostAddress,
                                      type);
            String sType =
                (type == ThresholdEvent.BREACHED?"BREACHED":"CLEARED");
            SLAPolicyEvent localEvent =
                new SLAPolicyEvent(this, sla, "THRESHOLD_"+sType);
            localEvent.setSLAThresholdEvent(event);
            notifyListeners(localEvent);
            
            /* Enqueue the remote notification */
            if(logger.isLoggable(Level.FINE))
                logger.fine("Enqueue SLAThresholdEvent notification for "+sla);
            eventQ.add(event);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Creating a SLAThresholdEvent", e);
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
                    logger.log(Level.WARNING,
                               "Notifying SLAThresholdEvent consumers",
                               e);
                }
            }
        }
    }
}


