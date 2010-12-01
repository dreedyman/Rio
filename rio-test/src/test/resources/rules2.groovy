import org.rioproject.config.Constants

deployment(name: 'Gnostic') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    
    rules {
        rule {
            resource 'ScalingRuleHandler'
            ruleClassPath 'org.sample:model:1.0'
            ['Hello', 'Hello Event Consumer'].each {
                serviceFeed(name: "$it") {
                    watches 'load'
                }
            }
        }
        rule {
            resource 'http://something:80/foo.drl, ScalingRuleHandler'
            serviceFeed(name: "Something") {
                watches 'CPU, Memory'
            }
        }
    }
}