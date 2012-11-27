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
    final String UI_PROPS = "rio-ui.properties";
    final String TREE = "tree";
    final String TREE_NODES = "tree.nodes";
    final String TREE_EDGES = "tree.edges";
    final String LINEAR = "linear";
    final String USER_OBJECT = "user-object";
    final String STATE = "state";
    final String NODE_DECORATORS = "nodeDeco";
    final String ROOT = "(root)";
    final String COMPONENT = "org.rioproject.tools.ui";
    final String INFO = "Rio Administrative Console";
    final String THREAD_POOL_SIZE_KEY = "threadPoolSize";
    final int DEFAULT_THREAD_POOL_SIZE = 1;
    final int MIN_THREAD_POOL_SIZE = 1;
    final int MAX_THREAD_POOL_SIZE = Integer.MAX_VALUE;
    final int EMPTY=0;
    final int WARNING=1;
    final int ACTIVE=2;
    final int ACTIVE_NO_SERVICE_ITEM=3;
    final int FAILED=4;
    final long AVAILABLE_ID = 0;
    final long DEFAULT_DELAY=1000*30;
    final int DEFAULT_CYBERNODE_REFRESH_RATE = 30;
    final Font ITEM_FONT = new Font("Lucida Grande", 0, 12);
    final Font NOTIFY_COUNT_FONT = new Font("Verdana", 0, 11);
    final DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss,SSS aa, MMM dd, yyyy");

    /* Property values */
    final String FRAME_WIDTH = "frame.width";
    final String FRAME_HEIGHT = "frame.height";
    final String FRAME_X_POS = "frame.xpos";
    final String FRAME_Y_POS = "frame.ypos";
    final String FRAME_DIVIDER = "frame.divider";
    final String EVENTS_DIVIDER = "events.divider";

    final String LAST_ARTIFACT = "last.artifact";
    final String LAST_DIRECTORY = "last.dir";
    final String FAILURE_COLOR = "color.failure";
    final String OKAY_COLOR = "color.okay";
    final String WARNING_COLOR = "color.warning";
    final String ADMIN_FRAME_WIDTH = "admin.frame.width";
    final String ADMIN_FRAME_HEIGHT = "admin.frame.height";
    final String ADMIN_FRAME_X_POS = "admin.frame.xpos";
    final String ADMIN_FRAME_Y_POS = "admin.frame.ypos";
    final String ADMIN_FRAME_WINDOW_LAYOUT = "admin.frame.layout";
    final String ADMIN_FRAME_WINDOW_TILE = "admin.frame.tile";
    final String ADMIN_FRAME_WINDOW_CASCADE = "admin.frame.cascade";
    final String GRAPH_ORIENTATION = "graph.orientation";
    final String GRAPH_ORIENTATION_NORTH = "2";
    final String GRAPH_ORIENTATION_WEST = "0";

    final String TREE_TABLE_AUTO_EXPAND = "tree.table.expand";

    final String CYBERNODE_REFRESH_RATE = "cybernode.refresh.rate";

    final String USE_EVENT_COLLECTOR = "use.event.collector";

    /*
     * Percentage of CPU utilization on the machine
     */
    final String UTIL_PERCENT_CPU = "%CPU";
    /*
     * Percentage of used memory on the machine
     */
    final String UTIL_PERCENT_MEMORY = "%Mem";
    /*
     * Total memory (in MB) on the machine
     */
    final String UTIL_TOTAL_MEMORY = "Mem Total";
    /*
     * Amount (in MB) of free memory on the machine
     */
    final String UTIL_FREE_MEMORY = "Mem Free";
    /*
     * Amount (in MB) of used memory on the machine
     */
    final String UTIL_USED_MEMORY = "Mem Used";
    /*
     * Percentage of used disk space
     */
    final String UTIL_PERCENT_DISK = "%Disk";
    /*
     * Amount (in GB) of available disk space
     */
    final String UTIL_AVAIL_DISK = "Disk Avail";
    /*
     * Amount (in GB) of total disk space
     */
    final String UTIL_TOTAL_DISK = "Disk Total";

    /*
     * Percentage of CPU utilization for the process (cybernode and/or
     * forked services)
     */
    final String UTIL_PERCENT_CPU_PROC = "%CPU (Proc)";
    /*
     * Percentage of Memory (heap) utilization for the JVM (cybernode and/or
     * forked java services)
     */
    final String UTIL_PERCENT_HEAP_JVM = "%Heap Mem";
    /*
     * Amount of heap memory (in MB) the JVM is using
     */
    final String UTIL_HEAP_MEM_JVM = "Heap Used";
    /*
     * Amount of heap memory (in MB) the JVM has available
     */
    final String UTIL_HEAP_MEM_AVAIL = "Heap Avail";
    /*
     * Amount of real memory (in MB) the process has allocated
     */
    final String UTIL_REAL_MEM_PROC = "RMem";
}
