/*
 * Configuration file for integration test cases
 */
ITSpringBeanDeployTest {
    groups = "SpringBean"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/springbean.groovy'
    autoDeploy = true
}

