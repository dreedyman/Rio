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
import java.lang.management.ManagementFactory
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.AsyncAppender
import static ch.qos.logback.classic.Level.*

context = new LevelChangePropagator()
context.resetJUL = true

/* Scan for changes every minute. */
scan()

jmxConfigurator()

statusListener(NopStatusListener)

/*
 * Utility to check if the passed in string ends with a File.separator
 */
static String checkEndsWithFileSeparator(String s) {
    if (!s.endsWith(File.separator))
        s = s+File.separator
    s
}

/*
 * Naming pattern for the output file:
 *
 * a) The output file is placed in the directory defined by the "rio.log.dir" System property
 * b) With a name based on the "org.rioproject.service" System property.
 */
static String getLogLocationAndName() {
    String logDir = checkEndsWithFileSeparator(System.getProperty("rio.log.dir"))
    String name = ManagementFactory.getRuntimeMXBean().getName();
    String pid = name;
    int ndx = name.indexOf("@");
    if(ndx>=1) {
        pid = name.substring(0, ndx);
    }
    "$logDir${System.getProperty("org.rioproject.service")}-$pid"
}
def appenders = []

if (System.console() != null) {
    appender("Console-Appender", ConsoleAppender) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            withJansi = true

            encoder(PatternLayoutEncoder) {
                pattern = "%d{yyyy-MM-dd} %d{HH:mm:ss.SSS} %highlight(%-5level) %magenta([%thread]) %cyan(%-36.36logger{36}) : %msg%n%rEx"
            }
        } else {
            encoder(PatternLayoutEncoder) {
                pattern = "%d{yyyy-MM-dd} %d{HH:mm:ss.SSS} %-5level [%thread] %-36.36logger{36} : %msg%n%rEx"
            }
        }
    }
    appenders << "Console-Appender"
}

/*
 * Only add the rolling file appender if we are logging for a service
 */
if (System.getProperty("org.rioproject.service")!=null) {
    def serviceLogFilename = getLogLocationAndName()

    appender("RollingFile-Appender", RollingFileAppender) {
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
            pattern = "%d{yyyy-MM-dd} %d{HH:mm:ss.SSS} %-5level [%thread] %-36.36logger{36} : %msg%n%rEx"
        }
    }

    appender("Async-Appender", AsyncAppender) {
        appenderRef("RollingFile-Appender")
    }

    appenders << "Async-Appender"
}

/* Set up loggers */
logger("org.rioproject.cybernode.service", INFO)
logger("org.rioproject.cybernode.loader", INFO)
logger("org.rioproject.config", INFO)
logger("org.rioproject.resources.servicecore", INFO)
logger("org.rioproject.system", DEBUG)
logger("org.rioproject.exec", DEBUG)
logger("org.rioproject.impl.exec", INFO)
logger("org.rioproject.impl.fdh", INFO)
logger("org.rioproject.impl.container.ServiceBeanLoader", INFO)
logger("org.rioproject.system.measurable", INFO)
logger("org.rioproject.impl.servicebean", INFO)
logger("org.rioproject.associations", INFO)
logger("org.rioproject.impl.servicebean", INFO)

logger("org.rioproject.tools.webster", OFF)

logger("org.rioproject.monitor.service", INFO)

logger("org.rioproject.resolver.aether", OFF)
logger("org.rioproject.resolver.aether.ProjectWorkspaceReader", WARN)

logger("org.rioproject.rmi.ResolvingLoader", OFF)
logger("org.rioproject.config.GroovyConfig", INFO)

logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)

logger("org.eclipse", INFO)

root(INFO, appenders)


