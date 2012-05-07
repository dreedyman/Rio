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

import com.sun.jini.proxy.ConstrainableProxyUtil;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.id.Uuid;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

/** 
 * A subclass of ServiceAdminProxy that implements RemoteMethodControl.
 *
 * @author Dennis Reedy
 */
public class ConstrainableServiceAdminProxy extends ServiceAdminProxy
                                            implements RemoteMethodControl {
    
    private static final long serialVersionUID = 2L;
    /** Client constraints for this proxy, or null */
    private final MethodConstraints constraints;
    
    /* Creates an instance of this class. */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    ConstrainableServiceAdminProxy(final ServiceAdmin serviceAdmin,
                                   final Uuid id,
                                   final MethodConstraints constraints) {
        super(constrainServer(serviceAdmin, constraints), id);
        this.constraints = constraints;
    }
    
    /*
     * Returns a copy of the server proxy with the specified client
     * constraints and methods mapping.
     */
    private static ServiceAdmin constrainServer(final ServiceAdmin serviceAdmin,
                                                final MethodConstraints constraints) {
                   
        java.lang.reflect.Method[] methods = ServiceAdmin.class.getMethods();
        java.lang.reflect.Method[] methodMapping = new java.lang.reflect.Method[methods.length*2];
        for(int i=0; i<methodMapping.length; i++)
            methodMapping[i] = methods[i/2];
        return((ServiceAdmin)((RemoteMethodControl)serviceAdmin).setConstraints(ConstrainableProxyUtil.translateConstraints(
                                                                                                                                  constraints,
                                                                                                                                  methodMapping)));
    }

    public RemoteMethodControl setConstraints(final MethodConstraints constraints) {
        return(new ConstrainableServiceAdminProxy(serviceAdmin, uuid, constraints));
    }

    public MethodConstraints getConstraints() {
        return(constraints);
    }
    
    /* Note that the superclass's hashCode method is OK as is. */
    
    /*
     * Returns a proxy trust iterator that is used in
     * <code>ProxyTrustVerifier</code> to retrieve this object's
     * trust verifier.
     */
    @SuppressWarnings("unused")
    private ProxyTrustIterator getProxyTrustIterator() {
        return(new SingletonProxyTrustIterator(serviceAdmin));
    }
    
    /* 
     * Performs various functions related to the trust verification
     * process for the current instance of this proxy class, as
     * detailed in the description for this class.
     *
     * @throws InvalidObjectException if any of the
     *         requirements for trust verification (as detailed in the
     *         class description) are not satisfied.
     */
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        /* Note that basic validation of the fields of this class was
         * already performed in the readObject() method of this class'
         * super class.
         */
        s.defaultReadObject();
        // Verify that the server implements RemoteMethodControl
        if(!(serviceAdmin instanceof RemoteMethodControl)) {
            throw new InvalidObjectException("ConstrainableServiceAdminProxy.readObject "+
                                             "failure - server does not implement constrainable functionality ");
        }
    }    
}
