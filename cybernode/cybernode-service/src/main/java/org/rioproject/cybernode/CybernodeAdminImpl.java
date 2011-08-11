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
import net.jini.id.UuidFactory;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.admin.ServiceAdminImpl;
import org.rioproject.system.MeasuredResource;
import org.rioproject.sla.SLA;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.rioproject.admin.ServiceAdmin;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.measurable.MeasurableCapability;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * The CybernodeAdminImpl class implements the ServiceAdmin interface providing
 * administrative support for ServiceProducer implementations.
 *
 * @author Dennis Reedy
 */
public class CybernodeAdminImpl extends ServiceAdminImpl
    implements CybernodeAdmin, ServerProxyTrust {
    /** A Logger */
    static Logger logger = Logger.getLogger("org.rioproject.cybernode");
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
    public void setPersistentProvisioning(boolean support) 
    throws IOException {
        backend.setPersistentProvisioning(support);
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#setSLA(org.rioproject.sla.SLA)
     */
    public boolean setSLA(SLA serviceLevelAgreement) {
        if(serviceLevelAgreement==null)
            throw new IllegalArgumentException("serviceLevelAgreement is null");
        String identifier = serviceLevelAgreement.getIdentifier();
        if(identifier==null)
            throw new IllegalArgumentException("SLA.identifier is null");
        MeasurableCapability mCap = getMeasurableCapability(identifier);
        if(mCap==null)
            return(false);
        mCap.setSLA(serviceLevelAgreement);
        return(true);
        //return(backend.setSLA(serviceLevelAgreement));
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#getSLAs()
     */
    public SLA[] getSLAs() {
        MeasurableCapability[] mCaps =
            backend.getComputeResource().getMeasurableCapabilities();
        SLA[] serviceLevelAgreement = new SLA[mCaps.length];
        for(int i=0; i<mCaps.length; i++) {
            serviceLevelAgreement[i] = mCaps[i].getSLA();
        }
        return(serviceLevelAgreement);
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#getPlatformCapabilties()
     */
    public PlatformCapability[] getPlatformCapabilties() {
        return(backend.getComputeResource().getPlatformCapabilities());
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#getMeasuredResources()
     */
    public MeasuredResource[] getMeasuredResources() {
        return(backend.getComputeResource().getMeasuredResources());
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.ComputeResourceAdmin#getUtilization()
     */
    public double getUtilization() {
        return(backend.getUtilization());
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

    public long getReportInterval() {
        return backend.getComputeResource().getReportInterval();
    }

    public void setReportInterval(long reportInterval) {
        backend.getComputeResource().setReportInterval(reportInterval);
    }

    public TrustVerifier getProxyVerifier() throws RemoteException {
        return(new CybernodeAdminProxy.Verifier(remoteRef));
    }

    /*
     * Get a MeasurableCapability from an identifier
     *
     * @param identifier The identifier of a MeasurableCapability to get
     */
    private MeasurableCapability getMeasurableCapability(String identifier) {
        if(identifier==null)
            throw new IllegalArgumentException("identifier is null");
        MeasurableCapability[] mCaps =
            backend.getComputeResource().getMeasurableCapabilities();
        for (MeasurableCapability mCap : mCaps) {
            if (mCap.getId().equals(identifier)) {
                return (mCap);
            }
        }
        return(null);
    }
}
