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
package org.rioproject.monitor.service.channel;

import org.rioproject.opstring.ServiceElement;

import java.util.EventObject;

/**
 * An event sent through the {@code ServiceChannel}.
 *
 * @author Dennis Reedy
 */
public class ServiceChannelEvent extends EventObject {
    public enum Type {PROVISIONED, FAILED, IDLE}
    private final ServiceElement element;
    private final Type type;

    public ServiceChannelEvent(final Object source, final ServiceElement element, final Type type) {
        super(source);
        this.element = element;
        this.type = type;
    }

    public ServiceElement getServiceElement() {
        return element;
    }

    public Type getType() {
        return type;
    }
}
