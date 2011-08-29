deployment(name:'Space') {
    groups('${user.name}')

    resources id:'outrigger-dl', 'outrigger-dl.jar', 'jsk-dl.jar', 'rio-lookup-entry.jar'
    resources id:'outrigger-impl', 'outrigger.jar'

    service(name:'Result Space') {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            resources ref:'outrigger-dl'
        }
        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            resources ref:'outrigger-impl'
        }
        maintain 1
        maxPerMachine 1
        configuration '''
            import net.jini.jrmp.JrmpExporter;
            com.sun.jini.outrigger {
                serverExporter = new JrmpExporter();
            }
        '''
    }

    service(name:'Task Space') {
        interfaces {
            classes 'net.jini.space.JavaSpace05'
            resources ref:'outrigger-dl'
        }
        implementation(class: 'com.sun.jini.outrigger.TransientOutriggerImpl') {
            resources ref:'outrigger-impl'
        }
        maintain 1
        maxPerMachine 1
        association name:'Result Space', type:'opposed'
        configuration '''
            import net.jini.jrmp.JrmpExporter;
            com.sun.jini.outrigger {
                serverExporter = new JrmpExporter();
            }
        '''
    }
}
