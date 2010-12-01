/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * Lookup Service, and an embedded Webster
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
        String reggieConfig = rioHome+'/config/reggie.groovy'

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', (String[])websterRoots),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfig)
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
