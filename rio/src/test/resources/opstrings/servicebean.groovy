deployment(name:'ServiceBean Example') {
    groups('rio')
    service(name: 'Hello') {
        interfaces {
            classes('servicebean.Hello')
            resources('servicebean/lib/servicebean-dl.jar')
        }
        implementation(class: 'servicebean.service.HelloImpl') {
            resources('servicebean/lib/servicebean.jar')
        }
        maintain 1
    }
}
