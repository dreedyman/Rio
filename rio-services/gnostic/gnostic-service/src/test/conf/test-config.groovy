/*
 * Configuration file for integration test cases
 */
ITScalingCounterTest {
   groups = "ITScalingCounterTest"
   numCybernodes = 1
   numMonitors = 1
}

ITMonitorRulesTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITMonitorRulesTest"
    opstring = 'src/test/opstring/testOpstring.groovy'
    autoDeploy = true
}

ITSpaceUtilizationTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITSpaceUtilizationTest"
    //opstring = 'src/test/opstring/testspace.groovy'
    //autoDeploy = true
}

ITSystemUtilizationTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITSystemUtilizationTest"
    opstring = 'src/test/opstring/testOpstring2.groovy'
    autoDeploy = true
}

ITNotificationUsingArtifactTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITNotificationUsingArtifactTest"
}

