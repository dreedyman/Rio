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
statusListener(NopStatusListener)

def appenders = []

appender("Console-Appender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%-5level %d{HH:mm:ss.SSS} [%thread] %logger{36} - %msg%n%rEx"
    }
}
appenders << "Console-Appender"

/* Set up loggers */
logger("org.rioproject.cybernode.service", INFO)
logger("org.rioproject.cybernode.loader", DEBUG)
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

logger("net.jini", TRACE)
logger("com.sun.jini", TRACE)
logger("net.jini.lookup.JoinManager", OFF)
logger("org.rioproject.resolver.aether.util.ConsoleRepositoryListener", WARN)

root(INFO, appenders)


