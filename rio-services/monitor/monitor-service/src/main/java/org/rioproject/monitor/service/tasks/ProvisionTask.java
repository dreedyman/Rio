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
package org.rioproject.monitor.service.tasks;

import net.jini.core.event.UnknownEventException;
import org.rioproject.deploy.DeployedService;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceBeanInstantiationException;
import org.rioproject.deploy.ServiceProvisionEvent;
import org.rioproject.monitor.*;
import org.rioproject.monitor.service.InstantiatorResource;
import org.rioproject.monitor.service.ProvisionRequest;
import org.rioproject.monitor.service.ServiceProvisionContext;
import org.rioproject.monitor.service.ServiceProvisioner;
import org.rioproject.monitor.service.managers.PendingManager;
import org.rioproject.monitor.service.util.LoggingUtil;
import org.rioproject.impl.service.ServiceResource;
import org.rioproject.impl.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.util.concurrent.RejectedExecutionException;

/**
 * The ProvisionTask is created to process and provision dispatch request
 */
public class ProvisionTask implements Runnable {
    private long index;
    private ServiceBeanInstance jsbInstance = null;
    private Throwable thrown = null;
    private String failureReason = null;
    private ServiceProvisionContext context;
    private PendingManager pendingManager;
    private final Logger logger = LoggerFactory.getLogger(ProvisionTask.class);

    /**
     * Create a ProvisionTask
     *
     * @param context The ServiceProvisionContext
     * @param pendingManager The manager for pending service provisioning requests
     */
    public ProvisionTask(ServiceProvisionContext context, PendingManager pendingManager) {
        this.context = context;
        this.pendingManager = pendingManager;
    }

    /**
     * Create a ProvisionTask
     *
     * @param context The ServiceProvisionContext
     * @param pendingManager The manager for pending service provisioning requests
     * @param index The index of the ServiceElement in the PendingServiceElementManager
     */
    public ProvisionTask(ServiceProvisionContext context, PendingManager pendingManager, long index) {
        this.context = context;
        this.pendingManager = pendingManager;
        this.index = index;
    }

    public void run() {
        try {
            jsbInstance = null;
            int result = doProvision(context.getProvisionRequest(), context.getServiceResource());
            if ((result & ServiceProvisioner.PROVISION_FAILURE) != 0) {
                boolean resubmitted = true;
                logger.debug("Provision attempt failed for [{}]", LoggingUtil.getLoggingName(context.getProvisionRequest()));
                if ((result & ServiceProvisioner.UNINSTANTIABLE_JSB) != 0) {
                    /* Notify ServiceProvisionListener of failure */
                    context.getProvisionRequest().getListener().uninstantiable(context.getProvisionRequest());
                    resubmitted = false;
                    logger.debug("Service [{}] is un-instantiable, do not resubmit",
                                LoggingUtil.getLoggingName(context.getProvisionRequest()));
                } else if ((result & ServiceProvisioner.BAD_CYBERNODE) != 0) {
                    /* Provision request encountered a bad Cybernode,
                     * dispatch another request immediately. Note the
                     * subsequent provision request occurs in a separate
                     * thread */
                    context.getDispatcher().dispatch(context.getProvisionRequest());
                    return;
                } else {
                    resubmit();
                }
                /* Send a ProvisionFailureEvent */
                if (thrown != null || result != 0) {
                    try {
                        processProvisionFailure(new ProvisionFailureEvent(context.getEventSource(),
                                                                          context.getProvisionRequest().getServiceElement(),
                                                                          failureReason,
                                                                          thrown));
                    } catch (RejectedExecutionException e) {
                        logger.warn("RejectedExecutionException: Unable to submit ProvisionFailureEvent for {}." +
                                    "Provision Failure reason: {}.",
                                    LoggingUtil.getLoggingName(context.getProvisionRequest()), failureReason, thrown);
                    }
                }
                /* If we have a ServiceProvisionListener,
                 * notify the listener */
                if (context.getProvisionRequest().getServiceProvisionListener() != null) {
                    try {
                        context.getProvisionRequest().getServiceProvisionListener()
                            .failed(context.getProvisionRequest().getServiceElement(), resubmitted);
                    } catch (Exception e) {
                        Throwable t = e;
                        if (e.getCause() != null)
                            t = e.getCause();
                        logger.trace("Error Notifying ServiceProvisionListeners on failure " +
                                      "[{}:{}], this is usually a benign error and indicates " +
                                      "that the ServiceProvisionListener gave up listening",
                                      t.getClass().getName(), t.getLocalizedMessage());
                    }
                }
            } else {
                if (jsbInstance == null) {
                    if (logger.isDebugEnabled()) {
                        String addr = ((InstantiatorResource) context.getServiceResource().getResource()).getHostAddress();
                        String name = ((InstantiatorResource) context.getServiceResource().getResource()).getName();
                        logger.debug("{} at [{}] did not allocate [{}], service limit assumed to have been met",
                                     name, addr, LoggingUtil.getLoggingName(context.getProvisionRequest()));
                    }
                    resubmit();
                    return;
                }
                /* Notify ServiceProvisionListener of success */
                context.getProvisionRequest().getListener()
                    .serviceProvisioned(jsbInstance,
                                        (InstantiatorResource) context.getServiceResource().getResource());
                /* If we have an ServiceBeanInstantiatorListener, 
                 * notify the listener */
                if (context.getProvisionRequest().getServiceProvisionListener() != null) {
                    try {
                        context.getProvisionRequest().getServiceProvisionListener().succeeded(jsbInstance);
                    } catch (Exception e) {
                        logger.trace("Notifying ServiceProvisionListeners on success.", e);
                    }
                }
            }
        } finally {
            context.getInProcess().remove(context.getProvisionRequest().getServiceElement());
        }
    }

