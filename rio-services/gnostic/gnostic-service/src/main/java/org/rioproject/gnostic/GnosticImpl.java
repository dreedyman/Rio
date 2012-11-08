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

import edu.emory.mathcs.util.classloader.URIClassLoader;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.drools.agent.KnowledgeAgent;
import org.drools.io.ResourceFactory;
import org.drools.io.impl.ResourceChangeScannerImpl;
import org.rioproject.associations.Association;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.associations.AssociationMgmt;
import org.rioproject.associations.AssociationType;
import org.rioproject.bean.Initialized;
import org.rioproject.bean.PreDestroy;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.logging.WrappedLogger;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.sla.RuleMap;
import org.rioproject.util.StringUtil;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Gnostic provides support for Complex Event Processing, associating
 * distributed metrics of associated services to rules that in turn
 * provide decisions based on the temporal data accumulated in a knowledge base.
 *
 * @author Dennis Reedy
 */
public class GnosticImpl implements Gnostic {
    private KnowledgeAgent kAgent;
    //private KnowledgeBase kBase;
    private ServiceBeanContext context;
    //private ProvisionMonitor monitor;
    Future<ProvisionMonitor> monitorFuture;
    private ExecutorService execService;
    private final List<RuleMap> managedRuleMaps = new ArrayList<RuleMap>();
    private final List<RuleMap> ruleMapsInProcess = new ArrayList<RuleMap>();
    private static BlockingQueue<RuleMap> addRuleMapQ = new LinkedBlockingQueue<RuleMap>();
    private final List<RuleMapAssociationController> controllers = new ArrayList<RuleMapAssociationController>();
    private static final WrappedLogger logger = WrappedLogger.getLogger(Gnostic.class.getName());
    private final AtomicBoolean droolsInitialized = new AtomicBoolean(false);

