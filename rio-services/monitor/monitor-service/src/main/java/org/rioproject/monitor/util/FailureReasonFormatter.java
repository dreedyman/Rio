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

import org.rioproject.monitor.ProvisionRequest;
import org.rioproject.monitor.selectors.ServiceResourceSelector;

/**
 * Simple utility to format service provision failure reasons.
 *
 * @author Dennis Reedy
 */
public final class FailureReasonFormatter {
    private FailureReasonFormatter() {}

    /**
     * Format service provision failure
     *
     * @param request The {@code ProvisionRequest}, must not be {@code null}.
     * @param selector The {@code ServiceResourceSelector}, must not be {@code null}.
     *
     * @return A formatted string
     *
     * @throws IllegalArgumentException if any of the parameters are {@code null}
     */
    public static String format(final ProvisionRequest request, final ServiceResourceSelector selector) {
        if(request==null)
            throw new IllegalArgumentException("request is null");
        if(selector==null)
            throw new IllegalArgumentException("selector is null");
        StringBuilder failureReasonBuilder = new StringBuilder();
        if(!request.getFailureReasons().isEmpty()) {
            failureReasonBuilder.append("Failure Reason(s):\n");
            for(String reason : request.getFailureReasons()) {
                failureReasonBuilder.append(reason);
            }
        } else {
            String action = request.getType().name().toLowerCase();
            int total = selector.getServiceResources().length;
            String failureReason =
                String.format("A compute resource could not be obtained to %s [%s], total registered=%d",
                              action, LoggingUtil.getLoggingName(request), total);
            failureReasonBuilder.append(failureReason);
        }
        return failureReasonBuilder.toString();
    }
}
