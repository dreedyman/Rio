/*
 * Configuration file for integration test cases
 */
ITScalingCounterTest {
   groups = "ITScalingCounterTest"
   numCybernodes = 1
   numMonitors = 1
}

ITScannerIntervalTest {
    groups = "ITScannerIntervalTest"
    numCybernodes = 1
    numMonitors = 1
    opstring = 'src/main/conf/gnostic.groovy'
    autoDeploy = true
}

ITMonitorRulesTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITMonitorRulesTest"
    opstring = 'src/test/conf/test-opstring.groovy'
    autoDeploy = true
}

ITForkedSpaceUtilizationTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITForkedSpaceUtilizationTest"
    opstring = 'src/test/conf/testforkedspace.groovy'
    autoDeploy = true
}

ITSpaceUtilizationTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITSpaceUtilizationTest"
    opstring = 'src/test/conf/testspace.groovy'
    autoDeploy = true
}

ITSystemUtilizationTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITSystemUtilizationTest"
    opstring = 'src/test/conf/test-opstring2.groovy'
    autoDeploy = true
}

ITNotificationUsingArtifactTest {
    numCybernodes = 1
    numMonitors = 1
    groups = "ITNotificationUsingArtifactTest"
}

