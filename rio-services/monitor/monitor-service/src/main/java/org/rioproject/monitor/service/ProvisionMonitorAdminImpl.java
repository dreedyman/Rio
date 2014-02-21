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
package org.rioproject.monitor.service;

import net.jini.export.Exporter;
import net.jini.id.UuidFactory;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.admin.ServiceAdmin;
import org.rioproject.deploy.DeploymentResult;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.impl.admin.ServiceAdminImpl;
import org.rioproject.impl.persistence.SnapshotHandler;
import org.rioproject.monitor.ProvisionMonitor.PeerInfo;
import org.rioproject.monitor.ProvisionMonitorAdmin;
import org.rioproject.monitor.proxy.ProvisionMonitorAdminProxy;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.ResourceCapability;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * The ProvisionMonitorAdminImpl class implements the ServiceAdmin interface providing 
 * administrative support.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorAdminImpl extends ServiceAdminImpl implements ProvisionMonitorAdmin, ServerProxyTrust {
    /** Reference to the backend */
    private final ProvisionMonitorImpl backend;
    private ProvisionMonitorAdmin remoteRef;
    
    /**
     * Create a ProvisionMonitorAdminImpl
     * 
     * @param service Concrete implementation of a ServiceBeanAdapter
     * @param exporter The Exporter to export this object
     */
    public ProvisionMonitorAdminImpl(ProvisionMonitorImpl service, Exporter exporter) {
        this(service, exporter, null);
    }

    /**
     * Create a ProvisionMonitorAdminImpl
     * 
     * @param service Concrete implementation of a ServiceBeanAdapter
     * @param exporter The Exporter to export this object
     * @param snapshotHandler The service's snapshot handler used for persistence
     */
    public ProvisionMonitorAdminImpl(ProvisionMonitorImpl service, 
                                     Exporter exporter, 
                                     SnapshotHandler snapshotHandler) {
        super(service, exporter, snapshotHandler);
        backend = service;
    }

    /**
     * Override parent's method to return <code>TrustVerifier</code> which can
     * be used to verify that the given proxy to this service can be trusted
     *
     * @return TrustVerifier The TrustVerifier used to verify the proxy
     *
     */
    public TrustVerifier getProxyVerifier() {
        return (new ProvisionMonitorAdminProxy.Verifier(remoteRef));
    }

    /**
     * Override parents getServiceAdmin method
     */
    public ServiceAdmin getServiceAdmin() throws RemoteException {
        if(adminProxy==null) {
            remoteRef = (ProvisionMonitorAdmin)exporter.export(this);
            adminProxy =  ProvisionMonitorAdminProxy.getInstance(remoteRef, UuidFactory.generate());
        }
        return(adminProxy);
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public DeploymentResult deploy(URL opStringURL) throws OperationalStringException {
        return backend.deploy(opStringURL, null);
    }
    
    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public DeploymentResult deploy(URL opStringURL, ServiceProvisionListener listener) throws OperationalStringException {
        return backend.deploy(opStringURL, listener);
    }

     /* (non-Javadoc)
    * @see org.rioproject.monitor.DeployAdmin#deploy
    */
    public DeploymentResult deploy(String location) throws OperationalStringException {
        return backend.deploy(location, null);
    }

    /* (non-Javadoc)
    * @see org.rioproject.monitor.DeployAdmin#deploy
    */
    public DeploymentResult deploy(String location, ServiceProvisionListener listener)
        throws OperationalStringException {
        return backend.deploy(location, listener);
    }

    /* (non-Javadoc)
    * @see org.rioproject.monitor.DeployAdmin#deploy
    */
    public DeploymentResult deploy(OperationalString opString) throws OperationalStringException {
        return backend.deploy(opString, null);
    }
    
    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public DeploymentResult deploy(OperationalString opString, ServiceProvisionListener listener)
        throws OperationalStringException {
        return backend.deploy(opString, listener);
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#undeploy(java.lang.String, boolean)
     */
    public boolean undeploy(String opStringName) throws OperationalStringException {
        return(backend.undeploy(opStringName, true));
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#hasDeployed(java.lang.String)
     */
    public boolean hasDeployed(String opStringName) {
        return(backend.hasDeployed(opStringName));
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManagers()
     */
    public OperationalStringManager[] getOperationalStringManagers()  {
        return(backend.getOperationalStringManagers());
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManager(java.lang.String)
     */
    public OperationalStringManager getOperationalStringManager(String name) throws OperationalStringException {
        return(backend.getOperationalStringManager(name));
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.ProvisionMonitorAdmin#getBackupInfo()
     */
    public PeerInfo[] getBackupInfo() {
        return(backend.getBackupInfo());
    }

    @Override
    public ResourceCapability getResourceCapability() {
        return null;
    }

    @Override
    public ComputeResourceUtilization getComputeResourceUtilization() {
        return backend.getComputeResource().getComputeResourceUtilization();
    }
}
