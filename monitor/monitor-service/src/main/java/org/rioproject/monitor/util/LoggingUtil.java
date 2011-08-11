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
package org.rioproject.monitor.util;

import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.monitor.ProvisionRequest;

/**
 * Utilities for creating names to use with logging
 */
public class LoggingUtil {

    public static String getLoggingName(ProvisionRequest request) {
        return getLoggingName(request.getServiceElement());
    }

    public static String getLoggingName(ServiceElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append(element.getOperationalStringName()).append("/").append(element.getName());
        return sb.toString();
    }

     public static String getLoggingName(ServiceBeanConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getOperationalStringName()).append("/").append(config.getName());
        return sb.toString();
    }
}
