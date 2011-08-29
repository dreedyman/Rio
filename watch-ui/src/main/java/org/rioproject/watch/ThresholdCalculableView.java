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
package org.rioproject.watch;
import java.text.*;

/**
 *  A ThresholdCalculableView is used to render threshold watches
 */
public class ThresholdCalculableView extends DefaultCalculableView {
    private static final CalculableDisplayAttributes 
        defaultDisplayAttributes = 
            new CalculableDisplayAttributes(
                            "Count",
                            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                            0,
                            (NumberFormat)new DecimalFormat("####0"),
                            "Samples",
                            new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                            0,
                            (NumberFormat)new DecimalFormat("####0"),
                            "Threshold",
                            new FontDescriptor("Dialog", java.awt.Font.BOLD, 12),
                            Calculable.class);

    /** 
     * Create a new ThresholdCalculableView 
     */
    public ThresholdCalculableView() {
        super(ThresholdCalculableView.defaultDisplayAttributes);
    }

    /**
     * Create a new DefaultCalculableView
     * 
     * @param calcDisplayAttrs The Calculable Display Attributes used to
     * format the graph
     */
    public ThresholdCalculableView(CalculableDisplayAttributes calcDisplayAttrs) {
        setCalculableDisplayAttributes(calcDisplayAttrs);
    }

}
