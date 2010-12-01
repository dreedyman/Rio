/*
 * Configuration file for integration test cases
 */
ITHelloEventDeployTest {
    groups = "HelloEvent"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/events.groovy'
    autoDeploy = true
}

