import org.rioproject.config.Constants

deployment(name:'Hello World Spring Example') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    spring(name: 'Hello', config: 'hello-spring.xml') {
        interfaces {
            classes 'org.rioproject.examples.springbean.Hello'
            artifact 'org.rioproject.examples.springbean:springbean-api:2.1'
        }
        implementation(class: 'org.rioproject.examples.springbean.service.HelloImpl') {
            artifact 'org.rioproject.examples.springbean:springbean-service:2.1'
        }
        maintain 1
    }
}
