import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jmx.JMXConfigurator
import ch.qos.logback.classic.jmx.MBeanUtil
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

import java.lang.management.ManagementFactory

import static ch.qos.logback.classic.Level.*

/* Scan for changes every minute. */
scan()

/* Set up JMX  */
def jmxConfigurator() {
    def contextName = context.name
    def objectNameAsString = MBeanUtil.getObjectNameFor(contextName, JMXConfigurator.class)
    def objectName = MBeanUtil.string2ObjectName(context, this, objectNameAsString)
    def platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
    if (!MBeanUtil.isRegistered(platformMBeanServer, objectName)) {
        JMXConfigurator jmxConfigurator = new JMXConfigurator((LoggerContext) context, platformMBeanServer, objectName)
        try {
            platformMBeanServer.registerMBean(jmxConfigurator, objectName)
        } catch (all) {
            addError("Failed to create MBean", all)
        }
    }
}

jmxConfigurator()

/*
 * Naming pattern for the output file:
 *
 * a) The output file is placed in the directory defined by the "RIO_LOG_DIR" System property
 * b) With a name based on the "org.rioproject.service" System property.
 * c) The return value from ManagementFactory.getRuntimeMXBean().getName(). This value is expected to have the
 * following format: pid@hostname. If the return includes the @hostname, the @hostname is stripped off.
 */
def getLogLocationAndName() {
    String logDir = System.getProperty("RIO_LOG_DIR")
    if (!logDir.endsWith(File.separator))
        logDir = logDir+File.separator
    String name = ManagementFactory.getRuntimeMXBean().getName();
    String pid = name;
    int ndx = name.indexOf("@");
    if(ndx>=1) {
        pid = name.substring(0, ndx);
    }
    return "$logDir${System.getProperty("org.rioproject.service")}-$pid"
}

def appenders = []

/*
 * Only add the CONSOLE appender if we have a console
 */
if (System.console() != null) {
    appender("CONSOLE", ConsoleAppender) {
        if(!System.getProperty("os.name").startsWith("Windows"))
            withJansi = true
        encoder(PatternLayoutEncoder) {
            pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} [%thread] - %msg%n"
        }
    }
    appenders << "CONSOLE"
}

/*
 * Only add the rolling file appender if we are logging for a service
 */
if (System.getProperty("org.rioproject.service")!=null) {
    def serviceLogFilename = getLogLocationAndName()

    appender("ROLLING", RollingFileAppender) {
        file = serviceLogFilename+".log"
        rollingPolicy(TimeBasedRollingPolicy) {

            /* Rollover daily */
            fileNamePattern = "${serviceLogFilename}-%d{yyyy-MM-dd}.%i.log"

            /* Or whenever the file size reaches 5MB */
            timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
                maxFileSize = "5MB"
            }

            /* Keep 5 archived logs */
            maxHistory = 5

        }
        encoder(PatternLayoutEncoder) {
            pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} [%thread] - %msg%n"
        }
    }
    appenders << "ROLLING"
}

/* Set up loggers */
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


root(INFO, appenders)
