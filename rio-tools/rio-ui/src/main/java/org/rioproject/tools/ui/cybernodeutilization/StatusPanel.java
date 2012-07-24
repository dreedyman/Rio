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

import javax.swing.*;
import java.awt.*;

/**
 * @author Dennis Reedy
 */
public class StatusPanel extends JPanel {
    private final Font COMMON_FONT = new Font("Lucida Grande", 0, 10);
    private final UtilizationTreeModel utilizationTreeModel;

    public StatusPanel(UtilizationTreeModel utilizationTreeModel) {
        super();
        this.utilizationTreeModel = utilizationTreeModel;
        setDoubleBuffered(true);
    }

    public void paint(final Graphics g) {
        super.paint(g);
        drawStatus(g);
    }

    /*
    * Draw status items
    */
    void drawStatus(Graphics g) {
        int LEFT_MARGIN = 10;
        int LINE_HEIGHT = 14;
        int BASE_LINE = 15;

        /*ComputeResourceUtilization[] crus = utilizationModel.getValues();*/

        //long uTime = System.currentTimeMillis();
        //long cpuTime = uTime;

        /*for (ComputeResourceUtilization cru : crus) {
            if (cru == null)
                continue;
            double value = cru.getUtilization();
            if (value >= uHigh) {
                uHigh = value;
                //uHighAt = crus[i].getAddress();
                //uTime = System.currentTimeMillis();
            }
        }*/

        /*for (ComputeResourceUtilization cru : crus) {
            if (cru == null)
                continue;
            double value = getMeasuredValue("CPU", cru);
            if (value >= cpuHigh) {
                cpuHigh = value;
                //cpuHighAt = crus[i].getAddress();
                //cpuTime = System.currentTimeMillis();
            }
        }*/

        Font defaultFont = COMMON_FONT;
        FontMetrics fontMetrics = g.getFontMetrics(defaultFont);
        /* Get the length of the longest string*/
        int sLen = fontMetrics.stringWidth("Highest Utilization: ");
        g.setColor(Color.BLACK);
        g.setFont(defaultFont);
        int totalCybernodes = utilizationTreeModel.getCybernodeCount();
        int inUse = utilizationTreeModel.getCybernodesInUse();
        int free = totalCybernodes - inUse;
        g.drawString("Total Cybernodes: ",
                     LEFT_MARGIN,
                     BASE_LINE);
        g.drawString("" + totalCybernodes,
                     LEFT_MARGIN + sLen,
                     BASE_LINE);

        g.drawString("Cybernodes in use: ",
                     LEFT_MARGIN,
                     BASE_LINE + LINE_HEIGHT);
        g.drawString(inUse + ",   Cybernodes Free: " + free,
                     LEFT_MARGIN + sLen,
                     BASE_LINE + LINE_HEIGHT);
    }
}
