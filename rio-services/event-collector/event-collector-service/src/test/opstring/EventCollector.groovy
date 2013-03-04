import org.rioproject.config.Constants

String getConfigEntry() {
    String entry
    String value = System.getProperty('event.config')
    //if(value.equals('foo'))
        entry = '''
            @org.rioproject.config.Component('org.rioproject.eventcollector.service')
            class PersistentEventManagerConfig {
                File getPersistentDirectoryRoot() {
                    String userDir = System.getProperty("user.dir")
                    return new File(userDir, "target/events")
                }
            }
        '''
    //else
    //    entry = ""

    return entry
}

deployment(name:'Event Collector') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    artifact id: 'service-dl',   'org.rioproject.event-collector:event-collector-proxy:5.0-M3'
    artifact id: 'service-impl', 'org.rioproject.event-collector:event-collector-service:5.0-M3'

    configuration getConfigEntry()

    service(name:'Bones') {
        interfaces {
            classes 'org.rioproject.eventcollector.api.EventCollector'
            artifact ref: 'service-dl'
        }
        implementation(class: 'org.rioproject.eventcollector.service.EventCollectorImpl') {
            artifact ref: 'service-impl'
        }
        maintain 1
    }
}
