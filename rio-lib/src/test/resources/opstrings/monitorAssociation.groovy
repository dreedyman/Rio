
import org.rioproject.config.Constants

deployment(name:'Foo') {
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* Declares the artifacts required for deployment. Note the 'dl'
     * classifier used for the 'download' jar */
    artifact id:'service', 'net.example.association:association-service:1.0-SNAPSHOT'
    artifact id:'service-dl', 'net.example.association:association-api:1.0-SNAPSHOT'

    /*
     * Declare the service to be deployed. The number of instances deployed
     * defaults to 1. If you require > 1 instances change as needed
     */
    service(name: 'Foo') {
        interfaces {
            classes 'net.example.association.Foo'
            artifact ref:'service-dl'
        }
        implementation(class:'net.example.association.FooImpl') {
            artifact ref:'service'
        }

        association(type: "uses",
                    serviceType: "org.rioproject.monitor.ProvisionMonitor",
                    property: "monitor", name: "Blutarsky",
                    matchOnName: false)
        maintain 1
    }
}