import org.rioproject.config.Constants

deployment(name:'$service') {
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))

    /* Declares the artifacts required for deployment. Note the 'dl'
     * classifier used for the 'download' jar */
    artifact id:'service', '$groupId.$artifactId:${artifactId}-service:$version'
    artifact id:'service-dl', '$groupId.$artifactId:${artifactId}-api:$version'

    /*
     * Declare the service to be deployed. The number of instances deployed
     * defaults to 1. If you require > 1 instances change as needed
     */
    service(name: '$service') {
        interfaces {
            classes '$package.$artifactId.$service'
            artifact ref:'service-dl'
        }
        implementation(class:'$package.$artifactId.${service}Impl') {
            artifact ref:'service'
        }
        maintain 1
    }
}