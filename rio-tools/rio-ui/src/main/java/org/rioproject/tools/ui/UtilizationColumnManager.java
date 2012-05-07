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
package org.rioproject.tools.ui;

import java.util.*;

/**
 * Manages columns to display for the utilization table
 */
public class UtilizationColumnManager {
    private List<String> selectedColumns = new ArrayList<String>();
    private String[] defaultColumns = new String[]{Constants.UTIL_PERCENT_CPU,
                                                   Constants.UTIL_PERCENT_CPU_PROC,
                                                   Constants.UTIL_PERCENT_HEAP_JVM,
                                                   Constants.UTIL_PERCENT_DISK
    };

    public UtilizationColumnManager(Properties props) {
        String[] columns = new String[]{Constants.UTIL_PERCENT_CPU,
                                        Constants.UTIL_PERCENT_MEMORY,
                                        Constants.UTIL_TOTAL_MEMORY,
                                        Constants.UTIL_FREE_MEMORY,
                                        Constants.UTIL_USED_MEMORY,
                                        Constants.UTIL_PERCENT_DISK,
                                        Constants.UTIL_AVAIL_DISK,
                                        Constants.UTIL_TOTAL_DISK,
                                        Constants.UTIL_PERCENT_CPU_PROC,
                                        Constants.UTIL_PERCENT_HEAP_JVM,
                                        Constants.UTIL_HEAP_MEM_JVM,
                                        Constants.UTIL_HEAP_MEM_AVAIL,
                                        Constants.UTIL_REAL_MEM_PROC
        };
        checkProperties(props, columns);
        if(selectedColumns.isEmpty()) {
            selectedColumns.addAll(Arrays.asList(defaultColumns));
        }

    }

    public String[] getSelectedColumns() {
        return selectedColumns.toArray(new String[selectedColumns.size()]);
    }

    public String[] getDefaultColumns() {
        return defaultColumns;
    }

    public void setSelectedColumns(String[] columns) {
        selectedColumns.clear();
        selectedColumns.addAll(Arrays.asList(columns));
    }

    private void checkProperties(Properties props, String[] columns) {
        Collection<Ordered> set = new TreeSet<Ordered>();
        for(String col : columns) {
            String s = props.getProperty(col);
            if(s!=null)
                set.add(new Ordered(col, Integer.parseInt(s)));
        }

        for(Ordered o : set) {
            selectedColumns.add(o.header);
        }
    }

    class Ordered implements Comparable<Ordered> {
        String header;
        int index;

        Ordered(String header, int index) {
            this.header = header;
            this.index = index;
        }

        public int compareTo(Ordered other) {
            return index<other.index?-1:(index>other.index?1:0);
        }
    }

}
