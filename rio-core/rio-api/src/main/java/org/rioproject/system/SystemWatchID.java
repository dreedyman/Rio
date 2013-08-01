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
package org.rioproject.system;

/**
 * Defines system based watch ids.
 */
public interface SystemWatchID {
    static final String SYSTEM_CPU = "CPU";
    static final String PROC_CPU = "CPU (Proc)";
    static final String DISK_SPACE = "DiskSpace";
    static final String SYSTEM_MEMORY = "System Memory";
    static final String JVM_MEMORY = "Process Memory";
    static final String JVM_PERM_GEN = "Perm Gen";
    static final String[] IDs = new String[]{SYSTEM_CPU,
                                             PROC_CPU,
                                             DISK_SPACE,
                                             SYSTEM_MEMORY,
                                             JVM_MEMORY,
                                             JVM_PERM_GEN};
}
