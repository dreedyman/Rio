/*
 * Configuration file for integration test cases
 */
ITTomcatDeployTest {
    groups = "Tomcat"
    numCybernodes = 1
    numMonitors = 1
    opstring = 'src/main/opstring/tomcat.groovy'
    autoDeploy = true
}

