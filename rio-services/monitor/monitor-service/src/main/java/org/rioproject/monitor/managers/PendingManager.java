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
package org.rioproject.monitor.managers;

import org.rioproject.monitor.ProvisionRequest;
import org.rioproject.monitor.ServiceProvisionContext;
import org.rioproject.monitor.selectors.Selector;
import org.rioproject.monitor.util.FailureReasonFormatter;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.servicecore.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class is used to manage the provisioning of pending ServiceElement
 * objects that have a ServiceProvisionManagement type of DYNAMIC.
 */
public class PendingManager extends PendingServiceElementManager {
    private final ServiceProvisionContext context;
    private final Logger logger = LoggerFactory.getLogger(PendingManager.class.getName());

    /**
     * Create a PendingManager
     *
     * @param context The ServiceProvisionContext
     */
    public PendingManager(ServiceProvisionContext context) {
        super("Dynamic-Service TestManager");
        this.context = context;
    }

    /**
     * Override parent's getCount to include the number of in-process elements
     * in addition to the number of pending ServiceElement instances
     */
    public int getCount(ServiceElement serviceElement) {
        int count = super.getCount(serviceElement);
        ServiceElement[] services = context.getInProcess().toArray(new ServiceElement[context.getInProcess().size()]);
        for (ServiceElement service : services) {
            if (service.equals(serviceElement))
                count++;
        }
        return (count);
    }

    /**
     * Process the pending collection
     */
    public void process() {
        int pendingSize = getSize();
        dumpCollection();
        if (pendingSize == 0)
            return;
        try {
            Key[] keys;
            synchronized (collection) {
                Set<Key> keySet = collection.keySet();
                keys = keySet.toArray(new Key[keySet.size()]);
            }
            for (Key key : keys) {
                ProvisionRequest request;
                ServiceResource resource = null;
                synchronized (collection) {
                    request = collection.get(key);
                    if (request != null && request.getServiceElement() != null) {
                        request.getFailureReasons().clear();
                        resource = Selector.acquireServiceResource(request, context.getSelector());
                        if (resource != null) {
                            synchronized (collection) {
                                collection.remove(key);
                            }
                        } else {
                            logger.warn(FailureReasonFormatter.format(request, context.getSelector()));
                        }
                    }
                }

                if (resource == null) {
                    continue;
                }
                try {
                    context.getDispatcher().dispatch(request, resource, key.index);
                } catch (Exception e) {
                    logger.trace("Dispatching Pending Collection Element", e);
                }
                /* Slow the dispatching down, this will avoid pummeling
                 * a single InstantiatorResource */
                Thread.sleep(500);
            }
        } catch (Throwable t) {
            logger.warn("Processing Pending Collection", t);
        }
    }
} // End PendingManager

