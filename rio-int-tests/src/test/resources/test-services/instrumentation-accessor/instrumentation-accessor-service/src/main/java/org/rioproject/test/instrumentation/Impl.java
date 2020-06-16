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
package org.rioproject.test.instrumentation;

import com.sun.tools.attach.VirtualMachine;
import org.rioproject.annotation.Started;
import org.rioproject.impl.exec.VirtualMachineHelper;
import org.rioproject.start.AgentHook;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class Impl implements API {
    private Instrumentation instrumentation;

    @Started
    public void started() {
        String myID = VirtualMachineHelper.getID();
        String rioHome = System.getProperty("rio.home");
        File libDir = new File(rioHome, "lib");
        String jar = null;
        for(File f : libDir.listFiles()) {
            if(f.getName().startsWith("rio-start")) {
                jar = f.getPath();
                break;
            }
        }
        System.out.println("LOADING "+jar);
        try {
            VirtualMachine virtualMachine = VirtualMachine.attach(myID);
            virtualMachine.loadAgent(jar);
        } catch (Exception e) {
            e.printStackTrace();
        }
        instrumentation = AgentHook.getInstrumentation();
    }

    public boolean isClassNameLoaded(String className) {
        boolean loaded = false;
        for(Class c : instrumentation.getAllLoadedClasses()) {
            if(c.getName().equals(className)) {
                loaded = true;
                break;
            }
        }
        return loaded;
    }
}
