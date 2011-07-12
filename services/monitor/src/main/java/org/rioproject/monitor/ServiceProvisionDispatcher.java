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

import org.rioproject.resources.servicecore.ServiceResource;

/**
 * Provides support to dispatch {@link ProvisionRequest}s
 */
public interface ServiceProvisionDispatcher {

    /**
     * Dispatch a provision request. This method is used to provision ServiceElement
     * object that has a provision type of DYNAMIC
     *
     * @param request The ProvisionRequest
     */
    void dispatch(ProvisionRequest request);

    /**
     * Provision a pending ServiceElement with a provision type of DYNAMIC with an
     * index into the Collection of pending ServiceElement instances managed by the
     * PendingManager. If a ServiceResource cannot be found, put the ServiceElement
     * under the management of the PendingManager and fire a ProvisionFailureEvent
     *
     * @param request The ProvisionRequest
     * @param resource A ServiceResource that contains an InstantiatorResource
     * which meets the operational requirements of the ServiceElement
     * @param index Index of the ServiceElement in the pending collection
     */
    void dispatch(ProvisionRequest request, ServiceResource resource, long index);
}
