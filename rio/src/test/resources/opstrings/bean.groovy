deployment(name:'Hello World Example') {
    groups 'rio'
    service(name: 'Hello', fork: 'yes') {
        interfaces {
            classes 'bean.Hello'
            resources 'bean/lib/bean-dl.jar'
        }
        implementation(class: 'bean.service.HelloImpl') {
            resources 'bean/lib/bean.jar'
        }
        maintain 1
        serviceLevelAgreements {
            systemRequirements {
                utilization id:'System', high:0.7
                utilization id:'CPU', high:0.8
            }
        }
    }
}
