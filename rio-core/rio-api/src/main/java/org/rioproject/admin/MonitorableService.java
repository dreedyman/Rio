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
package org.rioproject.admin;

import net.jini.config.ConfigurationException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;

import java.rmi.RemoteException;

/**
 * A MonitorableService provides the semantics to determine if a service (which
 * implements this interface) is reachable. One of two mechanisms can be used:
 * <ol>
 * <li>Establish a Lease to monitor the reachability of the Service. Using this
 * method a Lease is returned. If the service is unreachable the Lease can not
 * be renewed, a RemoteException will be thrown
 * <li>The Service will use a heartbeat mechanism to notify the caller of it's
 * existence
 * </ol>
 *
 * @author Dennis Reedy
 */
public interface MonitorableService {
    /**
     * Low cost roundtrip check
     * 
     * @throws RemoteException If an error occured during communication with the
     * service
     */
    void ping() throws RemoteException;

    /**
     * Establish a Lease to monitor the reachability of the Service
     * 
     * @param duration The duration (in milliseconds) of the requested Lease
     *
     * @return Lease The Lease to maintain
     *
     * @throws LeaseDeniedException If requested Lease is denied
     * @throws RemoteException If communication errors occur
     */
    Lease monitor(long duration) throws LeaseDeniedException, RemoteException;

    /**
     * Start a heartbeat mechanism to determine the reachability of the Service.
     * The endpoint to send the heartbeat to will be found by the configuration
     * property
     * 
     * <pre>
     * org.rioproject.fdh.HeartbeatFaultDetectionHandler.heartbeatServer
     * </pre>
     * 
     * @param configArgs Configuration attributes the Service will use to
     * establish a heartbeat mechanism
     *
     * @throws ConfigurationException If there are errors processing the
     * configuration
     * @throws RemoteException If communication errors occur
     */
    void startHeartbeat(String[] configArgs) throws ConfigurationException,
        RemoteException;
}
