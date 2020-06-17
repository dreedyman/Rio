package opstring

import org.rioproject.config.Constants

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'ServiceLogEvent Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Simple Logging Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'classes/groovy/test/'
        }
        implementation(class: 'org.rioproject.test.simple.logging.LoggingSimpleImpl') {
            resources 'classes/groovy/test/'
        }

        maintain 1
    }
}
