package opstring
import org.rioproject.config.Constants

import java.util.concurrent.TimeUnit

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Idle Service Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    undeploy idle:10, TimeUnit.SECONDS

    service(name: 'Idle') {
        interfaces {
            classes 'org.rioproject.test.idle.Idle'
            resources 'integrationTest/'
        }
        implementation(class: 'org.rioproject.test.idle.IdleImpl') {
            resources 'integrationTest/'
        }

        maintain 3
        
    }
}
