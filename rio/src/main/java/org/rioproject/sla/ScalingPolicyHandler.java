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

import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.config.ExporterConfig;
import org.rioproject.core.*;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceElementChangeListener;
import org.rioproject.event.EventHandler;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdEvent;
import org.rioproject.watch.ThresholdManager;
import org.rioproject.watch.ThresholdValues;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ScalingPolicyHandler will increment and optionally decrement instances of
 * the ServiceBean it is associated to based on limits set for the SLA. The
 * ScalingPolicyHandler will look for attributes set that can control it's
 * operational behavior,.
 * <p>
 * The ScalingPolicyHandler supports the following configuration entries; where
 * each configuration entry name is associated with the component name
 * <code>scalingPolicyHandler</code><br>

 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">provisionListenerExporter </span> <br
 * style="font-family: courier new,courier,monospace;"> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;">Exporter</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;">A new <code>BasicJeriExporter</code> with
 * <ul>
 * <li>a <code>TcpServerEndpoint</code> created on a random port,</li>
 * <li>a <code>BasicILFactory</code>,</li>
 * <li>distributed garbage collection turned off,</li>
 * <li>keep alive on.</li>
 * </ul>
 * <code></code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The Exporter used to export the
 * ProvisionListener server. A new exporter is obtained every time a
 * ScalablePolicyHandler needs to export itself.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public class ScalingPolicyHandler extends SLAPolicyHandler
        implements
            ServiceProvisionListener,
            ServerProxyTrust {
    /** The description of the SLA Handler */
    private static final String description = "Scaling Policy Handler";
    /** Action that indicates an increment request is pending */
    public static final String INCREMENT_PENDING = "INCREMENT_PENDING";
    /** Action that indicates an increment request has failed */
    public static final String INCREMENT_FAILURE = "INCREMENT_FAILURE";
    /** Action that indicates an increment request has succeeded. The resultant
     * Object in the SLAPolicyEvent will be the proxy of the new service */
    public static final String INCREMENT_SUCCEEDED = "INCREMENT_SUCCEEDED";
    /** Action that indicates that a decrement command with destroy set to true
     * has been sent */
    public static final String DECREMENT_DESTROY_SENT = "DECREMENT_DESTROY_SENT";
    /** Action that indicates that a decrement command failed to be sent */
    public static final String DECREMENT_FAILED = "DECREMENT_FAILED";
    /** The total number of services the ScalingPolicyHandler is aware of */
    private int totalServices;
    /** Flag that indicates we have requested to decrement (and destroy) ourself */
    private boolean haveDecremented = false;
    /**
     * The maximum number of services to increment. If the value is -1, then no
     * limit has been set
     */
    protected int maxServices;
    /**
     * The minimum number of services that are to exist on the network, defaults
     * to 1
     */
    private int minServices = 1;
    /** The ServiceElement */
    private ServiceElement sElem;
    /** Dampening value for upper thresholds being crossed */
    private long upperThresholdDampeningTime;
    /** Dampening value for lower thresholds being crossed */
    private long lowerThresholdDampeningTime;
    /** The Timer to use for scheduling an increment or decrement task */
    private Timer taskTimer;
    /** The ScalingTask for incrementing */
    private ScalingTask incrementTask;
    /** The ScalingTask for decrementing */
    private ScalingTask decrementTask;
    /** The remote ref (e.g. stub or dynamic proxy) */
    private Object ourRemoteRef;
    /** The Exporter */
    private Exporter exporter;
    /** A ServiceElementChangeListener to listen for ServiceElement changes */
    private ServiceElementChangeManager svcElementListener;
    /** The number of pending provision requests */
    private int pendingRequests;
    /** Flag to indicate whether this policy handler is 'connected' */
    private boolean connected;
    /** The last calculable */
    protected Calculable lastCalculable;
    /** The last ThresholdValue */
    protected ThresholdValues lastThresholdValues;
    private final Object serviceElementLock = new Object();
    /** Component name */
    private static final String CONFIG_COMPONENT = "scalingPolicyHandler";
    /** A Logger for this component */
    static Logger logger = Logger.getLogger("org.rioproject.sla");

    /**
     * Construct a ScalingPolicyHandler
     * 
     * @param sla The SLA for the ScalingPolicyHandler
     */
    public ScalingPolicyHandler(SLA sla) {
        super(sla);
        taskTimer = new Timer(true);
    }

    /*
     * Override parent's method to return description for this SLA Handler
     * 
     * @return The descriptive attribute for this SLA Handler
     */
    @Override
    public String getDescription() {
        return (description);
    }

    /*
     * Override parent's method to export the object to the RMI runtime. 
     */
    @Override
    public void setThresholdManager(ThresholdManager thresholdManager) {
        if(ourRemoteRef==null)
            exportDo();
        super.setThresholdManager(thresholdManager);
    }

    /*
     * Override parent's method to unexport the object from the RMI runtime and
     * perform cleanup tasks
     */
    @Override
    public void disconnect() {
        if(svcElementListener!=null && context!=null)
            context.getServiceBeanManager().removeListener(svcElementListener);
        try {
            exporter.unexport(true);
        } catch(IllegalStateException e) {
            if(logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST,
                           "ScalingPolicyHandler unexport failed",
                           e);
        }
        if(taskTimer != null)
            taskTimer.cancel();
        connected = false;
        super.disconnect();
    }

    /*
     * Override parent's setSLA method to initialize operational attributes
     */
    @Override
    public void setSLA(SLA sla) {
        boolean firstTime = getSLA()==null;
        super.setSLA(sla);
        if(!firstTime)
            initProperties(true);
    }
    
    /*
     * Override parent's initialize method to initialize operational attributes
     */
    @Override
    public void initialize(Object eventSource,
                           EventHandler eventHandler,
                           ServiceBeanContext context) {

        if(context==null)
            throw new IllegalArgumentException("context is null");
        super.initialize(eventSource, eventHandler, context);
        if(exporter==null) {
            try {
                exporter = ExporterConfig.getExporter(getConfiguration(),
                                                      CONFIG_COMPONENT,
                                                      "provisionListenerExporter");

            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "Getting provisionListenerExporter, use default",
                           e);
            }
            /* If we still dont have an exporter create a default one */
            if(exporter==null) {
                exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                 new BasicILFactory(),
                                                 false,
                                                 true);
            }
        }

        boolean update = (this.context==null);
        this.context = context;
        sElem = context.getServiceElement();
        if(svcElementListener==null)
            svcElementListener = new ServiceElementChangeManager();
        context.getServiceBeanManager().addListener(svcElementListener);
        initProperties(update);
        connected = true;
        try {
            totalServices = getTotalKnownServices();
        } catch(Throwable t) {
            logger.log(Level.WARNING,
                       "["+getName()+"] "+
                       "ScalingPolicyHandler ["+getID()+"]:"+
                       "Discovering peer environment",
                       t);
        }
    }      
    
    /*
     * Set the ServiceElement
     */
    private void setServiceElement(ServiceElement newElem) {
        synchronized(serviceElementLock) {
            sElem = newElem;
            //minServices = sElem.getPlanned();
            if(logger.isLoggable(Level.FINEST)) {
                try {
                    totalServices = getTotalKnownServices();
                } catch(Exception e) {
                    logger.log(Level.WARNING,
                               "["+getName()+"] "+"ScalingPolicyHandler ["+
                               getID()+"]:"+"Discovering peer environment",
                               e);
                }
                logger.finest("["+getName()+"] "+
                              "ScalingPolicyHandler ["+getID()+"]: "+
                              "planned ["+sElem.getPlanned()+"], "+
                              "totalServices ["+totalServices+"], "+
                              "minServices ["+minServices+"], "+
                              "maxServices ["+maxServices+"]");
            }
        }
    }
    
    /*
     * Get the ServiceElement
     */
    protected ServiceElement getServiceElement() {
        ServiceElement elem;
        synchronized(serviceElementLock) {
            elem = sElem;
        }
        return(elem);
    }

    /*
     * Initialize properties from the SLA
     */
    private void initProperties(boolean update) {
        try {
            maxServices = getSLA().getMaxServices();

            if(!update) {
                minServices = context.getServiceElement().getPlanned();
                Integer ips =
                    (Integer)context.getServiceBeanConfig().
                            getConfigurationParameters().
                        get(ServiceBeanConfig.INITIAL_PLANNED_SERVICES);
                if(ips != null)
                    minServices = ips;
            }

            upperThresholdDampeningTime =
                (getSLA().getUpperThresholdDampeningTime()==0?1000:
                    getSLA().getUpperThresholdDampeningTime());

            lowerThresholdDampeningTime =
                (getSLA().getLowerThresholdDampeningTime()==0?1000:
                    getSLA().getLowerThresholdDampeningTime());

            if(logger.isLoggable(Level.FINE)) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("[").append(getName()).append("] ");
                buffer.append(update?"UPDATED ":"INIT ");
                buffer.append("ScalingPolicyHandler [").append(getID()).append("] properties: ");
                buffer.append("low Threshold=").append(getSLA().getLowThreshold()).append(", ");
                buffer.append("high Threshold=").append(getSLA().getHighThreshold()).append(", ");
                buffer.append("maxServices=").append(maxServices).append(", ");
                buffer.append("minServices=").append(minServices).append(", ");
                buffer.append("upperThresholdDampeningTime=").append(upperThresholdDampeningTime).append(", ");
                buffer.append("lowerThresholdDampeningTime=").append(lowerThresholdDampeningTime);
                logger.fine(buffer.toString());
            }
        } catch(Exception e) {
            logger.log(Level.WARNING, "Getting Operational Configuration", e);
        }
    }

    /**
     * @see org.rioproject.watch.ThresholdListener#notify
     */
    @Override
    public void notify(Calculable calculable, 
                       ThresholdValues thresholdValues,
                       int type) {
        if(!connected) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "has been disconnected");
            return;
        }
        lastCalculable = calculable;
        lastThresholdValues = thresholdValues;
        String status = (type == ThresholdEvent.BREACHED? "breached":"cleared");
        if(logger.isLoggable(Level.INFO))
            logger.info("["+getName()+"] "+
                        "ScalingPolicyHandler ["+getID()+"]: "+
                        "Threshold ["+calculable.getId()+"] "+status+ " "+
                        "value ["+calculable.getValue()+"] "+
                        "low ["+thresholdValues.getCurrentLowThreshold()+"] "+ 
                        "high ["+thresholdValues.getCurrentHighThreshold()+"]");
        
        if(context.getServiceBeanManager().getOperationalStringManager()==null) {
            logger.warning("["+getName()+"] ["+getID()+"]:"+
                           "No OperationalStringManager, "+
                           "unable to process event");
            return;
        }
        if(type == ThresholdEvent.BREACHED) {
            double tValue = calculable.getValue();
            try {
                totalServices = getTotalKnownServices();
            } catch(Exception e) {
                logger.log(Level.WARNING,
                           "["+getName()+"] "+
                           "ScalingPolicyHandler ["+getID()+"]:"+
                           "Getting instance count",
                           e);
            }
            if(tValue > thresholdValues.getCurrentHighThreshold()) {
                boolean increment = false;
                if(maxServices == SLA.UNDEFINED) {
                    if(logger.isLoggable(Level.FINE))
                        logger.fine("["+getName()+"] "+
                                    "ScalingPolicyHandler ["+getID()+"]: "+
                                    "Unknown MaxServices number, choose to "+
                                    "increment");
                    increment = true;
                } else {
                    /*
                     * The planned attribute changes as services scale up,
                     * get the latest value for comparision
                     */
                    int planned = getServiceElement().getPlanned();
                    if(logger.isLoggable(Level.FINE)) {                        
                        logger.fine("["+getName()+"] "+
                                      "ScalingPolicyHandler ["+getID()+"]: "+
                                      "planned ["+planned+"], "+
                                      "totalServices [" + totalServices+"], "+
                                      "maxServices   [" + maxServices + "]");
                    }
                                                            
                    if(maxServices > totalServices && 
                       totalServices <= planned && maxServices > planned)
                        increment = true;
                    else {
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("["+getName()+"] "+
                                        "ScalingPolicyHandler ["+getID()+"]: "+
                                        "MaxServices ["+maxServices+"] "+
                                        "reached, do not increment");
                    }
                }
                if(increment) {
                    if(incrementTask != null) {
                        logger.fine("["+getName()+"] "+
                                    "ScalingPolicyHandler ["+getID()+"]: "+
                                    "Breached notification, already have an " +
                                    "increment task scheduled. Send " +
                                    "SLAThresholdEvent and return");
                        sendSLAThresholdEvent(calculable, thresholdValues, type);
                        return;
                    }
                    /* Cancel scheduled decrements */
                    cancelDecrementTask();
                    if(upperThresholdDampeningTime > 0) {
                        incrementTask = new ScalingTask(true);
                        long now = System.currentTimeMillis();
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("["+getName()+"] "+
                                        "ScalingPolicyHandler ["+getID()+"]: "+
                                        "Schedule increment task in "+
                                        "["+upperThresholdDampeningTime+"] millis");                        
                        try {
                            taskTimer.schedule(incrementTask, 
                                               new Date(now+upperThresholdDampeningTime));
                        } catch (IllegalStateException e) {
                            logger.warning("Force disconnect of "+
                                           "["+getName()+"] "+
                                           "ScalingPolicyHandler "+
                                           e.getClass().getName()+": "+
                                           e.getMessage());
                            disconnect();    
                        }
                    } else {
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("["+getName()+"] "+
                                        "ScalingPolicyHandler ["+getID()+"]: "+
                                        "no upper dampener, perform increment");
                        doIncrement();
                    }
                }
            } else {
                if(decrementTask != null) {
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler ["+getID()+"]: "+
                                "Breached notification, already have an " +
                                "decrement task scheduled. Send " +
                                "SLAThresholdEvent and return");
                    sendSLAThresholdEvent(calculable, thresholdValues, type);
                    return;
                }
                if(totalServices > minServices) {
                    /* cancel scheduled increments */
                    cancelIncrementTask();
                    if(lowerThresholdDampeningTime > 0) {
                        scheduleDecrement();                        
                    } else {
                        if(logger.isLoggable(Level.FINE))
                            logger.fine("["+getName()+"] "+
                                        "ScalingPolicyHandler ["+getID()+"]: "+
                                        "no lower dampener, "+
                                        "perform decrement");
                        doDecrement();
                    }
                } else {
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler ["+getID()+"]: "+
                                "MinServices ["+minServices+"] "+
                                "reached, Total services ["+totalServices+"] "+
                                "do not decrement");
                }
            }

        /* Threshold has been CLEARED */
        } else {
            cancelIncrementTask();
            cancelDecrementTask();
            
            /* If we have pending increment tasks, trim them up. Make sure however
             * that we are not trimming pending requests that will drop the 
             * minServices and/or maintain value below what has been declared */
            OperationalStringManager opMgr = context.getServiceBeanManager().
                                                     getOperationalStringManager();            
            int pendingCount = getPendingRequestCount(opMgr);
            if(logger.isLoggable(Level.FINEST))
                logger.finest("["+getName()+"] "+
                              "ScalingPolicyHandler ["+getID()+"] "+
                              "totalServices="+totalServices+", "+
                              "pendingCount="+pendingRequests+", "+
                              "pendingRequests="+pendingRequests+", "+
                              "planned="+getServiceElement().getPlanned());
            if(((totalServices+pendingCount)+pendingRequests) >
               getServiceElement().getPlanned()) {
                try {
                    int numTrimmed = opMgr.trim(getServiceElement(),
                                                pendingRequests);
                    if(logger.isLoggable(Level.FINEST))
                        logger.finest("["+getID()+"] numTrimmed="+numTrimmed);
                } catch(NoSuchObjectException e) {
                    logger.log(Level.WARNING,
                               "Remote manager decomissioned for "+
                               "["+getName()+"] "+
                               "ScalingPolicyHandler ["+getID()+"], "+
                               "force disconnect");
                    disconnect();
                } catch(Exception e) {
                    logger.log(Level.WARNING,
                               "["+getID()+"] Trimming Pending Requests",
                               e);
                }
            }
            
        }
        sendSLAThresholdEvent(calculable, thresholdValues, type);
    }

    private void cancelIncrementTask() {
        if(incrementTask != null) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "cancel increment task");
            incrementTask.cancel();
            incrementTask = null;
        }
    }

    private void cancelDecrementTask() {
        if(decrementTask != null) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "cancel decrement task");
            decrementTask.cancel();
            decrementTask = null;
        }
    }

    /**
     * @see org.rioproject.core.ServiceProvisionListener#succeeded
     */
    public void succeeded(ServiceBeanInstance jsbInstance)
    throws RemoteException {
        try {
            pendingRequests--;
            notifyListeners(new SLAPolicyEvent(this,
                                               getSLA(),
                                               INCREMENT_SUCCEEDED,
                                               jsbInstance.getService()));
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "Getting service to create SLAPolicyEvent",
                       e);
        }
    }

    /**
     * @see org.rioproject.core.ServiceProvisionListener#failed
     */
    public void failed(ServiceElement sElem, boolean resubmitted) 
    throws RemoteException {
        if(!resubmitted)
            pendingRequests--;
        notifyListeners(new SLAPolicyEvent(this, getSLA(), INCREMENT_FAILURE));
    }

    /**
     * Returns a <code>TrustVerifier</code> which can be used to verify that a
     * given proxy to this policy handler can be trusted
     */
    public TrustVerifier getProxyVerifier() {
        if(logger.isLoggable(Level.FINEST))
            logger.entering(this.getClass().getName(), "getProxyVerifier");
        if(ourRemoteRef==null)
            exportDo();
        return (new BasicProxyTrustVerifier(ourRemoteRef));
    }

    /*
     * Get the number of pending requests from the OperationalStringManager
     */
    protected int getPendingRequestCount(OperationalStringManager opMgr) {
        int pendingCount = 0;
        try {            
            opMgr.getClass().getMethod("getPendingCount",
                                       ServiceElement.class);
            ServiceElement elem = getServiceElement();
            pendingCount = opMgr.getPendingCount(elem);
        } catch (NoSuchObjectException e) {           
            logger.log(Level.WARNING,
                       "Remote manager decomissioned for "+
                       "["+getName()+"] "+
                       "ScalingPolicyHandler ["+getID()+"], "+
                       "force disconnect");
            disconnect();
        } catch (Throwable t) {
            logger.warning("Using pre-3.2 "+opMgr.getClass().getName()+", "+
                           "pending count not available");
        }    
        return(pendingCount);
    }

    protected int getTotalKnownServices() throws Exception {
        OperationalStringManager opMgr = context.getServiceBeanManager().
            getOperationalStringManager();
        if(opMgr == null)
            throw new Exception("OperationalStringManager is null");
        ServiceBeanInstance[] instances =
            opMgr.getServiceBeanInstances(getServiceElement());
        return (instances.length);
    }

    /**
     * Do the increment
     */
    protected void doIncrement() {
        if(!(//lastType == ThresholdEvent.BREACHED &&
             (lastCalculable.getValue() >
              lastThresholdValues.getCurrentHighThreshold()))) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "INCREMENT CANCELLED, operating below " +
                            "High Threshold, "+
                            "value ["+lastCalculable.getValue()+"] "+
                            "high ["+
                            lastThresholdValues.getCurrentHighThreshold()+"]");
            return;
        }
        try {
            OperationalStringManager opMgr =
                context.getServiceBeanManager().getOperationalStringManager();
            if(opMgr==null) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("["+getName()+"] "+
                                "No OperationalStringManager, increment " +
                                "aborted");
                return;
            }
            ServiceElement elem = getServiceElement();
            ServiceBeanInstance[] instances = opMgr.getServiceBeanInstances(elem);
            int pendingCount = getPendingRequestCount(opMgr);                       
            int realTotal = instances.length+pendingCount;
            
            /* If we have an unbounded maxServices property, always increment. 
             * Otherwise check values to determine if incrementing is needed */
            boolean increment = false;            
            if(maxServices==SLA.UNDEFINED) {
                increment = true;
            } else if(maxServices > realTotal && 
                realTotal <= getServiceElement().getPlanned() && 
                maxServices > getServiceElement().getPlanned()) {                    
                increment = true;
            }
            
            if(increment) {                            
                if(logger.isLoggable(Level.FINE)) {
                    String sMax = (maxServices==SLA.UNDEFINED? "undefined":
                                                Integer.toString(maxServices));
                    logger.fine("["+getName()+"] "+
                                "Current instance count=["+instances.length+"], "+
                                "Current pending count=["+pendingCount+"], "+
                                "Planned=["+getServiceElement().getPlanned()+"], "+
                                "MaxServices=["+sMax+"], "+
                                "ScalingPolicyHandler ["+getID()+"]: "+
                                "INCREMENT_PENDING");
                }
                notifyListeners(new SLAPolicyEvent(this,
                                                   getSLA(),
                                                   INCREMENT_PENDING));
                if(ourRemoteRef==null)
                    exportDo();
                
                context.getServiceBeanManager().increment(
                                            (ServiceProvisionListener)ourRemoteRef);
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("["+getName()+"] "+
                                  "Requested increment through ServiceBeanManager");
                pendingRequests++;
            } else {
                if(logger.isLoggable(Level.FINE)) {
                    String sMax = (maxServices==SLA.UNDEFINED? "Undefined":
                                                    Integer.toString(maxServices));
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler ["+getID()+"]: "+
                                "Current instance count=["+instances.length+"], "+
                                "Current pending count=["+pendingCount+"], "+
                                "Planned=["+getServiceElement().getPlanned()+"], "+
                                "MaxServices=["+sMax+"], "+                                
                                "INCREMENT CANCELLED");
                }
            }
        } catch(java.rmi.NoSuchObjectException e) {
            logger.log(Level.WARNING,
                       "Remote manager decomissioned for "+
                       "["+getName()+"] "+
                       "ScalingPolicyHandler ["+getID()+"], "+
                       "force disconnect");            
            disconnect();
        } catch(Throwable t) {
            logger.log(Level.WARNING, "INCREMENT FAILED", t);
            notifyListeners(new SLAPolicyEvent(this,
                                               getSLA(),
                                               INCREMENT_FAILURE));
        }
    }

    /**
     * Create and schedule a decrement request
     */
    void scheduleDecrement() {
        decrementTask = new ScalingTask(false);
        long now = System.currentTimeMillis();
        logger.fine("["+getName()+"] "+
                    "ScalingPolicyHandler ["+getID()+"]: "+
                    "schedule decrement task in "+
                    "["+lowerThresholdDampeningTime+"] millis");
        try {
            taskTimer.schedule(decrementTask,
                               new Date(now+lowerThresholdDampeningTime));
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING,
                       "Force disconnect of ["+getName()+"] "+
                       "ScalingPolicyHandler", 
                       e);
            disconnect();    
        }
    }

    /**
     * Do the decrement
     *
     * @return true if the decrement needs to be rescheduled
     */
    protected boolean doDecrement() {
        if(!(//lastType == ThresholdEvent.BREACHED &&
             (lastCalculable.getValue() <
              lastThresholdValues.getCurrentLowThreshold()))) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "DECREMENT CANCELLED, operating above "+
                            "Low Threshold, "+
                            "value ["+lastCalculable.getValue()+"] "+
                            "low ["+
                            lastThresholdValues.getCurrentLowThreshold()+"]");
            return false;
        }
        OperationalStringManager opMgr =
            context.getServiceBeanManager().getOperationalStringManager();
        if(opMgr==null) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: unable to " +
                            "process decrement, null OperationalStringManager, " +
                            "abort decrement request");
            //scheduleDecrement();
            return false;
        }
        boolean reschedule = true;
        try {
            ServiceBeanInstance[] instances =
                opMgr.getServiceBeanInstances(getServiceElement());
            totalServices = instances.length;
            for (ServiceBeanInstance instance : instances) {
                if (instance.getServiceBeanID().equals(
                    context.getServiceBeanManager().getServiceID())) {
                    reschedule = false;
                    break;
                }
            }
        } catch(Exception e) {
            logger.log(Level.WARNING,
                       "["+getName()+"]"+
                       "ScalingPolicyHandler ["+getID()+"] getting "+
                       "instance count",
                       e);
        }
        
        if(reschedule) {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: instance " +
                            "not in "+
                            "OperationalStringManager, reschedule "+
                            "decrement request");
            //scheduleDecrement();
            return true;
        }
        if(totalServices > minServices) {
            try {
                haveDecremented = true;
                context.getServiceBeanManager().decrement(true);
                notifyListeners(new SLAPolicyEvent(this,
                                                   getSLA(),
                                                   DECREMENT_DESTROY_SENT));
                if(logger.isLoggable(Level.FINE))
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler ["+getID()+"]: "+
                                "DECREMENT_DESTROY_SENT. " +
                                "totalServices=["+totalServices+"], "+
                                "minServices=["+minServices+"], " +
                                "lastCalculable=["+lastCalculable.getValue()+"], " +
                                "currentLowThreshold=["+lastThresholdValues.getCurrentLowThreshold()+"]");
            } catch(Exception e) {
                logger.log(Level.WARNING, "DECREMENT FAILED", e);
                notifyListeners(new SLAPolicyEvent(this,
                                                   getSLA(),
                                                   DECREMENT_FAILED));
            }
        } else {
            if(logger.isLoggable(Level.FINE))
                logger.fine("["+getName()+"] "+
                            "ScalingPolicyHandler ["+getID()+"]: "+
                            "INCREMENT CANCELLED, "+
                            "totalServices=["+totalServices+"], "+
                            "minServices=["+minServices+"]");
        }
        return false;
    }

    /**
     * Export the ScalingPolicyHandler
     */
    private void exportDo() {
        try {            
            ourRemoteRef = exporter.export(this);
        } catch(RemoteException e) {
            logger.log(Level.WARNING, 
                       "Exporting ScalingPolicyHandler ["+getID()+"]", 
                       e);
        }
    }

    /**
     * The ServiceElementChangeManager listens for changes made to the
     * ServiceElement
     */
    class ServiceElementChangeManager implements ServiceElementChangeListener {

        /* (non-Javadoc)
         * @see org.rioproject.core.jsb.ServiceElementChangeListener#changed
         */
        public void changed(ServiceElement preElem, ServiceElement postElem) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest("["+getName()+"] " +
                              "ScalingPolicyHandler["+getID()+"]: "+
                              "ServiceElement change notification");
            setServiceElement(postElem);                
        }
    }
    
    /**
     * The ScalingTask is used to schedule either an increment or decrement
     * task to be performed at some time in the future. This behavior provides
     * better control over command to either increase or decrease the number of
     * services the ScalingPolicyHandler is attached to
     */
    class ScalingTask extends TimerTask {
        boolean increment;

        /**
         * Create a ScalingTask
         * 
         * @param increment Flag to indicate whether to increment or
         * decrement. If true then increment, otherwise decrement
         */
        ScalingTask(boolean increment) {
            this.increment = increment;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if(increment) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler [" + getID()
                                + "]: running increment task");
                try {
                    doIncrement();
                } finally {
                    incrementTask = null;
                }
            } else {
                if(logger.isLoggable(Level.FINE))
                    logger.fine("["+getName()+"] "+
                                "ScalingPolicyHandler [" + getID()
                                + "]: running decrement task");
                boolean reschedule = false;
                try {
                    if(!haveDecremented)
                        reschedule = doDecrement();
                } finally {
                    if(reschedule)
                        scheduleDecrement();
                    else
                        decrementTask = null;
                }
            }
        }
    }
}
