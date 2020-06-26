/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.test

import groovy.util.logging.Slf4j
import net.jini.config.Configuration
import net.jini.core.entry.Entry
import net.jini.core.lookup.ServiceItem
import net.jini.core.lookup.ServiceRegistrar
import net.jini.core.lookup.ServiceTemplate
import net.jini.discovery.DiscoveryManagement
import net.jini.lease.LeaseRenewalManager
import net.jini.lookup.ServiceDiscoveryManager
import net.jini.lookup.entry.Name
import org.junit.Assert
import org.rioproject.RioVersion
import org.rioproject.config.Constants
import org.rioproject.config.GroovyConfig
import org.rioproject.cybernode.Cybernode
import org.rioproject.deploy.DeployAdmin
import org.rioproject.deploy.ServiceBeanInstantiator
import org.rioproject.impl.client.DiscoveryManagementPool
import org.rioproject.impl.client.JiniClient
import org.rioproject.impl.exec.JVMOptionChecker
import org.rioproject.impl.exec.Util
import org.rioproject.impl.opstring.OAR
import org.rioproject.impl.opstring.OpStringLoader
import org.rioproject.impl.opstring.OpStringManagerProxy
import org.rioproject.impl.service.ServiceStopHandler
import org.rioproject.impl.util.FileUtils
import org.rioproject.monitor.ProvisionMonitor
import org.rioproject.opstring.OperationalString
import org.rioproject.opstring.OperationalStringException
import org.rioproject.opstring.OperationalStringManager
import org.rioproject.opstring.ServiceElement
import org.rioproject.resolver.Artifact
import org.rioproject.resolver.ResolverHelper
import org.rioproject.resolver.maven2.Repository
import org.rioproject.tools.harvest.HarvesterAgent
import org.rioproject.tools.harvest.HarvesterBean
import org.rioproject.tools.webster.Webster
import org.rioproject.util.PropertyHelper

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
/**
 * Simplifies the running of core Rio services
 *
 * @author Dennis Reedy
 */
@Slf4j
class TestManager {
    static final String TEST_HOSTS = 'org.rioproject.test.hosts'
    List<Webster> websters = new ArrayList<Webster>()
    List<Process> processes = new ArrayList<Process>()
    JiniClient client
    String rioHome
    String groups
    String opStringToDeploy
    OperationalStringManager deployedOperationalStringManager
    ServiceDiscoveryManager serviceDiscoveryManager
    List<String> hostList = new ArrayList<String>()
    def config
    boolean createShutdownHook
    TestConfig testConfig
    def additionalExecProps=  [:]
    ThreadPoolExecutor execPool = (ThreadPoolExecutor) Executors.newCachedThreadPool()

    /**
     * Create a TestManager without installing a shutdown hook
     */
    TestManager() {
        this(false)
    }

    /**
     * Create a TestManager
     *
     * @param createShutdownHook If true create a shutdown hook. When the JVM
     * is exiting, any service started by the TestManager will be shutdown.
     */
    TestManager(boolean createShutdownHook) {
        this.createShutdownHook = createShutdownHook
    }

