deployment(name:'Hello World Example') {

    service(name: 'Hello') {
        interfaces {
            classes 'bean.Hello'
        }
        implementation(class: 'bean.service.HelloImpl')

        maintain 1

    }
}
