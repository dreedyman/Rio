package opstring
import org.rioproject.config.Constants

import java.util.concurrent.TimeUnit

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'association stuff') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Darrel') {
        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'integrationTest/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'integrationTest/'
        }
        associations {
            association(name: 'Add',
                        serviceType:'org.rioproject.test.associations.Dummy',
                        type: 'requires',
                        property: 'dummy') {
                management inject: 'eager',
                           strategy: 'org.rioproject.impl.associations.strategy.Utilization',
                           serviceDiscoveryTimeout: 15,
                           serviceDiscoveryTimeoutUnits: TimeUnit.SECONDS
            }
        }
        maintain 1
    }

}
