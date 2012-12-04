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
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceItemFilter;
import org.rioproject.jmx.JMXUtil;
import org.rioproject.resources.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

/**
 * The JMXFaultDetectionHandler is a fault detection handler that uses JMX remote
 * connectivity to determine if a peer JVM is active. The JMXFaultDetectionHandler
 * checks the liveness of a {@link javax.management.MBeanServerConnection} that
 * connects to a peer JVM's {@link javax.management.remote.JMXConnectorServer}.
 * <p>
 * If the peer JVM has not established a {@link javax.management.remote.JMXConnectorServer},
 * the JMXFaultDetectionHandler will only create an event consumer for Lookup
 * Service TRANSITION_MATCH_NOMATCH transitions.
 *
 * <p>
 * <b><font size="+1">Configuring JMXFaultDetectionHandler</font> </b>
 * <p>
 * This implementation of <code>JMXFaultDetectionHandler</code> supports
 * the following configuration entries; where each configuration entry name is
 * associated with the component name
 * <code>org.rioproject.fdh.JMXFaultDetectionHandler</code>.
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
 * between obtaining a {@link javax.management.MBeanServerConnection} to the service being monitored</td>
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
 * the service. If the service cannot be reached within the retry count specified
 * the service will be determined to be unreachable
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
 *
 * @author Dennis Reedy
 */
public class JMXFaultDetectionHandler extends AbstractFaultDetectionHandler {
    private String jmxConnection;
    static final int DEFAULT_INVOCATION_DELAY = 1000 * 60;
    public static final String INVOCATION_DELAY_KEY = "invocationDelay";
    private long invocationDelay = DEFAULT_INVOCATION_DELAY;
    /** Component name, used for config and logger */
    private static final String COMPONENT = JMXFaultDetectionHandler.class.getName();
    /**
     * A Logger
     */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    @Override
    public void monitor(Object proxy, final ServiceID id, LookupCache lCache)
        throws Exception {

        ServiceItem item = lCache.lookup(new ServiceItemFilter() {
            public boolean check(ServiceItem item) {
                return item.serviceID.equals(id);
            }
        });
        jmxConnection = JMXUtil.getJMXConnection(item.attributeSets);
        super.monitor(proxy, id, lCache);
    }

    /**
     * Monitor the peer service
     *
     * @throws IllegalStateException If the <tt>jmxConnection</tt> property
     * has not been set
     * @throws Exception If there are problems creating the underlying service
     * monitor
     */
    public void monitor() throws Exception {
        getServiceMonitor();
    }

    protected ServiceMonitor getServiceMonitor() throws Exception {
        ServiceMonitor monitor = null;
        if (jmxConnection != null) {
            monitor = new MBeanServerConnectionMonitor();
        }
        return (monitor);
    }

    public void setJMXConnection(String jmxConnection) {
        this.jmxConnection = jmxConnection;
    }

    public void setConfiguration(String[] configArgs) {
        if (configArgs == null)
            throw new IllegalArgumentException("configArgs is null");
        try {
            this.configArgs = new String[configArgs.length];
            this.configArgs = new String[configArgs.length];
            System.arraycopy(configArgs,
                             0,
                             this.configArgs,
                             0,
                             configArgs.length);

            setConfiguration(ConfigurationProvider.getInstance(configArgs));
        } catch (ConfigurationException e) {
            logger.error("Setting Configuration", e);
        }
    }

    public void setConfiguration(Configuration config) throws ConfigurationException {
        if (config == null)
            throw new IllegalArgumentException("config is null");
        this.config = config;

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
    }

    public void setInvocationDelay(long invocationDelay) {
        this.invocationDelay = invocationDelay;
    }

    class MBeanServerConnectionMonitor extends Thread implements ServiceMonitor {
        boolean keepAlive = true;

        MBeanServerConnectionMonitor() {
            super("MBeanServerConnectionMonitor:"+ System.currentTimeMillis());
            setDaemon(true);
            start();
        }

        @Override
        public void interrupt() {
            if (logger.isTraceEnabled())
                logger.trace("Terminating ServiceMonitor Thread");
            keepAlive = false;
            super.interrupt();
        }

        /**
         * Its all over
         */
        public void drop() {
            interrupt();
        }

        /**
         * Verify service can be reached. If the service cannot be reached
         * return false
         */
        public boolean verify() {
            if (!keepAlive)
                return (false);
            boolean verified = false;
            JMXConnector connector = null;
            try {
                if (logger.isTraceEnabled())
                    logger.trace("Getting an MBeanServerConnection to {}", jmxConnection);
                connector =
                    JMXConnectorFactory.connect(new JMXServiceURL(jmxConnection),
                                                null);
                connector.getMBeanServerConnection();

                if (logger.isTraceEnabled())
                    logger.trace("MBeanServerConnection to {} succeeded", jmxConnection);
                verified = true;
            } catch (IOException e) {
                if (logger.isDebugEnabled())
                    logger.debug("Unable create MBeanServerConnection to {}, service cannot be reached", jmxConnection);
                keepAlive = false;
            } catch (Throwable t) {
                if (!ThrowableUtil.isRetryable(t)) {
                    keepAlive = false;
                    if (logger.isDebugEnabled())
                        logger.debug("Unrecoverable Exception getting MBeanServerConnection", t);
                }
            } finally {
                if(connector!=null) {
                    try {
                        connector.close();
                    } catch (IOException e) {
                        logger.warn("Non-fatal error, unable to close JMXConnector to ["+jmxConnection+"]", e);
                    }
                }
            }
            return (verified);
        }

        public void run() {
            while (!interrupted()) {
                if (!keepAlive) {
                    return;
                }
                if (logger.isTraceEnabled())
                    logger.trace("Wait for [{}] millis to obtain MBeanServerConnection for {}",
                                  invocationDelay, jmxConnection);
                try {
                    sleep(invocationDelay);
                } catch (InterruptedException ie) {
                    /* should not happen */
                } catch (IllegalArgumentException iae) {
                    logger.warn("Sleep time is off : "+ invocationDelay);
                }

                /*
                * If we failed to invoke the method and the failure is
                * not fatal, retry
                */
                if (!verify()) {
                    if (logger.isTraceEnabled())
                        logger.trace("Unable create MBeanServerConnection to {}, retry [{}] times, waiting [{}] " +
                                     "millis between attempts", jmxConnection, retryCount, retryTimeout);
                    boolean connected = false;
                    for (int i = 0; i < retryCount; i++) {
                        long t0 = System.currentTimeMillis();
                        connected = verify();
                        if (!connected) {
                            long t1 = System.currentTimeMillis();
                            if (logger.isTraceEnabled())
                                logger.trace("Invocation attempt [{}] took [{}] millis to fail", i, (t1-t0));
                            if (retryTimeout > 0) {
                                try {
                                    sleep(retryTimeout);
                                } catch (InterruptedException ie) {
                                    /* should not happen */
                                }
                            }
                        }
                    }
                    if (!connected) {
                        keepAlive = false;
                        if (logger.isTraceEnabled()) {
                            logger.trace(
                                "Unable create MBeanServerConnection to {}, notify listeners and exit", jmxConnection);
                        }
                        notifyListeners();
                        break;
                    }
                } //else {
                  //  notifyListeners();
                  //  break;
                //}
            }
            terminate();
        }
    }
}
