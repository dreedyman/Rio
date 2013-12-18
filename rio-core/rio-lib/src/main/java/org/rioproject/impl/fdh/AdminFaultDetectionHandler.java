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
package org.rioproject.impl.fdh;

import com.sun.jini.config.Config;
import net.jini.admin.Administrable;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import org.rioproject.impl.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

/**
 * The AdminFaultDetectionHandler is used to monitor services that implement
 * the {@link net.jini.admin.Administrable} interface.
 * <p>
 * If the service implements the {@link net.jini.admin.Administrable} interface,
 * the AdminFaultDetectionHandler invokes the
 * {@link net.jini.admin.Administrable#getAdmin()} method periodically. If the
 * method invocation returns successfully, the service is assumed to be available. If
 * the method invocation results in a failure,  and all retry attempts have failed, 
 * the AdminFaultDetectionHandler will notify FaultDetectionListener instances of
 * the failure.
 * <p>
 * Additionally, the AdminFaultDetectionHandler will register with Lookup
 * Services for
 * {@link net.jini.core.lookup.ServiceRegistrar#TRANSITION_MATCH_NOMATCH}
 * transitions for the
 * service being monitored. If the service is adminstratively removed from the
 * network, the transition will be noted and FaultDetectionListener instances will 
 * be notified of the failure.
 * <p>
 * If the service does not implement the
 * <code>net.jini.admin.Administrable</code> interface, the
 * AdminFaultDetectionHandler will only create the event consumer for
 * Lookup Service TRANSITION_MATCH_NOMATCH transitions.
 * <p>
 * <b><font size="+1">Configuring AdminFaultDetectionHandler</font> </b>
 * <p>
 * This implementation of AdminFaultDetectionHandler supports
 * the following configuration entries; where each configuration entry name is
 * associated with the component name
 * <code>org.rioproject.impl.fdh.AdminFaultDetectionHandler</code>.
 * <br>
 * <br>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">invocationDelay</span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top; font-family: monospace;">long</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>60*1000 (60 seconds)</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The amount of time in milliseconds to wait
 * between {@link net.jini.admin.Administrable#getAdmin()} method invocations</td>
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
 * the service when invoking the 
 * {@link net.jini.admin.Administrable#getAdmin()} method. If the service cannot be 
 * reached within the retry count specified the service will be determined to be 
 * unreachable
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
 * <br>
 * The amount of time it takes for the AdminFaultDetectionHandler to
 * determine service failure for a service that implements the
 * {@link net.jini.admin.Administrable} interface is calculated as
 * follows :<br>
 * 
 * <pre>
 * ((num_retries + 1) * (connectivity_timeout)) + (retry_delay * num_retries)
 * </pre>
 *
 * @author Dennis Reedy
 */
public class AdminFaultDetectionHandler extends AbstractFaultDetectionHandler {
    static final int DEFAULT_INVOCATION_DELAY = 1000 * 60;
    public static final String INVOCATION_DELAY_KEY = "invocationDelay";
    private long invocationDelay = DEFAULT_INVOCATION_DELAY;
    /** Component name, used for config and logger */
    private static final String COMPONENT =  AdminFaultDetectionHandler.class.getName();
    /** A Logger */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

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

            setInvocationDelay(Config.getLongEntry(config,
                                                   COMPONENT,
                                                   INVOCATION_DELAY_KEY,
                                                   DEFAULT_INVOCATION_DELAY,
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
            
            if(logger.isTraceEnabled()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("AdminFaultDetectionHandler Properties : ");
                buffer.append("invocation delay=").append(invocationDelay).append(", ");
                buffer.append("retry count=").append(retryCount).append(", ");
                buffer.append("retry timeout=").append(retryTimeout);
                logger.trace(buffer.toString());
            }
        } catch(ConfigurationException e) {
            logger.warn("Setting Configuration", e);
        }
    }

    public void setInvocationDelay(long invocationDelay) {
        this.invocationDelay = invocationDelay;
    }

    /**
     * Get the class which implements the ServiceMonitor
     */
    protected ServiceMonitor getServiceMonitor() throws Exception {
        ServiceMonitor monitor = null;
        if(proxy instanceof Administrable) {
            ((Administrable)proxy).getAdmin();
            monitor = new ServiceAdminManager();
        }
        return(monitor);
    }
        
    /**
     * Invoke the service's {@link net.jini.admin.Administrable#getAdmin()} method 
     * periodically
     */
    class ServiceAdminManager extends Thread implements ServiceMonitor {
        boolean keepAlive = true;

