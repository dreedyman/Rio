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
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.event.EventHandler;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.ThresholdManager;
import org.rioproject.watch.ThresholdType;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The RelocationPolicyHandler will inform the OperationalStringManager to
 * relocate the ServiceBean that uses this policy handler. The
 * RelocationPolicyHandler will look for attributes set that can control it's
 * operational behavior.
 * <p>
 * The <span style="font-family: monospace;">RelocationPolicyHandler </span>
 * supports the following configuration entries; where each configuration entry
 * name is associated with the component name <span style="font-family:
 * monospace;">relocationPolicyHandler </span> <br>
 *
 * <ul>
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
 * RelocationPolicyHandler needs to export itself.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <span style="font-weight: bold; font-family: courier new,courier,monospace;">
 * <br>
 * </span>
 *
 * @author Dennis Reedy
 */
public class RelocationPolicyHandler extends SLAPolicyHandler
        implements
            ServiceProvisionListener,
            ServerProxyTrust {
    /** The description of the SLA Handler */
    private static final String description = "Relocation Policy Handler";
    /** Dampening value for upper thresholds being crossed */
    private long upperThresholdDampeningTime;
    /** Dampening value for lower thresholds being crossed */
    private long lowerThresholdDampeningTime;
    /** The Timer to use for scheduling a relocation task */
    private Timer taskTimer;
    /** The RelocationTask for incrementing */
    private RelocationTask relocationTask;
    /** Action that indicates an relocation request is pending */
    public static final String RELOCATION_PENDING = "RELOCATION_PENDING";
    /** Action that indicates an relocation request has failed */
    public static final String RELOCATION_FAILURE = "RELOCATION_FAILURE";
    /**
     * Action that indicates an relocation request has succeeded. The resultant
     * Object in the SLAPolicyEvent will be the proxy of the new service
     */
    public static final String RELOCATION_SUCCEEDED = "RELOCATION_SUCCEEDED";
    /** The remote ref (e.g. stub or dynamic proxy) */
    private Object ourRemoteRef;
    /** The Exporter */
    private Exporter exporter;
    /** Component name */
    private static final String CONFIG_COMPONENT = "relocationPolicyHandler";
    /** Logger for this component */
    static Logger logger = LoggerFactory.getLogger("org.rioproject.sla");

    /**
     * Construct a RelocationPolicyHandler
     * 
     * @param sla The SLA for the RelocationPolicyHandler
     */
    public RelocationPolicyHandler(SLA sla) {
        super(sla);
        taskTimer = new Timer(true);
    }

    /**
     * Override parent's method to return description for this SLA Handler
     * 
     * @return The descriptive attribute for this SLA Handler
     */
    @Override
    public String getDescription() {
        return (description);
    }

    /**
     * Override parent's method to export the object to the RMI runtime
     */
    @Override
    public void setThresholdManager(ThresholdManager thresholdManager) {
        if(ourRemoteRef==null)
            exportDo();
        super.setThresholdManager(thresholdManager);
    }

    /**
     * Override parent's method to unexport
     */
    @Override
    public void disconnect() {
        try {
            exporter.unexport(true);
        } catch(IllegalStateException e) {
            if(logger.isTraceEnabled())
                logger.trace("RelocationPolicyHandler unexport failed", e);
        }
        ourRemoteRef = null;
        if(taskTimer != null)
            taskTimer.cancel();
        super.disconnect();
    }

    /**
     * Override parent's initialize method to initialize operational attributes
     */
    @Override
    public void initialize(Object eventSource,
                           EventHandler eventHandler,
                           ServiceBeanContext context) {
        super.initialize(eventSource, eventHandler, context);
        if(exporter==null) {
            try {
                exporter = ExporterConfig.getExporter(getConfiguration(),
                                                      CONFIG_COMPONENT,
                                                      "provisionListenerExporter");

            } catch(Exception e) {
                logger.warn("Getting provisionListenerExporter, use default", e);
            }
            /* If we still dont have an exporter create a default one */
            if(exporter==null) {
                exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                 new BasicILFactory(),
                                                 false,
                                                 true);
            }
        }
        try {
            upperThresholdDampeningTime =
                (getSLA().getUpperThresholdDampeningTime()==0?1000:
                    getSLA().getUpperThresholdDampeningTime());

            lowerThresholdDampeningTime =
                (getSLA().getLowerThresholdDampeningTime()==0?1000:
                    getSLA().getLowerThresholdDampeningTime());

            if(logger.isDebugEnabled()) {
                logger.debug("["+context.getServiceElement().getName()+"] "+
                            "RelocationPolicyHandler ["+getID()+"]: properties, "+
                            "low Threshold="+getSLA().getLowThreshold()+", "+
                            "high Threshold="+getSLA().getHighThreshold()+", "+
                            "upperThresholdDampeningTime="+upperThresholdDampeningTime+", "+
                            "lowerThresholdDampeningTime="+lowerThresholdDampeningTime);
            }
        } catch(Exception e) {
            logger.error("Getting Operational Configuration", e);
        }
    }

    /**
     * @see org.rioproject.watch.ThresholdListener#notify
     */
    @Override
    public void notify(Calculable calculable, ThresholdValues thresholdValues, ThresholdType type) {
        if(logger.isDebugEnabled()) {
            String status = type.name().toLowerCase();
            logger.debug("RelocationPolicyHandler [" + getID() + "]: Threshold ["
                        + calculable.getId() + "] " + status + " value ["
                        + calculable.getValue() + "\n] low ["
                        + thresholdValues.getCurrentLowThreshold() + "]" + " high ["
                        + thresholdValues.getCurrentHighThreshold() + "]");
        }
        if(type == ThresholdType.BREACHED) {
            double tValue = calculable.getValue();
            if(tValue > thresholdValues.getCurrentHighThreshold()) {
                fireRelocation(upperThresholdDampeningTime, "upper");
            } else {
                fireRelocation(lowerThresholdDampeningTime, "lower");
            }

        /* Threshold has been cleared */
        } else {
            if(relocationTask!=null) {
                relocationTask.cancel();
                relocationTask = null;
            }
        }
        sendSLAThresholdEvent(calculable, thresholdValues, type);
    }

    /*
     * Determine whether a RelocationTask should be created based on the
     * dampening value provided, or to relocate immediately
     */
    void fireRelocation(long dampener, String type) {
        if(dampener > 0) {
            relocationTask = new RelocationTask();
            long now = System.currentTimeMillis();
            if(logger.isDebugEnabled())
                logger.debug("["+context.getServiceElement().getName()+"] "+
                            "RelocationPolicyHandler ["+getID()+"]: "+
                            "Schedule relocation task in "+
                            "["+dampener+"] millis");
            try {
                taskTimer.schedule(relocationTask,
                                   new Date(now+ dampener));
            } catch(IllegalStateException e) {
                logger.warn("Force disconnect of "+
                           "["+context.getServiceElement().getName()+"] "+
                           "RelocationPolicyHandler",
                           e);
                disconnect();
            }
        } else {
            if(logger.isDebugEnabled())
                logger.debug("["+context.getServiceElement().getName()+"] "+
                            "RelocationPolicyHandler ["+getID()+"]: "+
                            "no "+type+" dampener, perform relocation");
            doRelocate();
        }
    }

    /**
     * @see org.rioproject.deploy.ServiceProvisionListener#succeeded
     */
    public void succeeded(ServiceBeanInstance jsbInstance)
    throws RemoteException {
        try {
            notifyListeners(new SLAPolicyEvent(this,
                                               getSLA(),
                                               RELOCATION_SUCCEEDED,
                                               jsbInstance.getService()));
        } catch(Exception e) {
            logger.warn("Getting service to create SLAPolicyEvent", e);
        }
    }

    /**
     * @see org.rioproject.deploy.ServiceProvisionListener#failed
     */
    public void failed(ServiceElement sElem, boolean resubmitted)
    throws RemoteException {
        notifyListeners(new SLAPolicyEvent(this, getSLA(), RELOCATION_FAILURE));
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
    
    /**
     * Export the RelocationPolicyHandler
     */
    private void exportDo() {
        try {            
            ourRemoteRef = exporter.export(this);
        } catch(RemoteException e) {
            logger.error(
                       "Exporting RelocationPolicyHandler ["+getID()+"]",
                       e);
        }
    }

    /**
     * Perform the relocation
     */
    void doRelocate() {
        notifyListeners(new SLAPolicyEvent(this,
                                           getSLA(),
                                           RELOCATION_PENDING));
        try {
            if(ourRemoteRef == null)
                exportDo();
            
            context.getServiceBeanManager().relocate(
                (ServiceProvisionListener)ourRemoteRef,
                null);               
        } catch(Exception e) {
            if(!logger.isTraceEnabled()) {
                logger.warn("Attempt to invoke relocate method on " +
                               "ProvisionManager ["+
                               e.getClass().getName()+" : "+
                               e.getLocalizedMessage()+"]");
            } else {
                logger.warn(
                           "Attempt to invoke relocate method on ProvisionManager",
                           e);
            }
            notifyListeners(new SLAPolicyEvent(this,
                                               getSLA(),
                                               RELOCATION_FAILURE));
        }
    }

    /**
     * The RelocationTask is used to schedule a relocation be performed at
     * some time in the future.
     */
    class RelocationTask extends TimerTask {

        public void run() {
            doRelocate();
        }
    }
}
