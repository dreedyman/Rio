/*
 * This configuration is used to start a ProvisionMonitor, Cybernode, Webster and a Lookup Service
 */

import org.rioproject.boot.ServiceDescriptorUtil
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.resolver.maven2.Repository

@Component('com.sun.jini.start')
class StartAllConfig {
    ServiceDescriptor[] getServiceDescriptors() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = System.getProperty('RIO_HOME')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioHome+'/deploy', ';',
                            m2Repo]

        String policyFile = rioHome+'/policy/policy.all'
        def monitorConfigs = [rioHome+'/config/common.groovy',
                              rioHome+'/config/monitor.groovy']
        def reggieConfigs = [rioHome+'/config/common.groovy',
                             rioHome+'/config/reggie.groovy']
        def cybernodeConfigs = [rioHome+'/config/common.groovy',
                                rioHome+'/config/cybernode.groovy',
                                rioHome+'/config/compute_resource.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', websterRoots as String[]),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfigs as String[]),
            ServiceDescriptorUtil.getMonitor(policyFile, monitorConfigs as String[]),
            ServiceDescriptorUtil.getCybernode(policyFile, cybernodeConfigs as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }
}