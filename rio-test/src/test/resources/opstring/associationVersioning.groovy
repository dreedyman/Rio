package opstring

import org.rioproject.config.Constants
import org.rioproject.entry.VersionEntry
import org.rioproject.net.HostUtil

def String getCodebase() {
    return 'http://'+HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)+":9010"
}

deployment(name:'association stuff') {

    codebase getCodebase()

    groups System.getProperty('org.rioproject.groups')

    service(name: 'Darrel') {
        attributes(new VersionEntry("1.2"))

        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'test-classes/'
        }

        maintain 1
    }

    service(name: 'His Brother Darrel') {
        attributes(new VersionEntry("2.1"))

        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'test-classes/'
        }

        maintain 1
    }

    service(name: 'His Other Brother Darrel') {
        attributes(new VersionEntry("2.2, 3.1"))

        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'test-classes/'
        }

        maintain 1
    }

    service(name: 'His Other Brother Darrel Jr') {

        interfaces {
            classes('org.rioproject.test.associations.Dummy')
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.associations.DummyImpl') {
            resources 'test-classes/'
        }

        maintain 1
    }

}
