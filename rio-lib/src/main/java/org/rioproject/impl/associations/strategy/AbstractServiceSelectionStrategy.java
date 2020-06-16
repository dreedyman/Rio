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
package org.rioproject.impl.associations.strategy;

import org.rioproject.associations.ServiceSelectionStrategy;
import org.rioproject.associations.Association;

/**
 * Boilerplate implementation of the
 * {@link org.rioproject.associations.ServiceSelectionStrategy}.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
public abstract class AbstractServiceSelectionStrategy<T> implements ServiceSelectionStrategy<T> {
    protected Association<T> association;

    public void setAssociation(Association<T> association) {
        this.association = association;
    }

    public Association<T> getAssociation() {
        return association;
    }

    public abstract T getService();

    public void serviceAdded(T service) {
    }

    public void serviceRemoved(T service) {
    }

    public void terminate() {
    }


}
