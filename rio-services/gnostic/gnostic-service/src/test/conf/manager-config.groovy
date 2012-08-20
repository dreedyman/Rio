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

import org.rioproject.config.Constants

/*
* Configuration properties for the Gnostic
*/
manager {
    execClassPath =
        '${RIO_HOME}${/}lib${/}rio-start.jar${:}${RIO_HOME}${/}lib${/}resolver-api.jar${:}${RIO_HOME}${/}lib/${/}start.jar${:}${JAVA_HOME}${/}lib${/}tools.jar${:}${RIO_HOME}${/}lib${/}groovy-all.jar'

    inheritOptions = true

    /* Get the directory that the logging FileHandler will create the service log.  */
    String logExt = System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))
    String opSys = System.getProperty('os.name')
    String rootLogDir = opSys.startsWith("Windows")?'${java.io.tmpdir}':'/tmp'
    String name = System.getProperty('user.name')

    log = "${rootLogDir}/${name}/logs/${logExt}/"

    jvmOptions='''
        -javaagent:${RIO_HOME}${/}lib${/}rio-start.jar
        -Djava.protocol.handler.pkgs=org.rioproject.url
        -Djava.util.logging.config.file=${RIO_HOME}${/}config${/}logging${/}rio-logging.properties
        -XX:+HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC -XX:+AggressiveOpts -XX:HeapDumpPath=${RIO_HOME}${/}logs
        -server -Xms8m -Xmx256m -Djava.security.policy=${RIO_HOME}${/}policy${/}policy.all
        -DRIO_HOME=${RIO_HOME} -DRIO_TEST_ATTACH
        -Dorg.rioproject.groups=${org.rioproject.groups}
        -Dorg.rioproject.service=${service}'''


    mainClass = 'com.sun.jini.start.ServiceStarter'

    /*
    * Remove any previously created service log files
    */
    cleanLogs = true

    monitorStarter = '${user.dir}${/}src${/}test${/}conf${/}start-monitor.groovy'
    cybernodeStarter = '${RIO_HOME}/config/start-cybernode.groovy'
}