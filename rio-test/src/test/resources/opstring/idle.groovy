package opstring

import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

import java.util.concurrent.TimeUnit

def String getCodebase() {
    return 'http://'+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010"
}

deployment(name:'Idle Service Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    undeploy idle:10, TimeUnit.SECONDS

    service(name: 'Idle') {

        interfaces {
            classes 'org.rioproject.test.idle.Idle'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.idle.IdleImpl') {
            resources 'test-classes/'
        }

        maintain 3
        
    }
}
