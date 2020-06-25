/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.cybernode.proxy;

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.core.event.UnknownEventException;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.deploy.*;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.proxy.service.AbstractProxy;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.RemoteException;

/**
 * A <code>CybernodeProxy</code> is a proxy for the Cybernode server. This is
 * the object passed to clients of the Cybernode.
 *
 * @author Dennis Reedy
 */
public class CybernodeProxy extends AbstractProxy implements Cybernode, Serializable {
    private static final long serialVersionUID = 2L;
    private String name;
    final Cybernode cybernodeProxy;

    /**
     * Creates a Cybernode proxy, returning an instance that implements
     * RemoteMethodControl if the server does too.
     * 
     * @param cybernode The Cybernode server
     * @param id The Uuid of the Cybernode
     *
     * @return A CybernodeProxy
     */
    public static CybernodeProxy getInstance(final Cybernode cybernode, final Uuid id) {
        if(cybernode instanceof RemoteMethodControl) {
            return new ConstrainableCybernodeProxy(cybernode, id, null);
        } else {
            return (new CybernodeProxy(cybernode, id));
        }
    }

    /*
     * Private constructor
     */
    private CybernodeProxy(final Cybernode cybernode, final Uuid id) {
        super(cybernode, id);
        cybernodeProxy = cybernode;
    }

    /* -------- Implement Cybernode methods -------- */

    /** @see Cybernode#enlist()  */
    public void enlist() throws RemoteException {
        cybernodeProxy.enlist();
    }

    /** @see Cybernode#release(boolean) */
    public void release(final boolean terminateServices) throws RemoteException {
        cybernodeProxy.release(terminateServices);
    }

    public boolean isEnlisted() throws RemoteException {
        return cybernodeProxy.isEnlisted();
    }

    /* -------- Implement ServiceBeanInstantiator methods -------- */
    /** @see org.rioproject.deploy.ServiceBeanInstantiator#instantiate */
    public DeployedService instantiate(final ServiceProvisionEvent event)
    throws ServiceBeanInstantiationException, UnknownEventException, RemoteException {
        return (cybernodeProxy.instantiate(event));
    }

    /** @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceStatements */
    public ServiceStatement[] getServiceStatements() throws RemoteException {
        return (cybernodeProxy.getServiceStatements());
    }

    /** @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceRecords */
    public ServiceRecord[] getServiceRecords(final int filter) throws RemoteException {
        return (cybernodeProxy.getServiceRecords(filter));
    }

    /** @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceStatement */
    public ServiceStatement getServiceStatement(final ServiceElement elem) throws RemoteException {
        if(elem == null)
            throw new IllegalArgumentException("ServiceElement is null");
        return (cybernodeProxy.getServiceStatement(elem));
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getServiceBeanInstances
     */
    public ServiceBeanInstance[] getServiceBeanInstances(final ServiceElement element)
        throws RemoteException {
        return (cybernodeProxy.getServiceBeanInstances(element));
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#update
     */
    public void update(final ServiceElement[] sElements,
                       final OperationalStringManager opStringMgr) throws RemoteException {
        cybernodeProxy.update(sElements, opStringMgr);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getName
     */
    public String getName() throws RemoteException {
        if(name == null) {
            name = cybernodeProxy.getName();
        }
        return(name);
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getInstantiatorUuid()
     */
    public Uuid getInstantiatorUuid() throws RemoteException {
        return(getReferentUuid());
    }

    /**
     * @see org.rioproject.deploy.ServiceBeanInstantiator#getInetAddress()
     */
    public InetAddress getInetAddress() throws RemoteException {
        return cybernodeProxy.getInetAddress();
    }

    /* Constrainable CybernodeProxy */
    /**
     * A subclass of CybernodeProxy that implements RemoteMethodControl.
     */
    final static class ConstrainableCybernodeProxy extends CybernodeProxy
        implements
            RemoteMethodControl {
        private static final long serialVersionUID = 1L;

        /* Creates an instance of this class. */
        private ConstrainableCybernodeProxy(final Cybernode cybernode,
                                            final Uuid id,
                                            final MethodConstraints methodConstraints) {
            super(constrainServer(cybernode, methodConstraints), id);
        }

        /*
         * Returns a copy of the server proxy with the specified client
         * constraints and methods mapping.
         */
        private static Cybernode constrainServer(final Cybernode cybernode,
                                                 final MethodConstraints methodConstraints) {
            java.lang.reflect.Method[] methods = Cybernode.class.getMethods();
            java.lang.reflect.Method[] methodMapping = 
                new java.lang.reflect.Method[methods.length * 2];
            for(int i = 0; i < methodMapping.length; i++)
                methodMapping[i] = methods[i / 2];
            return ((Cybernode)((RemoteMethodControl)cybernode).
                                     setConstraints(
                                       ConstrainableProxyUtil.translateConstraints(
                                                                 methodConstraints,
                                                                 methodMapping)));            
        }

        /** @see net.jini.core.constraint.RemoteMethodControl#setConstraints  */
        public RemoteMethodControl setConstraints(final MethodConstraints constraints) {
            return (new ConstrainableCybernodeProxy((Cybernode)server,
                                                    uuid,
                                                    constraints));
        }

        /** @see RemoteMethodControl#getConstraints */
        public MethodConstraints getConstraints() {
            return ((RemoteMethodControl)cybernodeProxy).getConstraints();
        }

        /* Note that the superclass's hashCode method is OK as is. */
        /* Note that the superclass's equals method is OK as is. */
        /*
         * Returns a proxy trust iterator that is used in
         * <code>ProxyTrustVerifier</code> to retrieve this object's trust
         * verifier.
         */
        private ProxyTrustIterator getProxyTrustIterator() {
            return (new SingletonProxyTrustIterator(cybernodeProxy));
        }

        /*
         * Verify that the server implements RemoteMethodControl 
         */
        private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
            /*
             * Note that basic validation of the fields of this class was
             * already performed in the readObject() method of this class' super
             * class.
             */
            s.defaultReadObject();
            // Verify that the server implements RemoteMethodControl
            if(!(server instanceof RemoteMethodControl)) {
                throw new InvalidObjectException(
                       "ConstrainableCybernodeProxy.readObject failure - server "+
                       "does not implement constrainable functionality");
            }
        }
    }

    /**
     * A trust verifier for secure smart proxies
     */
    public final static class Verifier implements TrustVerifier, Serializable {
        private static final long serialVersionUID = 1L;
        private final RemoteMethodControl serverProxy;

        /**
         * Create the verifier, throwing UnsupportedOperationException if the
         * server proxy does not implement both RemoteMethodControl and
         * TrustEquivalence.
         *
         * @param serverProxy The proxy to verify
         */
        public Verifier(Object serverProxy) {
            if (serverProxy instanceof RemoteMethodControl &&
                serverProxy instanceof TrustEquivalence) {
                this.serverProxy = (RemoteMethodControl) serverProxy;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /* Implement TrustVerifier */
        public boolean isTrustedObject(Object obj, TrustVerifier.Context ctx) throws RemoteException {
            if (obj == null || ctx == null) {
                throw new IllegalArgumentException();
            } else if (!(obj instanceof ConstrainableCybernodeProxy)) {
                return false;
            }
            RemoteMethodControl otherServerProxy =
                (RemoteMethodControl) ((ConstrainableCybernodeProxy)obj).cybernodeProxy;
            MethodConstraints mc = otherServerProxy.getConstraints();
            TrustEquivalence trusted = (TrustEquivalence) serverProxy.setConstraints(mc);
            return(trusted.checkTrustEquivalence(otherServerProxy));
        }
    }
}
