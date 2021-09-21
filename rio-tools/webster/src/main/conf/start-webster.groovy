/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster, JMX Connector Service and
 * a Jini Lookup Service
 */

import org.rioproject.config.Component

import org.rioproject.start.util.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('org.rioproject.start')
class StartWebsterConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('rio.home')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioHome+'/deploy']

        String policyFile = rioHome+'/policy/policy.all'

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010',
                                             (String[])websterRoots,
                                             true)
        ]
        return (ServiceDescriptor[])serviceDescriptors
    }

}
