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
package org.rioproject.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides utilities to assist in using the {@code com.sun.tools.attach.VirtualMachine}
 *
 * @author Dennis Reedy
 */
public final class VirtualMachineHelper {
    static final Logger logger = LoggerFactory.getLogger(VirtualMachineHelper.class);

    private VirtualMachineHelper() {}

    /**
     * Get the ID (pid) using {@code ManagementFactory.getRuntimeMXBean().getName()}. This value is expected to have the
     * following format: pid@hostname. If the return includes the @hostname, the @hostname is stripped off.
     *
     * @return The identifier for the Java Virtual Machine.
     */
    public static String getID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name;
        int ndx = name.indexOf("@");
        if(ndx>=1) {
            pid = name.substring(0, ndx);
        }
        return pid;
    }

    /**
     * Using the <a href="http://docs.oracle.com/javase/6/docs/jdk/api/attach/spec/index.html">Attach API</a>,
     * list the available local Java Virtual Machines.
     *
     * <p>This utility requires Java 6 or greater.
     *
     * @return A String array of Java Virtual Machine IDs. If the Attach API cannot be loaded, return an empty array.
     *
     * @throws Exception if the Attach API cannot be loaded
     */
    public static String[] listIDs() throws Exception {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.error("The JMX Attach APIs require Java 6 or above. You are running Java {}", jvmVersion);
            return new String[0];
        }
        List<String> vmList = new ArrayList<String>();
        //try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method list = vmClass.getMethod("list");
            List vmDescriptors = (List)list.invoke(null);
            for (Object vmDesc : vmDescriptors) {
                Method id = vmDesc.getClass().getMethod("id");
                vmList.add((String)id.invoke(vmDesc));
            }
        /*} catch (Exception e) {
            logger.warn("Could not obtain list of VMs", e);
        }*/
        return vmList.toArray(new String[vmList.size()]);
    }

    /**
     * Using the <a href="http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html">
     * JMX Attach API </a>, list the available local Java Virtual Machines.
     *
     * <p>This utility requires Java 6 or greater.
     *
     * @return A String array of Java Virtual Machine IDs followed by the displayName of each discovered
     * {@code VirtualMachine}. If the Attach API cannot be loaded, return an empty array.
     *
     * @throws Exception if the Attach API cannot be loaded
     */
    public static String[] listManagedVMs() throws Exception {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5")) {
            logger.error("The JMX Attach APIs require Java 6 or above. You are running Java {}", jvmVersion);
            return new String[0];
        }
        List<String> vmList = new ArrayList<String>();
        //try {
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Method list = vmClass.getMethod("list");
            List vmDescriptors = (List)list.invoke(null);
            for (Object vmDesc : vmDescriptors) {
                Method displayName = vmDesc.getClass().getMethod("displayName");
                Method id = vmDesc.getClass().getMethod("id");
                vmList.add(id.invoke(vmDesc)+" "+displayName.invoke(vmDesc));
            }
        /*} catch (Exception e) {
            logger.warn("Could not obtain list of VMs", e);
        }*/
        return vmList.toArray(new String[vmList.size()]);
    }
}
