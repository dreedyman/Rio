
def String getCodebase() {
    return 'http://'+InetAddress.getLocalHost().getHostAddress()+":9010"
}

deployment(name:'Simple Test') {    

    codebase getCodebase()

    groups '${org.rioproject.groups}'

    service(name: 'Simple Simon') {
        interfaces {
            classes 'org.rioproject.test.simple.Simple'
            resources 'test-classes/'
        }
        implementation(class: 'org.rioproject.test.simple.SimpleImpl') {
            resources 'test-classes/'
        }

        parameters {
            parameter name: "food", value: "fries"
        }
        maintain 1        

        configuration '''
            import org.rioproject.config.Component
            @Component('bean')
            class bean {
                String getFood() {
                    return 'fries'
                }
            }
        '''
    }
}