    /**
     * Initialize the TestManager
     *
     * @param testConfig The configuration used to initialize
     *
     * @throws IllegalArgumentException if the testConfig is null
     */
    void init(TestConfig testConfig) {
        if(testConfig==null)
            throw new IllegalArgumentException("testConfig cannot be null")
        this.testConfig = testConfig
        config = loadManagerConfig()
        if(config.manager.cleanLogs) {
            cleanLogs()
        }
        rioHome = System.getProperty('rio.home')
        if(rioHome==null)
            throw new IllegalStateException('The rio.home system property must be set')

        groups = testConfig.groups
        log.info "Using [${groups}] group for discovery"

        DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance()
        if(config.manager.config) {
            def mgrConfig = [PropertyHelper.expandProperties(config.manager.config)]
            Configuration conf = new GroovyConfig((String[])mgrConfig, null)
            discoPool.setConfiguration(conf)
        }

        DiscoveryManagement dMgr = discoPool.getDiscoveryManager(testConfig.component,
                JiniClient.parseGroups(testConfig.groups),
                JiniClient.parseLocators(testConfig.locators))
        serviceDiscoveryManager = new ServiceDiscoveryManager(dMgr, new LeaseRenewalManager())
        //String hosts = System.getProperty(TEST_HOSTS, '')

        if(createShutdownHook) {
            Runtime rt = Runtime.getRuntime()
            log.info "Adding shutdown hook"

            Closure cl = {
                log.info "Running shutdown hook, stop Rio services..."
                shutdown()
            }
            Thread shutdownHook = new Thread(cl, "RunPostShutdownHook")
            rt.addShutdownHook(shutdownHook)
        }

        startConfiguredServices()
    }

    void cleanLogs() {
        File logs = getCreatedLogsFile(testConfig.component)
        if(logs.exists()) {
            logs.eachLine { line ->
                File f = new File((String)line)
                if(f.exists()) {
                    if(FileUtils.remove(f)) {
                        log.debug "Removed ${f.name}"
                    } else {
                        log.debug "Could not remove ${f.name}, check permissions"
                    }
                }
            }
            logs.delete()
        }
    }

    private void startConfiguredServices() {
        startWebster()
        if(testConfig.getNumLookups()>0) {
            int lookupCount = testConfig.getNumLookups()-countLookups()
            for(int i=0; i<lookupCount; i++)
                startReggie()
        }

        if(testConfig.getNumMonitors()>0) {
            int monitorCount = testConfig.getNumMonitors()-countMonitors()
            if(monitorCount>0) {
                for(int i=0; i<monitorCount; i++)
                    startProvisionMonitor()
                /*
                * Need to get an instance of DiscoveryManagement and set
                * it to the OpStringManagerProxy utility in order to
                * discover ProvisionManager instances.
                *
                * This is required if test clients use association
                * strategies, specifically the utilization strategy
                */
                OpStringManagerProxy.setDiscoveryManagement(
                        getServiceDiscoveryManager().getDiscoveryManager())
            }
        }

        if(testConfig.getNumCybernodes()>0) {
            int cybernodeCount = testConfig.getNumCybernodes() - countCybernodes()
            for (int i = 0; i < cybernodeCount; i++)
                startCybernode()
        }

        postInit()

        if(testConfig.getOpString()!=null) {
            setOpStringToDeploy(testConfig.getOpString())
            if(testConfig.autoDeploy()) {
                OperationalStringManager mgr = deploy()
                setDeployedOperationalStringManager(mgr)
            }
        }
    }

    /**
     * Lifecycle notification by RioTestRunner. Called after RioTestRunner has
     * asked the TestManager to start Lookups, Monitors and Cybernodes. Only
     * called once and guaranteed to be called after init().
     *
     * This implementation is empty, utilities that extend the TestManager may
     * choose to provide specific behavior.
     */
    def postInit() {
    }

    /**
     * Starts a Cybernode using the <tt>config/start-cybernode.groovy</tt>
     * start configuration.
     *
     * @return The started Cybernode
     */
    Cybernode startCybernode() {
        def starter = config.manager.cybernodeStarter
        Cybernode cybernode = null
        if(starterConfigOk(starter)) {
            String cybernodeStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(cybernodeStarter)
            cybernode =  (Cybernode)waitForService(Cybernode.class)
        } else {
            log.warn "A Cybernode has been declared to start in the test configuration "+
                     "but there is no cybernodeStarter declared in the test-config"
        }
        return cybernode
    }

