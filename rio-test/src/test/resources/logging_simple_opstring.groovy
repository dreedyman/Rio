
def String getCodebase() {
    return 'http://'+InetAddress.getLocalHost().getHostAddress()+":9010"
}

deployment(name:'ServiceLogEvent Test') {

    codebase getCodebase()

    groups '${org.rioproject.groups}'

    service(name: 'Simple Logging Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.simple.LoggingSimpleImpl') {
            resources 'test-classes/'
        }

        maintain 1
    }
}