        /**
         * Create a ServiceLeaseManager
         */
        ServiceAdminManager() {
            super(String.format("ServiceAdminManager: %s:%d",
                                proxy.getClass().getName(), System.currentTimeMillis()));
            setDaemon(true);
            start();
        }

        /**
         * Its all over
         */
        public void drop() {
            interrupt();
        }

        public void interrupt() {
            if(logger.isTraceEnabled())
                logger.trace("Terminating ServiceMonitor Thread...");
            keepAlive = false;
            super.interrupt();
            if(logger.isTraceEnabled())
                logger.trace("ServiceMonitor Thread Terminated");
        }

        /**
         * Verify service can be reached. If the service cannot be reached
         * return false
         */
        public boolean verify() {
            if(!keepAlive)
                return (false);
            boolean verified = false;
            try {
                if(logger.isTraceEnabled())
                    logger.trace("Invoke getAdmin() on : {}", proxy.getClass().getName());
                ((Administrable)proxy).getAdmin();
                if(logger.isTraceEnabled())
                    logger.trace("Invocation to getAdmin() on : {} returned", proxy.getClass().getName());
                verified = true;
            } catch(RemoteException e) {
                if(logger.isDebugEnabled())
                    logger.debug("RemoteException reaching service, service cannot be reached");
                keepAlive = false;
            } catch(Throwable t) {
                if(!ThrowableUtil.isRetryable(t)) {
                    keepAlive = false;
                    if(logger.isDebugEnabled())
                        logger.debug("Unrecoverable Exception invoking getAdmin()", t);
                }
            }
            return (verified);
        }

        public void run() {
            while (!interrupted()) {
                if(!keepAlive) {
                    return;
                }
                if(logger.isTraceEnabled())
                    logger.trace("ServiceAdminManager: Wait for [{}] millis to invoke getAdmin() on {}",
                                  invocationDelay, proxy.getClass().getName());
                try {
                    sleep(invocationDelay);
                } catch(InterruptedException ie) {
                    /* should not happen */
                } catch(IllegalArgumentException iae) {
                    logger.warn("ServiceAdminManager: sleep time is off : {}", invocationDelay);
                }
                try {
                    if(logger.isTraceEnabled())
                        logger.trace("Invoke getAdmin() on : {}", proxy.getClass().getName());
                    ((Administrable)proxy).getAdmin();
                    if(logger.isTraceEnabled())
                        logger.trace("Invocation to getAdmin() on : {} returned", proxy.getClass().getName());
                } catch(Exception e) {
                    if(!ThrowableUtil.isRetryable(e)) {
                        keepAlive = false;
                        if(logger.isDebugEnabled())
                            logger.debug("Unrecoverable Exception invoking getAdmin()", e);
                    }
                    /*
                     * If we failed to invoke the method and the failure is
                     * not fatal, retry
                     */
                    if(keepAlive) {
                        if(logger.isTraceEnabled())
                            logger.trace("Failed to invoke getAdmin() on : {}, retry [{}] times, waiting [{}] millis between attempts",
                                         proxy.getClass().getName(), retryCount, retryTimeout);
                        boolean connected = false;
                        for(int i = 0; i < retryCount; i++) {
                            long t0 = 0;
                            long t1;
                            try {
                                if(logger.isTraceEnabled())
                                    logger.trace("Attempt to invoke getAdmin() on : {}, attempt [{}]",
                                                 proxy,getClass().getName(), i);
                                t0 = System.currentTimeMillis();
                                ((Administrable)proxy).getAdmin();
                                if(logger.isTraceEnabled())
                                    logger.trace("Re-established connection to : {}", proxy.getClass().getName());
                                connected = true;
                                break;
                            } catch(Exception e1) {
                                t1 = System.currentTimeMillis();
                                if(logger.isTraceEnabled())
                                    logger.trace("Invocation attempt [{}] took [{}] millis to fail for : {}",
                                                  i, (t1 - t0), proxy.getClass().getName());
                                if(retryTimeout > 0) {
                                    try {
                                        sleep(retryTimeout);
                                    } catch(InterruptedException ie) {
                                        /* should not happen */
                                    }
                                }
                            }
                        }
                        if(!connected) {
                            if(logger.isTraceEnabled()) {
                                String proxyName = (proxy==null?"null proxy":proxy.getClass().getName());
                                logger.trace("Unable to invoke getAdmin() on [{}], notify listeners and exit", proxyName);
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
            terminate();
        }
    }
}
