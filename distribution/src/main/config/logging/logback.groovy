import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN
import static ch.qos.logback.classic.Level.OFF

appender("STDOUT", ConsoleAppender) {
    if(!System.getProperty("os.name").startsWith("Windows"))
        withJansi = true
    encoder(PatternLayoutEncoder) {
        pattern = "%highlight(%-5level) | %d{HH:mm:ss.SSS} %logger{36} [%thread] - %msg%n"
    }
}

logger("org.rioproject.cybernode", DEBUG)
logger("org.rioproject.cybernode.loader", DEBUG)
logger("org.rioproject.config", INFO)
logger("org.rioproject.resources.servicecore", INFO)
logger("org.rioproject.system", DEBUG)
logger("org.rioproject.cybernode.ServiceBeanLoader", INFO)
logger("org.rioproject.system.measurable", INFO)
logger("org.rioproject.jsb", INFO)

logger("org.rioproject.monitor", DEBUG)
logger("org.rioproject.monitor.sbi", DEBUG)
logger("org.rioproject.monitor.provision", DEBUG)
logger("org.rioproject.monitor.selector", OFF)
logger("org.rioproject.monitor.services", DEBUG)
logger("org.rioproject.resolver.aether", OFF)

logger("org.rioproject.gnostic", INFO)

logger("net.jini.discovery.LookupDiscovery", OFF)
logger("net.jini.lookup.JoinManager", OFF)

logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)
root(INFO, ["STDOUT"])
