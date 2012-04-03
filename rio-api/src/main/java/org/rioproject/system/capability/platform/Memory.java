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
package org.rioproject.system.capability.platform;

import org.rioproject.system.measurable.memory.CalculableMemory;
import org.rioproject.watch.WatchDataReplicator;

import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

/**
 * Describes the amount of memory available for the Java Virtual Machine as
 * a qualitative resource.
 *
 * @author Dennis Reedy
 */
public class Memory extends ByteOrientedDevice implements WatchDataReplicator<CalculableMemory> {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Memory";

    /**
     * Create a Memory capability
     */
    public Memory() {
        this(DEFAULT_DESCRIPTION);
    }

    /**
     * Create a Memory capability with a description
     *
     * @param description The description
     */
    public Memory(String description) {
        this.description = description;
        define(NAME, "Memory");
    }

    public void addCalculable(CalculableMemory calculable) {
        Double dFree = calculable.getFreeMemory();
        Double dTotal = calculable.getTotalMemory();

        /* The values will come to us in MB, need to convert to bytes */
        dTotal = dTotal*MB;
        dFree = dFree*MB;
        capabilities.put(CAPACITY, dTotal);
        capabilities.put(AVAILABLE, dFree);
    }

    public void close() {
    }
}
