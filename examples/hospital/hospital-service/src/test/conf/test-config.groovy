/*
 * Configuration file for integration test cases
 */
ITHospitalDeployTest {
    groups = "Hospital"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/hospital.groovy'
    autoDeploy = true
}

