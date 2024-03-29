
import org.rioproject.config.Component

import org.rioproject.security.SecureEnv
import org.rioproject.util.RioHome
import org.rioproject.start.util.ServiceDescriptorUtil
import com.sun.jini.start.ServiceDescriptor

@Component('org.rioproject.start')
class StartReggieConfig {
    final boolean secure

    StartReggieConfig() {
        secure = SecureEnv.setup()
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('rio.home')
        String rioTestHome = System.getProperty('rio.test.home')

        def websterRoots = [rioHome + '/lib-dl', ';',
                            rioHome + '/lib',    ';',
                            rioTestHome + '/build/']

        String policyFile = rioHome+'/policy/policy.all'
        def reggieConfigs = [rioHome+'/config/common.groovy', rioHome+'/config/reggie.groovy']

        def serviceDescriptors = [
            //ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
            ServiceDescriptorUtil.getJetty('0', websterRoots as String[], secure),
            ServiceDescriptorUtil.getLookup(policyFile, reggieConfigs as String[])
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}