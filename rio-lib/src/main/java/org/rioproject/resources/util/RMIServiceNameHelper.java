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
package org.rioproject.resources.util;

import org.rioproject.opstring.ServiceElement;

/**
 * Create a name suitable as a RMI Registry bind name.
 */
public final class RMIServiceNameHelper {
    private RMIServiceNameHelper() {}

    /**
     * Create a normalized service name
     *
     * @param elem The {@link org.rioproject.opstring.ServiceElement}, must not be null
     *
     * @return A normalized service name, translating " " to "_" and
     * appending the instance ID to the name. This will be used for the
     * log name and the registry bind name
     */
    public static String createNormalizedServiceName(final ServiceElement elem) {

        String normalizedServiceName = replaceIllegalChars(elem.getName());
        return normalizedServiceName+"-"+elem.getServiceBeanConfig().getInstanceID();
    }


    /**
     * Create a service bind name
     *
     * @param elem The {@link org.rioproject.opstring.ServiceElement}, must not be null
     *
     * @return A normalized service name, translating " " to "_" and
     * appending the instance ID to the name. This will be used for the
     * log name and the registry bind name
     */
    public static String createBindName(final ServiceElement elem) {
        String normalizedServiceName = createNormalizedServiceName(elem);
        /* Build the RMI registry bind name*/
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(replaceIllegalChars(elem.getOperationalStringName()));
        nameBuilder.append("/");
        nameBuilder.append(normalizedServiceName);
        return nameBuilder.toString();
    }

    private static String replaceIllegalChars(final String s) {
        char[] toReplace = new char[]{'/', '!', '#', '$', '&', '*',
                                      '(', ')', '\'', '`', '[', ']',
                                      '{', '}', '|', '~', ' '};
        String newString = s;
        for(char c : toReplace) {
            newString = newString.replace(c, '_');
        }
        return newString;
    }
}
