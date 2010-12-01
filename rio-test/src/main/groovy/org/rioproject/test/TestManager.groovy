/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test

import java.util.logging.Logger
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
import org.rioproject.config.Constants
import org.rioproject.config.GroovyConfig
import org.rioproject.core.OperationalString
import org.rioproject.core.OperationalStringManager
import org.rioproject.core.ServiceElement
import org.rioproject.core.provision.ServiceBeanInstantiator
import org.rioproject.cybernode.Cybernode
import org.rioproject.exec.JVMOptionChecker
import org.rioproject.exec.Util
import org.rioproject.monitor.DeployAdmin
import org.rioproject.monitor.ProvisionMonitor
import org.rioproject.opstring.OpStringLoader
import org.rioproject.opstring.OpStringManagerProxy
import org.rioproject.resources.client.DiscoveryManagementPool
import org.rioproject.resources.client.JiniClient
import org.rioproject.resources.util.PropertyHelper
import org.rioproject.tools.cli.CLI.StopHandler
import org.rioproject.tools.harvest.HarvesterAgent
import org.rioproject.tools.harvest.HarvesterBean
import org.rioproject.tools.webster.Webster

/**
 * Simplifies the running of core Rio services
 */
class TestManager {
    static final String TEST_HOSTS = 'org.rioproject.test.hosts'
    static Logger logger = Logger.getLogger(TestManager.class.getPackage().name);
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

    def init(TestConfig testConfig) {
        if(testConfig==null)
            throw new IllegalArgumentException("testConfig cannot be null")
        this.testConfig = testConfig
        config = loadManagerConfig()
        if(config.manager.cleanLogs) {
            File logs = getCreatedLogsFile()
            if(logs.exists()) {
                logs.eachLine { line ->
                    File f = new File((String)line)
                    if(f.exists())
                        f.delete()
                }
                logs.delete()
            }
        }
        rioHome = System.getProperty('RIO_HOME')
        if(rioHome==null)
            throw new IllegalStateException('The RIO_HOME system property must be set')
        groups = System.getProperty(Constants.GROUPS_PROPERTY_NAME)
        if(groups==null)
            throw new IllegalStateException("The ${Constants.GROUPS_PROPERTY_NAME} system "+
                                            "property must be set")
        logger.info "Using [${groups}] group for discovery"

        DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
        if(config.manager.config) {
            def mgrConfig = [PropertyHelper.expandProperties(config.manager.config)]
            Configuration conf = new GroovyConfig((String[])mgrConfig, null)
            discoPool.setConfiguration(conf)
        }

        DiscoveryManagement dMgr = discoPool.getDiscoveryManager(null);
        serviceDiscoveryManager =
            new ServiceDiscoveryManager(dMgr, new LeaseRenewalManager())
        //String hosts = System.getProperty(TEST_HOSTS, '')

        if(createShutdownHook) {
            Runtime rt = Runtime.getRuntime();
            logger.info ":: Adding shutdown hook"

            Closure cl = {
                logger.info ":: Running shutdown hook, stop Rio services..."
                shutdown()
            }
            Thread shutdownHook = new Thread(cl, "RunPostShutdownHook")
            rt.addShutdownHook(shutdownHook)
        }

        startConfiguredServices()
    }

