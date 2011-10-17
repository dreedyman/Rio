/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.system.cpu;

import org.rioproject.system.measurable.cpu.CalculableCPU;
import org.rioproject.watch.CalculableDisplayAttributes;
import org.rioproject.watch.DefaultCalculableView;
import org.rioproject.watch.FontDescriptor;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * View elements for a CalculableCPU
 *
 * @author Dennis Reedy
 */
public class CalculableCPUView extends DefaultCalculableView {
    public static final CalculableDisplayAttributes
        CPU_DISPLAY_ATTRS =
            new CalculableDisplayAttributes(
                            "CPU",
                            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                            0,
                            (NumberFormat)new DecimalFormat("##0.000"),
                            "Samples",
                            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                            0,
                            (NumberFormat)new DecimalFormat("####0"),
                            "CPU Utilization",
                            new FontDescriptor("Dialog", java.awt.Font.BOLD, 12),
                            CalculableCPU.class);

    /**
     * Creates new CalculableCPUView
     */
    public CalculableCPUView() {
        super(CalculableCPUView.CPU_DISPLAY_ATTRS);
    }

    /**
     * Creates new CalculableCPUView
     * 
     * @param calcDisplayAttrs the Calculable Display Attributes used to format
     * the graph
     */
    public CalculableCPUView(CalculableDisplayAttributes calcDisplayAttrs) {
        setCalculableDisplayAttributes(calcDisplayAttrs);
    }

    protected double getTopLineValue() {
        double topLine = 1.0;
        if(highThreshold>topLine)
            topLine = Math.max(highThreshold, max);
        return topLine;
    }

    protected double getBottomLineValue() {
        return(0.0);
    }
}
