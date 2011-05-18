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
package org.rioproject.gnostic;

import org.rioproject.sla.RuleMap;
import org.rioproject.watch.Calculable;

import java.io.IOException;
import java.util.List;

/**
 * An interface allowing the setup, insertion and closing of a
 * Complex Event Processor session.
 */
public interface CEPSession {
    /**
     * Initialize the CEPSession, creating a session as needed.
     *
     * @param serviceHandles The services that will be used along with the CEPSession.
     * @param ruleMap The information on the rules to use.
     * @param loader The ClassLoader used to load additional classes
     *
     * @throws IOException if for any reason problems arise accessing resources
     */
    void initialize(List<ServiceHandle> serviceHandles, RuleMap ruleMap, ClassLoader loader) throws
                                                                         IOException;

    /**
     * Insert a <code>Calculable</code> into the CEPSession
     *
     * @param calculable The <code>Calculable</code> to insert
     */
    void insert(Calculable calculable);

    /**
     *  Close, and/or terminate the CEPSession session
     */
    void close();
}
