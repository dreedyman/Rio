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
package org.rioproject.associations;

import org.rioproject.opstring.ServiceElement;

/**
 * The <tt>AssociationMatchFilter</tt> defines the method used by an object
 * to check if {@link AssociationDescriptor} requirements
 * can be met by a {@link org.rioproject.opstring.ServiceElement}. Entities
 * requiring the application of specific criteria to determine association
 * matching (in the case of colocation, opposed or isolated association types)
 * should construct an implementation of this interface that defines the
 * additional criteria.
 * <p>
 * The filtering mechanism provided by implementations of this interface is
 * particularly useful to entities that wish to extend the capabilities of the
 * standard association matching scheme. For example, because association
 * matching does not allow one to match services based on specific attribute
 * values, this additional matching mechanism can be exploited by the entity
 * to match on specific service configuration attributes.
 * <p>
 * Concrete implementations of this interface are obtained during the
 * provisioning of services and declared as part of the
 * <tt>&lt;Association&gt;</tt> element.
 * <p>
 * <tt>
 * &lt;Association&gt; Name="..." Type="..."&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;management filter="foo.bar.Baz"/&gt;<br>
 * &lt;/Association&gt;<br>
 * </tt>
 *
 * @author Dennis Reedy
 */
public interface AssociationMatchFilter {

    /**
     * Defines the implementation of association matching criteria.
     * Implementations of this method should expect that neither of the
     * parameters passed will contain <code>null</code> references
     *
     * @param descriptor The {@link AssociationDescriptor}
     * that defines the association.
     * @param element The {@link org.rioproject.opstring.ServiceElement} that will be
     * checked
     *
     * @return <code>true</code> if the <code>ServiceElement</code> matches
     * the requirements of the <code>AssociationDescriptor</code>,
     * <code>false</code> otherwise
     */
    boolean check(AssociationDescriptor descriptor, ServiceElement element);

}
