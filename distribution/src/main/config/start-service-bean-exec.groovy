/*
 * This configuration is used to start a service that will exec a single service bean
 */

import org.rioproject.boot.RioServiceDescriptor
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.boot.ServiceDescriptorUtil

@Component('com.sun.jini.start')
class StartServiceBeanExecConfig {

    String[] getConfigArgs(String rioHome) {
        def configArgs = [rioHome+'/config/common.groovy',
                          rioHome+'/config/forked_service.groovy',
                          rioHome+'/config/compute_resource.groovy']
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        String codebase = ServiceDescriptorUtil.getCybernodeCodebase()
        String classpath = ServiceDescriptorUtil.getCybernodeClasspath()
        
        String policyFile = rioHome + '/policy/policy.all'
        def configArgs = getConfigArgs(rioHome)

        def serviceDescriptors = [
            new RioServiceDescriptor(codebase,
                                     policyFile,
                                     classpath,
                                     'org.rioproject.cybernode.exec.ServiceBeanExec',
                                     (String[]) configArgs)
        ]

        return (ServiceDescriptor[]) serviceDescriptors
    }
}
