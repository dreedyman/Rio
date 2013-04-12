import org.rioproject.test.TestConfig

/*
* Configuration file for Rio test cases
*/
SimpleDeployTest {
    groups = "SimpleDeployTest"
    //locators = ''
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/simple_opstring.groovy'
    //harvest = true
    //testManager = new org.rioproject.test.TestManager(true)
}

AssociationManagementTest {
    groups = "AssociationManagementTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/associationManagement.groovy'
    autoDeploy = true
}

SLAThresholdEventNotificationTest {
    groups = "SLAThresholdEventNotificationTest"
    //locators = ''
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/slathresholdeventnotify.groovy'
    autoDeploy = true
    //testManager = new org.rioproject.test.TestManager(true)
}

SystemWatchAccessorTest {
    groups = "SystemWatchAccessorTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/scaling_service_test.groovy'
    autoDeploy = true
}

ForkedServicePreDestroyTest {
    groups = "ForkedServicePreDestroyTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/fork.groovy'
    autoDeploy = true
    //harvest = true
}

BackupTest {
    groups = "BackupTest"
}

SimpleForkTest {
    groups = "SimpleForkTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/fork.groovy'
    autoDeploy = true
    //harvest = true
}

DeployMapTest {
    groups = "DeployMapTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/simple_opstring.groovy'
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
    groups = "HarvesterTest"
    numLookups = 1
    numMonitors = 1
    numCybernodes = 2
    opstring = 'src/test/resources/opstring/harvester.groovy'
    autoDeploy = true
}

UndeployTest {
    groups = "UndeployTest"
}

ScalingServiceTest {
    groups = "ScalingServiceTest"
}

JULServiceLogEventHandlerTest {
    groups = "JULServiceLogEventHandlerTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    loggingSystem = TestConfig.LoggingSystem.JUL
}

LogbackServiceLogEventAppenderTest {
    groups = "LogbackServiceLogEventAppenderTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}

PermGenTest {
    groups = "PermGenTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/simple_opstring.groovy'
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
    groups = "LoadArtifactAsOpStringTest"
    opstring = 'org.rioproject.test:deploy-oar-test:1.0'
}

AssociationFutureTest {
    groups = "AssociationFutureTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}

HasExtraAttributesTest {
    groups = "HasExtraAttributesTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
    opstring = 'src/test/resources/opstring/space.groovy'
    autoDeploy = true
}

AdvertiseLifecycleTest {
    groups = "AdvertiseLifecycleTest"
    numCybernodes = 1
    numMonitors = 1
    numLookups = 1
}
