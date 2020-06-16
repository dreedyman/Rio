package opstring

import org.rioproject.config.Constants

deployment(name: 'Spaced') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'outrigger-dl', "org.apache.river:outrigger-dl:2.2.2"
    artifact id: 'outrigger-impl', "org.apache.river:outrigger:2.2.2"

    service(name: 'Spaced Out') {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            artifact ref: 'outrigger-dl'
        }
        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            artifact ref: 'outrigger-impl'
        }

        maintain 1

    }
}
