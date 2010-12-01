import org.rioproject.config.Constants

deployment(name: 'Tomcat Deploy') {
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    serviceExec(name: 'Tomcat') {

        /*
         * This declaration will remove the downloaded Tomcat distribution
         * when the Tomcat service is terminated (undeployed and/or
         * administratively stopped using Rio).
         *
         * If you want to keep the installed software (rather than overwrite
         * it each time), modify the declaration below to include:
         *
         * overwrite: 'no', removeOnDestroy: false
         */
        software(name: 'Tomcat', version: '6.0.16', removeOnDestroy: true) {
            install source: 'https://elastic-grid.s3.amazonaws.com/tomcat/apache-tomcat-6.0.16.zip',
                    target: '${RIO_HOME}/system/external/tomcat',
                    unarchive: true
            postInstall(removeOnCompletion: false) {
                execute command: '/bin/chmod +x ${RIO_HOME}/system/external/tomcat/apache-tomcat-6.0.16/bin/*sh'
            }
        }

        execute(inDirectory: 'bin', command: 'catalina.sh run') {
            environment {
                property name: "CATALINA_OPTS", value: "-Dcom.sun.management.jmxremote"
            }
        }

        sla(id:'ThreadPool', high: 100) {
            policy type: 'notify'
            monitor name: 'Tomcat Thread Pool',
                    objectName: "Catalina:name=http-8080,type=ThreadPool",
                    attribute: 'currentThreadsBusy', period: 5000
        }


        maintain 1
    }
}