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
package org.rioproject.system.measurable.memory.pool;

import org.rioproject.watch.Calculable;

/**
 * A @{code CalculableMemoryPool} holds details about memory pool utilization.
 * @author Dennis Reedy
 */
public class CalculableMemoryPool extends Calculable {
    /**
     * Holds value of property containing details about memory utilization for a
     * memory pool.
     */
    private final MemoryPoolUtilization memoryPoolUtilization;

    public CalculableMemoryPool(final String id,
                                final double value,
                                final long when,
                                final MemoryPoolUtilization memoryPoolUtilization) {
        super(id, value, when);
        this.memoryPoolUtilization = memoryPoolUtilization;
    }

    @SuppressWarnings("unused")
    public MemoryPoolUtilization getMemoryUtilization() {
        return memoryPoolUtilization;
    }

    @Override
    public String toString() {
        return String.format("%s - id: [%s], committed: [%s], max: [%s], init: [%s], used: [%s]",
                             getFormattedDate(),
                             getId(),
                             memoryPoolUtilization.getCommitted(),
                             memoryPoolUtilization.getMax(),
                             memoryPoolUtilization.getInit(),
                             memoryPoolUtilization.getUsed());
    }
}
