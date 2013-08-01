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
package org.rioproject.fdh;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.id.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The heartbeat client that produces heartbeat broadcasts
 *
 * @author Dennis Reedy
 */
public class HeartbeatClient {
    private static final long DEFAULT_HEARTBEAT_PERIOD = 1000 * 30;
    public static final String HEARTBEAT_SERVER_KEY = "heartbeatServer";
    public static final String HEARTBEAT_PERIOD_KEY = "heartbeatPeriod";
    /** Uuid to send with heartbeat */
    private Uuid uuid;
    /** The Timer to use for scheduling heartbeat timeout tasks */
    private Timer taskTimer;
    /** Component name, used for config and logger */
    private static final String COMPONENT = 
        "org.rioproject.fdh.HeartbeatFaultDetectionHandler";
    /** A Logger */
    static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Create a HeartbeatClient with the uuid for the Service
     *
     * @param uuid The unique identifier of the service
     */
    public HeartbeatClient(Uuid uuid) {
        this.uuid = uuid;
        taskTimer = new Timer(true);
    }

    /**
     * Stop sending heartbeats to all heartbeat server instances
     */
    public void terminate() {
        taskTimer.cancel();
    }

    /**
     * Start sending heartbeats to another server
     * 
     * @param configArgs Configuration entries
     *
     * @throws ConfigurationException if the configuration cannot be read
     */
    public void addHeartbeatServer(String[] configArgs)
        throws ConfigurationException {
        Configuration config = ConfigurationProvider.getInstance(configArgs);
        String heartbeatServer = (String)config.getEntry(COMPONENT,
                                                         HEARTBEAT_SERVER_KEY,
                                                         String.class);
        int port  ;
        int ndx = heartbeatServer.indexOf(':');
        if(ndx != -1) {
            String portString = heartbeatServer.substring(ndx + 1);
            heartbeatServer = heartbeatServer.substring(0, ndx);
            port = Integer.parseInt(portString);
        } else {
            throw new ConfigurationException("heartbeatServer wrong format, no port");
        }
        long heartbeatPeriod = Config.getLongEntry(config,
                                                   COMPONENT,
                                                   HEARTBEAT_PERIOD_KEY,
                                                   DEFAULT_HEARTBEAT_PERIOD,
                                                   0,
                                                   Long.MAX_VALUE);
        
        if(logger.isTraceEnabled()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("HeartbeatClient Properties : ");
            buffer.append("heartbeatPeriod=").append(heartbeatPeriod).append(", ");
            buffer.append("heartbeatServer=").append(heartbeatServer).append(", ");
            buffer.append("port=").append(port);
            logger.trace(buffer.toString());
        }
        try {
            InetAddress address = InetAddress.getByName(heartbeatServer);
            taskTimer.scheduleAtFixedRate(new HeartbeatTask(address, port),
                                          1000,
                                          heartbeatPeriod);
        } catch(UnknownHostException e) {
            throw new ConfigurationException("heartbeatServer "+ 
                                             "["+heartbeatServer+"] Unknown host", 
                                             e);
        } 
    }
    /**
     * Scheduled Task which gets submitted to see if the service responds with a
     * heartbeat in a certain amount of time
     */
    class HeartbeatTask extends TimerTask {
        InetAddress address;
        int port;

        /**
         * Create a HeartbeatTimeoutTask
         */
        HeartbeatTask(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            try {
                Socket socket = new Socket(address, port);
                socket.getOutputStream().write(uuid.toString().getBytes());
                socket.close();
            } catch(java.net.NoRouteToHostException e) {
                logger.warn(e.getClass().getName()+" "+
                               "Heartbeat server "+
                               "["+address.getHostAddress()+":"+port+"] "+
                               "cannot be reached, cancel HeartbeatTask");
                cancel();
            } catch(IOException e) {
                StringBuilder s = new StringBuilder();
                s.append("Heartbeat server communication dropped, cancel HeartbeatTask to [");
                s.append(address.getHostAddress()).append(":").append(port).append("]");
                if(logger.isDebugEnabled()) 
                    logger.debug("{} {}",
                                 e.getClass().getName(), s.toString());
                if(logger.isTraceEnabled()) 
                    logger.trace(s.toString(), e);
                cancel();
            }
        }
    }
}
