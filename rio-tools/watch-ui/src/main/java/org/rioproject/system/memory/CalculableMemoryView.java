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
package org.rioproject.system.memory;

import org.rioproject.system.measurable.memory.CalculableMemory;
import org.rioproject.watch.CalculableDisplayAttributes;
import org.rioproject.watch.DefaultCalculableView;
import org.rioproject.watch.FontDescriptor;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * View elements for a CalculableMemory
 *
 * @author Dennis Reedy
 */
public class CalculableMemoryView extends DefaultCalculableView {
    private static final CalculableDisplayAttributes
        defaultDisplayAttributes =
        new CalculableDisplayAttributes(
            "Memory",
            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
            0,
            (NumberFormat)new DecimalFormat("##0.000"),
            "Samples",
            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
            0,
            (NumberFormat)new DecimalFormat("####0"),
            "Process Memory",
            new FontDescriptor("Dialog", java.awt.Font.BOLD, 12),
            CalculableMemory.class);
    /** 
     * Creates CalculableMemoryView 
     */
    public CalculableMemoryView() {
        super(CalculableMemoryView.defaultDisplayAttributes);
    }

    /** 
     * Creates new CalculableMemoryView 
     *
     * @param calcDisplayAttrs The CalculableDisplayAttributes used to format the graph
     */
    public CalculableMemoryView(CalculableDisplayAttributes calcDisplayAttrs) {
        setCalculableDisplayAttributes(calcDisplayAttrs);
    }

    protected double getTopLineValue() {
        return(1.0);
    }

    protected double getBottomLineValue() {
        return(0.0);
    }
}
