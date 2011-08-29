deployment(name:'Outrigger') {
    groups('${user.name}')

    resources id:'outrigger-dl', 'outrigger-dl.jar', 'jsk-dl.jar', 'rio-lookup-entry.jar'
    resources id:'outrigger-impl', 'outrigger.jar'

    service(name:'Mahalo') {
        interfaces {
            classes 'net.jini.core.transaction.server.TransactionManager'
            resources 'mahalo-dl.jar', 'jsk-dl.jar'
        }
        implementation(class: 'com.sun.jini.mahalo.TransientMahaloImpl') {
            resources 'mahalo.jar'
        }
        maintain 1
        maxPerMachine 1
    }

    service(name:'Outrigger') {
        interfaces {
            classes 'net.jini.space.JavaSpace'
            resources 'outrigger-dl.jar', 'jsk-dl.jar'
        }
        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            resources 'outrigger.jar'
        }
        maintain 1
        maxPerMachine 1
    }
}
