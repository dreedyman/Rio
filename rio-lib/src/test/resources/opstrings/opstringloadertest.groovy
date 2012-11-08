deployment(name:'opstringloader test') {

    service(name: "foo") {

        implementation(class: 'bean.service.HelloImpl')

        maintain 1
    }

}
