deployment(name:'Level 2') {
    groups 'rio'
    service(name: 'Level2') {
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
