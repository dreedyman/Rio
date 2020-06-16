import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator

context = new LevelChangePropagator()
context.resetJUL = true

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}

//logger("org.rioproject.rmi.ResolvingLoader", INFO)
logger("org.rioproject.net.HostUtil", INFO)
logger("org.rioproject.impl.client", TRACE)
logger("sun.rmi.loader", INFO)
logger ("unknown.jul.logger", OFF)
logger("net.jini.config", OFF)
logger("java.io.serialization", INFO)
logger("sun.rmi", INFO)

root(DEBUG, ["STDOUT"])