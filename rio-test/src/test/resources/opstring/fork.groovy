package opstring
import org.rioproject.config.Constants


def String getCodebase() {
    return System.getProperty(Constants.WEBSTER)
}

deployment(name:'Forked Service Test') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    /* Use a funky name to make sure illegal chars get converted. The resulting name
     * will be:   "T62___W_FL___SK_" */
    service(name: 'T62 /(W FL & SK)', 
            fork: 'yes',
            type: 'fixed',
            jvmArgs: '-Xms8m -Xmx2000m -Dfoo=bar',
            environment:'LD_LIBRARY_PATH=$EG_HOME/native/blahblah') {

        interfaces {
            classes 'org.rioproject.test.simple.Fork'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.simple.ForkImpl') {
            resources 'test-classes/'
        }

        maintain 1
        
    }
}
