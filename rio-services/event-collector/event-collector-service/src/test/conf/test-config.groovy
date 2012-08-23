/*
 * Configuration file for integration test cases
 */

EventCollectorITest {
    groups = "EventCollectorITest"
    numMonitors = 1
    numCybernodes = 1
    opstring = "src/test/opstring/EventCollector.groovy"
    autoDeploy = true
}
