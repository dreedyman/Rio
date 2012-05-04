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

import java.text.NumberFormat;
import java.io.Serializable;
import java.beans.*;

/**
 * Default attributes to display when providing rendering support for a Calculable
 */
public class CalculableDisplayAttributes implements Serializable {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /** Holds value of property yLegend. */
    private final String yLegend;
    /** Holds value of property yLegendFont. */
    private final FontDescriptor yLegendFont;
    /** Holds value of property yScale. */
    private final int yScale;
    /** Holds value of property yFormat. */
    private final NumberFormat yFormat;
    /** Holds value of property xLegend. */
    private final String xLegend;
    /** Holds value of property xLegendFont. */
    private final FontDescriptor xLegendFont;
    /** Holds value of property xScale. */
    private final int xScale;
    /** Holds value of property xFormat. */
    private final NumberFormat xFormat;
    /** Holds value of property title. */
    private final String title;
    /** Holds value of property titleFont. */
    private final FontDescriptor titleFont;
    /** Holds value of property calculableClass. */
    private final Class calculableClass;

    /**
     * Creates new CalculableDisplay with provided inputs:
     *
     * @param yLegend String name of the Y Axis
     * @param yLegendFont Font to use for the Y Axis
     * @param yScale scale for the Y Axis
     * @param yFormat NumberFormat of the Y Axis
     * @param xLegend String name of the Y Axis
     * @param xLegendFont Font to use for the X Axis
     * @param xScale scale for the X Axis
     * @param xFormat NumberFormat of the X Axis
     * @param title String Title of the display, ex: Memory Usage
     * @param titleFont Font to use for the title
     * @param calculableClass Class Calculable class object for this display
     */
    public CalculableDisplayAttributes(final String yLegend,
                                       final FontDescriptor yLegendFont,
                                       final int yScale,
                                       final NumberFormat yFormat,
                                       final String xLegend,
                                       final FontDescriptor xLegendFont,
                                       final int xScale,
                                       final NumberFormat xFormat,
                                       final String title,
                                       final FontDescriptor titleFont,
                                       final Class calculableClass) {
        this.yLegend = yLegend;
        this.yLegendFont = yLegendFont;
        this.yScale = yScale;
        this.yFormat = yFormat;
        this.xLegend = xLegend;
        this.xLegendFont = xLegendFont;
        this.xScale = xScale;
        this.xFormat = xFormat;
        this.title = title;
        this.titleFont = titleFont;
        this.calculableClass = calculableClass;

    }

    /** 
     * Getter for property yLegend.
     * @return Value of property yLegend.
     */
    public String getYLegend() {
        return(yLegend);
    }

    /** 
     * Getter for property yLegendFont.
     * @return Value of property yLegendFont.
     */
    public FontDescriptor getYLegendFont() {
        return(yLegendFont);
    }

    /** 
     * Getter for property yScale.
     * @return Value of property yScale.
     */
    public int getYScale() {
        return(yScale);
    }

    /** 
     * Getter for property yFormat.
     * @return Value of property yFormat.
     */
    public NumberFormat getYFormat() {
        return(yFormat);
    }

    /**
     * Getter for property xLegend.
     * @return Value of property xLegend.
     */
    public String getXLegend() {
        return(xLegend);
    }

    /**
     * Getter for property xLegendFont.
     * @return Value of property xLegendFont.
     */
    public FontDescriptor getXLegendFont() {
        return(xLegendFont);
    }

    /** 
     * Getter for property xScale.
     * @return Value of property xScale.
     */
    public int getXScale() {
        return(xScale);
    }

    /**
     * Getter for property xFormat.
     * @return Value of property xFormat.
     */
    public NumberFormat getXFormat() {
        return(xFormat);
    }

    /**
     * Getter for property title.
     * @return Value of property title.
     */
    public String getTitle() {
        return(title);
    }

    /**
     * Getter for property titleFont.
     * @return Value of property titleFont.
     */
    public FontDescriptor getTitleFont() {
        return(titleFont);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Title = ").append(title).append("\n");
        builder.append("Title Font = ").append(titleFont).append("\n");
        builder.append("Y Legend = ").append(yLegend).append("\n");
        builder.append("Y Legend Font = ").append(yLegendFont).append("\n");
        builder.append("Y Scale = ").append(yScale).append("\n");
        builder.append("Y Format = ").append(yFormat).append("\n");
        builder.append("X Legend = ").append(xLegend).append("\n");
        builder.append("X Legend Font = ").append(xLegendFont).append("\n");
        builder.append("X Scale = ").append(xScale).append("\n");
        builder.append("X Format = ").append(xFormat).append("\n");
        builder.append("CalculableClass = ").append(calculableClass.getName());
        return builder.toString();
    }

    /** 
     * Getter for property calculableClass.
     * @return Value of property calculableClass.
     */
    @SuppressWarnings("unused")
    public Class getCalculableClass() {
        return(calculableClass);
    }

    @SuppressWarnings("unused")
    public BeanInfo getCalculableBeanInfo() throws IntrospectionException {
        return(calculableClass!=null? Introspector.getBeanInfo(calculableClass): null);
    }
}
