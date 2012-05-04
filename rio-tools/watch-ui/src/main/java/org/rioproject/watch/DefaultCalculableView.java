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

import java.awt.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Default attributes for viewing a collection of Calculables
 */
public class DefaultCalculableView implements CalculableViewable {
    final static protected int TOP_AXIS_MARGIN = 20;
    final static protected int BOTTOM_AXIS_MARGIN = 20;
    final static protected int LEFT_AXIS_MARGIN = 50;
    final static protected int RIGHT_AXIS_MARGIN = 35;
    final static protected int TOP_GRAPH_MARGIN = 40;
    final static protected int BOTTOM_GRAPH_MARGIN = 21;
    final static protected int LEFT_GRAPH_MARGIN = 51;
    final static protected int RIGHT_GRAPH_MARGIN = 45;
    final static protected int VERTICAL_GRAPH_OFFSET = 10;
    final static protected int TOTAL_H_GRAPH_MARGIN = 
        LEFT_GRAPH_MARGIN + RIGHT_GRAPH_MARGIN;
    final static protected int TOTAL_V_GRAPH_MARGIN = 
        TOP_GRAPH_MARGIN + BOTTOM_GRAPH_MARGIN;
    final static protected int VERTICAL__MIN_GRID_HEIGHT = 15;
    final static protected int HORIZONTAL__MIN_GRID_WIDTH = 40;
    final static protected Color  devColor = new Color(204,0,204);
    protected static final Color avgColor = new Color(68,187,121);
    final public static DecimalFormat defaultFormater = new DecimalFormat("#0.0");
    public final static Font defaultFont = new Font("Dialog", Font.PLAIN, 10);

    protected static final CalculableDisplayAttributes defaultDisplayAttributes = 
        new CalculableDisplayAttributes(
                              "Value",
                              new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                              0,
                              (NumberFormat)new DecimalFormat("#,##0.###"),
                              "Events",
                              new FontDescriptor("Dialog", java.awt.Font.PLAIN, 10),
                              0,
                              (NumberFormat)new DecimalFormat("#,##0.###"),
                              "Measurement",
                              new FontDescriptor("Dialog", java.awt.Font.BOLD, 12),
                              Calculable.class);  
    protected WatchDataSource watchDataSource;
    protected Accumulator accum;
    protected Calculable[] data = null;
    protected double highThreshold = Double.NaN;
    protected double lowThreshold = Double.NaN;
    protected double startValue = 0;
    protected double stopValue = 0;
    protected double min = 0;
    protected double max = 0;
    protected double mean = 0;
    protected double stdDev = 0;
    protected String yLegend = "";
    protected Font yLegendFont = defaultFont;
    protected double yScale = 0;
    protected NumberFormat yFormat = defaultFormater;
    protected String xLegend = "";
    protected Font xLegendFont = defaultFont;
    protected double xScale = 0;
    protected NumberFormat xFormat = defaultFormater;
    protected String title = "";
    protected Font titleFont = defaultFont;
    protected CalculableDisplayAttributes calcDisplayAttrs;
    protected int currentSize = 0;
    protected double drawWidth = 0.0;
    protected double rangeSize = 0.0;
    protected double drawHeight = 0.0;
    protected double topLineValue = 0.0;
    protected double bottomLineValue = 0.0;
    protected double valuesRange = 0.0;
    protected double vStep = 0.0;
    protected double vStepMultiplier = 1;
    protected double nbrHGrids = 0.0;
    protected double maxY = 0.0;
    protected double minY = 0.0;
    private final java.util.List<PlottedCalculable> plotted =
        new ArrayList<PlottedCalculable>();

    /**
     * Creates new DefaultCalculableView
     */
    public DefaultCalculableView() {
        this(defaultDisplayAttributes);
    }

    /**
     * Creates new DefaultCalculableView
     * 
     * @param calcDisplayAttrs The Calculable Display Attributes used to
     * format the graph
     */
    public DefaultCalculableView(CalculableDisplayAttributes calcDisplayAttrs) {
        this.calcDisplayAttrs = calcDisplayAttrs;
    }

    /**
     * Sets the Calculable Display Attributes used to format the graph
     * 
     * @param calcDisplayAttrs The Calculable Display Attributes used to
     * format the graph
     */
    public void setCalculableDisplayAttributes(CalculableDisplayAttributes calcDisplayAttrs) {
        this.calcDisplayAttrs = calcDisplayAttrs;
    }

