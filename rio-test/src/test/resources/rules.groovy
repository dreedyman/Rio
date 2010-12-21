import org.rioproject.config.Constants

deployment(name: 'Gnostic') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    artifact id: 'service', 'org.rioproject:gnostic:4.1'
    artifact id: 'service-dl', 'org.rioproject:gnostic:dl:4.1'

    service(name: 'Gnostic') {
        interfaces {
            classes 'org.rioproject.gnostic.Gnostic'
            artifact ref: 'service-dl'
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