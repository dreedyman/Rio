import java.util.logging.Level

def String getCodebase() {
    return 'http://'+InetAddress.getLocalHost().getHostAddress()+":9010"
}

deployment(name:'SLA Threshold Event Notification') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'SLA Threshold Event Producer') {
        interfaces {
            classes 'org.rioproject.test.sla.SLAThresholdEventProducer'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.sla.SLAThresholdEventProducerImpl') {
            resources 'test-classes/'
        }

        sla (id:'messageCount', high:3, low:1){
            policy type: 'notify', lowerDampener: 10, upperDampener: 10
            monitor(name: 'count', property: 'count', period: 1000)
        }

        maintain 1

        logging {
            logger('org.rioproject.sla', Level.FINEST)
            logger('org.rioproject.event', Level.FINEST)
        }
    }
}

