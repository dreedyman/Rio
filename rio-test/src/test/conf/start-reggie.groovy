/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster, JMX Connector Service and
 * a Jini Lookup Service
 */

import org.rioproject.config.Component

import org.rioproject.boot.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('com.sun.jini.start')
class StartReggieConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')
        String rioTestHome = System.getProperty('RIO_TEST_HOME')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioTestHome+'/target/']

        String policyFile = rioHome+'/policy/policy.all'
        String reggieConfig = rioHome+'/config/reggie.groovy'

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '9010', (String[])websterRoots),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfig)
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
