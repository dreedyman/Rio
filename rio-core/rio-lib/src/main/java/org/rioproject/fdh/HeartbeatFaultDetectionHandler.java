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
import com.sun.jini.config.ConfigUtil;
import net.jini.config.ConfigurationProvider;
import net.jini.core.lookup.ServiceID;
import net.jini.lookup.LookupCache;
import org.rioproject.admin.MonitorableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The HeartbeatFaultDetectionHandler is used to monitor services that implement the
 * {@link org.rioproject.admin.MonitorableService} interface.
 *
 * <p>
 * The HeartbeatFaultDetectionHandler creates a server socket and listens for
 * notifications from the service to determine a service's reachability. If the service
 * fails to provide a heartbeat notification within the timeframe specified, the
 * HeartbeatFaultDetectionHandler will notify {@link FaultDetectionListener} instances
 * of the failure.
 * <p>
 * Additionally, the HeartbeatFaultDetectionHandler will register with Lookup
 * Services for {@link net.jini.core.lookup.ServiceRegistrar#TRANSITION_MATCH_NOMATCH}
 * transitions for the service being monitored. If the service is adminstratively removed from the
 * network, or the service monitoring lease is between lease renewal points and
 * the service has actually been removed from the network, the transition will
 * be noted and FaultDetectionListener instances will be notified of the
 * failure.
 * <p>
 * If the service does not implement the
 * <tt>org.rioproject.admin.MonitorableService</tt> interface, the
 * HeartbeatFaultDetectionHandler will not perform heartbeat monitoring, but
 * will only create the event consumer for Lookup Service
 * TRANSITION_MATCH_NOMATCH transitions.
 *  
 * <p>
 * <b><font size="+1">Configuring HeartbeatFautDetectionHandler</font></b>
 * <p>
 * This implementation of HeartbeatFaultDetectionHandler supports
 * the following configuration entries; where each configuration entry
 * name is associated with the component name
 * <code>org.rioproject.fdh.HeartbeatFautDetectionHandler</code>.
 * <br>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">serverSocket </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top; font-family: monospace;">
 * java.net.ServerSocket</td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>new
 * java.net.ServerSocket(0)</code>
 * </td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">Creates the <span style="font-family:
 * monospace;">ServerSocket </span> instance with an anonymous port, which will
 * listen for service heartbeats</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">heartbeatPeriod </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;"><code>long</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>1000*30 (30 seconds)<br>
 *           </code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">Indicates the amount of time (in
 * milliseconds) the hearbeat will be sent by the <span style="font-family:
 * monospace;">org.rioproject.core.MonitorableService </span> to the <span
 * style="font-family: monospace;">HearbeatFaultDetectionHandler </span></td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 * <ul>
 * <li><span style="font-weight: bold; font-family: courier
 * new,courier,monospace;">heartbeatGracePeriod </span> <table cellpadding="2"
 * cellspacing="2" border="0" style="text-align: left; width: 100%;"> <tbody>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Type: <br>
 * </td>
 * <td style="vertical-align: top;"><code>long</code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Default: <br>
 * </td>
 * <td style="vertical-align: top;"><code>1000 (1 second)<br>
 *           </code></td>
 * </tr>
 * <tr><td style="vertical-align: top; text-align: right; font-weight:
 * bold;">Description: <br>
 * </td>
 * <td style="vertical-align: top;">The heartbeat grace period, that is the
 * maximum time (in milliseconds) outside of the period the Heartbeat will be
 * accepted before its determined as being late</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public class HeartbeatFaultDetectionHandler extends AbstractFaultDetectionHandler {
    private static final long DEFAULT_HEARTBEAT_PERIOD = 1000 * 30;
    private static final long DEFAULT_HEARTBEAT_GRACE_PERIOD = 1000;    
    public static final String SERVER_SOCKET_KEY = "serverSocket";    
    public static final String HEARTBEAT_PERIOD_KEY = "heartbeatPeriod";
    public static final String HEARTBEAT_GRACE_PERIOD_KEY = "heartbeatGracePeriod";    
    /** The heartbeat period */
    private long heartbeatPeriod = DEFAULT_HEARTBEAT_PERIOD;
    /**
     * The heartbeat grace period, that is the maximum time outside of the
     * period the Heartbeat will be accepted before its determined as being late
     */
    private long heartbeatGracePeriod = DEFAULT_HEARTBEAT_GRACE_PERIOD;
    /** The server socket */
    private ServerSocket serverSocket;
    /** Inner classs which listens for heartbeats */
    private HeartbeatManager heartbeatManager;    
    /** Modified configuration array with server listening entry added */
    private String[] monitorableConfig;
    /** The Timer to use for scheduling heartbeat timeout tasks */
    private Timer taskTimer;
    /** Component name, used for config and logger */
    private static final String COMPONENT = "org.rioproject.fdh.HeartbeatFaultDetectionHandler";
    /** A Logger */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * @see FaultDetectionHandler#setConfiguration
     */
    public void setConfiguration(String[] configArgs) {
        if(configArgs == null) {
            throw new IllegalArgumentException("configArgs is null");
        }
        try {
            this.config = ConfigurationProvider.getInstance(configArgs);
            setHeartbeatPeriod(Config.getLongEntry(config,
                                                   COMPONENT,
                                                   HEARTBEAT_PERIOD_KEY,
                                                   DEFAULT_HEARTBEAT_PERIOD,
                                                   0,
                                                   Long.MAX_VALUE));
            setHeartbeatGracePeriod(Config.getLongEntry(config,
                                                        COMPONENT,
                                                        HEARTBEAT_GRACE_PERIOD_KEY,
                                                        DEFAULT_HEARTBEAT_GRACE_PERIOD,
                                                        0,
                                                        Long.MAX_VALUE));
            ServerSocket defaultServerSocket =
                new ServerSocket(0,
                                 50,
                                 java.net.InetAddress.getLocalHost());
            serverSocket =
                (ServerSocket)Config.getNonNullEntry(config,
                                                     COMPONENT,
                                                     SERVER_SOCKET_KEY,
                                                     ServerSocket.class,
                                                     defaultServerSocket);

            if(defaultServerSocket.getLocalPort()!=serverSocket.getLocalPort())
                defaultServerSocket.close();
            String hostAddress = serverSocket.getInetAddress().getHostAddress();                                    
            int port = serverSocket.getLocalPort();
            monitorableConfig = new String[configArgs.length + 1];
            System.arraycopy(configArgs, 0, monitorableConfig, 0,
                             configArgs.length);
            String configEntry = COMPONENT+".heartbeatServer=" + "\""
                                          + hostAddress + ":" + port + "\"";
            monitorableConfig[configArgs.length] = ConfigUtil.concat(
                                                         new Object[]{configEntry});
            if(logger.isTraceEnabled()) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("HeartbeatFaultDetectionHandler Properties : ");
                buffer.append("heartbeatPeriod=").append(heartbeatPeriod).append(", ");
                buffer.append("heartbeatGracePeriod=").append(heartbeatGracePeriod).append(", ");
                buffer.append("heartbeatServer=").append(hostAddress).append(":").append(port);
                logger.trace(buffer.toString());
            }
        } catch(Exception e) {
            logger.error("Setting Configuration", e);
        }
    }
    

    /**
     * @see FaultDetectionHandler#monitor
     */
    public void monitor(Object proxy, ServiceID id, LookupCache lCache)
    throws Exception {
        taskTimer = new Timer(true);
        super.monitor(proxy, id, lCache);                        
    }
    
    /**
     * Override parent's getServiceMonitor() method to create 
     * the ServiceLeaseManager
     */
    protected ServiceMonitor getServiceMonitor() throws Exception {                
        if(proxy instanceof MonitorableService) {
            if(serverSocket != null) {
                heartbeatManager = new HeartbeatManager();
                MonitorableService service = (MonitorableService)proxy;
                try {
                    service.startHeartbeat(monitorableConfig);
                } catch(Exception e) {
                    //notifyListeners();
                    throw e;
                }
            } else {
                logger.warn("No ServerSocket, unable to create HeartbeatManager");
            }
        } else {
            logger.info("Service [{}] not an instanceof {}, ServiceRegistrar.TRANSITION_MATCH_NOMATCH transitions will "+
                        "only be monitored", proxy.getClass().getName(), MonitorableService.class.getName());
        }
        return(heartbeatManager);
    }
    
    /**
     * @see FaultDetectionHandler#terminate
     */
    public void terminate() {
        super.terminate();
        if(taskTimer != null) {
            taskTimer.cancel();
            taskTimer = null;
        }
    }

    public void setHeartbeatPeriod(long heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
    }

    public void setHeartbeatGracePeriod(long heartbeatGracePeriod) {
        this.heartbeatGracePeriod = heartbeatGracePeriod;
    }

    /**
     * Listen for heartbeats from the MonitorableService
     */
    class HeartbeatManager extends Thread implements ServiceMonitor {
        boolean keepAlive = true;
        long lastHeartbeat;
        InetAddress remoteAddress;

        /**
         * Create a HeartbeatManager
         */
        HeartbeatManager() {
            super("HeartbeatManager");
            setDaemon(true);
            start();
        }

        /**
         * Verify service can be reached. If the service cannot be reached
         * return false
         */
        public boolean verify() {
            if(!keepAlive) {
                return (false);
            }
            boolean verified = false;
            if(remoteAddress!=null) {
                try {
                    /*
                    * Invoke the service. If the service isnt active we'll get a
                    * RemoteException
                    */
                    //MonitorableService service = (MonitorableService)proxy;
                    //service.ping();
                    verified = remoteAddress.isReachable(1000);
                } catch(Exception e) {
                    logger.warn("RemoteException reaching service, service cannot be reached");
                }
            }
            return (verified);
        }

        /**
         * Close the socket, its all over
         */
        public void drop() {
            interrupt();
        }

        public void interrupt() {
            keepAlive = false;
            try {
                serverSocket.close();
            } catch(Exception ignore) {
            }
            super.interrupt();
        }

        public void run() {
            long now = System.currentTimeMillis();
            lastHeartbeat = now;
            /* Schedule repeated fixed-delay execution of HeartbeatTimeoutTasks, 
             * the first execution being the now + heartbeatPeriod. Subsequent 
             * executions take place at  approximately regular intervals defined 
             * by the heartbeatPeriod */
            HeartbeatTimeoutTask hbTask = new HeartbeatTimeoutTask(false);
            taskTimer.scheduleAtFixedRate(hbTask,
                                          new Date(now+heartbeatPeriod), 
                                          heartbeatPeriod);
            while(!isInterrupted()) {
                if(!keepAlive) {
                    return;
                }                                
                try {                                                            
                    Socket socket = serverSocket.accept();
                    now = System.currentTimeMillis();
                    lastHeartbeat = now;
                    if(logger.isTraceEnabled()) {
                        BufferedInputStream bis = new BufferedInputStream(
                                socket.getInputStream(), 256);
                        StringBuilder buf = new StringBuilder(80);
                        while(bis.available() > 0) {
                            byte[] data = new byte[bis.available()];
                            bis.read(data);
                            buf.append(new String(data));
                        }
                        remoteAddress = socket.getInetAddress();
                        String remoteAddr = (remoteAddress==null?"<unknown>":
                                             remoteAddress.getHostAddress());
                        if(remoteAddress!=null)
                            hbTask.remoteAddress = remoteAddr;

                        logger.trace("Received heartbeat from host={}, ID={}", remoteAddr, buf.toString().trim());
                    }
                    /* Close the client socket */
                    socket.close();
                } catch(SocketException e) {
                    if(logger.isTraceEnabled()) {
                        logger.trace("ServerSocket closed, leave thread");
                    }
                    drop();
                    terminate();
                } catch(IOException e) {
                    logger.error("ServerSocket IOException", e);
                }
            }
        }
    }
    /**
     * Scheduled Task which gets submitted to see if the service responds with a
     * heartbeat in a certain amount of time
     */
    class HeartbeatTimeoutTask extends TimerTask {
        boolean retry;
        String remoteAddress;

        /**
         * Create a HeartbeatTimeoutTask
         *
         * @param retry The retry flag, if true schedule a retry if needed
         */
        HeartbeatTimeoutTask(boolean retry) {
            this.retry = retry;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            long now = System.currentTimeMillis();
            long period = now - heartbeatManager.lastHeartbeat;
            if(period > (heartbeatPeriod + heartbeatGracePeriod)) {
                if(!retry) {
                    if(logger.isDebugEnabled())
                        logger.debug("Heartbeat period exceeded, schedule follow-up task");
                    HeartbeatTimeoutTask task = new HeartbeatTimeoutTask(true);
                    if(logger.isDebugEnabled()) {
                        logger.debug("HeartbeatTimeoutTask: schedule heartbeat timeout task in [{}] millis",
                                     heartbeatPeriod);
                    }
                    taskTimer.schedule(task, new Date(now + heartbeatPeriod));
                } else {
                    if(logger.isDebugEnabled()) {
                        String s =
                            (remoteAddress == null?"":" at "+remoteAddress);
                        logger.debug("Service {} is ambiguous, assume the worst", s);
                    }
                    if(!terminating) {
                        heartbeatManager.drop();
                        notifyListeners();
                        cancel();
                        terminate();
                    }
                }
            } else {
                if(logger.isTraceEnabled()) {
                    String s = (remoteAddress==null?"":"from "+remoteAddress);
                    logger.trace("Heartbeat within period {}", s);
                }
            }
        }
    }
}
