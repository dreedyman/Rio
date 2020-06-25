/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.start;

import java.lang.instrument.Instrumentation;

/**
 * Allows the instrumentation class to be accessed.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public final class AgentHook {
    static Instrumentation instrumentation;

    private AgentHook() {}

    public static Instrumentation getInstrumentation() {
        return(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }
}
