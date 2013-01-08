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

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.admin.ServiceAdminProxy;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.ResourceCapability;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * A <code>CybernodeAdminProxy</code> is a proxy for the CybernodeAdmin
 * server. This is the object passed to clients of the CybernodeAdmin.
 *
 * @author Dennis Reedy
 */
public class CybernodeAdminProxy extends ServiceAdminProxy implements CybernodeAdmin, Serializable {
    private static final long serialVersionUID = 3L;
    final CybernodeAdmin cybernodeAdminProxy;


    /**
     * Creates a CybernodeAdmin proxy, returning an instance that implements
     * RemoteMethodControl if the server does too.
     * 
     * @param serviceAdmin The CybernodeAdmin server
     * @param id The Uuid of the CybernodeAdmin
     *
     * @return A CybernodeAdminProxy
     */
    static CybernodeAdminProxy getInstance(CybernodeAdmin serviceAdmin, Uuid id) {
        if(serviceAdmin instanceof RemoteMethodControl) {
            return new ConstrainableCybernodeAdminProxy(serviceAdmin, id, null);
        } else {
            return (new CybernodeAdminProxy(serviceAdmin, id));
        }
    }

    /*
     * Private constructor
     */
    private CybernodeAdminProxy(CybernodeAdmin serviceAdmin, Uuid uuid) {
        super(serviceAdmin, uuid);
        cybernodeAdminProxy = serviceAdmin;
    }
    /**
     * A subclass of CybernodeAdminProxy that implements RemoteMethodControl.
     */
    final static class ConstrainableCybernodeAdminProxy extends CybernodeAdminProxy implements RemoteMethodControl {
        private static final long serialVersionUID = 2L;

        /* Creates an instance of this class. */
        private ConstrainableCybernodeAdminProxy(CybernodeAdmin serviceAdmin,
                                                 Uuid id, 
                                                 MethodConstraints constraints) {
            super(constrainServer(serviceAdmin, constraints), id);
        }

        /*
         * Returns a copy of the server proxy with the specified client
         * constraints and methods mapping.
         */
        private static CybernodeAdmin constrainServer(CybernodeAdmin serviceAdmin, MethodConstraints constraints) {
            java.lang.reflect.Method[] methods = CybernodeAdmin.class.getMethods();
            java.lang.reflect.Method[] methodMapping = new java.lang.reflect.Method[methods.length * 2];
            for(int i = 0; i < methodMapping.length; i++)
                methodMapping[i] = methods[i / 2];
            MethodConstraints methodConstraints = ConstrainableProxyUtil.translateConstraints(constraints, methodMapping);
            return (CybernodeAdmin)((RemoteMethodControl)serviceAdmin).setConstraints(methodConstraints);
        }

        /* @see net.jini.core.constraint.RemoteMethodControl#setConstraints  */
        public RemoteMethodControl setConstraints(MethodConstraints constraints) {
            return (new ConstrainableCybernodeAdminProxy((CybernodeAdmin)serviceAdmin,
                                                         uuid,
                                                         constraints));
        }

        /** @see RemoteMethodControl#getConstraints */
        public MethodConstraints getConstraints() {
            return ((RemoteMethodControl)serviceAdmin).getConstraints();
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
            return (new SingletonProxyTrustIterator(serviceAdmin));
        }

        /*
         * Performs various functions related to the trust verification process
         * for the current instance of this proxy class, as detailed in the
         * description for this class.
         * 
         * @throws InvalidObjectException if any of the
         * requirements for trust verification (as detailed in the class
         * description) are not satisfied.
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            /*
             * Note that basic validation of the fields of this class was
             * already performed in the readObject() method of this class' super
             * class.
             */
            s.defaultReadObject();
            // Verify that the server implements RemoteMethodControl
            if(!(serviceAdmin instanceof RemoteMethodControl)) {
                throw new InvalidObjectException("ConstrainableCybernodeAdminProxy.readObject "+
                                                 "failure : serviceAdmin does not implement " +
                                                 "constrainable functionality");
            }
        }
    }

    /* -------- Implement CybernodeAdmin methods -------- */
    /*
     * (non-Javadoc)
     * 
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceLimit()
     */
    public Integer getServiceLimit() throws RemoteException {
        return (((CybernodeAdmin)serviceAdmin).getServiceLimit());
    }

    public int getRegistryPort() throws RemoteException {
        return ((CybernodeAdmin)serviceAdmin).getRegistryPort();
    }

    /*
    * (non-Javadoc)
    *
    * @see org.rioproject.cybernode.CybernodeAdmin#setServiceLimit(java.lang.Integer)
    */
    public void setServiceLimit(Integer count) throws RemoteException {
        ((CybernodeAdmin)serviceAdmin).setServiceLimit(count);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.rioproject.cybernode.CybernodeAdmin#getServiceCount()
     */
    public Integer getServiceCount() throws RemoteException {
        return (((CybernodeAdmin)serviceAdmin).getServiceCount());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.rioproject.cybernode.CybernodeAdmin#getPersistentProvisioning()
     */
    public boolean getPersistentProvisioning() throws RemoteException {
        return (((CybernodeAdmin)serviceAdmin).getPersistentProvisioning());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.rioproject.cybernode.CybernodeAdmin#setPersistentProvisioning(boolean)
     */
    public void setPersistentProvisioning(boolean support) throws IOException {
        ((CybernodeAdmin)serviceAdmin).setPersistentProvisioning(support);
    }

    public ResourceCapability getResourceCapability() throws RemoteException {
        return ((CybernodeAdmin)serviceAdmin).getResourceCapability();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.rioproject.system.ComputeResourceAdmin#getComputeResourceUtilization()
     */
    public ComputeResourceUtilization getComputeResourceUtilization() throws RemoteException {
        return (((CybernodeAdmin)serviceAdmin).getComputeResourceUtilization());
    }

    public ComputeResourceUtilization getComputeResourceUtilization(Uuid serviceUuid) throws RemoteException {
        return (((CybernodeAdmin)serviceAdmin).getComputeResourceUtilization(serviceUuid));
    }

    /**
     * A trust verifier for secure smart proxies.
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

        /**
         * Implement TrustVerifier
         */
        public boolean isTrustedObject(Object obj, TrustVerifier.Context ctx) throws RemoteException {
            if (obj == null || ctx == null) {
                throw new IllegalArgumentException();
            } else if (!(obj instanceof ConstrainableCybernodeAdminProxy)) {
                return false;
            }
            RemoteMethodControl otherServerProxy =
                (RemoteMethodControl)((ConstrainableCybernodeAdminProxy)obj).cybernodeAdminProxy;
            MethodConstraints mc = otherServerProxy.getConstraints();
            TrustEquivalence trusted = (TrustEquivalence) serverProxy.setConstraints(mc);
            return(trusted.checkTrustEquivalence(otherServerProxy));
        }
    }
}
