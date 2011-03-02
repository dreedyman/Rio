/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster
 */

import org.rioproject.config.Component

import org.rioproject.boot.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor;

@Component('com.sun.jini.start')
class StartMonitorConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String m2Home = "${System.getProperty("user.home")}/.m2"
        String rioHome = System.getProperty('RIO_HOME')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',     ';',
                            rioHome+'/lib',
                            m2Home+'/repository']

        String policyFile = rioHome+'/policy/policy.all'
        def monitorConfigs = [rioHome+'/config/common.groovy', rioHome+'/config/monitor.groovy']
        //String reggieConfig = rioHome+'/config/reggie.groovy'

        def serviceDescriptors = [
            ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
            //ServiceDescriptorUtil.getLookup(policyFile, reggieConfig),
            ServiceDescriptorUtil.getMonitor(policyFile, monitorConfigs  as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
