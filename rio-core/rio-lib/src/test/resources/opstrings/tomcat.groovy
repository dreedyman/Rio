package opstrings

deployment(name:'Tomcat Deploy') {
    groups('rio')
    serviceExec(name:'Tomcat') {
        software(name:'Tomcat', version:'6.0.16', removeOnDestroy: true) {
            install source:'https://elastic-grid.s3.amazonaws.com/tomcat/apache-tomcat-6.0.16.zip',
                    target:'${rio.home}/system/external/tomcat',
                    unarchive: true
            postInstall(removeOnCompletion: false) {
                execute command: '/bin/chmod +x ${rio.home}/system/external/tomcat/apache-tomcat-6.0.16/bin/*sh'
            }
        }

        data source: 'https://elastic-grid.s3.amazonaws.com/tomcat/sample.war',
             target: '${rio.home}/system/external/tomcat/apache-tomcat-6.0.16/webapps',
             unarchive: true, perms: 'ugo+rwx'
             
        execute inDirectory:'bin', command: 'catalina.sh start',  pidFile: "/tmp/tomcat.pid"
        maintain 1
    }
}
