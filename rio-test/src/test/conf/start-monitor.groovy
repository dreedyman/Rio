/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster
 */

import org.rioproject.config.Component

import org.rioproject.util.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.resolver.maven2.Repository;

@Component('org.rioproject.start')
class StartMonitorConfig {

    String[] getMonitorConfigArgs(String rioHome) {
        def configArgs = [rioHome+'/config/common.groovy', rioHome+'/config/monitor.groovy']
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = System.getProperty('RIO_HOME')

        def websterRoots = [rioHome+'/lib-dl', ';',
                rioHome+'/lib',    ';',
                rioHome+'/deploy', ';',
                m2Repo]

        String policyFile = rioHome+'/policy/policy.all'

        def serviceDescriptors = [
                ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
                ServiceDescriptorUtil.getMonitor(policyFile, getMonitorConfigArgs(rioHome))
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }
}
