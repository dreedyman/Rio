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
package org.rioproject.cybernode;

import net.jini.core.discovery.LookupLocator;
import org.rioproject.deploy.ServiceProvisionEvent;
import org.rioproject.opstring.ServiceElement;

/**
 * Provides utilities for dealing with common logging formats for services being instantiated.
 */
public final class CybernodeLogUtil {
    private CybernodeLogUtil() {
    }
    
    public static String logName(final ServiceProvisionEvent event) {
        return logName(event.getServiceElement());
    }

    public static String simpleLogName(final ServiceElement element) {
        StringBuilder builder = new StringBuilder();
        builder.append(element.getOperationalStringName()).append("/");
        builder.append(element.getName());
        return builder.toString();
    }

    public static String logName(final ServiceElement element) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(element.getOperationalStringName()).append("/");
        builder.append(element.getName());
        builder.append(", instance:").append(getInstanceID(element));
        builder.append("]");
        return builder.toString();
    }

    public static String discoveryInfo(final ServiceElement element) {
        StringBuilder builder = new StringBuilder();
        builder.append("[groups: ");
        int i=0;
        for(String group : element.getServiceBeanConfig().getGroups()) {
            if(i>0)
                builder.append(", ");
            builder.append(group);
            i++;
        }
        if(element.getServiceBeanConfig().getLocators()!=null && element.getServiceBeanConfig().getLocators().length>0) {
            builder.append(", locators: ");
            i = 0;
            for(LookupLocator locator : element.getServiceBeanConfig().getLocators()) {
                if(i>0)
                    builder.append(", ");
                builder.append(locator.toString());
                i++;
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public static Long getInstanceID(final ServiceElement element) {
        return element.getServiceBeanConfig().getInstanceID();
    }


}