    /**
     * Sets the watch data source for this view
     * 
     * @param watchDataSource The WatchDataSource
     */
    public void setWatchDataSource(WatchDataSource watchDataSource) {
        resetValues();
        this.watchDataSource = watchDataSource;
        initValues();
    }

    /**
     * Reset the graph values to default values
     */
    protected void resetValues() {
        watchDataSource = null;
        data = null;
        highThreshold = Double.NaN;
        lowThreshold = Double.NaN;
        startValue = 0;
        stopValue = 0;
        min = 0;
        max = 0;
        mean = 0;
        stdDev = 0;
        yLegend = "";
        yLegendFont = defaultFont;
        yScale = 0;
        yFormat = defaultFormater;
        xLegend = "";
        xLegendFont = defaultFont;
        xScale = 0;
        xFormat = defaultFormater;
        title = "";
        titleFont = defaultFont;
        currentSize = 0;
        plotted.clear();
    }

    /**
     * Initialize the graph values from the WatchDataSource
     */
    protected void initValues() {
        try {
            currentSize = watchDataSource.getCurrentSize();
            ThresholdValues tvalues = watchDataSource.getThresholdValues();
            highThreshold = tvalues.getHighThreshold();
            lowThreshold = tvalues.getLowThreshold();
            accum = new Accumulator(watchDataSource);
            accum.init();
            data = accum.getCalcs();
            //if(data == null || data.length == 0) {
            //    return;
            //}
            yLegend = calcDisplayAttrs.getYLegend();
            FontDescriptor fd = calcDisplayAttrs.getYLegendFont();
            yLegendFont = new Font(fd.getName(), fd.getStyle(), fd.getSize());
            yScale = Math.pow(10, calcDisplayAttrs.getYScale());
            yFormat = calcDisplayAttrs.getYFormat();
            xLegend = calcDisplayAttrs.getXLegend();
            fd = calcDisplayAttrs.getXLegendFont();
            xLegendFont = new Font(fd.getName(), fd.getStyle(), fd.getSize());
            xScale = Math.pow(10, calcDisplayAttrs.getXScale());
            xFormat = calcDisplayAttrs.getXFormat();
            title = calcDisplayAttrs.getTitle();
            fd = calcDisplayAttrs.getTitleFont();
            titleFont = new Font(fd.getName(), fd.getStyle(), fd.getSize());
            //plot the records
            min = accum.min() / yScale;
            max = accum.max() / yScale;
            mean = accum.mean() / yScale; //average
            stdDev = accum.standardDeviation() / yScale;
            if(!Double.isNaN(highThreshold))
                highThreshold /= yScale;
            if(!Double.isNaN(lowThreshold))
                lowThreshold /= yScale;
            startValue = 0;
            stopValue = data.length - 1;
        } catch(RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Paints the view to the graphics context
     * 
     * @param g The Graphics context
     */
    public void paint(Graphics g, Dimension size) {
        try {
            g.setColor(new Color(232, 232, 232));
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(Color.black);
            
            // draw axis
            g.drawLine(LEFT_AXIS_MARGIN,
                       TOP_AXIS_MARGIN,
                       LEFT_AXIS_MARGIN,
                       size.height - BOTTOM_AXIS_MARGIN);
            g.drawLine(LEFT_AXIS_MARGIN,
                       size.height - BOTTOM_AXIS_MARGIN,
                       size.width - RIGHT_AXIS_MARGIN,
                       size.height - BOTTOM_AXIS_MARGIN);
            // draw axis labels
            g.setFont(yLegendFont);
            g.drawString(yLegend, LEFT_AXIS_MARGIN - 5, 15);
            g.setFont(xLegendFont);
            g.drawString(xLegend, size.width - 30, size.height - 15);
            g.setFont(titleFont);
            g.drawString(title, LEFT_GRAPH_MARGIN + 320, 20);
            if(data == null || data.length == 0)
                return;
            
            // draw textual info
            g.setFont(defaultFont);
            g.drawString("min=" + yFormat.format(min),
                         LEFT_GRAPH_MARGIN + 50,
                         10);
            g.drawString("max=" + yFormat.format(max),
                         LEFT_GRAPH_MARGIN + 50,
                         20);
            g.drawString("stdDev=" + yFormat.format(stdDev),
                         LEFT_GRAPH_MARGIN + 130,
                         10);
            g.drawString("mean=" + yFormat.format(mean),
                         LEFT_GRAPH_MARGIN + 130,
                         20);
            if(!Double.isNaN(highThreshold)) {
                g.drawString("highThreshold=" + yFormat.format(highThreshold),
                             LEFT_GRAPH_MARGIN + 210,
                             10);
            }
            if(!Double.isNaN(lowThreshold)) {
                g.drawString("lowThreshold=" + yFormat.format(lowThreshold),
                             LEFT_GRAPH_MARGIN + 210,
                             20);
            }
            
            // calculate key values
            drawWidth = size.width - TOTAL_H_GRAPH_MARGIN;
            drawHeight = size.height
                         - TOTAL_V_GRAPH_MARGIN
                         - VERTICAL_GRAPH_OFFSET;
            topLineValue = getTopLineValue();
            bottomLineValue = getBottomLineValue();
            if (bottomLineValue == topLineValue) {
                // zero range detected. We need to select some non-zero range
                if (bottomLineValue == 0) {
                    // if the range is [0,0], set it to [0,1]
                    topLineValue = 1;
                } else {
                    // if the range is [x,x], set it to [0,x]
                    bottomLineValue = 0;
                }
            }
            valuesRange = topLineValue - bottomLineValue;
            vStep = drawHeight / valuesRange;
            vStepMultiplier = 1;
            while ((vStep * vStepMultiplier) < VERTICAL__MIN_GRID_HEIGHT)
                vStepMultiplier++;
            nbrHGrids = valuesRange / vStepMultiplier + 1;
            maxY = drawHeight - vStep * valuesRange + TOP_GRAPH_MARGIN;
            minY = drawHeight + TOP_GRAPH_MARGIN;
            
            double startValue = this.startValue;
            double stopValue = this.stopValue;
            if (stopValue == startValue) {
                stopValue++;
            }
            rangeSize = stopValue - startValue;

            // draw vertical grid            
            double divider = 1;
            while (drawWidth / (rangeSize / divider) < HORIZONTAL__MIN_GRID_WIDTH)
                divider++;
            double nbrVLabels = rangeSize / divider;
            double xLabelsStep = drawWidth / nbrVLabels;
            double xLabelValIncrement = rangeSize / nbrVLabels;
            g.setColor(Color.lightGray);
            for(double i = 1; i <= nbrVLabels; i++) {
                int x = (int) (LEFT_AXIS_MARGIN + i * xLabelsStep);
                g.drawLine(x,
                           TOP_AXIS_MARGIN + 10,
                           x,
                           size.height - BOTTOM_GRAPH_MARGIN);
            }            
            // draw x-axis labels
            g.setFont(xLegendFont);
            g.setColor(Color.black);
            for(double i = 0, lValue = /* startRange */startValue; 
                     i <= nbrVLabels; i++, lValue += xLabelValIncrement) {
                g.drawString(String.valueOf((int)lValue + 1),
                             (int)(45 + i * xLabelsStep),
                             (size.height - 10));
            }
            
            // draw horizontal grid and y-axis labels            
            g.setFont(yLegendFont);
            FontMetrics fm = g.getFontMetrics();
            double vGridStart = ((int) (topLineValue / vStepMultiplier))
                                * vStepMultiplier;
            double lValue = vGridStart;
            for(int i = 0; i < nbrHGrids; i++, lValue -= vStepMultiplier) {
                double y = vGridStart - i * vStepMultiplier;
                int pY = (int) (minY - vStep * (y - bottomLineValue));
                if (pY >= size.height - BOTTOM_AXIS_MARGIN) {
                    // nbrHGrids is not always calculated correctly,
                    // that's why we need this check
                    break;
                }                
                // line
                g.setColor(Color.lightGray);
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY);
                // label
                g.setColor(Color.black);
                String fmtVal = yFormat.format(lValue);
                g.drawString(fmtVal, 48 - fm.stringWidth(fmtVal), pY + 3);
            }
            
            // draw highThreshold
            if(!Double.isNaN(highThreshold)) {
                g.setColor(Color.red);
                int pY = (int) (minY - vStep * (highThreshold - bottomLineValue));
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY);
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY + 1,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY + 1);
            }
            // draw lowThreshold
            if(!Double.isNaN(lowThreshold)) {
                g.setColor(Color.orange);
                int pY = (int) (minY - vStep * (lowThreshold - bottomLineValue));
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY);
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY + 1,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY + 1);
            }
            
            // draw mean
            if (stdDev > 0) {
                g.setColor(avgColor);
                int pY = (int) (minY - vStep * (mean - bottomLineValue));
                g.drawLine(LEFT_GRAPH_MARGIN,
                           pY,
                           size.width - RIGHT_AXIS_MARGIN,
                           pY);
                g.drawString("mean", size.width - 30, pY + 2);
            }
                
            // draw deviations
            if (!Double.isNaN(stdDev) && stdDev > 0) {
                int uY = (int) (minY
                               - vStep
                               * ((mean + stdDev) - bottomLineValue));
                int dY = (int) (minY
                               - vStep
                               * ((mean - stdDev) - bottomLineValue));
                g.setColor(devColor);
                for(int x = LEFT_GRAPH_MARGIN, len = 
                    size.width - RIGHT_AXIS_MARGIN; x < len; x += 10) {
                    g.drawLine(x, uY, x + 5, uY);
                    g.drawLine(x, dY, x + 5, dY);
                }
                g.drawString("dev", size.width - 30, uY + 2);
                g.drawString("dev", size.width - 30, dY + 2);
            }

            // draw graph of Calculable values
            double hStep = drawWidth / rangeSize;
            double idxJump = 1;
            if(hStep == 0) {
                hStep = 1;
                idxJump = rangeSize / drawWidth;
            }
            double dataIdx =0;            
            g.setColor(Color.blue);

            synchronized(plotted) {
                plotted.clear();
            }
            Point lastPoint = null;
            Calculable lastCalc = null;
            // if their is only one point - draw it in a special way
            int len = getLength();
            if (len == 1 && dataIdx < len) {
                lastCalc = data[(int)dataIdx];
                double value = lastCalc.getValue();
                double y = value / yScale;
                int pY = (int) (minY - vStep * (y - bottomLineValue));
                lastPoint = new Point(LEFT_GRAPH_MARGIN - 2, pY - 1);
                g.fillOval(LEFT_GRAPH_MARGIN - 2, pY - 1, 3, 3);
            }
            
            // draw segments

            for(double pX = LEFT_GRAPH_MARGIN;
                dataIdx < len;
                dataIdx += idxJump, pX += hStep) {

                Calculable calc = data[(int)dataIdx];
                double y = calc.getValue() / yScale;
                double pY = minY - vStep * (y - bottomLineValue);
                int index = (int)(dataIdx + idxJump);
                if(index >= len)
                    break;
                if(pX >= (size.width - RIGHT_GRAPH_MARGIN))
                    break;
                Calculable nextCalc = data[index];
                double nextY = nextCalc.getValue() / yScale;
                double pNextY = minY - vStep * (nextY - bottomLineValue);
                int startX = (int)pX;
                int startY = (int)(pY > minY ? minY : pY);
                synchronized(plotted) {
                    plotted.add(new PlottedCalculable(new Point(startX, startY),
                                                      calc));
                }
                int endX = (int)(pX + hStep);
                int endY = (int)(pNextY > minY ? minY : pNextY);
                g.drawLine(startX, startY, endX, endY);
                lastCalc = nextCalc;
                lastPoint = new Point(endX, endY);
            }
            if(lastPoint!=null && lastCalc!=null) {
                synchronized(plotted) {
                    plotted.add(new PlottedCalculable(lastPoint, lastCalc));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public PlottedCalculable getCalcForPoint(Point point) {
        PlottedCalculable calc = null;
        synchronized(plotted) {
            for(PlottedCalculable p : plotted) {
                if(p.getPoint().getX()==point.getX()) {
                    calc = p;
                    break;
                }
            }
        }
        return calc;
    }

    /**
     * @return Get the length property
     */
    protected int getLength() {
        return (data == null ? 0 : data.length);
    }

    /**
     * @return The value for the top Y axis
     */
    protected double getTopLineValue() {
        double topLineValue = mean + 3 * stdDev;
        topLineValue = Math.max(topLineValue, max);
        if(!Double.isNaN(highThreshold)) {
            topLineValue = Math.max(topLineValue, highThreshold);
        }
        return topLineValue;
    }

    /**
     * @return The value for the bottom Y axis
     */
    protected double getBottomLineValue() {
        double bottomLineValue = mean - 3 * stdDev;
        bottomLineValue = Math.min(bottomLineValue, min);
        if(!Double.isNaN(lowThreshold)) {
          bottomLineValue = Math.min(bottomLineValue, lowThreshold);
        }        
        return bottomLineValue;
    }

}
