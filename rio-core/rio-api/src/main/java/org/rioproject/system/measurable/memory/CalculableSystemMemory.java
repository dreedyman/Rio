/*
 * Copyright 2008 to the original author or authors.
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
package org.rioproject.system.measurable.memory;

import org.rioproject.watch.Calculable;

/**
 * A Calculable used to collect system memory utilization
 *
 * @author Dennis Reedy
 */
public class CalculableSystemMemory  extends Calculable {
    private static final long serialVersionUID = 1L;
    /**
     * Holds value of property containing details about system memory
     * utilization
     */
    private SystemMemoryUtilization memoryUtilization;

    public CalculableSystemMemory(String id,
                                  double value,
                                  long when,
                                  SystemMemoryUtilization memoryUtilization) {
        super(id, value, when);
        this.memoryUtilization = memoryUtilization;
    }

    public SystemMemoryUtilization getMemoryUtilization() {
        return memoryUtilization;
    }

    @Override
    public String toString() {
        return String.format("%s - id: [%s], total: [%s], free: [%s], used: [%s], free percent: [%s], used percent: [%s], ram: [%s], value: [%s]",
                             getFormattedDate(),
                             getId(),
                             memoryUtilization.getTotal(),
                             memoryUtilization.getFree(),
                             memoryUtilization.getUsed(),
                             memoryUtilization.getFreePercentage(),
                             memoryUtilization.getUsedPercentage(),
                             memoryUtilization.getRam(),
                             getValue());
    }
}
