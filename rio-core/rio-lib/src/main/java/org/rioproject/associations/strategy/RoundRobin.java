/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.associations.strategy;

import net.jini.core.lookup.ServiceItem;

/**
 * Alternates selection of services from the
 * {@link org.rioproject.associations.Association}, alternating the selection of
 * associated services using a round-robin approach
 *
 * @author Dennis Reedy
 */
public class RoundRobin<T> extends AbstractServiceSelectionStrategy<T> {

    @SuppressWarnings("unchecked")
    public synchronized T getService() {
        ServiceItem item = association.getNextServiceItem();
        T service = null;
        if(item!=null)
            service = (T) item.service;
        return service;
    }
}
