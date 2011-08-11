
['Level_1-A', 'Level_1-B'].each { name ->
    deployment(name:"${name}") {

        include 'level2.groovy'

        groups 'rio'
        service(name: 'Level 1') {
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
}
