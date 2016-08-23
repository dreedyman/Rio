package opstring

deployment(name:'ServiceLogEvent Test') {

    artifact id:'service', 'org.rioproject.test.simple:simple-logging-service:LATEST'
    artifact id:'service-dl', 'org.rioproject.test.simple:simple-api:LATEST'

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Logging Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
             artifact ref:'service-dl'
        }
        implementation(class: 'org.rioproject.test.simple.LoggingSimpleImpl') {
             artifact ref:'service'
        }

        maintain 1
    }
}
