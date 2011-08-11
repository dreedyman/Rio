deployment(name:'Hello World Example') {
    groups('rio')
    spring(name: 'Hello', config: 'hello-spring.xml') {
        interfaces {
            classes 'springbean.Hello'
            resources 'springbean/lib/springbean-dl.jar'
        }
        implementation(class: 'springbean.service.HelloImpl') {
            resources 'springbean/lib/springbean.jar'
        }
        maintain 1
    }
}
