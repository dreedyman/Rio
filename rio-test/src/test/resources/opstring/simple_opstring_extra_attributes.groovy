package opstring

import org.rioproject.RioVersion

def artifactTable = ["api":"org.rioproject.test.simple:simple-api:LATEST",
                     "service":"org.rioproject.test.simple:simple-service:LATEST"]

def getEntries() {
    return new org.rioproject.test.simple.SimpleEntry(RioVersion.VERSION)
}

deployment(name:'Simple Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {

        attributes getEntries(); using artifactTable.api

        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact artifactTable.api
        }

        implementation(class: 'org.rioproject.test.simple.SimpleImpl') {
            artifact artifactTable.service
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
