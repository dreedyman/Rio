/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, Cybernode, Webster and a Jini Lookup Service
 */

import org.rioproject.boot.ServiceDescriptorUtil
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.config.maven2.Repository

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
        String monitorConfig = rioHome+'/config/monitor.groovy'
        String reggieConfig = rioHome+'/config/reggie.groovy'
        def cybernodeConfigs = [rioHome+'/config/cybernode.groovy',
                                rioHome+'/config/compute_resource.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', (String[])websterRoots),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfig),
            ServiceDescriptorUtil.getMonitor(policyFile, monitorConfig),
            ServiceDescriptorUtil.getCybernode(policyFile, (String[])cybernodeConfigs)
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }
}