deployment(name:'Outer') {

    include 'level1_multi.groovy'

    groups 'rio'
    service(name: 'Outer') {
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
