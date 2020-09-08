package opstring

import net.jini.lookup.entry.Location
import org.rioproject.config.Constants

static def getEntries() {
    Location location = new Location()
    location.room = "office"
    location
}

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Simple Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Simon') {

        attributes getEntries(); using 'net.jini:jsk-dl:2.2.2'

        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'integrationTest/'
        }

        implementation(class: 'org.rioproject.test.simple.service.SimpleImpl') {
            resources 'integrationTest/'
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
