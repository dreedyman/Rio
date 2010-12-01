/*
 * Configuration file for integration test cases
 */
IT${service}DeployTest {
    groups = "${service}Test"
    numCybernodes = 1
    numMonitors = 1
    //numLookups = 1
    opstring = '../src/main/opstring/${rootArtifactId}.groovy'
    autoDeploy = true
    //harvest = true
}

