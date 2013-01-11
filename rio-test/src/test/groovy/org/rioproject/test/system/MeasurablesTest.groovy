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

/**
 * Test measurables
 *
 * @author Dennis Reedy
 */
class MeasurablesTest extends GroovyTestCase {

    void testMemory() {
        Configuration config = EmptyConfiguration.INSTANCE
        org.rioproject.system.measurable.memory.Memory memMeasurable =
            new org.rioproject.system.measurable.memory.Memory(config)

        org.rioproject.system.capability.platform.Memory memCapability =
            new org.rioproject.system.capability.platform.Memory()
        memMeasurable.addWatchDataReplicator(memCapability)
        memMeasurable.start()
        
        def req = [(memCapability.CAPACITY) : '250k']
        SystemComponent sysComp = new SystemComponent(memCapability.name, memCapability.class.name, req)
        assertTrue 'JVM should have the capacity of 250k but does not',
                   memCapability.supports(sysComp)

        req.put(memCapability.CAPACITY, '1m')
        sysComp = new SystemComponent(memCapability.name, req)
        assertEquals 'Should have 3 capabilities, only have '+memCapability.getCapabilities().size(),
                     3, memCapability.capabilities.size()
        assertTrue 'JVM should have the capacity of 1m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)
         req.put(memCapability.CAPACITY, '4m')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 4m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)
         req.put(memCapability.CAPACITY, '8m')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 8m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)
         req.put(memCapability.CAPACITY, '16m')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 16m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)
         req.put(memCapability.CAPACITY, '32m')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 32m but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.MB+'m',
                   memCapability.supports(sysComp)

        req.put(memCapability.CAPACITY, '1024k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 1024k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        req.put(memCapability.CAPACITY, '2048k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 2048k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        req.put(memCapability.CAPACITY, '4096k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 4096k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        req.put(memCapability.CAPACITY, '8192k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 8192k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        req.put(memCapability.CAPACITY, '16384k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 16384k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        req.put(memCapability.CAPACITY, '32768k')
        sysComp = new SystemComponent(memCapability.name, req)
        assertTrue 'JVM should have the capacity of 32768k but has '+
                   memCapability.getCapabilities().get(memCapability.CAPACITY)/memCapability.KB+'kb',
                   memCapability.supports(sysComp)
        memMeasurable.stop()        
    }

    void testDiskSpace() {
        Configuration config = EmptyConfiguration.INSTANCE
        org.rioproject.system.measurable.disk.DiskSpace diskMeasurable =
            new org.rioproject.system.measurable.disk.DiskSpace(config)

        org.rioproject.system.capability.platform.StorageCapability storageCapability =
            new org.rioproject.system.capability.platform.StorageCapability()
        diskMeasurable.addWatchDataReplicator(storageCapability)
        diskMeasurable.start()

        def req = [(storageCapability.CAPACITY) : '250k']
        SystemComponent sysComp = new SystemComponent(storageCapability.name, req)
        assertTrue 'System should have the disk space capacity of 250k but does not',
                   storageCapability.supports(sysComp)
    }
}
