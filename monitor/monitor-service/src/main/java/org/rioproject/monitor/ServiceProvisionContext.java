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

import org.rioproject.opstring.ServiceElement;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.selectors.ServiceResourceSelector;
import org.rioproject.resources.servicecore.ServiceResource;
import org.rioproject.watch.GaugeWatch;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container object having as it's properties classes that provide context to do the work required to do
 * service provisioning.
 */
public class ServiceProvisionContext {
    private final ServiceResourceSelector selector;
    private final ThreadPoolExecutor provisioningPool;
    private final List<ServiceElement> inProcess;
    private ProvisionRequest request;
    private ServiceResource svcResource;
    private Object eventSource;
    private final GaugeWatch watch;
    private final ServiceProvisionDispatcher dispatcher;
    private final ThreadPoolExecutor provisionFailurePool;
    private final EventHandler failureHandler;
    private final AtomicInteger serviceProvisionEventSequenceNumber;

    public ServiceProvisionContext(ServiceResourceSelector selector,
                                   ThreadPoolExecutor provisioningPool,
                                   List<ServiceElement> inProcess,
                                   Object eventSource,
                                   GaugeWatch watch,
                                   ServiceProvisionDispatcher dispatcher,
                                   ThreadPoolExecutor provisionFailurePool,
                                   EventHandler failureHandler,
                                   AtomicInteger serviceProvisionEventSequenceNumber) {
        this.selector = selector;
        this.provisioningPool = provisioningPool;
        this.inProcess = inProcess;
        this.eventSource = eventSource;
        this.watch = watch;
        this.dispatcher = dispatcher;
        this.provisionFailurePool = provisionFailurePool;
        this.failureHandler = failureHandler;
        this.serviceProvisionEventSequenceNumber = serviceProvisionEventSequenceNumber;
    }

    public void setProvisionRequest(ProvisionRequest request) {
        this.request = request;
    }

    public void setServiceResource(ServiceResource svcResource) {
        this.svcResource = svcResource;
    }

    public ServiceResourceSelector getSelector() {
        return selector;
    }

    public ThreadPoolExecutor getProvisioningPool() {
        return provisioningPool;
    }

    public List<ServiceElement> getInProcess() {
        return inProcess;
    }

    public ProvisionRequest getProvisionRequest() {
        return request;
    }

    public ServiceResource getServiceResource() {
        return svcResource;
    }

    public Object getEventSource() {
        return eventSource;
    }

    public GaugeWatch getWatch() {
        return watch;
    }

    public ServiceProvisionDispatcher getDispatcher() {
        return dispatcher;
    }

    public ThreadPoolExecutor getProvisionFailurePool() {
        return provisionFailurePool;
    }

    public EventHandler getFailureHandler() {
        return failureHandler;
    }

    public AtomicInteger getServiceProvisionEventSequenceNumber() {
        return serviceProvisionEventSequenceNumber;
    }
}
