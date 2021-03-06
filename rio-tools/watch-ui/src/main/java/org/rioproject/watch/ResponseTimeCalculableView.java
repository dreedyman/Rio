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
package org.rioproject.watch;
import java.text.*;

/**
 * A view for a response time Calculable
 */
public class ResponseTimeCalculableView extends DefaultCalculableView {
    private static final CalculableDisplayAttributes 
        defaultDisplayAttributes = 
            new CalculableDisplayAttributes(
                           "msecs",
                           new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                           0,
                           (NumberFormat)new DecimalFormat("#,##0"),
                           "Events",
                           new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                           0,
                           (NumberFormat)new DecimalFormat("#,##0"),
                           "Response Time",
                           new FontDescriptor("Dialog", java.awt.Font.BOLD, 12),
                           Calculable.class);

    /**
     * Create a new ResponseTimeCalculableView
     */
    public ResponseTimeCalculableView() {
        super(ResponseTimeCalculableView.defaultDisplayAttributes);
    }

    /**
     * Create a new DefaultCalculableView
     * 
     * @param calcDisplayAttrs The Calculable Display Attributes used to
     * format the graph
     */
    public ResponseTimeCalculableView(
        CalculableDisplayAttributes calcDisplayAttrs) {
        setCalculableDisplayAttributes(calcDisplayAttrs);
    }

    /**
     * Get the bottom line value
     * 
     * @return The value of the bottom line in the view
     */
    protected double getBottomLineValue() {
        double b = super.getBottomLineValue();
        return (b < 0.0 ? 0.0 : b);
    }
}

