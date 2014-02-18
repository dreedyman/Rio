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
package org.rioproject.impl.system;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.impl.config.DynamicConfiguration;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.system.measurable.cpu.CPU;
import org.rioproject.impl.system.measurable.disk.DiskSpace;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.system.capability.platform.*;
import org.rioproject.system.capability.software.J2SESupport;

/**
 * @author Dennis Reedy
 */
public class SystemCapabilitiesTest {
    SystemCapabilities systemCapabilities;
    static boolean clearProperty = false;

    @BeforeClass
    public static void checkProperty() {
        if(System.getProperty("StaticCybernode")==null) {
            System.setProperty("StaticCybernode", "1");
            clearProperty = true;
        }
    }

    @AfterClass
    public static void clearProperty() {
        if(clearProperty) {
            System.clearProperty("StaticCybernode");
        }
    }

    @Before
    public void createSystemCapabilities() {
        systemCapabilities = new SystemCapabilities();
    }

    @Test
    public void testGetMeasurableCapabilities() throws Exception {
        MeasurableCapability[] mCaps = systemCapabilities.getMeasurableCapabilities(new DynamicConfiguration());
        Assert.assertEquals("Expected 4", 4, mCaps.length);

        org.rioproject.impl.system.measurable.memory.Memory memory =
            getCapability(org.rioproject.impl.system.measurable.memory.Memory.class, mCaps);
        Assert.assertNotNull(memory);
        log(memory);

        CPU cpu = getCapability(CPU.class, "CPU", mCaps);
        Assert.assertNotNull(cpu);
        log(cpu);

        CPU cpuProc = getCapability(CPU.class, "CPU (Proc)", mCaps);
        Assert.assertNotNull(cpuProc);
        log(cpuProc);

        DiskSpace diskSpace = getCapability(DiskSpace.class, mCaps);
        Assert.assertNotNull(diskSpace);
        log(diskSpace);

    }

    @Test
    public void testGetPlatformCapabilities() throws Exception {
        PlatformCapability[] pCaps = systemCapabilities.getPlatformCapabilities(new DynamicConfiguration());
        Assert.assertTrue("Expected at least 7", pCaps.length>=7);

        ProcessorArchitecture processorArchitecture = getCapability(ProcessorArchitecture.class, pCaps);
        Assert.assertNotNull(processorArchitecture);
        log(processorArchitecture);

        OperatingSystem operatingSystem  = getCapability(OperatingSystem.class, pCaps);
        Assert.assertNotNull(operatingSystem);
        log(operatingSystem);

        TCPConnectivity tcpip  = getCapability(TCPConnectivity.class, pCaps);
        Assert.assertNotNull(tcpip);
        log(tcpip);

        StorageCapability storage  = getCapability(StorageCapability.class, pCaps);
        Assert.assertNotNull(storage);
        log(storage);

        Memory memory  = getCapability(Memory.class, pCaps);
        Assert.assertNotNull(memory);
        log(memory);

        SystemMemory systemMemory  = getCapability(SystemMemory.class, pCaps);
        Assert.assertNotNull(systemMemory);
        log(systemMemory);

        J2SESupport java  = getCapability(J2SESupport.class, pCaps);
        Assert.assertNotNull(java);
        log(java);
    }

    @SuppressWarnings("unchecked")
    private <T> T getCapability(Class<T> match, Object[] array) {
        return getCapability(match, null, array);
    }

    @SuppressWarnings("unchecked")
    private <T> T getCapability(Class<T> match, String toString, Object[] array) {
        T capability = null;
        for(Object o : array) {
            if(o.getClass().isAssignableFrom(match)) {
                if(toString!=null) {
                    if(toString.equals(o.toString())) {
                        capability = (T) o;
                        break;
                    }
                } else {
                    capability = (T) o;
                    break;
                }
            }
        }
        return capability;
    }

    private void log(Object o) {
        System.out.println(o.getClass().getName()+": "+o);
    }
}
