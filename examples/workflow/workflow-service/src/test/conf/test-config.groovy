/*
 * Configuration file for integration test cases
 */
ITWorkflowDeployTest {
    groups = "Workflow"
    numCybernodes = 1
    numMonitors = 1
    opstring = '../src/main/opstring/workflow.groovy'
    autoDeploy = true
}

