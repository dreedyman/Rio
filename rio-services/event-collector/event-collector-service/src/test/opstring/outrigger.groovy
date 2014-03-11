deployment(name:'Outrigger') {
    groups('${user.name}')

    artifact id: 'outrigger-dl', "org.apache.river:outrigger-dl:2.2.2"
    artifact id: 'outrigger-impl', "org.apache.river:outrigger:2.2.2"

    service(name:'Outrigger') {
        interfaces {
            classes 'net.jini.space.JavaSpace'
            artifact ref: 'outrigger-dl'
        }
        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            artifact ref: 'outrigger-impl'
        }
        maintain 1
        maxPerMachine 1
    }
}
