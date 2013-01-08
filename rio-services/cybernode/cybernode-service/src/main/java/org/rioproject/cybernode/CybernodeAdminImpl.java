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
package org.rioproject.cybernode;

import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.admin.ServiceAdmin;
import org.rioproject.admin.ServiceAdminImpl;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.ResourceCapability;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * The CybernodeAdminImpl class implements the ServiceAdmin interface providing
 * administrative support for ServiceProducer implementations.
 *
 * @author Dennis Reedy
 */
public class CybernodeAdminImpl extends ServiceAdminImpl implements CybernodeAdmin, ServerProxyTrust {
    /** Reference to the backend */
    CybernodeImpl backend;
    CybernodeAdmin remoteRef;
    int registryPort;

    /**
     * Create a CybernodeAdminImpl
     * 
     * @param service Concrete implementation of a ServiceBeanAdapter
     * @param exporter The Exporter to export this object
     */
    public CybernodeAdminImpl(CybernodeImpl service, Exporter exporter) {
        this(service, exporter, null);
    }

    /**
     * Create a CybernodeAdminImpl
     * 
     * @param service The CybernodeImpl
     * @param exporter The Exporter to export this object
     * @param snapshotHandler The service's snapshot handler used for
     * persistence
     */
    public CybernodeAdminImpl(CybernodeImpl service, 
                              Exporter exporter,
                              SnapshotHandler snapshotHandler) {
        super(service, exporter, snapshotHandler);
        backend = service;
    }

    /**
     * Override parents getServiceAdmin method
     */
    public ServiceAdmin getServiceAdmin() throws RemoteException {
        if(adminProxy == null) {
            remoteRef = (CybernodeAdmin)exporter.export(this);
            adminProxy = CybernodeAdminProxy.getInstance(remoteRef, UuidFactory.generate());
        }
        return (adminProxy);
    }

    /* (non-Javadoc)
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceLimit()
     */
    public Integer getServiceLimit() {
        return(backend.getServiceLimit());
    }

    /* (non-Javadoc)
     * @see org.rioproject.cybernode.CybernodeAdmin#setServiceLimit(java.lang.Integer)
     */
    public void setServiceLimit(Integer count) {
        backend.setServiceLimit(count);
    }

    /* (non-Javadoc)
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceCount()
     */
    public Integer getServiceCount() {
        return(backend.getServiceCount());
    }

    public int getRegistryPort() {
        return registryPort;
    }

    void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    /* (non-Javadoc)
    * @see org.rioproject.cybernode.CybernodeAdmin#getPersistentProvisioning()
    */
    public boolean getPersistentProvisioning() {
        return(backend.getPersistentProvisioning());
    }

    /* (non-Javadoc)
     * @see org.rioproject.cybernode.CybernodeAdmin#setPersistentProvisioning(boolean)
     */
    public void setPersistentProvisioning(boolean support) throws IOException {
        backend.setPersistentProvisioning(support);
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#getResourceCapability()
     */
    public ResourceCapability getResourceCapability() throws RemoteException {
        return backend.getComputeResource().getResourceCapability();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.rioproject.system.ComputeResourceAdmin#getComputeResourceUtilization()
     */
    public ComputeResourceUtilization getComputeResourceUtilization() {
        return (backend.getComputeResource().getComputeResourceUtilization());
    }

    public ComputeResourceUtilization getComputeResourceUtilization(Uuid serviceUuid) {
        ComputeResourceUtilization cru = null;
        ServiceBeanDelegate delegate =
            backend.getServiceBeanContainer().getServiceBeanDelegate(serviceUuid);
        if(delegate!=null)
            cru = delegate.getComputeResourceUtilization();
        return cru;  
    }

    public TrustVerifier getProxyVerifier() throws RemoteException {
        return(new CybernodeAdminProxy.Verifier(remoteRef));
    }

}
