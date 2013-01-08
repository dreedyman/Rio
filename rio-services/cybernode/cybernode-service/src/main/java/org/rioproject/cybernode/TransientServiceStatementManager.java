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
package org.rioproject.cybernode;

import net.jini.config.Configuration;
import org.rioproject.deploy.ServiceStatement;
import org.rioproject.deploy.ServiceStatementManager;
import org.rioproject.opstring.ServiceElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * The {@code TransientServiceStatementManager} provides an implementation of the
 * {@link ServiceStatementManager}, setting and accessing {@link ServiceStatement} instances in
 * a {@code Map}.
 *
 * @author Dennis Reedy
 */
public class TransientServiceStatementManager implements ServiceStatementManager {
    /** Map of ServiceElement to ServiceStatement instances */
    private final Map<ServiceElement,ServiceStatement> statementMap = new HashMap<ServiceElement,ServiceStatement>();
    private static final Logger logger = LoggerFactory.getLogger(TransientServiceStatementManager.class);

    /**
     * Create a TransientServiceStatementManager
     *
     * @param config The Configuration
     */
    public TransientServiceStatementManager(Configuration config) {
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#terminate
     */
    public void terminate() {
        statementMap.clear();
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#get
     */
    public ServiceStatement[] get() {
        ServiceStatement[] statements;
        synchronized (statementMap) {
            statements = statementMap.values().toArray(new ServiceStatement[statementMap.values().size()]);
        }
        return statements;
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#get
     */
    public ServiceStatement get(ServiceElement sElem) {
        ServiceStatement statement;
        synchronized (statementMap) {
            statement = statementMap.get(sElem);
        }
         return statement;
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#record
     */
    public void record(ServiceStatement statement) {
        synchronized (statementMap) {
            ServiceElement key = statement.getServiceElement();
            boolean active = statement.hasActiveServiceRecords();
            if (active) {
                statementMap.put(key, statement);

            } else {
                remove(key);
            }
        }
    }

    private void remove(ServiceElement key) {
        statementMap.remove(key);
        logger.debug("{} is Inactive, Removed Records", key.getName());
    }
}
