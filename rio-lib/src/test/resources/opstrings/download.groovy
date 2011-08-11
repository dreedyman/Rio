def String getCodebase() {
    return 'http://' + InetAddress.getLocalHost().getHostAddress() + ":9010"
}

deployment(name: 'Download Test') {
    groups('rio')

    service(name: 'Foo') {

        [   'ant': '/lib/ant/ant.jar',
            'zantlr': '/lib/antlr/antlr-2.7.6.jar',
            'aopalliance': '/lib/aopalliance/aopalliance.jar',
            'zasm': '/lib/asm/asm-2.2.3.jar',
            'aspectj': '/lib/aspectj/aspectjrt.jar',
            'zaxis': '/lib/axis/axis.jar'
        ].each {entry ->
            software(name: entry.key,
                     removeOnDestroy: ((String)entry.key).startsWith("a"),
                     classpathresource: ((String)entry.key).startsWith("a")) {
                install source: getCodebase() + entry.value,
                        target: 'platform-lib',
                        unarchive: false
            }
        }

        interfaces {
            classes 'bean.Hello'
            resources 'bean/lib/bean-dl.jar'
        }

        implementation(class: 'bean.service.HelloImpl') {
            resources 'bean/lib/bean.jar'
        }

        maintain 1

    }
}
