package opstrings

import net.jini.lookup.entry.Name
import org.rioproject.entry.VersionEntry

deployment(name: 'Extra attributes test') {

    groups('rio')

    spring(name: 'Hello', config: 'hello-spring.xml') {

        attributes(new Name("Foo"), new VersionEntry("banana"))

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
