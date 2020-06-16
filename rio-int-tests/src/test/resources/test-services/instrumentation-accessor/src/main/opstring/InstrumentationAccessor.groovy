
deployment(name:'Instrumentation Accessor') {

    artifact id:'service', 'org.rioproject.test.instrumentation-accessor:instrumentation-accessor-service:2.0'
    artifact id:'service-dl', 'org.rioproject.test.instrumentation-accessor:instrumentation-accessor-api:2.0'

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Instrumentation Accessor') {
        interfaces {
            classes 'org.rioproject.test.instrumentation.API'
            artifact ref:'service-dl'
        }
        implementation(class: 'org.rioproject.test.instrumentation.Impl') {
            artifact ref:'service'
        }

        maintain 1

    }
}
