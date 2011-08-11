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
package org.rioproject.monitor.handlers;

import org.rioproject.opstring.OperationalString;

import java.util.Date;
import java.util.List;

/**
 * An interface that returns {@link org.rioproject.opstring.OperationalString}s to
 * deploy
 *
 * @author Dennis Reedy
 */
public interface DeployHandler {

    /**
     * Get a list of {@link org.rioproject.opstring.OperationalString}s to deploy
     *
     * @return A <tt>List</tt> of
     * {@link org.rioproject.opstring.OperationalString}s to deploy. A new list is
     * allocated each time. If there are no <tt>OperationalString</tt>s
     * to deploy, a zero-length list is returned
     */
    List<OperationalString> listOfOperationalStrings();

    /**
     * Get a list of {@link org.rioproject.opstring.OperationalString}s to deploy
     * starting from a <tt>Date</tt>
     *
     * @param fromDate <tt>OperationalString</tt> that has been added after the
     * from date will be returned.
     *
     * @return A <tt>List</tt> of
     * {@link org.rioproject.opstring.OperationalString}s to deploy. A new list is
     * allocated each time. If there are no <tt>OperationalString</tt>s
     * to deploy, a zero-length list is returned
     *
     * @throws IllegalArgumentException if the <tt>fromDate</tt> is null
     */
    List<OperationalString> listOfOperationalStrings(Date fromDate);
}
