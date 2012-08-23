/*
 * This configuration is used to start the Event Collector, including an embedded Webster and
 * a Lookup Service
 */

import org.rioproject.config.Component

import org.rioproject.boot.ServiceDescriptorUtil;
import com.sun.jini.start.ServiceDescriptor
import org.rioproject.resolver.maven2.Repository
import org.rioproject.boot.RioServiceDescriptor

@Component('com.sun.jini.start')
class StartCollectorConfig {

    String[] getMusterConfigArgs(String rioHome) {
        def configArgs = [rioHome+'/config/common.groovy', rioHome+'/config/collector.groovy']
        return configArgs as String[]
    }

    ServiceDescriptor[] getServiceDescriptors() {
        String rioHome = System.getProperty('RIO_HOME')

        String policyFile = rioHome+'/policy/policy.all'

        StringBuilder pathBuilder = new StringBuilder()
        pathBuilder.append(rioHome).append(File.separator).append("lib").append(File.separator).append("event-collector-service.jar")

        def serviceDescriptors = [
                new RioServiceDescriptor("artifact:org.rioproject.event-collector/event-collector-proxy/5.0-SNAPSHOT",
                                         policyFile,
                                         pathBuilder.toString(),
                                         "org.rioproject.eventcollector.service.EventCollectorImpl",
                                         getMusterConfigArgs(rioHome))
        ]

        return (ServiceDescriptor[])serviceDescriptors
    }

}