    /**
     * Starts a Cybernode using the starter configuration obtain from the TestConfig.
     *
     * @param hostIndex The index of the host to start on
     *
     * @return The started Cybernode
     */
    Cybernode startCybernode(int hostIndex) {
        def starter = config.manager.cybernodeStarter
        Cybernode cybernode = null
        if(starterConfigOk(starter)) {
            String cybernodeStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(cybernodeStarter)
            cybernode =  (Cybernode)waitForService(Cybernode.class)
        } else {
            log.warn "A Cybernode has been declared to start in the test configuration "+
                     "but there is no cybernodeStarter declared in the test-config"
        }
        return cybernode
    }

    /**
     * Starts a ProvisionMonitor using the starter configuration obtain from the TestConfig.
     *
     * @return The started ProvisionMonitor
     */
    ProvisionMonitor startProvisionMonitor() {
        def starter = config.manager.monitorStarter
        ProvisionMonitor monitor = null
        if(starterConfigOk(starter)) {
            String monitorStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(monitorStarter)
            monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        } else {
            log.warn "A Monitor has been declared to start in the test configuration "+
                     "but there is no monitorStarter declared in the test-config"
        }
        return monitor
    }

    /**
     * Starts a ProvisionMonitor using the starter configuration obtain from the TestConfig.
     *
     * @param hostIndex The index of the host to start on
     *
     * @return The started ProvisionMonitor
     */
    ProvisionMonitor startProvisionMonitor(int hostIndex) {
        def starter = config.manager.monitorStarter
        ProvisionMonitor monitor = null
        if(starterConfigOk(starter)) {
            String monitorStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(monitorStarter)
            monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        } else {
            log.warn "A Monitor has been declared to start in the test configuration "+
                     "but there is no monitorStarter declared in the test-config"
        }
        return monitor
    }

    /**
     * Starts a Webster serving up rio-home/lib, rio-home/lib, rioTestHome/target
     *
     * @return The started Webster
     */
    Webster startWebster() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = System.getProperty('rio.home')

