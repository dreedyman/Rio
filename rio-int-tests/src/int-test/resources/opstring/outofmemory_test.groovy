package opstring
import org.rioproject.config.Constants

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Out Of Memory') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'OOME', fork:'yes') {
        interfaces {
            classes 'org.rioproject.test.memory.OutOfMemory'
            resources 'integrationTest/'
        }
        implementation(class: 'org.rioproject.test.memory.OutOfMemoryServiceImpl') {
            resources 'integrationTest/'
        }

        //faultDetectionHandler HeartbeatFaultDetectionHandler.class.getName()
        
        maintain 1
    }
}
