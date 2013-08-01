/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

/**
 * The AssociationListener interface specifies the semantics of a client that
 * when registered to an {@link AssociationManagement} instance will receive
 * notifications of {@link Association}s being discovered, changed and broken.
 *
 * @author Dennis Reedy
 */
public interface AssociationListener<T> {
    /**
     * Notify the AssociationListener that an Association has been discovered
     * 
     * @param association The Association
     * @param service The associated service that has been discovered
     */
    void discovered(Association<T> association, T service);

    /**
     * Notify the AssociationListener that a service endpoint has changed
     * 
     * @param association The Association
     * @param service The service that was removed, changing the endpoint
     */
    void changed(Association<T> association, T service);

    /**
     * Notify the AssociationListener that an Association is broken. If the
     * Association type is requires, the AssociationManagement object will
     * unadvertise the ServiceBean using the ServiceBean instances
     * ServiceBeanControl interface unless the unadvertiseOnBroken is set to
     * false
     * 
     * @param association The Association.
     * @param service The service that was removed, causing the Association to
     * be broken
     */
    void broken(Association<T> association, T service);
}