        String websterRoots = "${rioHome}/lib-dl;${rioHome}/lib;${m2Repo}"
        String testRoots = System.getProperty("rio.test.webster.roots")
        if (testRoots != null) {
            websterRoots = websterRoots +";${testRoots}"
        }
        Webster webster = new Webster(0, websterRoots)
        websters.add(webster)
        return webster
    }

    /**
     * Starts a ServiceRegistrar using the starter configuration obtain from the TestConfig.
     *
     * @return The started ServiceRegistrar
     */
    ServiceRegistrar startReggie() {
        def starter = config.manager.reggieStarter
        ServiceRegistrar reggie = null
        if(starterConfigOk(starter)) {
            String reggieStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(reggieStarter)
            reggie = (ServiceRegistrar)waitForService(ServiceRegistrar.class)
        } else {
            log.warn "A Lookup service has been declared to start in the test configuration "+
                     "but there is no reggieStarter declared in the test-config"
        }
        return reggie
    }

    /**
     * Deploys the configured OperationalString
     *
     * @return The OperationalStringManager that is managing the OperationalString
     *
     * @throws OperationalStringException if there are problems deploying
     */
    OperationalStringManager deploy() {
        Assert.assertNotNull "The OperationalString to deploy has not been "+
                             "set. Check to make sure a corresponding test "+
                             "configuration exists for the class being tested "+
                             "and set the <test-class-name>.opstring property",
                             opStringToDeploy
        URL opStringURL
        if(Artifact.isArtifact(opStringToDeploy)) {
            return deploy(opStringToDeploy)
        } else {
            opStringURL = new File(opStringToDeploy).toURI().toURL()
        }
        return deploy(opStringURL)
    }

    /**
     * Deploys an OperationalString OAR artifact
     *
     * @param opstring The OperationalString artifact
     *
     * @return The OperationalStringManager that is managing the OperationalString or null if the
     * opstring is not an artifact
     */
    OperationalStringManager deploy(String opstring) {
        if(Artifact.isArtifact(opstring)) {
            URL opStringURL = ResolverHelper.getResolver().getLocation(opstring, "oar")
            if(opStringURL==null)
                throw new OperationalStringException("Artifact "+opstring+" not resolvable")
            OAR oar = new OAR(new File(opStringURL.toURI()))
            ProvisionMonitor monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
            return deploy(oar.loadOperationalStrings()[0], monitor)
        } else {
            log.warn "The [${opstring}] is not an artifact"
        }
        return null
    }

    /**
     * Deploys an OperationalString
     *
     * @param opstring The OperationalString file object
     *
     * @return The OperationalStringManager that is managing the OperationalString
     */
    OperationalStringManager deploy(File opstring) {
        return deploy(opstring.toURI().toURL())
    }

    /**
     * Deploys an OperationalString
     *
     * @param opstring The OperationalString file object
     * @param monitor The ProvisionMonitor to deploy to
     *
     * @return The OperationalStringManager that is managing the OperationalString
     */
    OperationalStringManager deploy(File opstring, ProvisionMonitor monitor) {
        return deploy(opstring.toURI().toURL(), monitor)
    }

    /**
     * Deploys an OperationalString
     *
     * @param opstring The URL pointing to the OperationalString
     *
     * @return The OperationalStringManager that is managing the OperationalString
     */
    OperationalStringManager deploy(URL opstring) {
        ProvisionMonitor monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        return deploy(opstring, monitor)
    }

    /**
     * Deploys an OperationalString
     *
     * @param opstring The URL pointing to the OperationalString
     * @param monitor The ProvisionMonitor to deploy to
     *
     * @return The OperationalStringManager that is managing the OperationalString
     */
    OperationalStringManager deploy(URL opstring, ProvisionMonitor monitor) {
        OpStringLoader loader = new OpStringLoader(getClass().classLoader)
        OperationalString[] opstrings = loader.parseOperationalString(opstring)
        return deploy(opstrings[0], monitor)
    }

    /**
     * Deploys an OperationalString
     *
     * @param opstring The OperationalString object to deploy
     * @param monitor The ProvisionMonitor to deploy to
     *
     * @return The OperationalStringManager that is managing the OperationalString
     */
    OperationalStringManager deploy(OperationalString opstring, ProvisionMonitor monitor) {
        DeployAdmin dAdmin = (DeployAdmin)monitor.admin
        dAdmin.deploy(opstring)
        OperationalStringManager mgr = dAdmin.getOperationalStringManager(opstring.name)
        return mgr
    }

    /**
     * Undeploys an OperationalString
     *
     * @param name The name of a deployed OperationalString
     */
    boolean undeploy(String name) {
        ServiceItem[] items = getServiceItems(ProvisionMonitor.class)
        if(items.length==0) {
            log.warn "No ProvisionMonitor instances discovered, cannot undeploy ${name}"
            return false
        }
        return undeploy(name, (ProvisionMonitor)items[0].service)
    }

    /**
     * Undeploys an OperationalString
     *
     * @param name The name of a deployed OperationalString
     * @param monitor The ProvisionMonitor instance to perform the undeployment
     */
    static boolean undeploy(String name, ProvisionMonitor monitor) {
        if(monitor!=null) {
            DeployAdmin dAdmin = (DeployAdmin)monitor.admin
            return dAdmin.undeploy(name)
        } else {
            log.warn "Cannot undeploy ${name}, ProvisionMonitor provided is null"
            return false
        }
    }

    /**
     * Undeploy all deployed OperationalString
     *
     * @param monitor The ProvisionMonitor instance to perform the undeployment
     */
    static void undeployAll(ProvisionMonitor monitor) {
        try {
            DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin()
            OperationalStringManager[] opStringMgrs = deployAdmin.getOperationalStringManagers()
            for (OperationalStringManager mgr : opStringMgrs) {
                String opStringName = mgr.getOperationalString().name
                log.debug "Undeploying ${opStringName} ..."
                deployAdmin.undeploy(opStringName)
                log.debug "Undeployed ${opStringName}"
            }
        } catch (Exception e) {
            log.error("While undeploying", e)
        }
    }

    /**
     * Get the OperationalStringManager for an OperationalString that was configured to be autoDeployed
     *
     * @return The OperationalStringManager for the automatically deployed OperationalString
     */
    OperationalStringManager getOperationalStringManager() {
        Assert.assertNotNull "The deployed OperationalStringManager has not "+
                             "been set. This is most likely due to not having "+
                             "the declared opstring in your test case "+
                             "configuration set with autoDeploy=true",
                              deployedOperationalStringManager

        ProvisionMonitor monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        DeployAdmin dAdmin = (DeployAdmin)monitor.admin
        String name = deployedOperationalStringManager.getOperationalString().getName()
        return dAdmin.getOperationalStringManager(name)
    }

    /**
     * Get the OperationalStringManager for a deployment
     *
     * @name The name of the deployment
     *
     * @return The OperationalStringManager for the deployment
     */
    OperationalStringManager getOperationalStringManager(String name) {
        ProvisionMonitor monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        DeployAdmin dAdmin = (DeployAdmin)monitor.admin
        return dAdmin.getOperationalStringManager(name)
    }

    /**
     * Stop a Cybernode
     *
     * @param service The Cybernode service proxy
     */
    static void stopCybernode(service) {
        stopService(service, "Cybernode")
    }

    /**
     * Stop a ProvisionMonitor
     *
     * @param service The ProvisionMonitor service proxy
     */
    static void stopProvisionMonitor(service) {
        stopService(service, "Monitor")
    }

    /**
     * Stop a service
     *
     * @param service The service proxy
     * @param name The name of the service to stop
     */
    static void stopService(service, String name) {
        if(service==null)
            throw new IllegalArgumentException("service proxy is null for ${name}")

        ServiceStopHandler stopHandler = new ServiceStopHandler()
        stopHandler.destroyService(service, name, System.out)
    }

    /**
     * Shutdown all started services
     */
    def shutdown() {
        for(Webster w : websters)
            w.terminate()
        /* Make sure all services are terminated */
        for(Process p : processes) {
            p.destroy()
        }
    }

    /**
     * Determine if the Harvester needs to run. This is based on whether the test configuration
     * indicates that harvesting should occur
     */
    def maybeRunHarvester() {
        if(testConfig.runHarvester()) {
            ProvisionMonitor monitor
            ServiceItem[] items = getServiceItems(ProvisionMonitor.class)
            if(items.length==0) {
                log.warn "No discovered ProvisionMonitor instances, cannot deploy HarvesterAgents"
                return
            }
            monitor = (ProvisionMonitor)items[0].service
            String opstring
            if(config.manager.harvesterOpString)
                opstring = "${PropertyHelper.expandProperties(config.manager.harvesterOpString)}"
            else
                opstring = "${rioHome}/src/test/resources/harvester.groovy"
            URL opStringUrl
            try {
                opStringUrl = new URL(opstring)
            } catch (MalformedURLException e) {
                File opstringFile = new File(opstring)
                if(!opstringFile.exists())
                    log.warn "Cannot load [${opstringFile}], Unable to deploy Harvester support."
                opStringUrl = opstringFile.toURI().toURL()
            }
            OpStringLoader loader = new OpStringLoader(getClass().classLoader)
            OperationalString[] opstrings = loader.parseOperationalString(opStringUrl)
            Assert.assertEquals "Expected only 1 OperationalString", 1, opstrings.length
            for(ServiceElement elem : opstrings[0].services)
                elem.getServiceBeanConfig().addInitParameter(HarvesterAgent.PREFIX,
                                                             testConfig.getComponent())
            deploy(opstrings[0], monitor)
            /* Count the number of physical machines*/
            List<String> hosts = new ArrayList<String>()
            for(ServiceBeanInstantiator sbi : monitor.getServiceBeanInstantiators()) {
                String s = sbi.getInetAddress().toString()
                if(!hosts.contains(s))
                    hosts.add(s)
            }
            def h = getHarvester(serviceDiscoveryManager.discoveryManager)
            h.harvestDir = "${PropertyHelper.expandProperties(config.manager.harvestDir)}"
            log.info "Harvester:: Number of physical machines = ${hosts.size()}"
            long timeout = 1000*60
            long duration = 0
            while(h.agentsHandledCount<hosts.size()) {
                Thread.sleep(1000)
                duration += 1000
                if(duration >= timeout)
                    break
            }
            log.info "Number of HarvesterAgents handled = ${h.agentsHandledCount}"
            h.unadvertise()
        }
    }

    /**
     * Get a local Harvester instance
     *
     * @param dMgr The DiscoveryManagement to use
     *
     * @return A {@link org.rioproject.tools.harvest.Harvester} instance
     */
    static HarvesterBean getHarvester(DiscoveryManagement dMgr) {
        new HarvesterBean(dMgr)
    }

    def addExecProperty(String key, String value) {
        additionalExecProps.put(key, value)
    }

    def addExecProperties(Map options) {
        additionalExecProps.putAll(options)
    }

    String getAdditionalExecProps() {
        StringBuilder s = new StringBuilder()
        additionalExecProps.each {k, v ->
            if(s.length()>0)
                s.append(" ")
            s.append("-D").append(k).append("=").append(v)
        }
        s.toString()
    }

    private void exec(String starter) {
        String classpath = "${PropertyHelper.expandProperties(config.manager.execClassPath)}"
        String service = starter.substring(starter.lastIndexOf("-")+1)
        service = service.substring(0, service.indexOf("."))
        String jvmOptions = Util.replace("${config.manager.jvmOptions}", '${service}', service)
        jvmOptions = "${PropertyHelper.expandProperties(jvmOptions)}"
        if(config.manager.inheritOptions)
            jvmOptions = JVMOptionChecker.getJVMInputArgs(jvmOptions)
        jvmOptions = jvmOptions+ getAdditionalExecProps()
        jvmOptions = jvmOptions+' -D'+Constants.RIO_TEST_EXEC_DIR+'='+System.getProperty("user.dir")

        StringBuilder classpathBuilder = new StringBuilder()
        classpathBuilder.append(classpath)

        jvmOptions = jvmOptions + " -Dlogback.configurationFile=${rioHome}/config/logging/logback.groovy "
        jvmOptions = jvmOptions + " -Djava.util.logging.config.file=${rioHome}/config/logging/logging.properties "

        String logDir = null
        String mainClass = "${config.manager.mainClass}"
        if (config.manager.log.size() > 0) {
            logDir = "${config.manager.log}${File.separator}${testConfig.component}"
            File f = new File(logDir)
            if(!f.exists()) {
                f.mkdirs()
                File createdLogsFile = getAndCreateCreatedLogsFile(testConfig.component)
                createdLogsFile.append(logDir+'\n')
            }
        }

        jvmOptions = jvmOptions + ' -Drio.log.dir=' + logDir + ' '

        StringBuilder cmdLineBuilder = new StringBuilder()
        if (System.getProperty("os.name").contains("Windows")) {
            cmdLineBuilder.append("cmd.exe /c")
        }

        cmdLineBuilder.append(getJava()).append(" ")
                .append(jvmOptions)
                .append(" -cp ").append(classpathBuilder.toString()).append(" ")
                .append(mainClass).append(" ").append(starter)

        String cmdLine = cmdLineBuilder.toString()
        log.info "Logging for $service will be sent to ${logDir}"
        log.info "Starting ${service}, using starter config [${starter}]"
        log.info "Exec command line: ${cmdLine}"
        Process process = Runtime.runtime.exec(cmdLine)
        processes.add(process)
    }

    /**
     * Wait for a deployment to complete. This means to wait for all services
     * declared to activate and join the network
     *
     * @param mgr The OperationalStringManager to connect with to
     * wait for the deployment to complete
     *
     * @throws TimeoutException if the time waiting for the deployment exceeds
     * {@link ServiceMonitor#MAX_TIMEOUT}
     */
    void waitForDeployment(OperationalStringManager mgr) {
        OperationalString opstring  = mgr.getOperationalString()
        Map<ServiceElement, Integer> deploy = new HashMap<ServiceElement, Integer>()
        int total = 0
        for (ServiceElement elem: opstring.services) {
            deploy.put(elem, 0)
            total += elem.planned
        }
        int deployed = 0
        long sleptFor = 0
        while (deployed < total && sleptFor<ServiceMonitor.MAX_TIMEOUT) {
            deployed = 0
            for (Map.Entry<ServiceElement, Integer> entry: deploy.entrySet()) {
                int numDeployed = entry.value
                ServiceElement elem = entry.key
                if (numDeployed < elem.planned) {
                    log.info "Waiting for service ${elem.operationalStringName}/${elem.name} to be deployed. " +
                             "Planned [${elem.planned}], deployed [${numDeployed}]"
                    numDeployed = mgr.getServiceBeanInstances(elem).length
                    deploy.put(elem, numDeployed)
                } else {
                    deployed += elem.planned
                    log.info "Service ${elem.operationalStringName}/${elem.name} is deployed. " +
                             "Planned [${elem.planned}], deployed [${numDeployed}]"
                }
            }
            if(sleptFor==ServiceMonitor.MAX_TIMEOUT)
                break
            if (deployed < total) {
                Thread.sleep(1000)
                sleptFor += 1000
            }
        }

        if(sleptFor>=ServiceMonitor.MAX_TIMEOUT && deployed < total)
            throw new TimeoutException("Timeout waiting for service to be deployed")
    }

    private int countLookups() {
        countServices(ServiceRegistrar.class)
    }

    private int countMonitors() {
        countServices(ProvisionMonitor.class)
    }

    private int countCybernodes() {
        countServices(Cybernode.class)
    }

    private int countServices(Class serviceInterface) {
        long t0 = System.currentTimeMillis()
        ServiceItem[] items = getServiceItems(serviceInterface)
        log.info "Discovered $items.length instances of ${serviceInterface.name}, "+
                 "elapsed time: ${(System.currentTimeMillis()-t0)} millis"
        items.length
    }

    /**
     * Get all ServiceItems for a service
     *
     * @param type The service type
     *
     * @return An array of ServiceItem instances.
     */
    ServiceItem[] getServiceItems(Class type) {
        def classes = [type]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, null)
        ServiceItem[] items = serviceDiscoveryManager.lookup(template,
                                                             Integer.MAX_VALUE,
                                                             null)
        return items
    }

    /**
     * Get all service proxy instances
     *
     * @param serviceInterface The service type
     *
     * @return An array of service proxy instances.
     */
    def getServices(Class type) {
        return getServices(type, null)
    }

    /**
     * Get all service proxy instances
     *
     * @param serviceInterface The service type
     * @param name The service name to match. If null, return all services for the given type
     *
     * @return An array of service proxy instances.
     */
    def getServices(Class type, String name) {
        def classes = [type]
        def attrs = null
        if(name!=null)
            attrs = [new Name(name)]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, (Entry[])attrs)
        ServiceItem[] items = serviceDiscoveryManager.lookup(template,
                                                             Integer.MAX_VALUE,
                                                             null)
        def services = []
        for(int i = 0; i < items.length; i++)
            services << items[i].service
        services
    }

    /**
     * Wait for service to come online
     *
     * @param type The service type
     *
     * @return The discovered service instance.
     *
     * @throws TimeoutException is the service is not discovered in 60 seconds
     */
    public <T> T waitForService(Class<T> type) {
        waitForService(type, null)
    }

    /**
     * Wait for service to come online
     *
     * @param serviceName The name of the service
     *
     * @return The discovered service instance.
     *
     * @throws TimeoutException is the service is not discovered in 60 seconds
     */
    def waitForService(String serviceName) {
        waitForService(null, serviceName)
    }

    /**
     * Wait for service to come online
     *
     * @param type The service type
     * @param serviceName The name of the service
     *
     * @return The discovered service instance.
     *
     * @throws TimeoutException is the service is not discovered in 60 seconds
     */
    public <T> T waitForService(Class<T> type, String name) {
        def classes = null
        if(type!=null)
            classes = [type]
        def attrs = null
        if(name!=null)
            attrs = [new Name(name)]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, (Entry[])attrs)
        def service
        StringBuffer sb = new StringBuffer()
        if(type!=null)
            sb.append(type.name)
        if(name!=null) {
            if(sb.length()>0)
                sb.append(", ")
            sb.append("name: ").append(name)
        }
        log.info "Waiting for ${sb.toString()} to be discovered"
        long t0 = System.currentTimeMillis()
        ServiceItem serviceItem = serviceDiscoveryManager.lookup(template, null, 60000)
        if (serviceItem == null) {
            String info = type==null?(name==null?"<all services>":name):type.name
            throw new TimeoutException("Unable to discover service $info")
        }
        service = serviceItem.service
        log.info "${sb.toString()} has been discovered, elapsed time: ${(System.currentTimeMillis()-t0)} millis"
        service as T
    }

    private static String getJava() {
        StringBuilder jvmBuilder = new StringBuilder()
        jvmBuilder.append(System.getProperty("java.home"))
        jvmBuilder.append(File.separator)
        jvmBuilder.append("bin")
        jvmBuilder.append(File.separator)
        jvmBuilder.append("java")
        jvmBuilder.append(" ")
        jvmBuilder.toString()
    }

    private static def loadManagerConfig() {
        String defaultManagerConfig =
            "jar:file:${Utils.getRioHome()}/lib/rio-test-${RioVersion.VERSION}.jar!/default-manager-config.groovy"
        String mgrConfig = System.getProperty('org.rioproject.test.manager.config',
                                              defaultManagerConfig)
        log.info "Using TestManager configuration ${mgrConfig}"
        URL url = loadManagerConfig(mgrConfig)
        if (url == null) {
            url = loadManagerConfig(defaultManagerConfig)
        }
        if (url == null) {
            throw new RuntimeException(
                    "Cannot load [${mgrConfig}], or [${defaultManagerConfig}]. This file is needed "+
                            "by the Rio TestManager to initialize. Check the setting of the "+
                            "org.rioproject.test.manager.config system property.")
        }
        new ConfigSlurper().parse(url)
    }

    private static URL loadManagerConfig(String mgrConfig) {
        URL url = null
        try {
            url = new URL(mgrConfig)
        } catch (MalformedURLException e) {
            File mgrConfigFile = new File(mgrConfig)
            if (mgrConfigFile.exists()) {
                url = mgrConfigFile.toURI().toURL()
            }
        }
        url
    }

    private static File getCreatedLogsFile(String testName) {
        File parent = new File(System.getProperty('java.io.tmpdir')+File.separator+".rio")
        new File(parent, "$testName-test-logs")
    }

    private static File getAndCreateCreatedLogsFile(String testName) {
        File dir = new File(System.getProperty('java.io.tmpdir')+File.separator+".rio")
        if(!dir.exists())
            dir.mkdirs()
        File logs = new File(dir, "$testName-test-logs")
        if(!logs.exists())
            logs.createNewFile()
        logs
    }

    private static boolean starterConfigOk(def starter) {
        starter instanceof String
    }
}
