/*
 * This configuration is used by the com.sun.jini.start utility to start a
 * ProvisionMonitor, including an embedded Webster, JMX Connector Service and
 * a Jini Lookup Service
 */

/*
 * A configuration file for Jetty
 */
jetty {
    roots = [System.properties['user.dir']]
    putDirectory = System.properties['user.dir']
    secure = true
    port = 9020
}