    int doProvision(ProvisionRequest request,
                    ServiceResource serviceResource) {
        long start = System.currentTimeMillis();
        int result = 0;
        InstantiatorResource ir = (InstantiatorResource) serviceResource.getResource();
        try {
            //ir.incrementProvisionCounter(request.getServiceElement());
            try {
                ServiceProvisionEvent event = new ServiceProvisionEvent(context.getEventSource(),
                                                                        request.getOpStringManager(),
                                                                        request.getServiceElement());
                event.setSequenceNumber(context.getServiceProvisionEventSequenceNumber().incrementAndGet());
                event.setHandback(ir.getHandback());
                /*
                * Put the instantiate invocation in a for loop, the
                * Cybernode may return null if there is a race-condition
                * where a service is terminated, the FDH notifies the
                * ServiceElementManager of the failure, the
                * ProvisionRequest is dispatched (we get to this point),
                * and the Cybernode is in the process of doing
                * housekeeping related to service termination. If we get
                * a null returned, wait the specified time and retry
                */
                int numProvisionRetries = 3;
                for (int i = 0; i < numProvisionRetries; i++) {
                    if (logger.isDebugEnabled()) {
                        String retry = (i == 0 ? "" : ", retry (" + i + ") ");
                        logger.debug("Allocating {} [{}] ...", retry, LoggingUtil.getLoggingName(request));
                    }
                    DeployedService deployedService = ir.getInstantiator().instantiate(event);
                    if (deployedService != null) {
                        jsbInstance = deployedService.getServiceBeanInstance();
                        ir.addDeployedService(deployedService);
                        logger.info("Allocated [{}]", LoggingUtil.getLoggingName(request));
                        if (logger.isTraceEnabled()) {
                            Object service = jsbInstance.getService();
                            Class serviceClass = service.getClass();
                            logger.trace("{} ServiceBeanInstance {}, Annotation {}",
                                          LoggingUtil.getLoggingName(request),
                                          jsbInstance,
                                          RMIClassLoader.getClassAnnotation(serviceClass));
                        }
                        break;
                    } else {
                        logger.debug("{} at [{}] did not allocate [{}], retry ...",
                                     ir.getName(), ir.getHostAddress(), LoggingUtil.getLoggingName(request));
                        long retryWait = 1000;
                        try {
                            Thread.sleep(retryWait);
                        } catch (InterruptedException ie) {
                            logger.trace("Interrupted while sleeping [{}] milliseconds for provision retry",
                                          retryWait);
                        }
                    }
                }

            } catch (UnknownEventException e) {
                result = ServiceProvisioner.PROVISION_FAILURE;
                failureReason = e.getLocalizedMessage();
                logger.error(failureReason);
                thrown = e;
                context.getSelector().dropServiceResource(serviceResource);
            } catch (RemoteException e) {
                result = ServiceProvisioner.PROVISION_FAILURE;
                Throwable t = ThrowableUtil.getRootCause(e);
                failureReason = t.getLocalizedMessage();
                thrown = e;
            } catch (ServiceBeanInstantiationException e) {
                if (e.isUninstantiable())
                    result = ServiceProvisioner.PROVISION_FAILURE | ServiceProvisioner.UNINSTANTIABLE_JSB;
                else
                    result = ServiceProvisioner.PROVISION_FAILURE;
                thrown = e;
                Throwable t = ThrowableUtil.getRootCause(e);
                failureReason = t.getLocalizedMessage();

            } catch (Throwable t) {
                result = ServiceProvisioner.PROVISION_FAILURE | ServiceProvisioner.UNINSTANTIABLE_JSB;
                thrown = t;
                t = ThrowableUtil.getRootCause(t);
                failureReason = t.getLocalizedMessage();
            }
        } finally {
            if (thrown != null) {
                if (!ThrowableUtil.isRetryable(thrown)) {
                    logger.warn("Drop {} {} from collection, reason: {}",
                                ir.getName(), ir.getInstantiator(), failureReason, thrown);
                    context.getSelector().dropServiceResource(serviceResource);
                    result = ServiceProvisioner.PROVISION_FAILURE | ServiceProvisioner.BAD_CYBERNODE;
                } else {
                    if (logger.isTraceEnabled())
                        logger.warn("Provisioning [{}] to [{}]",
                                   LoggingUtil.getLoggingName(request), ir.getHostAddress(), thrown);
                    else
                        logger.warn("Provisioning [{}] to [{}], {}: {}",
                                    LoggingUtil.getLoggingName(request),
                                    ir.getHostAddress(),
                                    thrown.getClass().getName(),
                                    thrown.getLocalizedMessage());
                }

            }
            ir.decrementProvisionCounter(request.getServiceElement());
            long stop = System.currentTimeMillis();
            context.getWatch().addValue(stop - start);
        }
        return (result);
    }

    /*
     * Helper method to dispatch a ProvisionFailureEventTask and send a ProvisionFailureEvent
     */
    void processProvisionFailure(ProvisionFailureEvent event) {
        context.getProvisionFailurePool().execute(new ProvisionFailureEventTask(event, context.getFailureHandler()));
    }

    void resubmit() {
        if (pendingManager != null) {
            if (context.getProvisionRequest().getType() == ProvisionRequest.Type.PROVISION) {
                pendingManager.addProvisionRequest(context.getProvisionRequest(), index);
                logger.debug("Re-submitted [{}] to {}",
                             LoggingUtil.getLoggingName(context.getProvisionRequest()),
                             pendingManager.getType());
            }
        }
    }
}
