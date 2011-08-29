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

import org.rioproject.jsb.ServiceElementUtil;
import org.rioproject.monitor.InstantiatorResource;
import org.rioproject.monitor.ProvisionException;
import org.rioproject.monitor.ProvisionRequest;
import org.rioproject.monitor.ServiceProvisionContext;
import org.rioproject.monitor.tasks.ProvisionTask;
import org.rioproject.monitor.util.LoggingUtil;
import org.rioproject.resources.servicecore.ServiceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages services of type fixed.
 */
public class FixedServiceManager extends PendingServiceElementManager {
    private final List<ServiceResource> inProcessResource = Collections.synchronizedList(new ArrayList<ServiceResource>());
    private final ServiceProvisionContext context;
    private final PendingManager pendingManager;
    private final Logger logger = Logger.getLogger("org.rioproject.monitor.provision");

    /**
     * Create a FixedServiceManager
     *
     * @param context The ServiceProvisionContext
     * @param pendingManager The manager for pending service provisioning requests
     */
    public FixedServiceManager(ServiceProvisionContext context, PendingManager pendingManager) {
        super("Fixed-Service TestManager");
        this.context = context;
        this.pendingManager = pendingManager;
    }

    /**
     * Process the entire Fixed collection over all known ServiceResource
     * instances
     */
    void process() {
        ServiceResource[] resources = context.getSelector().getServiceResources();
        for (ServiceResource resource : resources)
            process(resource);
    }

    /**
     * Used to deploy a specific ProvisionRequest to all ServiceResource
     * instances that support the requirements of the ServiceElement
     *
     * @param request The ProvisionRequest to deploy
     */
    public void deploy(ProvisionRequest request) {
        try {
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER,  "Deploy [" + LoggingUtil.getLoggingName(request) + "]");
            ServiceResource[] resources = context.getSelector().getServiceResources(request.getServiceElement());
            /* Filter out isolated associations and max per machine levels
             * set at the physical level */
            resources = context.getSelector().filterMachineBoundaries(request.getServiceElement(), resources);
            if (resources.length > 0)
                resources = context.getSelector().filterIsolated(request.getServiceElement(), resources);

            if (resources.length > 0) {
                for (ServiceResource resource : resources) {
                    try {
                        inProcessResource.add(resource);
                        doDeploy(resource, request);
                    } finally {
                        inProcessResource.remove(resource);
                    }
                }
                while (context.getProvisioningPool().getActiveCount() > 0) {
                    Thread.sleep(100);
                }
                //if(list.size()>0)
                //    taskJoiner(list);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "FixedServiceManager deployNew", t);
        }
    }

    /* Process the fixed services collection with an input
     * ServiceResource 
     */
    public void process(ServiceResource resource) {
        if (resource == null)
            throw new IllegalArgumentException("ServiceResource is null");
        if (inProcessResource.contains(resource))
            return;
        inProcessResource.add(resource);
        InstantiatorResource ir = (InstantiatorResource) resource.getResource();
        try {
            if (getSize() == 0)
                return;
            if (logger.isLoggable(Level.FINEST)) {
                dumpCollection();
            }
            /* Now traverse the collection for everything else, skipping
             * the service elements that have been processed */
            synchronized (collection) {
                Set<Key> requests = collection.keySet();
                for (Key requestKey : requests) {
                    ProvisionRequest request = collection.get(requestKey);
                    try {
                        if (clearedMaxPerMachineAndIsolated(request, ir.getHostAddress()) &&
                            ir.canProvision(request.getServiceElement()))
                            doDeploy(resource, request);
                    } catch (ProvisionException e) {
                        request.setType(ProvisionRequest.Type.UNINSTANTIABLE);
                        if (logger.isLoggable(Level.FINE))
                            logger.log(Level.FINE,
                                                "Service [" + LoggingUtil.getLoggingName(request) + "] " +
                                                "is un-instantiable, " +
                                                "do not resubmit");
                    }
                }
                while (context.getProvisioningPool().getActiveCount() > 0) {
                    Thread.sleep(100);
                }
                //if(list.size()>0)
                //    taskJoiner(list);
            }

        } catch (Throwable t) {
            logger.log(Level.WARNING, "Processing FixedService Collection", t);
        } finally {
            inProcessResource.remove(resource);
            ir.setDynamicEnabledOn();
        }
    }

