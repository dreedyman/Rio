import org.rioproject.net.HostUtil

/**
 * This configuration is used as a base class for activatable service
 * configuration extensions
 */
class ActivatableConfig {
    String rioHome = System.getProperty('RIO_HOME')    
    String policyFile = "${rioHome}/policy/policy.all"
    String port = '9010'
    String host = HostUtil.getHostAddressFromProperty('java.rmi.server.hostname')

    String getGroupClasspath() {
        return rioHome+'/lib/boot.jar'+File.pathSeparator+
               rioHome+'/lib/start.jar'+File.pathSeparator+
               rioHome+'/lib/jsk-platform.jar'+File.pathSeparator+
               rioHome+'/lib/groovy-all.jar'
    }

    String getCodebase() {
        return "http://${host}:${port}/group-dl.jar"
    }

    String getGroupPersistenceDirectory(String name) {
        return "${rioHome}/logs/${name}"
    }

    String[] getSystemProperties() {
        /* System properties for the activation group */
        def sysProperties = ['RIO_HOME', rioHome,
                             'RIO_NATIVE_DIR', System.getProperty('RIO_NATIVE_DIR'),
                             'org.rioproject.home', rioHome,
                             'RIO_LOG_DIR', System.getProperty('RIO_LOG_DIR'),
                             'java.protocol.handler.pkgs', 'net.jini.url']
        return (String[]) sysProperties
    }

    String getServiceCodebase(List jars) {
        StringBuffer buffer = new StringBuffer()
        jars.each { jar->
            buffer.append("http://${host}:${port}/${jar} ")
        }
        return buffer.toString()
    }
}
