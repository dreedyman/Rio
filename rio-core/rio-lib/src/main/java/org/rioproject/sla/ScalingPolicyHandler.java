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
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.core.jsb.ServiceElementChangeListener;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.event.EventHandler;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdManager;
import org.rioproject.watch.ThresholdType;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
public class ScalingPolicyHandler extends SLAPolicyHandler implements ServiceProvisionListener, ServerProxyTrust {
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
     * The maximum number of services to increment. If the value is -1, then no limit has been set
     */
    protected int maxServices;
    /**
     * The minimum number of services that are to exist on the network, defaults to 1
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
    static Logger logger = LoggerFactory.getLogger(ScalingPolicyHandler.class);

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
            logger.trace("ScalingPolicyHandler unexport failed", e);
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
    public void initialize(Object eventSource, EventHandler eventHandler, ServiceBeanContext context) {

        if(context==null)
            throw new IllegalArgumentException("context is null");
        super.initialize(eventSource, eventHandler, context);
        if(exporter==null) {
            try {
                exporter = ExporterConfig.getExporter(getConfiguration(),
                                                      CONFIG_COMPONENT,
                                                      "provisionListenerExporter");
            } catch(Exception e) {
                logger.warn("Getting provisionListenerExporter, use default", e);
            }
            /* If we still don't have an exporter create a default one */
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
            logger.warn("[{}] ScalingPolicyHandler [{}]: Discovering peer environment", getName(), getID(), t);
        }
    }      
    
    /*
     * Set the ServiceElement
     */
    private void setServiceElement(ServiceElement newElem) {
        synchronized(serviceElementLock) {
            sElem = newElem;
            //minServices = sElem.getPlanned();
            if(logger.isTraceEnabled()) {
                try {
                    totalServices = getTotalKnownServices();
                } catch(Exception e) {
                    logger.warn("[{}] ScalingPolicyHandler [{}]: Discovering peer environment", getName(), getID(),e);
                }
                logger.trace("[{}] ScalingPolicyHandler [{}]: planned [{}], totalServices [{}], minServices [{}], maxServices [{}]",
                             getName(), getID(), sElem.getPlanned(), totalServices, minServices, maxServices);
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
                Integer ips = (Integer)context.getServiceBeanConfig().getConfigurationParameters().get(ServiceBeanConfig.INITIAL_PLANNED_SERVICES);
                if(ips != null)
                    minServices = ips;
            }

            upperThresholdDampeningTime =
                (getSLA().getUpperThresholdDampeningTime()==0?1000: getSLA().getUpperThresholdDampeningTime());

            lowerThresholdDampeningTime =
                (getSLA().getLowerThresholdDampeningTime()==0?1000:getSLA().getLowerThresholdDampeningTime());

            if(logger.isDebugEnabled()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("[").append(getName()).append("] ");
                buffer.append(update?"UPDATED ":"INIT ");
                buffer.append("ScalingPolicyHandler [").append(getID()).append("] properties: ");
                buffer.append("low Threshold=").append(getSLA().getLowThreshold()).append(", ");
                buffer.append("high Threshold=").append(getSLA().getHighThreshold()).append(", ");
                buffer.append("maxServices=").append(maxServices).append(", ");
                buffer.append("minServices=").append(minServices).append(", ");
                buffer.append("upperThresholdDampeningTime=").append(upperThresholdDampeningTime).append(", ");
                buffer.append("lowerThresholdDampeningTime=").append(lowerThresholdDampeningTime);
                logger.debug(buffer.toString());
            }
        } catch(Exception e) {
            logger.warn("Getting Operational Configuration", e);
        }
    }

    /**
     * @see org.rioproject.watch.ThresholdListener#notify
     */
    @Override
    public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
        if(!connected) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: has been disconnected", getName(), getID());
            return;
        }
        lastCalculable = calculable;
        lastThresholdValues = thresholdValues;
        String status = type.name().toLowerCase();
        logger.info("[{}] ScalingPolicyHandler [{}]: Threshold [{}] {} value [{}] low [{}] high [{}]",
                    getName(), getID(), calculable.getId(), status, calculable.getValue(),
                    thresholdValues.getLowThreshold(), thresholdValues.getHighThreshold());

        if(context.getServiceBeanManager().getOperationalStringManager()==null) {
            logger.warn("[{}] [{}]: OperationalStringManager, unable to process event", getName(), getID());
            return;
        }
        if(type == ThresholdType.BREACHED) {
            double tValue = calculable.getValue();
            try {
                totalServices = getTotalKnownServices();
            } catch(Exception e) {
                logger.warn("[{}] ScalingPolicyHandler [{}]: Getting instance count", getName(), getID(), e);
            }
            if(tValue > thresholdValues.getHighThreshold()) {
                boolean increment = false;
                if(maxServices == SLA.UNDEFINED) {
                    logger.debug("[{}] ScalingPolicyHandler [{}]: Unknown MaxServices number, choose to increment",
                                 getName(), getID());
                    increment = true;
                } else {
                    /*
                     * The planned attribute changes as services scale up,
                     * get the latest value for comparision
                     */
                    int planned = getServiceElement().getPlanned();
                    logger.debug("[{}] ScalingPolicyHandler [{}]: planned [{}], totalServices [{}], maxServices [{}]",
                                 getName(), getID(), planned, totalServices, maxServices);
                                                            
                    if(maxServices > totalServices && 
                       totalServices <= planned && maxServices > planned)
                        increment = true;
                    else {
                        logger.debug("[{}] ScalingPolicyHandler [{}]: MaxServices [{}] reached, do not increment",
                                     getName(), getID(), maxServices);
                    }
                }
                if(increment) {
                    if(incrementTask != null) {
                        logger.debug("[{}] ScalingPolicyHandler [{}]: Breached notification, already have an " +
                                     "increment task scheduled. Send SLAThresholdEvent and return", 
                                     getName(), getID());
                        sendSLAThresholdEvent(calculable, thresholdValues, type);
                        return;
                    }
                    /* Cancel scheduled decrements */
                    cancelDecrementTask();
                    if(upperThresholdDampeningTime > 0) {
                        incrementTask = new ScalingTask(true);
                        long now = System.currentTimeMillis();
                        logger.debug("[{}] ScalingPolicyHandler [{}]: Schedule increment task in [{}] millis",
                                     getName(), getID(), upperThresholdDampeningTime);
                        try {
                            taskTimer.schedule(incrementTask, new Date(now+upperThresholdDampeningTime));
                        } catch (IllegalStateException e) {
                            logger.warn("Force disconnect of [{}] ScalingPolicyHandler {}: {}",
                                        getName(), e.getClass().getName(), e.getMessage());
                            disconnect();    
                        }
                    } else {
                        logger.debug("[{}] ScalingPolicyHandler [{}]: no upper dampener, perform increment", getName(), getID());
                        doIncrement();
                    }
                }
            } else {
                if(decrementTask != null) {
                    logger.debug("[{}] ScalingPolicyHandler [{}]: Breached notification, already have an " +
                                 "decrement task scheduled. Send SLAThresholdEvent and return", getName(), getID());
                    sendSLAThresholdEvent(calculable, thresholdValues, type);
                    return;
                }
                if(totalServices > minServices) {
                    /* cancel scheduled increments */
                    cancelIncrementTask();
                    if(lowerThresholdDampeningTime > 0) {
                        scheduleDecrement();                        
                    } else {
                        logger.debug("[{}] ScalingPolicyHandler [{}]: no lower dampener, perform decrement",
                                     getName(), getID());
                        doDecrement();
                    }
                } else {
                    logger.debug("[{}] ScalingPolicyHandler [{}]: MinServices [{}] reached, Total services [{}] do not decrement", 
                                 getName(), getID(), minServices, totalServices);
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
            logger.trace("[{}] ScalingPolicyHandler [{}] totalServices={}, pendingCount={}, pendingRequests={}, planned={}",
                         getName(), getID(), totalServices, pendingCount, pendingRequests, getServiceElement().getPlanned());
            if(((totalServices+pendingCount)+pendingRequests) >
               getServiceElement().getPlanned()) {
                try {
                    int numTrimmed = opMgr.trim(getServiceElement(), pendingRequests);
                    logger.trace("[{}] numTrimmed={}", getID(), numTrimmed);
                } catch(NoSuchObjectException e) {
                    logger.warn("Remote manager decomissioned for [{}] ScalingPolicyHandler [{}], force disconnect", 
                                getName(), getID());
                    disconnect();
                } catch(Exception e) {
                    logger.warn("[{}] Trimming Pending Requests", getID(), e);
                }
            }
            
        }
        sendSLAThresholdEvent(calculable, thresholdValues, type);
    }

    private void cancelIncrementTask() {
        if(incrementTask != null) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: cancel increment task", getName(), getID());
            incrementTask.cancel();
            incrementTask = null;
        }
    }

    private void cancelDecrementTask() {
        if(decrementTask != null) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: cancel decrement task", getName(), getID());
            decrementTask.cancel();
            decrementTask = null;
        }
    }

    /**
     * @see org.rioproject.deploy.ServiceProvisionListener#succeeded
     */
    public void succeeded(ServiceBeanInstance jsbInstance) {
        try {
            pendingRequests--;
            notifyListeners(new SLAPolicyEvent(this, getSLA(), INCREMENT_SUCCEEDED, jsbInstance.getService()));
        } catch(Exception e) {
            logger.warn("Getting service to create SLAPolicyEvent", e);
        }
    }

    /**
     * @see org.rioproject.deploy.ServiceProvisionListener#failed
     */
    public void failed(ServiceElement sElem, boolean resubmitted) {
        if(!resubmitted)
            pendingRequests--;
        notifyListeners(new SLAPolicyEvent(this, getSLA(), INCREMENT_FAILURE));
    }

    /**
     * Returns a <code>TrustVerifier</code> which can be used to verify that a
     * given proxy to this policy handler can be trusted
     */
    public TrustVerifier getProxyVerifier() {
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
            opMgr.getClass().getMethod("getPendingCount", ServiceElement.class);
            ServiceElement elem = getServiceElement();
            pendingCount = opMgr.getPendingCount(elem);
        } catch (NoSuchObjectException e) {           
            logger.warn("Remote manager decomissioned for [{}] ScalingPolicyHandler [{}], force disconnect", 
                        getName(), getID());
            disconnect();
        } catch (Throwable t) {
            logger.warn("Using pre-3.2 {}, pending count not available", opMgr.getClass().getName());
        }    
        return(pendingCount);
    }

    protected int getTotalKnownServices() throws Exception {
        OperationalStringManager opMgr = context.getServiceBeanManager().getOperationalStringManager();
        if(opMgr == null)
            throw new Exception("OperationalStringManager is null");
        ServiceBeanInstance[] instances =opMgr.getServiceBeanInstances(getServiceElement());
        return (instances.length);
    }

    /**
     * Do the increment
     */
    protected void doIncrement() {
        if(!(//lastType == ThresholdEvent.BREACHED &&
             (lastCalculable.getValue() > lastThresholdValues.getHighThreshold()))) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: INCREMENT CANCELLED, operating below High Threshold, value [{}] high [{}]",
                         getName(), getID(), lastCalculable.getValue(), lastThresholdValues.getHighThreshold());
            return;
        }
        try {
            OperationalStringManager opMgr = context.getServiceBeanManager().getOperationalStringManager();
            if(opMgr==null) {
                logger.debug("[{}] No OperationalStringManager, increment aborted", getName());
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
                logger.debug("[{}] Current instance count=[{}], Current pending count=[{}], "+
                             "Planned=[{}], MaxServices=[{}], ScalingPolicyHandler [{}]: INCREMENT_PENDING",
                             getName(), instances.length, pendingCount, getServiceElement().getPlanned(),
                             (maxServices==SLA.UNDEFINED? "undefined": Integer.toString(maxServices)),
                             getID());
                notifyListeners(new SLAPolicyEvent(this, getSLA(), INCREMENT_PENDING));
                if(ourRemoteRef==null)
                    exportDo();
                
                context.getServiceBeanManager().increment((ServiceProvisionListener)ourRemoteRef);
                logger.trace("[{}] Requested increment through ServiceBeanManager", getName());
                pendingRequests++;
            } else {
                logger.debug("[{}] ScalingPolicyHandler [{}]: Current instance count=[{}], "+
                             "Current pending count=[{}], Planned=[{}], MaxServices=[{}], INCREMENT CANCELLED",
                             getName(), getID(), instances.length, pendingCount, getServiceElement().getPlanned(),
                             (maxServices==SLA.UNDEFINED? "Undefined": Integer.toString(maxServices)));
            }
        } catch(java.rmi.NoSuchObjectException e) {
            logger.warn("Remote manager decomissioned for [{}] ScalingPolicyHandler [{}], force disconnect",
                        getName(), getID());
            disconnect();
        } catch(Throwable t) {
            logger.warn("INCREMENT FAILED", t);
            notifyListeners(new SLAPolicyEvent(this, getSLA(), INCREMENT_FAILURE));
        }
    }

    /**
     * Create and schedule a decrement request
     */
    void scheduleDecrement() {
        decrementTask = new ScalingTask(false);
        long now = System.currentTimeMillis();
        logger.debug("[{}] ScalingPolicyHandler [{}]: schedule decrement task in [{}] millis",
                     getName(), getID(), lowerThresholdDampeningTime);
        try {
            taskTimer.schedule(decrementTask, new Date(now+lowerThresholdDampeningTime));
        } catch (IllegalStateException e) {
            logger.warn("Force disconnect of [{}] ScalingPolicyHandler", getName(), e);
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
                (lastCalculable.getValue() < lastThresholdValues.getLowThreshold()))) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: DECREMENT CANCELLED, operating above Low Threshold, value [{}] low [{}]",
                         getName(), getID(), lastCalculable.getValue(), lastThresholdValues.getLowThreshold());
            return false;
        }
        OperationalStringManager opMgr = context.getServiceBeanManager().getOperationalStringManager();
        if(opMgr==null) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: unable to process decrement, " +
                         "null OperationalStringManager, abort decrement request",
                         getName(), getID());
            //scheduleDecrement();
            return false;
        }
        boolean reschedule = true;
        try {
            ServiceBeanInstance[] instances = opMgr.getServiceBeanInstances(getServiceElement());
            totalServices = instances.length;
            for (ServiceBeanInstance instance : instances) {
                if (instance.getServiceBeanID().equals(
                    context.getServiceBeanManager().getServiceID())) {
                    reschedule = false;
                    break;
                }
            }
        } catch(Exception e) {
            logger.warn("[{}] ScalingPolicyHandler [{}] getting instance count", getName(), getID(), e);
        }
        
        if(reschedule) {
            logger.debug("[{}] ScalingPolicyHandler [{}]: instance not in OperationalStringManager, reschedule "+
                         "decrement request", getName(), getID());
            //scheduleDecrement();
            return true;
        }
        if(totalServices > minServices) {
            try {
                haveDecremented = true;
                context.getServiceBeanManager().decrement(true);
                notifyListeners(new SLAPolicyEvent(this, getSLA(), DECREMENT_DESTROY_SENT));
                logger.debug("[{}] ScalingPolicyHandler [{}]: DECREMENT_DESTROY_SENT. totalServices=[{}], "+
                             "minServices=[{}], lastCalculable=[{}], currentLowThreshold=[{}]",
                             getName(), getID(), totalServices, minServices, lastCalculable.getValue(),
                             lastThresholdValues.getLowThreshold());
            } catch(Exception e) {
                logger.warn("DECREMENT FAILED", e);
                notifyListeners(new SLAPolicyEvent(this, getSLA(), DECREMENT_FAILED));
            }
        } else {
            logger.debug("[{}] ScalingPolicyHandler [{}]: INCREMENT CANCELLED, totalServices=[{}], minServices=[{}]",
                         getName(), getID(), totalServices, minServices);
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
            logger.warn("Exporting ScalingPolicyHandler ["+getID()+"]", e);
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
            logger.trace("[{}] ScalingPolicyHandler[{}]: ServiceElement change notification", getName(), getID());
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
                logger.debug("[{}] ScalingPolicyHandler [{}]: running increment task", getName(), getID());
                try {
                    doIncrement();
                } finally {
                    incrementTask = null;
                }
            } else {
                logger.debug("[{}] ScalingPolicyHandler [{}]: running decrement task", getName(), getID());
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