    /**
     * Do the deployment for a fixed service
     *
     * @param resource The ServiceResource
     * @param request  The ProvisionRequest
     * @return The number deployed
     * @throws Exception If there are errors
     */
    int doDeploy(ServiceResource resource, ProvisionRequest request) throws Exception {
        return (doDeploy(resource, request, true));
    }

    /**
     * Do the deployment for a fixed service
     *
     * @param resource         The ServiceResource
     * @param req              The ProvisionRequest
     * @param changeInstanceID If true, increment the instanceID
     * @return The number deployed
     * @throws Exception If there are errors
     */
    int doDeploy(ServiceResource resource, ProvisionRequest req, boolean changeInstanceID) throws Exception {
        int numAllowed = getNumAllowed(resource, req);
        if (numAllowed > 0) {
            long currentID = req.getServiceElement().getServiceBeanConfig().getInstanceID();
            StringBuilder b = new StringBuilder();
            b.append("doDeploy ").append(numAllowed).append(" " + "[")
                .append(LoggingUtil.getLoggingName(req)).append("] instances");

            for (int i = 0; i < numAllowed; i++) {
                ProvisionRequest request = ProvisionRequest.copy(req);
                long nextID = (changeInstanceID ?
                               request.getInstanceIDMgr().getNextInstanceID() :
                               currentID);
                request.setServiceElement(ServiceElementUtil.prepareInstanceID(request.getServiceElement(),
                                                                               true,
                                                                               nextID));

                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "[" + LoggingUtil.getLoggingName(request) + "] " +
                                             "instanceID : " +
                                             request.getServiceElement().getServiceBeanConfig().getInstanceID());

                /*b.append("instanceID=").
                append(request.getServiceElement().getServiceBeanConfig().getInstanceID()).
                append(", " + "changeInstanceID=").
                append(changeInstanceID).
                append("\n");*/

                context.getInProcess().add(request.getServiceElement());
                context.setProvisionRequest(request);
                context.setServiceResource(resource);
                context.getProvisioningPool().execute(new ProvisionTask(context, pendingManager));
            }
            if (logger.isLoggable(Level.FINER))
                logger.finer(b.toString());
            //if(!changeInstanceID) {
            //    Throwable t = new Throwable();
            //    t.printStackTrace();
            //}
        }
        return (numAllowed);
    }

    /*
    * Determine how many services can be allocated based on how many
    * are already on the resource minus the number planned
    */
    int getNumAllowed(ServiceResource resource, ProvisionRequest request) {
        /* Filter out isolated associations and max per machine levels set
         * at the physical level */
        InstantiatorResource ir =
            (InstantiatorResource) resource.getResource();
        if (!clearedMaxPerMachineAndIsolated(request, ir.getHostAddress()))
            return 0;

        int planned = request.getServiceElement().getPlanned();
        int actual = ir.getServiceElementCount(request.getServiceElement());
        int numAllowed = planned - actual;
        if (request.getServiceElement().getMaxPerMachine() != -1 &&
            request.getServiceElement().getMaxPerMachine() < numAllowed)
            numAllowed = request.getServiceElement().getMaxPerMachine();
        return (numAllowed);
    }

    /*
    * Filter out isolated associations and max per machine levels set
    * at the physical level
    */
    boolean clearedMaxPerMachineAndIsolated(ProvisionRequest request, String hostAddress) {
        /* Filter out isolated associations and max per machine levels set
         * at the physical level */
        ServiceResource[] sr =
            context.getSelector().filterMachineBoundaries(request.getServiceElement(),
                                                          context.getSelector().getServiceResources(hostAddress, true));
        if (sr.length == 0)
            return false;
        sr = context.getSelector().filterIsolated(request.getServiceElement(), sr);
        return sr.length != 0;
    }

    /*
     * Wait until all ProvisionTask threads are complete
     *
    void taskJoiner(ArrayList list) {
        if(logger.isLoggable(Level.INFO))
            logger.log(Level.INFO,
                                "Wait until all ProvisionTask threads " +
                                "are complete ...");
        for (Object aList : list) {
            try {
                ((PoolableThread) aList).joinResource();
            } catch (Throwable e) {
                logger.log(Level.WARNING,
                                    "PoolableThread join interruption",
                                    e);
            }
        }
        if(logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST,
                                "ProvisionTask threads join complete");
    }
    */
} // End FixedServiceManager
