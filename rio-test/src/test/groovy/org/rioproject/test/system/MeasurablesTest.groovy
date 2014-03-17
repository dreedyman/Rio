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
package org.rioproject.test.system

import net.jini.config.Configuration
import net.jini.config.EmptyConfiguration
import org.rioproject.deploy.SystemComponent
import org.rioproject.impl.system.measurable.disk.DiskSpace
import org.rioproject.impl.system.measurable.memory.Memory
import org.rioproject.system.capability.platform.StorageCapability

/**
 * Test measurables
 *
 * @author Dennis Reedy
 */
class MeasurablesTest extends GroovyTestCase {

    void testMemory() {
        Configuration config = EmptyConfiguration.INSTANCE
        Memory memMeasurable =
            new Memory(config)

        org.rioproject.system.capability.platform.Memory memCapability =
            new org.rioproject.system.capability.platform.Memory()
        memMeasurable.addWatchDataReplicator(memCapability)
        memMeasurable.start()

        def req = [(memCapability.CAPACITY) : '250k']
        SystemComponent sysComp = new SystemComponent(memCapability.ID, memCapability.class.name)
        sysComp.putAll(req)
        assertTrue 'JVM should have the capacity of 250k but does not',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '1m')
        assertEquals 'Should have 3 capabilities, only have '+memCapability.getCapabilities().size(),
                     3, memCapability.capabilities.size()
        assertTrue 'JVM should have the capacity of 1m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '4m')
        assertTrue 'JVM should have the capacity of 4m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '8m')
        assertTrue 'JVM should have the capacity of 8m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '16m')
        assertTrue 'JVM should have the capacity of 16m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '32m')
        assertTrue 'JVM should have the capacity of 32m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '1024k')
        assertTrue 'JVM should have the capacity of 1024k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '2048k')
        assertTrue 'JVM should have the capacity of 2048k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '4096k')
        assertTrue 'JVM should have the capacity of 4096k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '8192k')
        assertTrue 'JVM should have the capacity of 8192k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '16384k')
        assertTrue 'JVM should have the capacity of 16384k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)

        sysComp = new SystemComponent(memCapability.name)
        sysComp.put(memCapability.CAPACITY, '32768k')
        assertTrue 'JVM should have the capacity of 32768k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        memMeasurable.stop()        
    }

    void testDiskSpace() {
        Configuration config = EmptyConfiguration.INSTANCE
        DiskSpace diskMeasurable = new DiskSpace(config)

        StorageCapability storageCapability = new StorageCapability()
        diskMeasurable.addWatchDataReplicator(storageCapability)
        diskMeasurable.start()

        SystemComponent sysComp = new SystemComponent(StorageCapability.ID)
        sysComp.put(storageCapability.CAPACITY, '250k')
        assertTrue 'System should have the disk space capacity of 250k but does not',
                   storageCapability.supports(sysComp)
    }
}
