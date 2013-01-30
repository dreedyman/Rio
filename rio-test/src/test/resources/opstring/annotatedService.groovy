package opstring
import org.rioproject.config.Constants

deployment(name: 'Annotation Test') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    service(name: 'Annotated-1') {
        interfaces {
            classes 'org.rioproject.test.bean.ServiceInterface'
            resources 'test-classes/'
        }

        implementation(class: 'org.rioproject.test.bean.Service') {
            resources 'test-classes/'
        }

        associations {
            association(name: 'Annotated-2', type: 'requires', property: 'service') {
                management inject: 'eager'
            }
        }

        maintain 1
    }

    service(name: 'Annotated-2') {
        interfaces {
            classes 'org.rioproject.test.bean.ServiceInterface'
            resources 'test-classes/'
        }

        implementation(class: 'org.rioproject.test.bean.Service') {
            resources 'test-classes/'
        }

        maintain 1

    }
}
