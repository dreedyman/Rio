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
package org.rioproject.tools.ui;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Defines constants for the utility
 *
 * @author Dennis Reedy
 */
public interface Constants {
    String UI_PROPS = "rio-ui.properties";
    String TREE = "tree";
    String TREE_NODES = "tree.nodes";
    String TREE_EDGES = "tree.edges";
    String LINEAR = "linear";
    String USER_OBJECT = "user-object";
    String STATE = "state";
    String NODE_DECORATORS = "nodeDeco";
    String ROOT = "(root)";
    String COMPONENT = "org.rioproject.tools.ui";
    String INFO = "Rio Administrative Console";
    String THREAD_POOL_SIZE_KEY = "threadPoolSize";
    int DEFAULT_THREAD_POOL_SIZE = 1;
    int MIN_THREAD_POOL_SIZE = 1;
    int MAX_THREAD_POOL_SIZE = Integer.MAX_VALUE;
    int EMPTY=0;
    int WARNING=1;
    int ACTIVE=2;
    int ACTIVE_NO_SERVICE_ITEM=3;
    int FAILED=4;
    int ACTIVE_UNMANAGED=5;
    long AVAILABLE_ID = 0;
    int DEFAULT_CYBERNODE_REFRESH_RATE = 30;
    Font ITEM_FONT = new Font("Lucida Grande", 0, 12);
    Font NOTIFY_COUNT_FONT = new Font("Verdana", 0, 11);
    DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss,SSS aa, MMM dd, yyyy");

    /* Property values */
    String FRAME_WIDTH = "frame.width";
    String FRAME_HEIGHT = "frame.height";
    String FRAME_X_POS = "frame.xpos";
    String FRAME_Y_POS = "frame.ypos";
    String FRAME_DIVIDER = "frame.divider";
    String EVENTS_DIVIDER = "events.divider";

    String LAST_ARTIFACT = "last.artifact";
    String LAST_DIRECTORY = "last.dir";
    String FAILURE_COLOR = "color.failure";
    String OKAY_COLOR = "color.okay";
    String UNMANAGED_COLOR = "color.unmanaged";
    String WARNING_COLOR = "color.warning";
    String ADMIN_FRAME_WIDTH = "admin.frame.width";
    String ADMIN_FRAME_HEIGHT = "admin.frame.height";
    String ADMIN_FRAME_X_POS = "admin.frame.xpos";
    String ADMIN_FRAME_Y_POS = "admin.frame.ypos";
    String ADMIN_FRAME_WINDOW_LAYOUT = "admin.frame.layout";
    String ADMIN_FRAME_WINDOW_TILE = "admin.frame.tile";
    String ADMIN_FRAME_WINDOW_CASCADE = "admin.frame.cascade";
    String GRAPH_ORIENTATION = "graph.orientation";
    String GRAPH_ORIENTATION_NORTH = "2";
    String GRAPH_ORIENTATION_WEST = "0";
    String UNMANAGED = "Unmanaged";

    String TREE_TABLE_AUTO_EXPAND = "tree.table.expand";

    String CYBERNODE_REFRESH_RATE = "cybernode.refresh.rate";

    /*
     * Percentage of CPU utilization on the machine
     */
    String UTIL_PERCENT_CPU = "%CPU";
    /*
     * Percentage of used memory on the machine
     */
    String UTIL_PERCENT_MEMORY = "%Mem";
    /*
     * Total memory (in MB) on the machine
     */
    String UTIL_TOTAL_MEMORY = "Mem Total";
    /*
     * Amount (in MB) of free memory on the machine
     */
    String UTIL_FREE_MEMORY = "Mem Free";
    /*
     * Amount (in MB) of used memory on the machine
     */
    String UTIL_USED_MEMORY = "Mem Used";
    /*
     * Percentage of used disk space
     */
    String UTIL_PERCENT_DISK = "%Disk";
    /*
     * Amount (in GB) of available disk space
     */
    String UTIL_AVAIL_DISK = "Disk Avail";
    /*
     * Amount (in GB) of total disk space
     */
    String UTIL_TOTAL_DISK = "Disk Total";

    /*
     * Percentage of CPU utilization for the process (cybernode and/or
     * forked services)
     */
    String UTIL_PERCENT_CPU_PROC = "%CPU (Proc)";
    /*
     * Percentage of Memory (heap) utilization for the JVM (cybernode and/or
     * forked java services)
     */
    String UTIL_PERCENT_HEAP_JVM = "%Heap Mem";
    /*
     * Amount of heap memory (in MB) the JVM is using
     */
    String UTIL_HEAP_MEM_JVM = "Heap Used";
    /*
     * Amount of heap memory (in MB) the JVM has available
     */
    String UTIL_HEAP_MEM_AVAIL = "Heap Avail";
    /*
     * Amount of real memory (in MB) the process has allocated
     */
    String UTIL_REAL_MEM_PROC = "RMem";
}
