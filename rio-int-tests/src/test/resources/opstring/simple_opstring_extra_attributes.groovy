package opstring

import org.rioproject.RioVersion

def artifactTable = ["ref":"org.rioproject:rio-int-tests:LATEST"]

def getEntries() {
    return new org.rioproject.test.simple.SimpleEntry(RioVersion.VERSION)
}

deployment(name:'Simple Test') {

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {

        attributes getEntries(); using artifactTable.api

        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            artifact artifactTable["ref"]
        }

        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            artifact artifactTable["ref"]
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
