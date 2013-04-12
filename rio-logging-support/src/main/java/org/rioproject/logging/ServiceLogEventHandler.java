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
package org.rioproject.logging;

/**
 * A {@code ServiceLogEventHandler} is an entity that interfaces with a logging system and listens
 * for log events that contain {@code Exception}s. If identified a {@code ServiceLogEvent} is produced and
 * sent to registered remote event consumers.
 *
 * <p>The <code>ServiceLogEventHandler</code> will also publish
 * {@code ServiceLogEvent}s to interested consumers if the <code>level</code>
 * has a level that is greater than or equal to the <code>ServiceLogEventHandler</code>'s
 * configured {@code Level} property, and whose name has been configured as a
 * <i>publishable</i> logger.
 * </p>
 *
 * @author Dennis Reedy
 */
public interface ServiceLogEventHandler  {

    /**
     * Set the {@code ServiceLogEventPublisher} to use for publishing log events. The
     * {@code ServiceLogEventHandler} will only publish events if the {@code ServiceLogEventPublisher} is
     * not {@code null}.
     *
     * @param publisher The {@code ServiceLogEventPublisher} to use. The
     */
    void setServiceLogEventPublisher(ServiceLogEventPublisher publisher);

    /**
     * Get the {@code ServiceLogEventPublisher}
     *
     * @return The {@code ServiceLogEventPublisher}, may be {@code null}
     */
    ServiceLogEventPublisher getServiceLogEventPublisher();

    /**
     * Set the level to publish on. The default level is {@code Level.SEVERE}(for JUL) and {@code Level.ERROR} for
     * Logback. Note all log notification that include a stacktrace are always published.
     *
     * @param level The level to publish on
     */
    void setPublishOnLevel(String level);

    /**
     * Add a specific logger to publish for.
     *
     * @param publishableLogger A logger to publish
     */
    void addPublishableLogger(String publishableLogger);
}
