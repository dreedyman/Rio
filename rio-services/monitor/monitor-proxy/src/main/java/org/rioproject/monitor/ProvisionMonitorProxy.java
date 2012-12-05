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

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.event.EventRegistration;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.resources.servicecore.AbstractProxy;
import org.rioproject.system.ResourceCapability;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A <code>ProvisionMonitorProxy</code> is a proxy for the ProvisionMonitor
 * server. This is the object passed to clients of the ProvisionMonitor.
 *
 * @author Dennis Reedy
 */
class ProvisionMonitorProxy extends AbstractProxy implements ProvisionMonitor, Serializable {
    private static final long serialVersionUID = 2L;
    final ProvisionMonitor monitorProxy;

    /**
     * Creates a ProvisionMonitor proxy, returning an instance that implements
     * RemoteMethodControl if the server does too.
     *
     * @param monitor The ProvisionMonitor server
     * @param id The Uuid of the ProvisionMonitor
     *
     * @return A ProvisionMonitorProxy
     */
    static ProvisionMonitorProxy getInstance(ProvisionMonitor monitor, Uuid id) {
        if(monitor instanceof RemoteMethodControl) {
            return new ConstrainableProvisionMonitorProxy(monitor, id, null);
        } else {
            return (new ProvisionMonitorProxy(monitor, id));
        }
    }

    /*
     * Private constructor
     */
    private ProvisionMonitorProxy(ProvisionMonitor monitor, Uuid id) {
        super(monitor, id);
        this.monitorProxy = monitor;
    }

    /* -------- Implement ProvisionMonitor methods -------- */
    /** @see org.rioproject.monitor.ProvisionMonitor#assignBackupFor */
    public boolean assignBackupFor(ProvisionMonitor monitor) throws RemoteException {
        return (monitorProxy.assignBackupFor(monitor));
    }

    /** @see org.rioproject.monitor.ProvisionMonitor#removeBackupFor */
    public boolean removeBackupFor(ProvisionMonitor monitor) throws RemoteException {
        return (monitorProxy.removeBackupFor(monitor));
    }

    /** @see org.rioproject.monitor.ProvisionMonitor#update */
    public void update(PeerInfo info) throws RemoteException {
        monitorProxy.update(info);
    }

    /** @see org.rioproject.monitor.ProvisionMonitor#getPeerInfo */
    public PeerInfo getPeerInfo() throws RemoteException {
        return (monitorProxy.getPeerInfo());
    }


    /*
     * Implement org.rioproject.deploy.ProvisionManager methods
     */
    /** @see org.rioproject.deploy.ProvisionManager#register */
    public EventRegistration register(MarshalledObject<ServiceBeanInstantiator> instantiator,
                                      MarshalledObject handback,
                                      ResourceCapability resourceCapability,
                                      List<DeployedService> deployedServices,
                                      int serviceLimit,
                                      long duration) throws LeaseDeniedException, RemoteException {
        return (monitorProxy.register(instantiator,
                                      handback,
                                      resourceCapability,
                                      deployedServices,
                                      serviceLimit,
                                      duration));
    }

    public Collection<MarshalledObject<ServiceBeanInstantiator>> getWrappedServiceBeanInstantiators() throws RemoteException {
        return monitorProxy.getWrappedServiceBeanInstantiators();
    }

