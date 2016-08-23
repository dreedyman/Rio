package opstring

deployment(name:'Simple Test') {

    artifact id:'service', 'org.rioproject.test.simple:simple-service:LATEST'
    artifact id:'service-dl', 'org.rioproject.test.simple:simple-api:LATEST'

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact ref:'service-dl'
        }
        implementation(class: 'org.rioproject.test.simple.SimpleImpl') {
            artifact ref:'service'
        }

        parameters {
            parameter name: "food", value: "fries"
        }
        maintain 1        

        configuration '''
            import org.rioproject.config.Component
            @Component('bean')
            class bean {
                String getFood() {
                    return 'fries'
                }
            }
        '''
    }
}