    /*
     * Set the configuration and wire up the Monitor association.
     */
    @SuppressWarnings("unused")
    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
        AssociationMgmt associationMgmt =
            (AssociationMgmt) context.getAssociationManagement();
        boolean createCoreAssociations = true;
        String s = (String)context.getInitParameter("create-core-associations");
        if(s!=null && s.equals("no"))
            createCoreAssociations = false;
        if(createCoreAssociations) {
            AssociationDescriptor monitor =
                createAssociationDescriptor("Monitor", context.getServiceBeanConfig().getGroups());
            Association<ProvisionMonitor> monitorAssociation = associationMgmt.addAssociationDescriptor(monitor);
            monitorFuture = monitorAssociation.getServiceFuture();
        }
    }

    @Initialized
    @SuppressWarnings({"unchecked", "unused"})
    public void setupDrools() {
        try {
            execService = Executors.newSingleThreadExecutor();
            execService.submit(new RuleMapWorker());
            int scannerInterval = 60;
            try {
                scannerInterval =(Integer) context.getConfiguration().getEntry("org.rioproject.gnostic",
                                                                               "scannerInterval",
                                                                               int.class,
                                                                               scannerInterval);
            } catch (ConfigurationException e) {
                logger.log(Level.WARNING,
                           "Non-fatal error, unable to obtain scannerInterval from configuration, defaulting to 30 seconds",
                           e);
            }
            kAgent = DroolsFactory.createKnowledgeAgent(scannerInterval);
        } finally {
            droolsInitialized.set(true);
        }
        List<RuleMap> ruleMappings = context.getServiceElement().getRuleMaps();
        List<RuleMap> otherMappings = null;
        try {
            /* Add from configuration as well? */
            Configuration config = context.getConfiguration();
            otherMappings = (List<RuleMap>)config.getEntry("org.rioproject.gnostic",
                                                           "ruleMappings",
                                                           List.class,
                                                           null);
        } catch(ConfigurationException e) {
            logger.log(Level.WARNING,
                       "Non-fatal error, unable to obtain ruleMappings from configuration",
                       e);
        }
        if(otherMappings!=null)
            ruleMappings.addAll(otherMappings);
        if(ruleMappings!=null) {
            for(RuleMap ruleMap : ruleMappings) {
                add(ruleMap);
            }
        }

    }

    private void checkDroolsHasInitialized() {
        long t0 = System.currentTimeMillis();
        while(!droolsInitialized.get()) {
            logger.info("Waiting for Drools to initialize ... %d", (System.currentTimeMillis()-t0));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted while waiting for Drools to initialize", e);
            }
        }
    }

    public int getScannerInterval() {
        checkDroolsHasInitialized();
        return ((ResourceChangeScannerImpl)ResourceFactory.getResourceChangeScannerService()).getInterval();
    }

    public void setScannerInterval(int interval) {
        if(interval<=0)
            throw new IllegalArgumentException("The scannerInterval must be > 0");
        checkDroolsHasInitialized();
        ResourceFactory.getResourceChangeScannerService().setInterval(interval);
    }

    public boolean add(RuleMap ruleMap) {
        checkDroolsHasInitialized();
        verify(ruleMap);
        synchronized(managedRuleMaps) {
            if(managedRuleMaps.contains(ruleMap))
                return false;
        }
        synchronized(ruleMapsInProcess) {
            if(ruleMapsInProcess.contains(ruleMap)) {
                return false;
            }
            ruleMapsInProcess.add(ruleMap);
        }
        logger.info("Adding %s", ruleMap);
        addRuleMapQ.add(ruleMap);
        return true;
    }

    public boolean remove(RuleMap ruleMap) {
        verify(ruleMap);
        synchronized(managedRuleMaps) {
            if(!managedRuleMaps.contains(ruleMap)) {
                logger.fine("RuleMap not found in collection of managed RuleMaps. %s", ruleMap);
                return false;
            }
        }        
        RuleMapAssociationController controller = null;
        synchronized(controllers) {
            for(RuleMapAssociationController c : controllers) {
                if(c.getRuleMap().equals(ruleMap)) {
                    controller = c;
                    break;
                }
            }
        }
        boolean removed = false;
        if(controller!=null) {
            controller.close();
            synchronized(controllers) {
                removed = controllers.remove(controller);
            }
            synchronized(managedRuleMaps) {
                managedRuleMaps.remove(ruleMap);
            }
            logger.info("Removed %s", ruleMap);
        } else {
            logger.fine("RuleMap not managed by any controllers. %s", ruleMap);
        }
        return removed;
    }

    public List<RuleMap> get() {
        List<RuleMap> ruleMaps;
        synchronized(managedRuleMaps) {
            ruleMaps = Collections.unmodifiableList(managedRuleMaps);
        }
        return ruleMaps;
    }

    private void verify(RuleMap ruleMap) {
        if(ruleMap==null)
            throw new IllegalArgumentException("ruleMap is null");
        if(ruleMap.getRuleDefinition()==null)
            throw new IllegalArgumentException("ruleMap has null rules");
        if(ruleMap.getRuleDefinition()==null)
            throw new IllegalArgumentException("ruleMap has no rules");
        if(ruleMap.getServiceDefinitions()==null)
            throw new IllegalArgumentException("ruleMap has null services");
        if(ruleMap.getServiceDefinitions().size()==0)
            throw new IllegalArgumentException("ruleMap has no services");
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanup() {
        logger.info("Gnostic shutting down");
        try {
            context.getAssociationManagement().terminate();
            ResourceFactory.getResourceChangeNotifierService().stop();
            ResourceFactory.getResourceChangeScannerService().stop();
            if(execService!=null)
                execService.shutdownNow();
            for (RuleMapAssociationController controller : controllers) {
                controller.close();
            }
        } catch(Throwable t) {
            t.printStackTrace();
        } finally {
            logger.info("Gnostic shutdown complete");
        }
    }

    private AssociationDescriptor createAssociationDescriptor(String name, String[] groups) {
        /*
         * Create the Association as a REQUIRES, we always require these associations
         * to be resolved. If for some reason we do not resolve these associations,
         * this service will never be advertised.
         */
        return createAssociationDescriptor(name, null, AssociationType.REQUIRES, groups);
    }

    private AssociationDescriptor createAssociationDescriptor(String name,
                                                              String opStringName,
                                                              AssociationType aType,
                                                              String[] groups) {

        AssociationDescriptor ad = new AssociationDescriptor(aType, name);
        ad.setInterfaceNames(ProvisionMonitor.class.getName());
        //ad.setPropertyName("service");
        ad.setGroups(groups);
        if(opStringName!=null)
            ad.setOperationalStringName(opStringName);
        ad.setMatchOnName(true);
        return ad;
    }

    class RuleMapWorker implements Runnable {
        ProvisionMonitor monitor;

        public void run() {
            while (true) {
                waitForMonitor();
                RuleMap ruleMap;
                try {
                    ruleMap = addRuleMapQ.take();
                } catch (InterruptedException e) {
                    logger.fine("RuleMapWorker breaking out of main loop");
                    break;
                }
                if (ruleMap != null) {
                    RuleMapAssociationController controller;
                    ClassLoader ruleLoader;
                    ClassLoader currentCL = null;
                    try {
                        ruleLoader = getRuleClassLoader(ruleMap);
                        if(ruleLoader!=null) {
                            currentCL = Thread.currentThread().getContextClassLoader();
                            Thread.currentThread().setContextClassLoader(ruleLoader);
                        }
                        controller = new RuleMapAssociationController(ruleMap,
                                                                      kAgent,
                                                                      //kBase,
                                                                      monitor,
                                                                      context.getServiceBeanConfig().getGroups(),
                                                                      ruleLoader);
                        controller.addRuleMapListener(new RuleMapNotificationListener());
                        controllers.add(controller);
                        controller.process();
                    } catch (ResolverException e) {
                        logger.log(Level.WARNING, e,
                                   "Unable to provision artifact [%s] for RuleMap %s",
                                   ruleMap.getRuleDefinition().getRuleClassPath(), ruleMap);
                    } catch (MalformedURLException e) {
                        logger.log(Level.WARNING, e,
                                   "Unable to create URL from rule classpath [%s], cannot set classpath for rule classpath jars",
                                   ruleMap.getRuleDefinition().getRuleClassPath());
                    } catch (URISyntaxException e) {
                        logger.log(Level.WARNING, e,
                                   "Unable to create URI from rule classpath [%s], cannot set classpath for rule classpath jars",
                                   ruleMap.getRuleDefinition().getRuleClassPath());
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Unable to create RuleMapAssociationController", e);
                    } finally {
                        if(currentCL!=null)
                            Thread.currentThread().setContextClassLoader(currentCL);
                    }
                }
            }
        }

        private void waitForMonitor() {
            if (monitor == null) {
                try {
                    monitor = monitorFuture.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private ClassLoader getRuleClassLoader(RuleMap ruleMap) throws
                                                                ResolverException,
                                                                MalformedURLException,
                                                                URISyntaxException {
            String ruleClassPath = ruleMap.getRuleDefinition().getRuleClassPath();
            if (ruleClassPath != null) {
                String[] classPath;
                if (Artifact.isArtifact(ruleClassPath)) {
                    String[] cp = ResolverHelper.getResolver().getClassPathFor(ruleClassPath,
                                                                               context.getServiceElement().getRemoteRepositories());
                    classPath = new String[cp.length];
                    for (int i = 0; i < classPath.length; i++) {
                        String s =
                            cp[i].startsWith("file:") ? cp[i] : "file:" + cp[i];
                        classPath[i] = ResolverHelper.handleWindows(s);
                    }
                    if(logger.isLoggable(Level.FINE)) {
                        StringBuilder sb = new StringBuilder();
                        for(String s : classPath) {
                            if(sb.length()>0)
                                sb.append(", ");
                            sb.append(s);
                        }
                        logger.fine("Resolved classpath for rule artifact [%s]: %s", ruleClassPath, sb.toString());
                    }
                } else {
                    classPath = StringUtil.toArray(ruleClassPath, " ,");
                }
                URI[] uris = new URI[classPath.length];
                for (int i = 0; i < classPath.length; i++) {
                    uris[i] = new URL(classPath[i]).toURI();
                }

                return new RulesClassLoader(new URIClassLoader(uris,
                                                               Thread.currentThread().getContextClassLoader()));
            } else {
                return null;
            }
        }

    }

    class RuleMapNotificationListener implements RuleMapListener {

        public void added(RuleMap ruleMap) {
            synchronized(managedRuleMaps) {
                managedRuleMaps.add(ruleMap);
            }
            remove(ruleMap);
            logger.info("Added %s", ruleMap);
        }

        public void failed(RuleMap ruleMap) {
            logger.info("Failed to add %s", ruleMap);
            remove(ruleMap);
        }

        private void remove(RuleMap ruleMap) {
            synchronized(ruleMapsInProcess) {
                ruleMapsInProcess.remove(ruleMap);
            }
        }
    }

    class RulesClassLoader extends ClassLoader /*implements DroolsClassLoader*/ {

        RulesClassLoader(ClassLoader parent) {
            super(parent);
        }

        public InputStream getResourceAsStream(String name) {
            return getParent().getResourceAsStream(name);
        }

        /*public Class<?> fastFindClass(String name) {
            Class<?> cl = null;
            try {
                cl = loadClass(name, false);
            } catch (ClassNotFoundException e) {
                //
            }
            return cl;
        }*/

        public Class<?> loadClass(String name, boolean resolve) throws
                                                                ClassNotFoundException {
            Class<?> cl = null;
            try {
                cl = Class.forName(name, true, getParent());
            } catch(ClassNotFoundException e) {
                //
            }
            return cl;
        }
    }

}
