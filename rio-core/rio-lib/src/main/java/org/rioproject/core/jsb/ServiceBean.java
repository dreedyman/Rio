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
package org.rioproject.core.jsb;

import java.io.IOException;

/**
 * The {@code ServiceBean} interface specifies the semantics that a dynamic service
 * must adhere to in order to be instantiated, initialized, started, advertised,
 * unadvertised, stopped and destroyed.
 *
 * @author Dennis Reedy
 */
public interface ServiceBean {
    /**
     * The start method provides the capability for a dynamic service to initialize
     * itself and make it ready to accept inbound communications, returning an
     * Object which can be used to communicate with the service. It is the
     * responsibility of the service to initiate appropriate startup logic.
     * If the service has started itself, subsequent invocations of this
     * method will not re-start the service, but return the Object created
     * during the initial start
     * 
     * @param context The ServiceBeanContext containing service
     * initialization attributes
     * @return An Object that can be used to communicate to the service
     * @throws Exception If any errors or unexpected conditions occur
     */
    Object start(ServiceBeanContext context) throws Exception;

    /**
     * The initialize method is invoked by the start method to initialize the
     * service. this method is called only once during the lifecycle of a
     * ServiceBean
     * 
     * @param context The ServiceBeanContext containing service
     * initialization attributes
     * @throws Exception If any errors or unexpected conditions occur
     */
    void initialize(ServiceBeanContext context) throws Exception;

    /**
     * The advertise method provides the capability for a service to
     * advertise itself on the network providing access to all clients. The
     * service must be ready to accept incoming communications (has been
     * started). If the service has advertised itself, subsequent
     * invocations of this method will not re-advertise the service
     * 
     * @throws IOException If errors occur access underlying communication
     * mechanisms
     */
    void advertise() throws IOException;

    /**
     * The unadvertise method informs the service to cancel all
     * advertisements (registrations, etc...) it has made on the network. The
     * service must still be available to accept incoming communications. If
     * the service has not advertised itself, this method has no defined
     * behavior
     * 
     * @throws IOException If errors occur access underlying communication
     * mechanisms
     */
    void unadvertise() throws IOException;

    /**
     * The stop method informs the service to unexport itself from any
     * underlying distributed Object communication mechanisms making it
     * incapable of accepting inbound communications
     * 
     * @param force If true, unexports the service even if there are
     * pending or in-progress calls; if false, only unexports the service if
     * there are no pending or in-progress calls
     */
    void stop(boolean force);

    /**
     * The destroy method is used to destroy an instance of a service. Once
     * this method is invoked the service should provide appropriate
     * termination logic including unadvertise() and stop() method invocations.
     * Note: The implementer of this method should not invoke System.exit()
     * during the processing of this method
     * 
     * @param force If true, unexports the service even if there are
     * pending or in-progress calls; if false, only unexports the service if
     * there are no pending or in-progress calls
     */
    void destroy(boolean force);
}
