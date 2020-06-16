package opstring

deployment(name:'Simple Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact "org.rioproject:rio-int-tests:LATEST"
        }
        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            artifact "org.rioproject:rio-int-tests:LATEST"
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
