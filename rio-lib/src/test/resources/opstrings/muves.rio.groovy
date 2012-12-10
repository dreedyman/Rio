import net.jini.space.JavaSpace05
import java.util.logging.Level

def String getCodebase() {
    return 'http://'+InetAddress.getLocalHost().getHostAddress()+":9000"
}

deployment(name:'Muves') {

    codebase getCodebase() 

    include 'outrigger_lite.groovy'

    groups '${user.name}'

    /*
     * The configuration for the services is loaded as a classpath resources.
     * All services in this opstring use the same configuration file. Declaring
     * the configuration globally allows all services to "inherit" this
     * declaration
     */
    configuration file: 'classpath:gomez.config'

    /*
     * This association results in a dynamic proxy being injected to the
     * Worker and Job Monitor service beans. The opposed attribute also
     * ensures that the service beans declared here are provisioned in a
     * different Cybernode then the Task Space.
     *
     * The strategy is used to ensure invocations made to a result space are
     * not made if the task space is running out of memory
     */
    association (type:'opposed', serviceType: JavaSpace05.name,
                 name: 'Task Space', property: 'taskSpace') {

        management proxy: "net.gomez.provider.space.SpaceProxy",
                   strategy: 'org.rioproject.associations.strategy.Utilization',
                   serviceDiscoveryTimeout: 1, serviceDiscoveryTimeoutUnits: 'seconds'
    }

    /*
     * This association is used to ensure that the service beans declared
     * here are provisioned in a different Cybernode the the Result Space.
     *
     * The strategy is used to ensure invocations made to a result space are
     * not made if the result space is running out of memory
     */
    association (type: "opposed", serviceType: JavaSpace05.name,
                 name: "Result Space") {
        management proxy: "net.gomez.provider.space.SpaceProxy",
                   strategy: 'org.rioproject.associations.strategy.Utilization'
    }

    /*
     * insert the xml content from this file into the generated opstring
     */
    //insert 'jars.xml'

    service(name: "Worker") {
        interfaces {
            classes "net.gomez.worker.Worker"
            resources ref:"client.jars"
        }
        implementation(class: "net.gomez.worker.WorkerImpl") {
            resources ref:"impl.jars"
            resources "lib/jscience.jar"
        }
        parameters {
            parameter name: "doSpin", value: "false"
        }
        maintain 3
        maxPerMachine 1, type: "physical"
    }

    service(name: "Job Monitor") {
        interfaces {
            classes "net.gomez.jobmonitor.JobMonitor"
            resources ref:"client.jars"
        }
        implementation(class: "net.gomez.jobmonitor.JobMonitorImpl") {
            resources ref:"impl.jars"
        }
        maintain 1
    }

    service(name: "Lurch") {
        interfaces {
            classes "net.gomez.lurch.Lurch"
            resources ref:"client.jars"
        }
        implementation(class: "net.gomez.lurch.LurchImpl") {
            resources ref:"impl.jars"
        }

        sla(id: 'waitq', high: 30000) {
            policy handler: 'net.kahona.dispatcher.SimScalingHandler', max: 2
        }

        maintain 1

        association type: "uses",
                    serviceType: "net.gomez.jobmonitor.JobMonitor",
                    name: "Job Monitor",
                    property: "jobMonitor"

        association type: "uses",
                    serviceType: "org.rioproject.watch.Watchable",
                    name: "Job Monitor",
                    property: "watchables"
    }

    service(name: "Fester") {
        interfaces {
            classes "net.gomez.fester.Fester"
            resources ref:"client.jars"
        }

        implementation(class: "net.gomez.fester.FesterImpl") {
            resources ref:"impl.jars"
            resources "lib/jscience.jar",
                      "lib/activemq/activemq-core-5.0.0.jar",
                      "lib/activemq/geronimo-jms_1.1_spec-1.0.jar",
                      "lib/activemq/jaxb-api-2.0.jar",
                      "lib/activemq/jaxb-impl-2.0.3.jar",
                      "lib/activemq/commons-logging-1.1.jar"
        }

        maintain 1
    }

    service(name: "GeometryService", fork: 'yes', type: 'fixed', jvmArgs: '-Xmx512m', environment: "LD_LIBRARY_PATH=bar  DYLD_LIBRARY_PATH=foo" ) {
        interfaces {
            classes "mil.army.arl.geometryservice.GeometryService"
            resources ref:"client.jars"
            resources "lib/arl/arl-support.jar",
                      "lib/jscience.jar",
                      "lib/arl/arl-brlcadservice-dl.jar"
        }

        implementation(class: "mil.army.arl.brlcadservice.impl.BrlcadServiceImpl") {
            resources ref:"impl.jars"
            resources "lib/arl/arl-support.jar",
                      "lib/jscience.jar",
                      "lib/log4j-1.2.11.jar",
                      "lib/arl/arl-brlcadservice.jar"
        }


        software(type: "NativeLibrarySupport",
                 name:'LinuxBrlcadServer') //{
            //install source: getCodebase()+'/lib/arl/brlcadserverlibs.zip',
            //        target:'brlcadserverlibs',
            //        unarchive: true
        //}

        configuration '''
            import mil.army.arl.brlcadservice.datatypes.BRLCADEntry;
                GeometryService {
                BRLCADEntry = new BRLCADEntry( 1, 8, "t62.g", "component");
            }
        '''

        logging {
            logger('mil.army.arl.brlcadservice', Level.INFO)
        }

        maintain 3
        maxPerMachine 1, type: "physical"
        comment "BrlcadService"
    }
}