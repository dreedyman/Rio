package opstring

import org.rioproject.config.Constants

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Simple Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'classes/groovy/test/'
        }
        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            resources 'classes/groovy/test/'
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
