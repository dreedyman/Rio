
deployment(name:'Locators') {

    locators 'machine1', 'machine2'

    service(name: 's1') {
        groups 'foo'
        interfaces {
            classes 'bean.Hello'
            resources 'bean/lib/bean-dl.jar'
        }
        implementation(class: 'bean.service.HelloImpl') {
            resources 'bean/lib/bean.jar'
        }
        maintain 1
    }

    /* By declaring the locator in the service we override the global definition */
    service(name: 's2') {
        locators 'machine3'
        interfaces {
            classes 'bean.Hello'
            resources 'bean/lib/bean-dl.jar'
        }
        implementation(class: 'bean.service.HelloImpl') {
            resources 'bean/lib/bean.jar'
        }
        maintain 1
    }
}
