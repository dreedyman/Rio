package opstring

import org.rioproject.RioVersion
import org.rioproject.test.simple.SimpleEntry

static def getEntries() {
    return new SimpleEntry(RioVersion.VERSION)
}

deployment(name:'Simple Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {

        attributes getEntries(); using 'classes/groovy/test/'

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
            }'''
    }
}
