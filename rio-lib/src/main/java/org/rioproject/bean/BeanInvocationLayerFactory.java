/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.bean;

import net.jini.core.constraint.MethodConstraints;
import net.jini.jeri.BasicILFactory;

import java.rmi.Remote;
import java.rmi.server.ExportException;

/**
 * The BeanInvocationLayerFactory is a simple extension of the
 * {@link net.jini.jeri.BasicILFactory}, in that it allows interfaces that do
 * not have remote semantics (extends Remote) to be included.
 *
 * @author Dennis Reedy
 */
public class BeanInvocationLayerFactory extends BasicILFactory {

    /*
     * @see net.jini.jeri.BasicILFactory#BasicILFactory()
     */
    public BeanInvocationLayerFactory() {
        super();
    }

    /*
     * @see net.jini.jeri.BasicILFactory#BasicILFactory(net.jini.core.constraint.MethodConstraints, Class)
     */

    public BeanInvocationLayerFactory(MethodConstraints serverConstraints,
                                      Class permissionClass) {
        super(serverConstraints, permissionClass);
    }

    /*
     * @see net.jini.jeri.BasicILFactory#BasicILFactory(net.jini.core.constraint.MethodConstraints, Class, ClassLoader)
     */
    public BeanInvocationLayerFactory(MethodConstraints serverConstraints,
                                      Class permissionClass,
                                      ClassLoader loader) {
        super(serverConstraints, permissionClass, loader);
    }

    /**
     * Override parent's behavior to return all interfaces
     *
     * @param impl The remote object to use, must not be null
     * 
     * @return Array of classes that can be used for remote invocations. In
     * this case, all interfaces {@link Class#getInterfaces()} are returned
     *
     * @throws ExportException
     */
    @Override
    protected Class[] getRemoteInterfaces(Remote impl) throws ExportException {
        if(impl == null) {
            throw new IllegalArgumentException("impl is null");
        }
        return(impl.getClass().getInterfaces());
    }
}
