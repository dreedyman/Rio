/*
 * This configuration is used to start a service that will exec a single service bean
 */

import org.rioproject.boot.RioServiceDescriptor
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.boot.ServiceDescriptorUtil

@Component('com.sun.jini.start')
class StartServiceBeanExecConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        String codebase = ServiceDescriptorUtil.getCybernodeCodebase()
        String classpath = ServiceDescriptorUtil.getCybernodeClasspath()
        
        String policyFile = rioHome + '/policy/policy.all'
        def configArgs = [rioHome + '/config/common.groovy',
                          rioHome + '/config/cybernode.groovy',
                          rioHome + '/config/compute_resource.groovy']

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
