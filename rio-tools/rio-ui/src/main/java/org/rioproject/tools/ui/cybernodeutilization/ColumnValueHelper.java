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
package org.rioproject.tools.ui.cybernodeutilization;

import org.jdesktop.swingx.JXTreeTable;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.SystemWatchID;
import org.rioproject.system.measurable.cpu.CpuUtilization;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.system.measurable.memory.ProcessMemoryUtilization;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.tools.ui.Constants;
import org.rioproject.tools.ui.UtilizationColumnManager;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.NumberFormat;

/**
 * @author Dennis Reedy
 */
public class ColumnValueHelper {
    final static String FIXED_COLUMN = "Host Name";
    private final JXTreeTable treeTable;
    private final UtilizationColumnManager utilizationColumnManager;
    private final NumberFormat percentFormatter;
    private final NumberFormat numberFormatter;

    public ColumnValueHelper(final UtilizationColumnManager utilizationColumnManager,
                             final JXTreeTable treeTable) {
        this.utilizationColumnManager = utilizationColumnManager;
        this.treeTable = treeTable;
        percentFormatter = NumberFormat.getPercentInstance();
        numberFormatter = NumberFormat.getNumberInstance();
        /* Display 3 digits of precision */
        percentFormatter.setMaximumFractionDigits(3);
        /* Display 2 digits of precision */
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
    }

    public int getColumnCount() {
        return utilizationColumnManager.getSelectedColumns().length+1;
    }

    public Object getColumnValue(final int columnIndex,
                                 final ComputeResourceUtilization cru,
                                 final boolean includeSystemInfo) {
        String cName = getColumnName(columnIndex);

        if(cName==null) {
            System.out.println(">> FAILED!!!, getColumnValue for index ("+columnIndex+")");
            return null;
        }
        String value = null;

        if(cru==null)
            return "?";

       if(includeSystemInfo) {
            if(cName.equals(Constants.UTIL_PERCENT_CPU)) {
                CpuUtilization cpu = cru.getCpuUtilization();
                value = (cpu == null ? "?" : formatPercent(cpu.getValue()));

            } else if(cName.equals(Constants.UTIL_PERCENT_MEMORY)) {
                SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                value = (mem == null ? "?" : formatPercent(mem.getValue()));

            } else if(cName.equals(Constants.UTIL_TOTAL_MEMORY)) {
                SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                value = (mem == null ? "?" : format(mem.getTotal()," MB"));

            } else if(cName.equals(Constants.UTIL_FREE_MEMORY)) {
                SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                value = (mem == null ? "?" : format(mem.getFree()," MB"));

            } else if(cName.equals(Constants.UTIL_USED_MEMORY)) {
                SystemMemoryUtilization mem = cru.getSystemMemoryUtilization();
                value = (mem == null ? "?" : format(mem.getUsed()," MB"));

            } else if(cName.equals(Constants.UTIL_PERCENT_DISK)) {
                DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                value = (disk==null?"?" : formatPercent(disk.getValue()));

            } else if(cName.equals(Constants.UTIL_AVAIL_DISK)) {
                DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                value = (disk==null?"?" : format(disk.getAvailable()/MeasuredValueHelper.GB," GB"));

            } else if(cName.equals(Constants.UTIL_TOTAL_DISK)) {
                DiskSpaceUtilization disk = cru.getDiskSpaceUtilization();
                value = (disk==null?"?" : format(disk.getCapacity()/MeasuredValueHelper.GB," GB"));
            }
        }

        if(cName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
            value = formatPercent(MeasuredValueHelper.getMeasuredValue(SystemWatchID.PROC_CPU, cru));

        } else if(cName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
            value = formatPercent(MeasuredValueHelper.getMeasuredValue(SystemWatchID.JVM_MEMORY, cru));
            //ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            //value = (mem==null?"?" : format(mem.getUsedHeap()));

        } else if(cName.equals(Constants.UTIL_HEAP_MEM_JVM)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            //value = (mem==null?"?" : format(mem.getCommittedHeap())+" MB");
            value = (mem==null?"?" : format(mem.getUsedHeap()," MB"));

        } else if(cName.equals(Constants.UTIL_HEAP_MEM_AVAIL)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            //value = (mem==null?"?" : format(mem.getCommittedHeap())+" MB");
            value = (mem==null?"?" : format(mem.getCommittedHeap(), " MB"));

        } else if(cName.equals(Constants.UTIL_REAL_MEM_PROC)) {
            ProcessMemoryUtilization mem = cru.getProcessMemoryUtilization();
            value = (mem==null?"?" : format(mem.getResident(), " MB"));
        }
        if(isAThresholdColumn(cName)) {
            return getThresholdLabel(cName, value, cru);
        }
        return (value);
    }

    private String getColumnName(final int index) {
        if(index==0)
            return FIXED_COLUMN;
        String cName = null;
        if(index < treeTable.getColumnModel().getColumnCount()) {
            TableColumn column = treeTable.getColumnModel().getColumn(index);
            cName = (String)column.getHeaderValue();
        }
        return cName;
    }

    private String formatPercent(final Double value) {
        if (value != null && !Double.isNaN(value))
            return (percentFormatter.format(value.doubleValue()));
        return ("?");
    }

    private String format(final Double value, final String units) {
        if (value != null && !Double.isNaN(value) && value!=-1)
            return (numberFormatter.format(value.doubleValue())+units);
        return ("?");
    }

    private boolean isAThresholdColumn(final String columnName) {
        boolean isA = false;
        if(columnName.equals(Constants.UTIL_PERCENT_CPU)      ||
           columnName.equals(Constants.UTIL_PERCENT_MEMORY)   ||
           columnName.equals(Constants.UTIL_PERCENT_DISK)     ||
           columnName.equals(Constants.UTIL_PERCENT_CPU_PROC) ||
           columnName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
            isA = true;
        }
        return isA;
    }

    private JLabel getThresholdLabel(final String columnName, final String text, final ComputeResourceUtilization cru) {
        JLabel label = new JLabel(text);
        MeasuredResource mRes = getMeasuredResource(columnName, cru);
        if(mRes!=null && mRes.thresholdCrossed()) {
            label.setForeground(new Color(178, 34, 34));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        } else {
            label.setForeground(new Color(0, 100, 0));
        }
        return label;
    }

    private MeasuredResource getMeasuredResource(final String columnName, final ComputeResourceUtilization cru) {
        MeasuredResource mRes = null;
        if(columnName.equals(Constants.UTIL_PERCENT_CPU)) {
            mRes = cru.getCpuUtilization();
        } else if(columnName.equals(Constants.UTIL_PERCENT_MEMORY)) {
            mRes = cru.getSystemMemoryUtilization();
        } else if(columnName.equals(Constants.UTIL_PERCENT_DISK)) {
            mRes = cru.getDiskSpaceUtilization();
        } else if(columnName.equals(Constants.UTIL_PERCENT_CPU_PROC)) {
            mRes = cru.getProcessCpuUtilization();
        } else if(columnName.equals(Constants.UTIL_PERCENT_HEAP_JVM)) {
            mRes = cru.getProcessMemoryUtilization();
        }
        return mRes;
    }

}
