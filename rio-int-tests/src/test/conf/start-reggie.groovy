/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster, JMX Connector Service and
 * a Jini Lookup Service
 */

import org.rioproject.config.Component

import org.rioproject.util.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('org.rioproject.start')
class StartReggieConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('rio.home')
        String rioTestHome = System.getProperty('rio.test.home')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioTestHome+'/build/']

        String policyFile = rioHome+'/policy/policy.all'
        def reggieConfigs = [rioHome+'/config/common.groovy', rioHome+'/config/reggie.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfigs as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
