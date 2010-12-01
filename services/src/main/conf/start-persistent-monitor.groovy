/*
 * This configuration is used by the com.sun.jini.start utility to start a 
 * persistent ProvisionMonitor, including an embedded Webster and
 * a Jini Lookup Service
 */

import org.rioproject.boot.ServiceDescriptorUtil
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor

@Component('com.sun.jini.start')
class StartPersistentMonitorConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        String m2Home = "${System.getProperty("user.home")}/.m2"
        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioHome+'/deploy', ';',
                            m2Home+'/repository']

        String policyFile = rioHome+'/policy/policy.all'

        def reggieConfigs = [rioHome+'/config/reggie.groovy',
                             rioHome+'/config/persistent_reggie.groovy']
                             
        def monitorConfigs = [rioHome+'/config/monitor.groovy',
                              rioHome+'/config/persistent_monitor.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', (String[])websterRoots),
            ServiceDescriptorUtil.getLookup(policyFile, (String[])reggieConfigs),
            ServiceDescriptorUtil.getMonitor(policyFile, (String[])monitorConfigs)
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}