import org.rioproject.config.Constants
import org.rioproject.net.HostUtil

def String getCodebase() {
    return 'http://'+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010"
}

deployment(name:'Out Of Memory') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'OOME', fork:'yes') {
        interfaces {
            classes 'org.rioproject.test.memory.OutOfMemory'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.memory.OutOfMemoryServiceImpl') {
            resources 'test-classes/'
        }

        //faultDetectionHandler HeartbeatFaultDetectionHandler.class.getName()
        
        maintain 1
    }
}
