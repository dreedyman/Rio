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
package org.rioproject.gnostic;

import org.drools.agent.KnowledgeAgent;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.reteoo.ReteooRuleBase;
import org.rioproject.associations.*;
import org.rioproject.logging.WrappedLogger;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.sla.RuleMap;

import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class RuleMapAssociationController {
    private static final WrappedLogger logger = WrappedLogger.getLogger(RuleMapAssociationController.class.getName());
    private final RuleMap ruleMap;
    private final AssociationMgmt associationMgmt;
    private final List<Association<Object>> associations = new ArrayList<Association<Object>>();
    private final KnowledgeAgent kAgent;
    //private final KnowledgeBase kBase;
    private ProvisionMonitor monitor;
    private final String[] groups;
    private AssociationsWatchDataReplicator wdr;
    private CEPSession cepSession;
    private final List<AssociatedServiceListener> aListeners = new ArrayList<AssociatedServiceListener>();
    private ClassLoader ruleLoader = null;
    private RuleMapListener listener;

    RuleMapAssociationController(RuleMap ruleMap,
                                 KnowledgeAgent kAgent,
                                 ProvisionMonitor monitor,
                                 String[] groups,
                                 ClassLoader ruleLoader) {
        if(kAgent==null)
            throw new IllegalArgumentException("kAgent is null");
        this.ruleMap = ruleMap;
        associationMgmt = new AssociationMgmt();
        associationMgmt.setBackend(this);
        this.kAgent = kAgent;
        this.monitor = monitor;
        this.groups = groups;
        this.ruleLoader = ruleLoader;
    }

    /*RuleMapAssociationController(RuleMap ruleMap,
                                 KnowledgeBase kBase,
                                 ProvisionMonitor monitor,
                                 String[] groups,
                                 ClassLoader ruleLoader) {
        this.ruleMap = ruleMap;
        associationMgmt = new AssociationMgmt();
        associationMgmt.setBackend(this);
        this.kBase = kBase;
        this.monitor = monitor;
        this.groups = groups;
        this.ruleLoader = ruleLoader;
    }*/

    RuleMap getRuleMap() {
        return ruleMap;
    }

    private List<Association<Object>> getAssociations() {
        return associations;
    }

    void addRuleMapListener(RuleMapListener listener) {
        this.listener = listener;
    }

    void close() {
        if(ruleLoader!=null) {
            KnowledgeBaseImpl kImpl = (KnowledgeBaseImpl)kAgent.getKnowledgeBase();
            //KnowledgeBaseImpl kImpl = (KnowledgeBaseImpl)kBase;
            ((ReteooRuleBase)kImpl.getRuleBase()).getRootClassLoader().removeClassLoader(ruleLoader);
        }
        ruleLoader=null;
        for (AssociatedServiceListener aListener : aListeners) {
            aListener.deregister();
        }
        if(associationMgmt!=null)
            associationMgmt.terminate();
        if(cepSession !=null)
            cepSession.close();
        if(wdr!=null) {
            wdr.close();
            wdr = null;
        }
    }

    void process() {
        for (RuleMap.ServiceDefinition service : ruleMap.getServiceDefinitions()) {
            AssociationDescriptor ad = new AssociationDescriptor(AssociationType.USES, service.getServiceName());
            //ad.setInterfaceNames(Remote.class.getName());
            ad.setPropertyName("service");
            ad.setGroups(groups);
            if(service.getOpStringName()!=null)
                ad.setOperationalStringName(service.getOpStringName());
            ad.setMatchOnName(true);
            associationMgmt.addAssociationDescriptors(ad);
        }
    }


    public void setService(Association<Object> association) {
        associations.add(association);
        AssociatedServiceListener aListener = new AssociatedServiceListener(association);
        association.registerAssociationServiceListener(aListener);
        aListeners.add(aListener);
        logger.fine("Set association for service: %s, received %d of %d",
                    association.getName(), associations.size(), ruleMap.getServiceDefinitions().size());
        if (associations.size() == ruleMap.getServiceDefinitions().size()) {
            logger.info(String.format("Have all services, starting replicator for [%s]", getRuleMap()));
            boolean notInitialized = initializeEngine();
            if(listener!=null) {
                if(notInitialized) {
                    close();
                    listener.failed(ruleMap);
                } else {
                    listener.added(ruleMap);
                }
            }

        }
    }

    private boolean initializeEngine() {
        if(ruleLoader!=null) {
            KnowledgeBaseImpl kImpl = (KnowledgeBaseImpl)kAgent.getKnowledgeBase();
            //KnowledgeBaseImpl kImpl = (KnowledgeBaseImpl)kBase;
            ((ReteooRuleBase)kImpl.getRuleBase()).getRootClassLoader().addClassLoader(ruleLoader);
        }
        boolean shutdownReplicator = false;
        DeployedServiceContext context = new DeployedServiceContext();
        context.setProvisionMonitor(monitor);
        cepSession = new DroolsCEPManager(context, kAgent);
        //cepSession = new DroolsCEPManager(context, kBase);
        try {
            wdr = new AssociationsWatchDataReplicator(cepSession, context, monitor);
            List<ServiceHandle> serviceHandles = wdr.init(ruleMap, getAssociations());
            if(serviceHandles.size()==0) {
                logger.warning("No service handles, cannot continue");
                shutdownReplicator = true;
            } else {
                logger.fine("Added WatchDataReplicators for [%s], creating KnowledgeSession...",
                            ruleMap.toString());
                cepSession.initialize(serviceHandles, ruleMap, ruleLoader);
                logger.info(String.format("Created StatefulKnowledgeSession for %s", ruleMap));
                /* Add the watches */
                for(ServiceHandle sh : serviceHandles) {
                    wdr.registerWatches(sh);

                }
            }
        } catch(IllegalStateException e) {
            shutdownReplicator = true;
            logger.warning(e.getMessage());
        } catch (ExportException e) {
            shutdownReplicator = true;
            logger.log(Level.WARNING, e,
                       "Could not create AssociationsWatchDataReplicator for [%s]" , ruleMap.toString());
        } catch (IOException e) {
            shutdownReplicator = true;
            logger.log(Level.WARNING, e,
                       "Could not initialize DroolsCEPManager for [%s]", ruleMap.toString());
        } finally {
            if(shutdownReplicator) {
                wdr.close();
                cepSession.close();
            }
        }
        return shutdownReplicator;
    }

    class AssociatedServiceListener implements AssociationServiceListener<Object> {
        Association<Object> association;

        AssociatedServiceListener(Association<Object> association) {
            this.association = association;
        }

        public void serviceAdded(Object o) {
            logger.fine("Added %s instance ", association.getName());
            if(wdr!=null)
                wdr.addService(o, association, ruleMap);
        }

        public void serviceRemoved(Object o) {
            // do nothing
        }
        void deregister() {
            association.removeAssociationServiceListener(this);
        }
    }
}