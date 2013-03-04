import org.rioproject.config.Constants

deployment(name: 'Gnostic') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject.gnostic:gnostic-service:5.0-M3'
    artifact id: 'service-api', 'org.rioproject.gnostic:gnostic-api:5.0-M3'

    service(name: 'Gnostic') {
        interfaces {
            classes 'org.rioproject.gnostic.Gnostic'
            artifact ref: 'service-api'
        }
        implementation(class: 'org.rioproject.gnostic.GnosticImpl') {
            artifact ref: 'service'
        }

        rules {
            rule{
                resource 'ScalingRuleHandler'
                ruleClassPath 'org.sample:model:1.0'
                ['Hello', 'Hello Event Consumer'].each {
                    serviceFeed(name: "$it") {
                        watches 'load'
                    }
                }                
            }
            rule{
                resource 'http://something:80/foo.drl, ScalingRuleHandler'
                serviceFeed(name: "Something") {
                    watches 'CPU, Memory'
                }
            }
        }

        maintain 1
    }
}