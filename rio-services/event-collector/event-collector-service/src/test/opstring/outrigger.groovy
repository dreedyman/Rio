deployment(name:'Outrigger') {
    groups('${user.name}')

    artifact id: 'outrigger-dl',   'com.sun.jini:outrigger-dl:2.1.1'
    artifact id: 'outrigger-impl', 'com.sun.jini:outrigger:2.1.1'

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
