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
package org.rioproject.monitor;

import org.rioproject.deploy.ProvisionManager;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.resources.servicecore.Service;

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * The ProvisionMonitor extends the ProvisionManager and Service interfaces, and
 * defines the semantics required for ProvisionMonitor instances to back-up each
 * other
 *
 * @author Dennis Reedy
 */
public interface ProvisionMonitor extends ProvisionManager, Service {

    /**
     * Assign the ProvisionMonitor as a backup for another ProvisionMonitor
     *
     * @param monitor The ProvisionMonitor that will be backed up. If this
     * ProvisionMonitor is removed from the network, then all OperationalString
     * instances the removed ProvisionManager was managing will be managed by this
     * ProvisionMonitor
     *
     * @return Return true if the ProvisionMonitor accepts the backup role, false
     * if it refuses.
     *
     * @throws RemoteException if communication errors occur
     */
    boolean assignBackupFor(ProvisionMonitor monitor) throws RemoteException;

    /**
     * Remove the ProvisionMonitor as a backup for another ProvisionMonitor
     *
     * @param monitor The <i>primary</i> ProvisionMonitor that no longer requires
     * the support of this ProvisionManager
     *
     * @return Return true if the ProvisionMonitor has removed itself as a backup
     *
     * @throws RemoteException if communication errors occur
     */
    boolean removeBackupFor(ProvisionMonitor monitor) throws RemoteException;

    /**
     * Notification from ProvisionMonitor peers updates to the PeerInfo object.
     * This notification is sent as ProvisionMonitor instances are selected by
     * other ProvisionMonitor instances for backup duty.
     *
     * @param info The PeerInfo object
     *
     * @throws RemoteException if communication errors occur
     */
    void update(PeerInfo info) throws RemoteException;

    /**
     * Get the PeerInfo object for the ProvisionMonitor
     *
     * @return The PeerInfo object for the ProvisionMonitor
     *
     * @throws RemoteException if communication errors occur
     */
    PeerInfo getPeerInfo() throws RemoteException;
    
    /**
     * Get all registered {@link org.rioproject.deploy.ServiceBeanInstantiator} instances, wrapped in a collection of
     * {@link MarshalledObject}s.
     *
     * @return An array of registered {@link org.rioproject.deploy.ServiceBeanInstantiator} instances as {@link MarshalledObject}.
     * If there are no registered <tt>ServiceBeanInstantiator</tt>s, return
     * a zero-length array. A new array is allocated each time
     *
     * @throws RemoteException If communication errors happen
     */
    Collection<MarshalledObject<ServiceBeanInstantiator>> getWrappedServiceBeanInstantiators() throws RemoteException;
    
    /**
     * Contains information about ProvisionMonitor peers involved in providing 
     * support for backup approach 
     */
    static class PeerInfo implements Comparable, Serializable {
        private static final long serialVersionUID = 2L;
        private ProvisionMonitor service;
        private Integer backupCount;
        private String address;
        private Long id;
        public final static int INITIAL_DEPLOYMENTS_PENDING = 0;
        public final static int LOADING_INITIAL_DEPLOYMENTS = 1;
        public final static int LOADED_INITIAL_DEPLOYMENTS = 2;
        private int initialDeploymentState;

        /**
         * Create a new PeerInfo
         * 
         * @param service ProvisionMonitor proxy
         * @param id A random number identifier to be used to break ties 
         * @param address The TCP/IP address of the machine the ProvisionMonitor
         * is running on
         */
        public PeerInfo(ProvisionMonitor service, long id, String address) {
            if(service==null)
                throw new IllegalArgumentException("service is null");
            if(address==null)
                throw new IllegalArgumentException("address is null");
            this.service = service;            
            this.address = address;
            this.id = id;
            backupCount = 0;
        }

        public Long getID() {
            return(id);
        }
        
        /**
         * Get the TCP/IP address of the machine the ProvisionMonitor
         * is running on
         *
         * @return The address of the machine the ProvisionMonitor is running on
         */
        public String getAddress() {
            return(address);    
        }
        
        /**
         * Set the count property
         * 
         * @param bCount The new count for the amount of backups the
         * ProvisionMonitor is a backup for
         */
        public void setBackupCount(int bCount) {
            if(bCount<0)
                throw new IllegalArgumentException("backupCount must be positive");
            synchronized(this) {
                this.backupCount = bCount;
            }
        }
        
        /**
         * Get the number of ProvisionMonitors that are backed up
         *
         * @return The number of ProvisionMonitors that are backed up
         */
        public Integer getBackupCount() {
            Integer bCount;
            synchronized(this) {
                bCount = backupCount;
            }
            return(bCount);
        }
        
        /**
         * Get the ProvisionMonitor service
         *
         * @return The ProvisionMonitor
         */
        public ProvisionMonitor getService() {
            return(service);
        }

        /**
         * Set the initial deployment load state
         *
         * @param state The state
         *
         * @throws IllegalStateException if the new state is not valid
         */
        public void setInitialDeploymentLoadState(int state) {
            if(state < INITIAL_DEPLOYMENTS_PENDING ||
               state > LOADED_INITIAL_DEPLOYMENTS)
                throw new IllegalStateException("state must be between "+
                                                INITIAL_DEPLOYMENTS_PENDING+" " +
                                                "and "+
                                                LOADED_INITIAL_DEPLOYMENTS);
            initialDeploymentState = state;
        }

        /**
         * Get the initial deployment state
         *
         * @return The initial deployment state
         */
        public int getInitialDeploymentLoadState() {
            return(initialDeploymentState);
        }

        /**
         * Override hashCode to return the hashCode of the service
         */
        public int hashCode() {
            return (service.hashCode());
        }

        /**
         * PeerInfo instances are equal when their ProvisionMonitor service
         * instance is equal
         */
        public boolean equals(Object o) {
            if(!(o instanceof PeerInfo))
                return (false);
            PeerInfo that = (PeerInfo)o;
            return (this.service.equals(that.service));
        }

        /**
         * Compare PeerInfo instances to each other to determine natural order.
         * Order is determined by backup count and lastly random ID
         */
        public int compareTo(Object o) {
            if(!(o instanceof PeerInfo))
                throw new ClassCastException();
            PeerInfo that = (PeerInfo)o;
            if(this.backupCount.equals(that.backupCount))
                return (this.id.compareTo(that.id));
            else
                return (this.backupCount.compareTo(that.backupCount));
        }
    }

}
