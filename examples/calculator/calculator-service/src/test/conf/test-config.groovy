/*
 * Configuration file for integration test cases
 */
ITCalculatorDeployTest {
    groups = "Calculator"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/calculator.groovy'
    autoDeploy = true
}

ITCalculatorClientTest {
    groups = "CalculatorClient"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/calculator.groovy'
    autoDeploy = true
}