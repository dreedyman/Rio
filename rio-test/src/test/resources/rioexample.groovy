import java.util.logging.Level

deployment(name:'Echo') {
    groups 'rio'
    systemRequirements(id:'Spring') {
        software name: 'Spring', version: '2.5'
    }
    service(name: 'Echo') {
        interfaces {
            classes 'tutorial.Echo'
            resources 'rio-example/lib/rio-example-dl.jar'
        }
        implementation(class: 'tutorial.EchoJSB') {
            resources 'rio-example/lib/rio-example.jar'
        }
        logging {
            logger 'tutorial', Level.ALL
            logger 'org.rioproject.jsb', Level.ALL
        }
        serviceLevelAgreements {
            systemRequirements ref: 'Spring'
            sla(id:'rate', low:0, high: 5) {
                policy type: 'scaling', max: 5, lowerDampener: 10000, upperDampener: 200
            }
            sla(id:'throughtput', high: 2) {
                policy type: 'notify'
            }
            sla(id:'backlog', low: 100, high: 500) {
                policy type: 'scaling', max: 10, lowerDampener: 3000, upperDampener: 3000
                monitor name: 'collector', property: 'count', period: 5000
            }
        }
        maintain 1
        maxPerMachine 3
    }
}