    public ServiceBeanInstantiator[] getServiceBeanInstantiators() throws IOException {
        Collection<ServiceBeanInstantiator> serviceBeanInstantiators = new ArrayList<ServiceBeanInstantiator>();
        try {
            for(MarshalledObject<ServiceBeanInstantiator> marshalledObject : getWrappedServiceBeanInstantiators()) {
                serviceBeanInstantiators.add(marshalledObject.get());
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Unwrapping a Cybernode", e);
        }
        return serviceBeanInstantiators.toArray(new ServiceBeanInstantiator[serviceBeanInstantiators.size()]);
    }

    /** @see org.rioproject.deploy.ProvisionManager#update */
    public void update(ServiceBeanInstantiator instantiator,
                       ResourceCapability resourceCapability,
                       List<DeployedService> deployedServices,
                       int serviceLimit) throws UnknownLeaseException, RemoteException {
        monitorProxy.update(instantiator, resourceCapability, deployedServices, serviceLimit);
    }

    /**
     * A subclass of ProvisionMonitorProxy that implements RemoteMethodControl.
     */
    final static class ConstrainableProvisionMonitorProxy extends ProvisionMonitorProxy implements RemoteMethodControl {
        private static final long serialVersionUID = 2L;


        /* Creates an instance of this class. */
        private ConstrainableProvisionMonitorProxy(ProvisionMonitor monitor, Uuid id, MethodConstraints constraints) {
            super(constrainServer(monitor, constraints), id);
        }


        /** @see net.jini.core.constraint.RemoteMethodControl#setConstraints  */
        public RemoteMethodControl setConstraints(MethodConstraints constraints) {
            return (new ConstrainableProvisionMonitorProxy((ProvisionMonitor)server, uuid, constraints));
        }

        /*
         * Returns a copy of the server proxy with the specified client
         * constraints and methods mapping.
         */
        private static ProvisionMonitor constrainServer( ProvisionMonitor monitor, MethodConstraints constraints) {
            Method[] methods = ProvisionMonitor.class.getMethods();
            Method[] methodMapping = new java.lang.reflect.Method[methods.length * 2];
            for(int i = 0; i < methodMapping.length; i++)
                methodMapping[i] = methods[i / 2];
            MethodConstraints mConstraints = ConstrainableProxyUtil.translateConstraints(constraints, methodMapping);
            return ((ProvisionMonitor)((RemoteMethodControl)monitor).setConstraints(mConstraints));
        }

        /** @see RemoteMethodControl#getConstraints */
        public MethodConstraints getConstraints() {
            return ((RemoteMethodControl)monitorProxy).getConstraints();
        }

        /* Note that the superclass's hashCode method is OK as is. */
        /* Note that the superclass's equals method is OK as is. */
        /*
         * Returns a proxy trust iterator that is used in
         * <code>ProxyTrustVerifier</code> to retrieve this object's trust
         * verifier.
         */
        @SuppressWarnings("unused")
        private ProxyTrustIterator getProxyTrustIterator() {
            return (new SingletonProxyTrustIterator(monitorProxy));
        }

        /*
         * Verify that the server implements RemoteMethodControl 
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            /*
             * Note that basic validation of the fields of this class was
             * already performed in the readObject() method of this class' super
             * class.
             */
            s.defaultReadObject();
            if(!(server instanceof RemoteMethodControl)) {
                throw new InvalidObjectException(
                                "ConstrainableProvisionMonitorProxy.readObject "+
                                "failure - server does not implement "+
                                "constrainable functionality ");
            }
        }
    }

    /**
     * A trust verifier for secure smart proxies
     */
    final static class Verifier implements TrustVerifier, Serializable {
        private static final long serialVersionUID = 1L;
        private final RemoteMethodControl serverProxy;

        /*
         * Create the verifier, throwing UnsupportedOperationException if the
         * server proxy does not implement both RemoteMethodControl and
         * TrustEquivalence.
         */
        Verifier(Object serverProxy) {
            if (serverProxy instanceof RemoteMethodControl && serverProxy instanceof TrustEquivalence) {
                this.serverProxy = (RemoteMethodControl) serverProxy;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /* Implement TrustVerifier */
        public boolean isTrustedObject(Object obj, TrustVerifier.Context ctx)
        throws RemoteException {
            if (obj == null || ctx == null) {
                throw new IllegalArgumentException();
            } else if (!(obj instanceof ConstrainableProvisionMonitorProxy)) {
                return false;
            }
            RemoteMethodControl otherServerProxy =
                (RemoteMethodControl) ((ConstrainableProvisionMonitorProxy)obj).monitorProxy;
            MethodConstraints mc = otherServerProxy.getConstraints();
            TrustEquivalence trusted = (TrustEquivalence) serverProxy.setConstraints(mc);
            return(trusted.checkTrustEquivalence(otherServerProxy));
        }
    }
}
