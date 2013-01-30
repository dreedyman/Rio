import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

import java.util.concurrent.TimeUnit

def String getCodebase() {
    return 'http://'+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010"
}

deployment(name:'association stuff') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Darrel') {
        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'test-classes/'
        }
        associations {
            association(name: 'Add',
                        serviceType:'org.rioproject.test.associations.Dummy',
                        type: 'requires',
                        property: 'dummy') {
                management inject: 'eager',
                           strategy: 'org.rioproject.associations.strategy.Utilization',
                           serviceDiscoveryTimeout: 15,
                           serviceDiscoveryTimeoutUnits: TimeUnit.SECONDS
            }
        }
        maintain 1
    }

}
