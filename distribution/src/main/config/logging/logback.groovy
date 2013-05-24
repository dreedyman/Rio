/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

import java.lang.management.ManagementFactory

import static ch.qos.logback.classic.Level.*

/* Scan for changes every minute. */
scan()

jmxConfigurator()

/*
 * Utility to check if the passed in string ends with a File.separator
 */
def checkEndsWithFileSeparator(String s) {
    if (!s.endsWith(File.separator))
        s = s+File.separator
    return s
}

/*
 * Naming pattern for the output file:
 *
 * a) The output file is placed in the directory defined by the "RIO_LOG_DIR" System property
 * b) With a name based on the "org.rioproject.service" System property.
 * c) The return value from ManagementFactory.getRuntimeMXBean().getName(). This value is expected to have the
 * following format: pid@hostname. If the return includes the @hostname, the @hostname is stripped off.
 */
def getLogLocationAndName() {
    String logDir = checkEndsWithFileSeparator(System.getProperty("RIO_LOG_DIR"))
    String name = ManagementFactory.getRuntimeMXBean().getName();
    String pid = name;
    int ndx = name.indexOf("@");
    if(ndx>=1) {
        pid = name.substring(0, ndx);
    }
    return "$logDir${System.getProperty("org.rioproject.service")}-$pid"
}

/*
 * Get the location of the watch.log. If the "RIO_WATCH_LOG_DIR" System property is not set, use
 * the "RIO_HOME" System property appended by /logs
 */
def getWatchLogDir() {
    String watchLogDir = System.getProperty("RIO_WATCH_LOG_DIR")
    if(watchLogDir==null) {
        watchLogDir = checkEndsWithFileSeparator(System.getProperty("RIO_HOME"))+"logs"
    }
    return checkEndsWithFileSeparator(watchLogDir)
}

def appenders = []

/*
 * Only add the CONSOLE appender if we have a console
 */
if (System.console() != null) {
    appender("CONSOLE", ConsoleAppender) {
        if(!System.getProperty("os.name").startsWith("Windows")) {
            withJansi = true

            encoder(PatternLayoutEncoder) {
                pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
            }
        } else {
            encoder(PatternLayoutEncoder) {
                pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
            }
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

            /* Or whenever the file size reaches 10MB */
            timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
                maxFileSize = "10MB"
            }

            /* Keep 5 archived logs */
            maxHistory = 5

        }
        encoder(PatternLayoutEncoder) {
            pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
        }
    }
    appenders << "ROLLING"
}

/*
 * This method needs to be called if watch logging is to be used
 */
def createWatchAppender() {
    String watchLogName = getWatchLogDir()+"watches"
    appender("WATCH-LOG", RollingFileAppender) {
        /*
         * In prudent mode, RollingFileAppender will safely write to the specified file,
         * even in the presence of other FileAppender instances running in different JVMs
         */
        prudent = true

        rollingPolicy(TimeBasedRollingPolicy) {

            /* Rollover daily */
            fileNamePattern = "${watchLogName}-%d{yyyy-MM-dd}.%i.log"

            /* Or whenever the file size reaches 10MB */
            timeBasedFileNamingAndTriggeringPolicy(SizeAndTimeBasedFNATP) {
                maxFileSize = "10MB"
            }

            /* Keep 5 archived logs */
            maxHistory = 5

        }
        encoder(PatternLayoutEncoder) {
            pattern = "%msg%n"
        }
    }
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
logger("org.rioproject.associations", INFO)

logger("org.rioproject.monitor", DEBUG)
logger("org.rioproject.monitor.sbi", DEBUG)
logger("org.rioproject.monitor.provision", DEBUG)
logger("org.rioproject.monitor.selector", OFF)
logger("org.rioproject.monitor.services", DEBUG)
logger("org.rioproject.monitor.DeploymentVerifier", INFO)
logger("org.rioproject.monitor.InstantiatorResource", INFO)
logger("org.rioproject.monitor.managers.FixedServiceManager", INFO)
logger("org.rioproject.resolver.aether", OFF)

logger("org.rioproject.rmi.ResolvingLoader", OFF)

logger("org.rioproject.gnostic", INFO)
logger("org.rioproject.gnostic.drools", INFO)
logger("org.rioproject.gnostic.DroolsCEPManager", INFO)
logger("org.rioproject.config.GroovyConfig", INFO)

logger("net.jini.discovery.LookupDiscovery", OFF)
logger("net.jini.lookup.JoinManager", OFF)
logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)

root(INFO, appenders)

/*
 * The following method call to create the Watch appender must be uncommented if you want to have watches log to a
 * file
 */
//createWatchAppender()

/* The following loggers are system watch loggers. When set to debug they will use the WATCH-LOG appender,
 * and have as output values being logged for these particular watches. Uncomment out the loggers you would like
 * to have logged.
 *
 * If you have watches in your service that you want put into a watch-log, then add them as needed, with the
 * logger name of:
 *     "watch.<name of your watch>"
 */
/*
logger("watch.CPU", DEBUG, ["WATCH-LOG"], false)
logger("watch.CPU (Proc)", DEBUG, ["WATCH-LOG"], false)
logger("watch.System Memory", DEBUG, ["WATCH-LOG"], false)
logger("watch.Process Memory", DEBUG, ["WATCH-LOG"], false)
logger("watch.Perm Gen", DEBUG, ["WATCH-LOG"], false)
*/


