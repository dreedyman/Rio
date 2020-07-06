/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * The deployment configuration for the Tomcat example
 *
 * @author Dennis Reedy
 */
deployment(name: 'Tomcat Deploy') {
    /* Configuration for the discovery group that the service should join.
     * This first checks if the org.rioproject.groups property is set, if not
     * the user name is used */
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME, System.getProperty('user.name'))

    serviceExec(name: 'Tomcat') {

        /*
         * This declaration will remove the downloaded Tomcat distribution
         * when the Tomcat service is terminated (undeployed and/or
         * administratively stopped using Rio).
         *
         * If you want to keep the installed software (rather than overwrite
         * it each time), modify the declaration below to include:
         *
         * overwrite: 'no', removeOnDestroy: false
         */
        software(name: 'Tomcat', version: '9.0.36', removeOnDestroy: true) {
            install source: 'https://us.mirrors.quenda.co/apache/tomcat/tomcat-9/v9.0.36/bin/apache-tomcat-9.0.36.zip',
                    target: 'tomcat',
                    unarchive: true
            postInstall(removeOnCompletion: false) {
                if (!System.properties['os.name'].toLowerCase().contains('windows')) {
                    execute command: '/bin/chmod +x ${rio.home}/system/external/tomcat/apache-tomcat-9.0.36/bin/*sh'
                }
            }
        }

        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            execute(inDirectory: 'bin', command: 'catalina.bat run') {
                environment {
                    property name: "CATALINA_OPTS", value: "-Dcom.sun.management.jmxremote"
                }
            }
        } else {
            execute(inDirectory: 'bin', command: 'catalina.sh run') {
                environment {
                    property name: "CATALINA_OPTS", value: "-Dcom.sun.management.jmxremote"
                }
            }
        }

        sla(id:'ThreadPool', high: 100) {
            policy type: 'notify'
            monitor name: 'Tomcat Thread Pool',
                    objectName: "Catalina:type=ThreadPool,name=\"http-nio-8080\"",
                    attribute: 'currentThreadsBusy', period: 5000
        }

        maintain 1
    }
}
