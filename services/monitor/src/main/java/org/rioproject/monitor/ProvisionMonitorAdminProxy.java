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
package org.rioproject.monitor;

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.id.Uuid;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;
import org.rioproject.core.OperationalString;
import org.rioproject.core.OperationalStringException;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.core.ServiceProvisionListener;
import org.rioproject.monitor.ProvisionMonitor.PeerInfo;
import org.rioproject.resources.servicecore.ServiceAdminProxy;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * A <code>ProvisionMonitorAdminProxy</code> is a proxy for the 
 * ProvisionMonitorAdmin server. This is the object passed to clients of the 
 * ProvisionMonitorAdmin.
 *
 * @author Dennis Reedy
 */
public class ProvisionMonitorAdminProxy extends ServiceAdminProxy 
                               implements ProvisionMonitorAdmin, Serializable {
    private static final long serialVersionUID = 2L;
    final ProvisionMonitorAdmin monitorAdminProxy;

    /**
     * Creates a ProvisionMonitorAdmin proxy, returning an instance that implements 
     * RemoteMethodControl if the server does too.
     *
     * @param serviceAdmin The ProvisionMonitorAdmin server
     * @param id The Uuid of the ProvisionMonitorAdmin
     */
    static ProvisionMonitorAdminProxy getInstance(ProvisionMonitorAdmin serviceAdmin, 
                                                  Uuid id) {
        
        if(serviceAdmin instanceof RemoteMethodControl) {
            return new ConstrainableProvisionMonitorAdminProxy(serviceAdmin, 
                                                               id, 
                                                               null);
        } else {
            return(new ProvisionMonitorAdminProxy(serviceAdmin, id));
        }
    }

    /*
     * Private constructor
     */
    private ProvisionMonitorAdminProxy(ProvisionMonitorAdmin serviceAdmin, 
                                       Uuid uuid) {
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
        private ConstrainableProvisionMonitorAdminProxy(
                                               ProvisionMonitorAdmin serviceAdmin, 
                                               Uuid id,
                                               MethodConstraints constraints) {
            super(constrainServer(serviceAdmin, constraints), id);
        }

        /*
          * Returns a copy of the server proxy with the specified client
          * constraints and methods mapping.
          */
        private static ProvisionMonitorAdmin constrainServer(
                                             ProvisionMonitorAdmin serviceAdmin,
                                             MethodConstraints constraints) {
            java.lang.reflect.Method[] methods = 
                ProvisionMonitorAdmin.class.getMethods();
            java.lang.reflect.Method[] methodMapping = 
                new java.lang.reflect.Method[methods.length*2];            
            for(int i=0; i<methodMapping.length; i++)
                methodMapping[i] = methods[i/2];                 
            return((ProvisionMonitorAdmin)((RemoteMethodControl)serviceAdmin).
                setConstraints(
                     ConstrainableProxyUtil.translateConstraints(constraints, 
                                                                 methodMapping)));
        }

        /** @see net.jini.core.constraint.RemoteMethodControl#setConstraints  */
        public RemoteMethodControl setConstraints(MethodConstraints constraints) {
            return(new ConstrainableProvisionMonitorAdminProxy(
                                 (ProvisionMonitorAdmin)serviceAdmin, 
                                 uuid, 
                                 constraints));
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
        private ProxyTrustIterator getProxyTrustIterator() {
            return(new SingletonProxyTrustIterator(monitorAdminProxy));
        }

        /*
         * Verify that the server implements RemoteMethodControl 
         */
        private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
            /* Note that basic validation of the fields of this class was
             * already performed in the readObject() method of this class'
             * super class.
             */
            s.defaultReadObject();
            // Verify that the server implements RemoteMethodControl
            if(!(serviceAdmin instanceof RemoteMethodControl)) {
                throw new InvalidObjectException(
                       "ConstrainableProvisionMonitorAdminProxy.readObject "+
                       "failure : serviceAdmin does not implement constrainable "+
                       "functionality");
            }
        }
    }


    /* -------- Implement ProvisionMonitorAdmin methods -------- */
            
    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(URL opStringURL)
    throws OperationalStringException, RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(opStringURL));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(URL opStringURL,
                                         ServiceProvisionListener listener)
    throws OperationalStringException,RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(opStringURL, listener));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(String location)
    throws OperationalStringException,RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(location, null));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(String location,
                                         ServiceProvisionListener listener)
    throws OperationalStringException,RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(location, listener));
    }
    
    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(OperationalString opString)
    throws OperationalStringException, RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(opString));
    }
    
    /**
     * @see org.rioproject.monitor.DeployAdmin#deploy
     */
    public Map<String, Throwable> deploy(OperationalString opString,
                                         ServiceProvisionListener listener) 
    throws OperationalStringException, RemoteException {
        return(((DeployAdmin)serviceAdmin).deploy(opString, listener));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#undeploy
     */
    public boolean undeploy(String opStringName) throws 
    OperationalStringException, RemoteException {
        return(((DeployAdmin)serviceAdmin).undeploy(opStringName));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#hasDeployed
     */
    public boolean hasDeployed(String opStringName) throws RemoteException {
        return(((DeployAdmin)serviceAdmin).hasDeployed(opStringName));
    }

    /**
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManagers
     */
    public OperationalStringManager[] getOperationalStringManagers() throws 
                                                                     RemoteException {
        return(((DeployAdmin)serviceAdmin).getOperationalStringManagers());
    }
    
    /* (non-Javadoc)
     * @see org.rioproject.monitor.DeployAdmin#getOperationalStringManager
     */
    public OperationalStringManager getOperationalStringManager(String name) 
    throws OperationalStringException, RemoteException {
        return(((DeployAdmin)serviceAdmin).getOperationalStringManager(name));
    }

    /* (non-Javadoc)
     * @see org.rioproject.monitor.ProvisionMonitorAdmin#getBackupInfo
     */
    public PeerInfo[] getBackupInfo() throws RemoteException {        
        return(((ProvisionMonitorAdmin)serviceAdmin).getBackupInfo());
    }

    /**
     * A trust verifier for secure smart proxies.
     */
    final static class Verifier implements TrustVerifier,
                                           Serializable {
        private static final long serialVersionUID = 1L;
        private final RemoteMethodControl serverProxy;

        /**
         * Create the verifier, throwing UnsupportedOperationException if the
         * server proxy does not implement both RemoteMethodControl and
         * TrustEquivalence.
         */
        Verifier(Object serverProxy) {
            if (serverProxy instanceof RemoteMethodControl &&
                serverProxy instanceof TrustEquivalence) {
                this.serverProxy = (RemoteMethodControl) serverProxy;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Implement TrustVerifier
         */
        public boolean isTrustedObject(Object obj, TrustVerifier.Context ctx)
        throws RemoteException {
            if (obj == null || ctx == null) {
                throw new NullPointerException();
            } else if (!(obj instanceof ConstrainableProvisionMonitorAdminProxy)) {
                return false;
            }
            RemoteMethodControl otherServerProxy =
                (RemoteMethodControl)((ConstrainableProvisionMonitorAdminProxy)obj).
                    monitorAdminProxy;
            MethodConstraints mc = otherServerProxy.getConstraints();
            TrustEquivalence trusted =
                (TrustEquivalence) serverProxy.setConstraints(mc);
            return(trusted.checkTrustEquivalence(otherServerProxy));
        }
    }
}
