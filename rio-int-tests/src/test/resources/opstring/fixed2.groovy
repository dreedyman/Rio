package opstring

import org.rioproject.config.Constants

static String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Fixed Service Test2') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Fixey McFixFix Foo',
            fork: 'yes',
            type: 'fixed',
            jvmArgs: '-Xms8m -Xmx2000m -Dfoo=bar -Dssleep=5',
            environment:'LD_LIBRARY_PATH=$EG_HOME/native/blahblah') {

        interfaces {
            classes 'org.rioproject.test.simple.Fork'
            resources 'classes/groovy/test/'
        }
        implementation(class: 'org.rioproject.test.simple.ForkImpl') {
            resources 'classes/groovy/test/'
        }

        maintain 1

    }
}