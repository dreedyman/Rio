package opstrings

import net.jini.lookup.entry.Name
import org.rioproject.entry.VersionEntry

import java.util.concurrent.TimeUnit

deployment(name: 'Extra attributes test') {

    groups('rio')

    undeploy idle:30, TimeUnit.SECONDS

    spring(name: 'Hello', config: 'hello-spring.xml') {

        attributes (new Name("Foo"), new VersionEntry("1.2"))

        interfaces {
            classes 'springbean.Hello'
            resources 'springbean/lib/springbean-dl.jar'
        }
        implementation(class: 'springbean.service.HelloImpl') {
            resources 'springbean/lib/springbean.jar'
        }
        maintain 1
    }
}
