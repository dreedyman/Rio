
/*
* Configuration file for Rio test cases
*/
SimpleDeployTest {
    groups = "SimpleDeployTest"
    //locators = ''
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/simple_opstring.groovy'
    //harvest = true
    //testManager = new org.rioproject.test.TestManager(true)
}

SLAThresholdEventNotificationTest {
    groups = "SLAThresholdEventNotificationTest"
    //locators = ''
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/slathresholdeventnotify.groovy'
    autoDeploy = true
    //testManager = new org.rioproject.test.TestManager(true)
}

SystemWatchAccessorTest {
    groups = "SystemWatchAccessorTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/scaling_service_test.groovy'
    autoDeploy = true
}

ForkedServicePreDestroyTest {
    groups = "ForkedServicePreDestroyTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/fork.groovy'
    autoDeploy = true
    //harvest = true
}

SimpleForkTest {
    groups = "SimpleForkTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/fork.groovy'
    autoDeploy = true
    //harvest = true
}

DeployMapTest {
    groups = "DeployMapTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/simple_opstring.groovy'
    autoDeploy = true
}

MaintainTest {
    groups = "MaintainTest"
    numLookups = 1
    numMonitors = 1
    //harvest = true
}

MaxPerMachineTest {
    groups = "MaxPerMachineTest"
    numLookups = 1
    numMonitors = 1
    //harvest = true
}

HarvesterTest {
    numLookups = 1
    numMonitors = 1
    numCybernodes = 2
    opstring = 'src/test/resources/harvester.groovy'
    autoDeploy = true
}

UndeployTest {
    groups = "UndeployTest"
}

ScalingServiceTest {
    groups = "ScalingServiceTest"
}

ReportIntervalTest {

}

ServiceEventLogTest {
    groups = "ServiceEventLogTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}

PermGenTest {
    groups = "PermGenTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'org.rioproject.test:simple:2.0'
}

OutOfMemoryTest {
    groups = "OutOfMemoryTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}

EnlistReleaseTest {
    groups = "EnlistReleaseTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}

LoadArtifactAsOpStringTest {
    opstring = 'org.rioproject.test:deploy-oar-test:1.0'
}

AssociationFutureTest {
    groups = "AssociationFutureTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}
