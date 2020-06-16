package opstring

deployment(name:'ServiceLogEvent Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Logging Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
             artifact "org.rioproject:rio-int-tests:LATEST"
        }
        implementation(class: 'org.rioproject.test.simple.logging.LoggingSimpleImpl') {
             artifact "org.rioproject:rio-int-tests:LATEST"
        }

        maintain 1
    }
}
