import org.rioproject.config.Constants

deployment(name: 'Spaced') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    artifact id: 'outrigger-dl', "com.sun.jini:outrigger-dl:2.1.1;http://www.rio-project.org/maven2"
    artifact id: 'outrigger-impl', "com.sun.jini:outrigger:2.1.1"

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
