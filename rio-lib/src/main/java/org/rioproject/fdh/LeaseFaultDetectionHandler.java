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
package org.rioproject.fdh;

import com.sun.jini.config.Config;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.lease.Lease;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.admin.MonitorableService;
import org.rioproject.resources.util.ThrowableUtil;
import org.rioproject.util.TimeUtil;

import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The LeaseFaultDetectionHandler is used to monitor services that implement the
 * {@link org.rioproject.admin.MonitorableService} interface.
 *
 * <p>The LeaseFaultDetectionHandler creates a client-side
 * {@link net.jini.core.lease.Lease} with the service,
 * and attempts to renew the lease based on a configurable lease duration time.
 * If the lease cannot be renewed and all retry attempts have failed, the
 * LeaseFaultDetectionHandler will notify
 * {@link FaultDetectionListener} instances of
 * the failure.
 * <p>
 * Additionally, the LeaseFaultDetectionHandler will register with Lookup
 * Services for
 * {@link net.jini.core.lookup.ServiceRegistrar#TRANSITION_MATCH_NOMATCH}
 * transitions for the service being monitored. If the service is
 * adminstratively removed from the network, or the service monitoring lease
 * is between lease renewal points and the service has actually been removed
 * from the network, the transition will be noted and FaultDetectionListener
 * instances will be notified of the failure.
 * <p>
 * If the service does not implement the
 * {@link org.rioproject.admin.MonitorableService} interface, the
 * LeaseFaultDetectionHandler will not manage a Lease, but will only create
 * the event consumer for Lookup Service TRANSITION_MATCH_NOMATCH transitions.
 * <p>
 * <b><font size="+1">Configuring LeaseFaultDetectionHandler </font> </b>
 * <p>
 * This implementation of LeaseFaultDetectionHandler supports
 * the following configuration entries; where each configuration entry name is
 * associated with the component name
 * <code>org.rioproject.fdh.LeaseFaultDetectionHandler</code>.
 * <br>
 * <br>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">leaseDuration </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top; font-family: monospace;">long</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>30*1000 (30 seconds)</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The duration of the Lease in milliseconds.
 * The Lease renewal time will be calculated to be 75% of the LeaseTime
 * attribute</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">retryCount </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;"><code>int</code><br>
 * </td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>3</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The number of times to retry connecting to
 * the service when renewing the Lease. If the service cannot be reached within
 * the retry count specified the service will be determined to be unreachable
 * </td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">retryTimeout </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;"><code>long</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>1000 (1 second)</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">How long to wait between retries (in
 * milliseconds). This value will be used between retry attempts, waiting the
 * specified amount of time to retry <br>
 * </td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">leasePreparer </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;"><code>ProxyPreparer</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;">
 * <code>new <code>BasicProxyPreparer</code>()</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">Preparer for the leases returned when the
 * <span style="font-family: monospace;">LeaseFaultDetectionHandler </span>
 * invokes the monitor method of a <span style="font-family:
 * monospace;">MonitorableService </span></td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <br>
 * The amount of time it takes for the StandardFaultDetectionListener to
 * determine service failure for a service that implements the
 * <code>org.rioproject.core.MonitorableService</code> interface is calculated as
 * follows :<br>
 * 
 * <pre>
 * ((num_retries + 1) * (connectivity_timeout)) + (retry_delay * num_retries)
 * </pre>
 *
 * @author Dennis Reedy
 */