    def startConfiguredServices() {
        int lookupCount = testConfig.getNumLookups()-countLookups();
        for(int i=0; i<lookupCount; i++)
            startReggie();

        int monitorCount = testConfig.getNumMonitors()-countMonitors();
        if(monitorCount>0) {
            for(int i=0; i<monitorCount; i++)
                startProvisionMonitor();
            /*
            * Need to get an instance of DiscoveryManagement and set
            * it to the OpStringManagerProxy utility in order to
            * discover ProvisionManager instances.
            *
            * This is required if test clients use association
            * strategies, specifically the utilization strategy
            */
            OpStringManagerProxy.setDiscoveryManagement(
                getServiceDiscoveryManager().getDiscoveryManager());
        }

        int cybernodeCount = testConfig.getNumCybernodes()-countCybernodes();
        for(int i=0; i<cybernodeCount; i++)
            startCybernode();

        postInit();

        if(testConfig.getOpString()!=null) {
            setOpStringToDeploy(testConfig.getOpString());
            if(testConfig.autoDeploy()) {
                OperationalStringManager mgr = deploy();
                setDeployedOperationalStringManager(mgr);
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
            println "[ERROR]: No cybernodeStarter declared in test-config"
        }
        return cybernode
    }

    Cybernode startCybernode(int hostIndex) {
        def starter = config.manager.cybernodeStarter
        Cybernode cybernode = null
        if(starterConfigOk(starter)) {
            String cybernodeStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(cybernodeStarter)
            cybernode =  (Cybernode)waitForService(Cybernode.class)
        } else {
            println "[ERROR]: No cybernodeStarter declared in test-config"
        }
        return cybernode
    }

    ProvisionMonitor startProvisionMonitor() {
        def starter = config.manager.monitorStarter
        ProvisionMonitor monitor = null
        if(starterConfigOk(starter)) {
            String monitorStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(monitorStarter)
            monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        } else {
            println "[ERROR]: No monitorStarter declared in test-config"
        }
        return monitor
    }

    ProvisionMonitor startProvisionMonitor(int hostIndex) {
        def starter = config.manager.monitorStarter
        ProvisionMonitor monitor = null
        if(starterConfigOk(starter)) {
            String monitorStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(monitorStarter)
            monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        } else {
            println "[ERROR]: No monitorStarter declared in test-config"
        }
        return monitor
    }

    Webster startWebster(int port, String dirs) {
        Webster webster = new Webster(port, dirs);
        websters.add(webster);
        return webster;
    }

    ServiceRegistrar startReggie() {
        def starter = config.manager.reggieStarter
        ServiceRegistrar reggie = null
        if(starterConfigOk(starter)) {
            String reggieStarter = "${PropertyHelper.expandProperties(starter)}"
            exec(reggieStarter)
            reggie = (ServiceRegistrar)waitForService(ServiceRegistrar.class)
        } else {
            println "[ERROR]: No reggieStarter declared in test-config"
        }
        return reggie
    }

    OperationalStringManager deploy() {
        Assert.assertNotNull "The OperationalString to deploy has not been "+
                             "set. Check to make sure a corresponding test "+
                             "configuration exists for the class being tested "+
                             "and set the <test-class-name>.opstring property",
                             opStringToDeploy
        return deploy(new File(opStringToDeploy).toURL())
    }

    OperationalStringManager deploy(File opstring) {
        return deploy(opstring.toURL())
    }

    OperationalStringManager deploy(File opstring, ProvisionMonitor monitor) {
        return deploy(opstring.toURL(), monitor)
    }

    OperationalStringManager deploy(URL opstring) {
        ProvisionMonitor monitor =
        (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        return deploy(opstring, monitor)
    }

    OperationalStringManager deploy(URL opstring, ProvisionMonitor monitor) {
        OpStringLoader loader = new OpStringLoader(getClass().classLoader)
        OperationalString[] opstrings = loader.parseOperationalString(opstring)
        return deploy(opstrings[0], monitor)        
    }

    OperationalStringManager deploy(OperationalString opstring, ProvisionMonitor monitor) {
        DeployAdmin dAdmin = (DeployAdmin)monitor.admin
        dAdmin.deploy(opstring)
        OperationalStringManager mgr = dAdmin.getOperationalStringManager(opstring.name)
        return mgr
    }

    def undeploy(String name) {
        ServiceItem[] items = getServiceItems(ProvisionMonitor.class)
        if(items.length==0) {
            println "No ProvisionMonitor instances discovered, cannot undeploy ${name}"
            return
        }
        undeploy(name, (ProvisionMonitor)items[0].service)
    }

    def undeploy(String name, ProvisionMonitor monitor) {
        if(monitor!=null) {
            DeployAdmin dAdmin = (DeployAdmin)monitor.admin
            dAdmin.undeploy(name)
        } else {
            println "Cannot undeploy ${name}, ProvisionMonitor provided is null"
        }
    }

    def undeployAll(ProvisionMonitor monitor) {
        DeployAdmin deployAdmin = (DeployAdmin) monitor.getAdmin();
        OperationalStringManager[] opStringMgrs =
                deployAdmin.getOperationalStringManagers();
        for (OperationalStringManager mgr : opStringMgrs) {
            String opStringName = mgr.getOperationalString().name
            deployAdmin.undeploy(opStringName);
        }
    }

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

    OperationalStringManager getOperationalStringManager(String name) {
        ProvisionMonitor monitor = (ProvisionMonitor)waitForService(ProvisionMonitor.class)
        DeployAdmin dAdmin = (DeployAdmin)monitor.admin
        return dAdmin.getOperationalStringManager(name)
    }

    def stopCybernode(Object service) {
        stopService(service, "Cybernode")
    }

    def stopProvisionMonitor(Object service) {
        stopService(service, "Monitor") 
    }

    def stopService(Object service, String name) {
        if(service==null)
            throw new IllegalArgumentException("service proxy is null for ${name}")

        StopHandler stopHandler = new StopHandler();
        stopHandler.destroyService(service, name, System.out)
    }

    def shutdown() {

        for(Webster w : websters)
            w.terminate();
        /* Make sure all services are terminated */
        for(Process p : processes) {
            p.destroy()
        }
    }

    def maybeRunHarvester() {
        if(testConfig.runHarvester()) {
            ProvisionMonitor monitor
            ServiceItem[] items = getServiceItems(ProvisionMonitor.class)
            if(items.length==0) {
                println "===> No discovered ProvisionMonitor instances, "+
                        "cannot deploy HarvesterAgents"
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
                    println "Cannot load [${opstringFile}], "+
                            "Unable to deploy Harvester support."
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
            println "Harvester:: Number of physical machines = ${hosts.size()}"
            long timeout = 1000*60
            long duration = 0
            while(h.agentsHandledCount<hosts.size()) {
                Thread.sleep(1000)
                duration += 1000
                if(duration >= timeout)
                    break
            }
            println "Number of HarvesterAgents handled = ${h.agentsHandledCount}"
            h.unadvertise()
        }
    }

    def getHarvester(DiscoveryManagement dMgr) {
        return new HarvesterBean(dMgr)
    }
    
    def exec(String starter) {
        String classpath = "${PropertyHelper.expandProperties(config.manager.execClassPath)}"
        String jvmOptions = "${PropertyHelper.expandProperties(config.manager.jvmOptions)}"
        if(config.manager.inheritOptions)
            jvmOptions = JVMOptionChecker.getJVMInputArgs(jvmOptions)
        jvmOptions = jvmOptions+' -D'+Constants.RIO_TEST_EXEC_DIR+'='+System.getProperty("user.dir")
        String logFile = null
        String mainClass = "${config.manager.mainClass}"
        if(config.manager.log.size()>0) {
            String service = starter.substring(starter.lastIndexOf("-")+1)
            service = service.substring(0, service.indexOf("."))
            logFile = Util.replace("${config.manager.log}", '${service}', service)
            logFile = "${PropertyHelper.expandProperties(logFile)}"
            File f = new File(logFile)
            if(f.exists()) {
                int i=1
                /* Get extension */
                String ext = f.getName()
                ext = ext.substring(ext.lastIndexOf("."))
                File dir = f.getParentFile()
                while(f.exists()) {
                    f = new File(dir, service+"-${i++}$ext")
                }
                logFile = f.canonicalPath
            } else {
                File parent = f.getParentFile()
                if(!parent.exists())
                    parent.mkdirs()
            }
            File createdLogsFile = getAndCreateCreatedLogsFile()
            createdLogsFile.append(logFile+'\n')
            logger.info "Output will be sent to [${logFile}]"
        }                

        String cmdLine = getJava()+' '+jvmOptions+' -cp '+classpath+' '+mainClass+' '+starter

        //if(logger.isLoggable(Level.FINE))
            logger.info "Exec command line: ${cmdLine}"
        //else
        //    logger.info "Exec starter ${starter}"
        Process process = Runtime.runtime.exec(cmdLine)
        if(logFile) {
            def fos= new FileOutputStream(logFile)
            process.consumeProcessOutputStream(fos)
            process.consumeProcessErrorStream(fos)
        } else {
            process.consumeProcessOutputStream(System.out)
            process.consumeProcessErrorStream(System.err)
        }
        processes.add(process)
    }

    def waitForDeployment(OperationalStringManager mgr) {
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
                    logger.info "Waiting for service ${elem.name} to be deployed. " +
                                "Planned [${elem.planned}], deployed [${numDeployed}]"
                    numDeployed = mgr.getServiceBeanInstances(elem).length
                    deploy.put(elem, numDeployed)
                } else {
                    deployed += elem.planned
                    logger.info "Service ${elem.name} is deployed. " +
                                "Planned [${elem.planned}], deployed [${numDeployed}]"
                }
            }
            if(sleptFor==ServiceMonitor.MAX_TIMEOUT)
                break;
            if (deployed < total) {
                Thread.sleep(1000)
                sleptFor += 1000
            }
        }

        if(sleptFor>=ServiceMonitor.MAX_TIMEOUT && deployed < total)
            throw new TimeoutException("Timeout waiting for service to be deployed");
    }

