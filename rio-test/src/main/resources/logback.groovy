import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.core.ConsoleAppender
import static ch.qos.logback.classic.Level.*

context = new LevelChangePropagator()
context.resetJUL = true

appender("CONSOLE", ConsoleAppender) {
    if(!System.getProperty("os.name").startsWith("Windows"))
        withJansi = true
    encoder(PatternLayoutEncoder) {
        pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} [%thread] - %msg%n%rEx"
    }
}

/* Set up loggers */
logger("org.rioproject.test", INFO)
logger("unknown.jul.logger", OFF)
logger("net.jini.jeri.connection.mux", OFF)

root(INFO, ["CONSOLE"])
