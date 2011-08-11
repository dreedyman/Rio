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

import java.rmi.RemoteException;

/**
 * The ServiceBeanControl interface specifies remote lifecycle control semantics
 * for a ServiceBean
 *
 * @author Dennis Reedy
 */
public interface ServiceBeanControl {
    /**
     * The start method provides informs a ServiceBean to make itself ready to
     * accept inbound communications, returning an Object which can be used to
     * communicate with the ServiceBean. If the ServiceBean has started itself,
     * subsequent invocations of this method will not re-start the ServiceBean,
     * but return the Object created during the initial start
     * 
     * @return An Object that can be used to communicate to the
     * ServiceBean
     * @throws ServiceBeanControlException If any errors occur
     * @throws RemoteException If any communications errors occur
     */
    Object start() throws ServiceBeanControlException, RemoteException;

    /**
     * The stop method informs the ServiceBean to unexport itself from any
     * underlying distributed Object communication mechanisms making it
     * incapable of accepting inbound communications. This call should be used
     * with great care as the ServiceBean will not be able to respond to <b>any
     * </b> remote invocations.
     * 
     * @param force If true, unexports the ServiceBean even if there are
     * pending or in-progress calls; if false, only unexports the ServiceBean if
     * there are no pending or in-progress calls
     * @throws ServiceBeanControlException If any errors occur
     * @throws RemoteException If any communications errors occur
     */
    void stop(boolean force) throws ServiceBeanControlException, RemoteException;

    /**
     * The advertise method informs a ServiceBean to advertise itself on the
     * network providing access to all clients. The ServiceBean must be ready to
     * accept incoming communications (has been started). If the ServiceBean has
     * advertised itself, subsequent invocations of this method will not
     * re-advertise the ServiceBean
     * 
     * @throws ServiceBeanControlException If any errors occur
     * @throws RemoteException If any communications errors occur
     */
    void advertise() throws ServiceBeanControlException, RemoteException;

    /**
     * The unadvertise method informs the ServiceBean to cancel all
     * advertisements (registrations, etc...) it has made on the network. The
     * ServiceBean must still be available to accept incoming communications. If
     * the ServiceBean has not advertised itself, this method has no defined
     * behavior
     * 
     * @throws ServiceBeanControlException If any errors occur
     * @throws RemoteException If any communications errors occur
     */
    void unadvertise() throws ServiceBeanControlException, RemoteException;
}
