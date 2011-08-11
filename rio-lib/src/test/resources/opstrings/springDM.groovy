import java.lang.management.ManagementFactory

deployment(name: 'Spring DM') {
  groups('rio')

    serviceExec(name: 'DM Server') {
        software(name: 'Spring DM', version: '1.0.0', removeOnDestroy: true) {
            install source: 'file://${RIO_HOME}/deploy/springsource-dm-server-1.0.0.RELEASE.zip',
                    target: 'springsource-dm-server',
                    unarchive: true
        }

        execute command: 'bin/startup.sh'

        sla(id:'thread-count', high: 1000) {
            policy type: 'notify'
            monitor name: 'Thread Count',
                    objectName: ManagementFactory.THREAD_MXBEAN_NAME,
                    attribute: 'ThreadCount', period: 5000
        }
        /*
        sla(id:'thread-deadlock', high: 1000) {
            policy type: 'notify'
            monitor name: 'Thread Deadlock',
                    objectName: ManagementFactory.THREAD_MXBEAN_NAME,
                    attribute: 'ThreadCount', period: 5000
        }
        */
        maintain 1
    }

}