public class LeaseFaultDetectionHandler extends AbstractFaultDetectionHandler {
    public static final long DEFAULT_LEASE_DURATION = 1000 * 60;    
    public static final String LEASE_DURATION_KEY = "leaseDuration";    
    private long leaseDuration = DEFAULT_LEASE_DURATION;    
    /** The proxyPreparer for Lease */
    private ProxyPreparer leasePreparer;    
    /** Component name, used for config and logger */
    private static final String COMPONENT = 
        "org.rioproject.fdh.LeaseFaultDetectionHandler";
    /** A Logger */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    public void setLeaseDuration(long leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public void setLeasePreparer(ProxyPreparer leasePreparer) {
        this.leasePreparer = leasePreparer;
    }

    /**
     * @see FaultDetectionHandler#setConfiguration
     */
    public void setConfiguration(String[] configArgs) {
        if(configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        try {
            this.configArgs = new String[configArgs.length];
            System.arraycopy(configArgs, 0, this.configArgs, 0, configArgs.length);
            
            this.config = ConfigurationProvider.getInstance(configArgs);
            setLeaseDuration(Config.getLongEntry(config,
                                                 COMPONENT,
                                                 LEASE_DURATION_KEY,
                                                 DEFAULT_LEASE_DURATION,
                                                 0,
                                                 Long.MAX_VALUE));
            setRetryCount(Config.getIntEntry(config,
                                             COMPONENT,
                                             RETRY_COUNT_KEY,
                                             DEFAULT_RETRY_COUNT,
                                             0,
                                             Integer.MAX_VALUE));
            setRetryTimeout(Config.getLongEntry(config,
                                                COMPONENT,
                                                RETRY_TIMEOUT_KEY,
                                                DEFAULT_RETRY_TIMEOUT,
                                                0,
                                                Long.MAX_VALUE));
            setLeasePreparer((ProxyPreparer)config.getEntry(COMPONENT,
                                                            "leasePreparer",
                                                            ProxyPreparer.class,
                                                            new BasicProxyPreparer()));
            if(logger.isTraceEnabled()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("LeaseFaultDetectionHandler Properties : ");
                buffer.append("lease duration=").append(leaseDuration).append(", ");
                buffer.append("retry count=").append(retryCount).append(", ");
                buffer.append("retry timeout=").append(retryTimeout);
                logger.trace(buffer.toString());
            }
        } catch(ConfigurationException e) {
            logger.error("Setting Configuration", e);
        }
    }    

    /**
     * Override parent's getServiceMonitor() method to create 
     * the ServiceLeaseManager
     */
    protected ServiceMonitor getServiceMonitor() throws Exception {
        ServiceMonitor monitor = null;
        if(proxy instanceof MonitorableService) {
            MonitorableService service = (MonitorableService)proxy;
            Lease lease = service.monitor(leaseDuration);
            lease = (Lease)leasePreparer.prepareProxy(lease);
            serviceMonitor = new ServiceLeaseManager(lease);
        } else {
            logger.info("Service ["+proxy.getClass().getName()+"] not an "+
                        "instanceof "+MonitorableService.class.getName()+", "+
                        "ServiceRegistrar.TRANSITION_MATCH_NOMATCH transitions will "+
                        "only be monitored");
        }
        return(monitor);
    }
    
    /**
     * Manage the Lease to the MonitorableService
     */
    class ServiceLeaseManager extends Thread implements ServiceMonitor {
        long leaseTime;
        boolean keepAlive = true;
        Lease lease;

        /**
         * Create a ServiceLeaseManager
         * 
         * @param lease The Lease to manage
         */
        ServiceLeaseManager(Lease lease) {
            super("ServiceLeaseManager:"
                  + proxy.getClass().getName()
                  + ":"
                  + System.currentTimeMillis());
            this.lease = lease;
            this.leaseTime = lease.getExpiration() - System.currentTimeMillis();
            setDaemon(true);
            start();
        }

        /**
         * @see AdminFaultDetectionHandler.ServiceMonitor#drop
         */
        public void drop() {            
            interrupt();
        }

        public void interrupt() {
            if(logger.isTraceEnabled())
                logger.trace("Terminating ServiceMonitor Thread");
            try {
                lease.cancel();
            } catch(Exception ignore) {
                /* ignore */
            }
            keepAlive = false;
            super.interrupt();
        }

        /**
         * @see AdminFaultDetectionHandler.ServiceMonitor#verify()
         */
        public boolean verify() {
            if(!keepAlive)
                return (false);
            boolean verified = false;
            try {
                /*
                 * Invoke the service. If the service isnt active we'll get a
                 * RemoteException
                 */
                MonitorableService service = (MonitorableService)proxy;
                service.ping();
                verified = true;
            } catch(RemoteException e) {
                if(logger.isTraceEnabled())
                    logger.trace("RemoteException reaching service, "+
                                  "service cannot be reached");
                keepAlive = false;
            }
            return (verified);
        }

        public void run() {
            while (!interrupted()) {
                if(!keepAlive) {
                    return;
                }
                long leaseRenewalTime = TimeUtil.computeLeaseRenewalTime(leaseTime);
                if(logger.isTraceEnabled())
                    logger.trace("ServiceLeaseManager: Lease renewal wait for ["
                                  + leaseRenewalTime
                                  + "] millis "
                                  + "for "
                                  + proxy.getClass().getName());
                try {
                    sleep(leaseRenewalTime);
                } catch(InterruptedException ie) {
                    /* ignore */
                } catch(IllegalArgumentException iae) {
                    logger.warn("ServiceLeaseManager: sleep time is off : "
                                   + leaseRenewalTime);
                }
                if(lease != null) {
                    try {
                        if(logger.isTraceEnabled())
                            logger.trace("Renew lease for : "
                                          + proxy.getClass().getName());
                        lease.renew(leaseTime);
                    } catch(Exception e) {
                        if(!ThrowableUtil.isRetryable(e)) {
                            keepAlive = false;
                            if(logger.isDebugEnabled())
                                logger.debug(
                                           "Unrecoverable Exception renewing"+
                                           "Lease",
                                           e);
                        }
                        /*
                         * If we failed to renew the Lease we should try and
                         * re-establish comms to the Service and get another
                         * Lease
                         */
                        if(keepAlive) {
                            if(logger.isTraceEnabled())
                                logger.trace("Failed to renew lease to : "
                                              + proxy.getClass().getName()
                                              + ", retry ["
                                              + retryCount
                                              + "] "
                                              + "times, waiting ["
                                              + retryTimeout
                                              + "] millis between "
                                              + "attempts");
                            boolean connected = false;
                            for(int i = 0; i < retryCount; i++) {
                                long t0 = 0;
                                long t1;
                                try {
                                    if(logger.isTraceEnabled())
                                        logger.trace("Attempt to re-establish "+
                                                      "Lease to : "
                                                      + proxy.getClass().getName()
                                                      + ", attempt "
                                                      + "["
                                                      + i
                                                      + "]");
                                    MonitorableService service = 
                                        (MonitorableService)proxy;
                                    t0 = System.currentTimeMillis();
                                    this.lease = service.monitor(leaseDuration);
                                    if(logger.isTraceEnabled())
                                        logger.trace("Re-established Lease to : "
                                                      + proxy.getClass().getName());
                                    connected = true;
                                    break;
                                } catch(Exception e1) {
                                    t1 = System.currentTimeMillis();
                                    if(logger.isTraceEnabled())
                                        logger.trace("Invocation attempt ["
                                                      + i
                                                      + "] took ["
                                                      + (t1 - t0)
                                                      + "] "
                                                      + "millis to fail for : "
                                                      + proxy.getClass().getName());
                                    if(retryTimeout > 0) {
                                        try {
                                            sleep(retryTimeout);
                                        } catch(InterruptedException ie) {
                                            /* ignore */
                                        }
                                    }
                                }
                            }
                            if(!connected) {
                                if(logger.isTraceEnabled()) {
                                    if(proxy != null)
                                        logger.trace("Unable to recover Lease to ["
                                                      + proxy.getClass().getName()
                                                      + "], notify "
                                                      + "listeners and exit");
                                    else
                                        logger.trace("Unable to recover Lease to "+
                                                      "[null proxy], "
                                                      + "notify listeners and exit");
                                }
                                notifyListeners();
                                break;
                            }
                        } else {
                            notifyListeners();
                            break;
                        }
                    }
                }
            }
            terminate();
        }
    }
       
}
