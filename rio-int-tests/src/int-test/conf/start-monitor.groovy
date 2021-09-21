
import org.rioproject.config.Component
import org.rioproject.resolver.maven2.Repository
import org.rioproject.security.SecureEnv
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.start.util.ServiceDescriptorUtil

@Component('org.rioproject.start')
class StartMonitorConfig {
    final boolean secure

    StartMonitorConfig() {
        secure = SecureEnv.setup()
    }

    static String[] getMonitorConfigArgs(String rioHome) {
        def configArgs = [rioHome+'/config/common.groovy', rioHome+'/config/monitor.groovy']
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String m2Repo = Repository.getLocalRepository().absolutePath
        String rioHome = System.getProperty('rio.home')
        String rioTestHome = System.getProperty('rio.test.home')

        def websterRoots = [rioHome+'/lib-dl', ';',
                            rioHome+'/lib',    ';',
                            rioHome+'/deploy', ';',
                            m2Repo
        ]

        String policyFile = rioHome+'/policy/policy.all'

        def serviceDescriptors = [
                //ServiceDescriptorUtil.getWebster(policyFile, '0', websterRoots as String[]),
                ServiceDescriptorUtil.getJetty('0', websterRoots as String[], secure),
                ServiceDescriptorUtil.getMonitor(policyFile, getMonitorConfigArgs(rioHome))
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }
}
