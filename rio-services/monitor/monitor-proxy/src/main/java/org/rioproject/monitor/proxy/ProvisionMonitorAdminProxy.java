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
package org.rioproject.monitor.proxy;

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.deploy.DeploymentResult;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.monitor.ProvisionMonitor.PeerInfo;
import org.rioproject.monitor.ProvisionMonitorAdmin;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.proxy.admin.ServiceAdminProxy;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.ResourceCapability;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * A <code>ProvisionMonitorAdminProxy</code> is a proxy for the 
 * ProvisionMonitorAdmin server. This is the object passed to clients of the 
 * ProvisionMonitorAdmin.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorAdminProxy extends ServiceAdminProxy implements ProvisionMonitorAdmin, Serializable {
    private static final long serialVersionUID = 2L;
    final ProvisionMonitorAdmin monitorAdminProxy;

    /**
     * Creates a ProvisionMonitorAdmin proxy, returning an instance that implements 
     * RemoteMethodControl if the server does too.
     *
     * @param serviceAdmin The ProvisionMonitorAdmin server
     * @param id The Uuid of the ProvisionMonitorAdmin
     */
    public static ProvisionMonitorAdminProxy getInstance(final ProvisionMonitorAdmin serviceAdmin, final Uuid id) {
        
        if(serviceAdmin instanceof RemoteMethodControl) {
            return new ConstrainableProvisionMonitorAdminProxy(serviceAdmin, id, null);
        } else {
            return(new ProvisionMonitorAdminProxy(serviceAdmin, id));
        }
    }

    /*
     * Private constructor
     */
    private ProvisionMonitorAdminProxy(final ProvisionMonitorAdmin serviceAdmin, final Uuid uuid) {
        super(serviceAdmin, uuid);
        this.monitorAdminProxy = serviceAdmin;
    }

    /** 
     * A subclass of ProvisionMonitorAdminProxy that implements RemoteMethodControl. 
     */
    final static class ConstrainableProvisionMonitorAdminProxy
        extends ProvisionMonitorAdminProxy implements RemoteMethodControl {
        private static final long serialVersionUID = 2L;        
        
        /* Creates an instance of this class. */
        private ConstrainableProvisionMonitorAdminProxy(final ProvisionMonitorAdmin serviceAdmin,
                                                        final Uuid id,
                                                        final MethodConstraints constraints) {
            super(constrainServer(serviceAdmin, constraints), id);
        }

        /*
          * Returns a copy of the server proxy with the specified client
          * constraints and methods mapping.
          */
        private static ProvisionMonitorAdmin constrainServer(final ProvisionMonitorAdmin serviceAdmin,
                                                             final MethodConstraints constraints) {
            Method[] methods = ProvisionMonitorAdmin.class.getMethods();
            Method[] methodMapping = new java.lang.reflect.Method[methods.length*2];
            for(int i=0; i<methodMapping.length; i++)
                methodMapping[i] = methods[i/2];
            MethodConstraints methodConstraints = ConstrainableProxyUtil.translateConstraints(constraints, methodMapping);
            return (ProvisionMonitorAdmin)((RemoteMethodControl)serviceAdmin).setConstraints(methodConstraints);
        }

        /** @see net.jini.core.constraint.RemoteMethodControl#setConstraints  */
        public RemoteMethodControl setConstraints(final MethodConstraints constraints) {
            return(new ConstrainableProvisionMonitorAdminProxy((ProvisionMonitorAdmin)serviceAdmin, uuid, constraints));
        }
        
        /** @see RemoteMethodControl#getConstraints */
        public MethodConstraints getConstraints() {
            return ((RemoteMethodControl)monitorAdminProxy).getConstraints();
        }

        /* Note that the superclass's hashCode method is OK as is. */
        /* Note that the superclass's equals method is OK as is. */

        /*
         * Returns a proxy trust iterator that is used in
         * <code>ProxyTrustVerifier</code> to retrieve this object's
         * trust verifier.
         */
        @SuppressWarnings("unused")
        private ProxyTrustIterator getProxyTrustIterator() {
            return(new SingletonProxyTrustIterator(monitorAdminProxy));
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(this);
        }

        /*
         * Verify that the server implements RemoteMethodControl 
         */
        private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
            /* Note that basic validation of the fields of this class was
             * already performed in the readObject() method of this class'
             * super class.
             */
            s.defaultReadObject();
            // Verify that the server implements RemoteMethodControl
            if(!(serviceAdmin instanceof RemoteMethodControl)) {
                throw new InvalidObjectException("ConstrainableProvisionMonitorAdminProxy.readObject "+
                                                 "failure : serviceAdmin does not implement constrainable functionality");
            }
        }
    }


    /* -------- Implement ProvisionMonitorAdmin methods -------- */
            
    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final URL opStringURL) throws OperationalStringException, RemoteException {
        return deploy(opStringURL, null);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final URL opStringURL, final ServiceProvisionListener listener)
    throws OperationalStringException, RemoteException {
        return monitorAdminProxy.deploy(opStringURL, listener);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final String location) throws OperationalStringException,RemoteException {
        return deploy(location, null);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final String location, final ServiceProvisionListener listener)
        throws OperationalStringException, RemoteException {
        return monitorAdminProxy.deploy(location, listener);
    }
    
    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final OperationalString opString) throws OperationalStringException, RemoteException {
        return deploy(opString, null);
    }
    
    /**
     * @see org.rioproject.deploy.DeployAdmin#deploy
     */
    public DeploymentResult deploy(final OperationalString opString, final ServiceProvisionListener listener)
    throws OperationalStringException, RemoteException {
        return monitorAdminProxy.deploy(opString, listener);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#undeploy
     */
    public boolean undeploy(final String opStringName) throws OperationalStringException, RemoteException {
        return monitorAdminProxy.undeploy(opStringName);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#hasDeployed
     */
    public boolean hasDeployed(final String opStringName) throws RemoteException {
        return monitorAdminProxy.hasDeployed(opStringName);
    }

    /**
     * @see org.rioproject.deploy.DeployAdmin#getOperationalStringManagers
     */
    public OperationalStringManager[] getOperationalStringManagers() throws RemoteException {
        return monitorAdminProxy.getOperationalStringManagers();
    }
    
    /* (non-Javadoc)
     * @see org.rioproject.deploy.DeployAdmin#getOperationalStringManager
     */
    public OperationalStringManager getOperationalStringManager(final String name) throws OperationalStringException,
                                                                                          RemoteException {
        return monitorAdminProxy.getOperationalStringManager(name);
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.ProvisionMonitorAdmin#getBackupInfo
     */
    public PeerInfo[] getBackupInfo() throws RemoteException {        
        return monitorAdminProxy.getBackupInfo();
    }

    @Override
    public ResourceCapability getResourceCapability() throws RemoteException {
        return monitorAdminProxy.getResourceCapability();
    }

    @Override
    public ComputeResourceUtilization getComputeResourceUtilization() throws RemoteException {
        return monitorAdminProxy.getComputeResourceUtilization();
    }

    /**
     * A trust verifier for secure smart proxies.
     */
    public final static class Verifier implements TrustVerifier, Serializable {
        private static final long serialVersionUID = 1L;
        private final RemoteMethodControl serverProxy;

        /**
         * Create the verifier, throwing UnsupportedOperationException if the
         * server proxy does not implement both RemoteMethodControl and
         * TrustEquivalence.
         */
        public Verifier(final Object serverProxy) {
            if (serverProxy instanceof RemoteMethodControl && serverProxy instanceof TrustEquivalence) {
                this.serverProxy = (RemoteMethodControl) serverProxy;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Implement TrustVerifier
         */
        public boolean isTrustedObject(final Object obj, final TrustVerifier.Context ctx) throws RemoteException {
            if (obj == null || ctx == null) {
                throw new IllegalArgumentException();
            } else if (!(obj instanceof ConstrainableProvisionMonitorAdminProxy)) {
                return false;
            }
            RemoteMethodControl otherServerProxy =
                (RemoteMethodControl)((ConstrainableProvisionMonitorAdminProxy)obj).monitorAdminProxy;
            MethodConstraints mc = otherServerProxy.getConstraints();
            TrustEquivalence trusted = (TrustEquivalence) serverProxy.setConstraints(mc);
            return(trusted.checkTrustEquivalence(otherServerProxy));
        }
    }
}
