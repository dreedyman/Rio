/*
 * This configuration is used by the com.sun.jini.start utility to start a 
 * Cybernode, including an embedded Webster
 */

import org.rioproject.boot.ServiceDescriptorUtil
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor

@Component('com.sun.jini.start')
class StartAllPersistentConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioHome+'/deploy']

        String policyFile = rioHome+'/policy/policy.all'

        /* Configuration args for monitor, reggie and cybernode include the
         * class that overrides (extends) the base class declarations */
        def monitorConfigs = [rioHome+'/config/common.groovy',
                              rioHome+'/config/monitor.groovy',
                              rioHome+'/config/persistent_monitor.groovy']

        def reggieConfigs = [rioHome+'/config/reggie.groovy',
                            rioHome+'/config/persistent_reggie.groovy']

        def cybernodeConfigs = [rioHome+'/config/cybernode.groovy',
                                rioHome+'/config/persistent_cybernode.groovy',
                                rioHome+'/config/compute_resource.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', (String[])websterRoots),
            ServiceDescriptorUtil.getLookup(policyFile, (String[])reggieConfigs),
            ServiceDescriptorUtil.getMonitor(policyFile, (String[])monitorConfigs),
            ServiceDescriptorUtil.getCybernode(policyFile, (String[])cybernodeConfigs)
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}