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
package org.rioproject.resolver.maven2

/**
 * A configured profile.
 *
 * <p>Profile activation is not supported using the following activation settings:
 * <ul>
 * <li>jdk</li>
 * <li>os</li>
 * <li>file</li>
 * </ul>
 */
class Profile {
    String id
    boolean activeByDefault = false
    def repositories = []
    private String activateOnPropertyName
    private String activateOnPropertyValue

    def isActive() {
        if(activeByDefault)
            return true
        if(activateOnPropertyName!=null && activateOnPropertyName.length()>0) {
            String value = System.getProperty(activateOnPropertyName)
            if(value==null)
                return false
            if(activateOnPropertyValue!=null && activateOnPropertyValue.length()>0)
                return value.equals(activateOnPropertyValue)
            return true
        }
        return false
    }

    def setActivateOnProperty(String name) {
        activateOnPropertyName = name
    }

    def setActivateOnProperty(String name, String value) {
        activateOnPropertyName = name
        activateOnPropertyValue = value
    }

}