    int countLookups() {
        return countServices(ServiceRegistrar.class)
    }

    int countMonitors() {
        return countServices(ProvisionMonitor.class)
    }

    int countCybernodes() {
        return countServices(Cybernode.class)
    }

    def countServices(Class serviceInterface) {
        long t0 = System.currentTimeMillis()
        ServiceItem[] items = getServiceItems(serviceInterface)
        logger.info "Discovered $items.length instances of ${serviceInterface.name}, "+
                    "elapsed time: ${(System.currentTimeMillis()-t0)} millis"
        return items.length
    }

    ServiceItem[] getServiceItems(Class serviceInterface) {
        def classes = [serviceInterface]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, null)
        ServiceItem[] items = serviceDiscoveryManager.lookup(template,
                                                             Integer.MAX_VALUE,
                                                             null)
        return items
    }

    def getServices(Class serviceInterface) {
        return getServices(serviceInterface, null)
    }

    def getServices(Class serviceInterface, String name) {
        def classes = [serviceInterface]
        def attrs = null
        if(name!=null)
            attrs = [new Name(name)]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, (Entry[])attrs)
        ServiceItem[] items = serviceDiscoveryManager.lookup(template,
                                                             Integer.MAX_VALUE,
                                                             null)
        def services = []
        for(int i=0; i<items.length; i++)
            services << items[i].service
        return services
    }

    def waitForService(Class serviceInterface) {
        return waitForService(serviceInterface, null)
    }

    def waitForService(String serviceName) {
        return waitForService(null, serviceName)
    }

    def waitForService(Class serviceInterface, String name) {
        def classes = null
        if(serviceInterface!=null)
            classes = [serviceInterface]
        def attrs = null
        if(name!=null)
            attrs = [new Name(name)]
        ServiceTemplate template = new ServiceTemplate(null, (Class[])classes, (Entry[])attrs)
        def service
        StringBuffer sb = new StringBuffer()
        if(serviceInterface!=null)
            sb.append(serviceInterface.name)
        if(name!=null) {
            if(sb.length()>0)
                sb.append(", ")
            sb.append("name: ").append(name)
        }
        logger.info "Waiting for ${sb.toString()} to be discovered"
        long t0 = System.currentTimeMillis()
        ServiceItem serviceItem = serviceDiscoveryManager.lookup(template, null, 60000)
        if (serviceItem == null) {
            throw new TimeoutException("Unable to discover service ${serviceInterface.name}")
        }
        service = serviceItem.service
        logger.info "${sb.toString()} has been discovered, elapsed time: "+
                    "${(System.currentTimeMillis()-t0)} millis"
        return service
    }

    def getJava() {
        StringBuilder jvmBuilder = new StringBuilder();
        jvmBuilder.append(System.getProperty("java.home"));
        jvmBuilder.append(File.separator);
        jvmBuilder.append("bin");
        jvmBuilder.append(File.separator);
        jvmBuilder.append("java");
        jvmBuilder.append(" ");
        return jvmBuilder.toString();
    }

    def loadManagerConfig() {
        String defaultManagerConfig =
            "jar:file:${Utils.getRioHome()}/lib/rio-test.jar!/default-manager-config.groovy"
        String mgrConfig = System.getProperty('org.rioproject.test.manager.config',
                                              defaultManagerConfig)
        logger.info "Using TestManager configuration ${mgrConfig}"
        URL url
        try {
            url = new URL(mgrConfig)
        } catch (MalformedURLException e) {
            File mgrConfigFile = new File(mgrConfig)
            if(!mgrConfigFile.exists())
                throw new RuntimeException(
                    "Cannot load [${mgrConfig}], it is not found in it's default "+
                    "location of [${defaultManagerConfig}], or "+
                    "your location of the file is incorrect. This file is needed "+
                    "by the Rio TestManager to initialize. Check the setting of the "+
                    "org.rioproject.test.manager.config system property, and/or copy this "+
                    "file from the Rio project distribution to the location "+
                    "mentioned above.", e)
            url = mgrConfigFile.toURI().toURL()
        }

        def config = new ConfigSlurper().parse(url)
        return config
    }

    def getCreatedLogsFile() {
        File logs = new File(System.getProperty('java.io.tmpdir')+File.separator+".rio"+File.separator+"test-logs")
        return logs
    }

    def getAndCreateCreatedLogsFile() {
        File dir = new File(System.getProperty('java.io.tmpdir')+File.separator+".rio")
        if(!dir.exists())
            dir.mkdirs()
        File logs = new File(dir, "test-logs")
        if(!logs.exists())
            logs.createNewFile()
        return logs
    }

    private boolean starterConfigOk(def starter) {
        return starter instanceof String
    }
}