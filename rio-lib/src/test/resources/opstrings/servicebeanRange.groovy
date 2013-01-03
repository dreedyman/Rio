package opstrings

deployment(name:'ServiceBean Example') {
    groups('rio')

    (1..50).each { n ->

        service(name: 'Hello-'+n) {
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
}
