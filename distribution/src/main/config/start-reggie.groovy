/*
 * This configuration is used to start a Lookup Service, and an embedded Webster
 */

import org.rioproject.config.Component

import org.rioproject.boot.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('com.sun.jini.start')
class StartReggieConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        def websterRoots = [rioHome+'/lib-dl', ';', rioHome+'/lib']

        String policyFile = rioHome+'/policy/policy.all'
        def reggieConfig = [rioHome+'/config/common.groovy', rioHome+'/config/reggie.groovy']

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '10000', websterRoots as String[]),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfig as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
