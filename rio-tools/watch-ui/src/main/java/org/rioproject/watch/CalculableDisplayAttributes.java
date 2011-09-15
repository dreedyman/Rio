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
    static final long serialVersionUID = 1L;

    /** Holds value of property yLegend. */
    private String yLegend;
    /** Holds value of property yLegendFont. */
    private FontDescriptor yLegendFont;
    /** Holds value of property yScale. */
    private int yScale;
    /** Holds value of property yFormat. */
    private NumberFormat yFormat;
    /** Holds value of property xLegend. */
    private String xLegend;
    /** Holds value of property xLegendFont. */
    private FontDescriptor xLegendFont;
    /** Holds value of property xScale. */
    private int xScale;
    /** Holds value of property xFormat. */
    private NumberFormat xFormat;
    /** Holds value of property title. */
    private String title;
    /** Holds value of property titleFont. */
    private FontDescriptor titleFont;
    /** Holds value of property calculableClass. */
    private Class calculableClass;

    /** 
     * Creates new CalculableDisplay 
     */
    public CalculableDisplayAttributes() {
    }

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
    public CalculableDisplayAttributes(String yLegend, 
                                       FontDescriptor yLegendFont, 
                                       int yScale,
                                       NumberFormat yFormat, 
                                       String xLegend, 
                                       FontDescriptor xLegendFont, 
                                       int xScale,
                                       NumberFormat xFormat, 
                                       String title, 
                                       FontDescriptor titleFont, 
                                       Class calculableClass) {
        setYLegend(yLegend);
        setYLegendFont(yLegendFont);
        setYScale(yScale);
        setYFormat(yFormat);
        setXLegend(xLegend);
        setXLegendFont(xLegendFont);
        setXScale(xScale);
        setXFormat(xFormat);
        setTitle(title);
        setTitleFont(titleFont);
        setCalculableClass(calculableClass);

    }

    /** 
     * Getter for property yLegend.
     * @return Value of property yLegend.
     */
    public String getYLegend() {
        return(yLegend);
    }

    /** 
     * Setter for property yLegend.
     * @param yLegend New value of property yLegend.
     */
    public void setYLegend(String yLegend) {
        this.yLegend = yLegend;
    }

    /** 
     * Getter for property yLegendFont.
     * @return Value of property yLegendFont.
     */
    public FontDescriptor getYLegendFont() {
        return(yLegendFont);
    }

    /** 
     * Setter for property yLegendFont.
     * @param yLegendFont New value of property yLegendFont.
     */
    public void setYLegendFont(FontDescriptor yLegendFont) {
        this.yLegendFont = yLegendFont;
    }

    /** 
     * Getter for property yScale.
     * @return Value of property yScale.
     */
    public int getYScale() {
        return(yScale);
    }

    /** 
     * Setter for property yScale.
     * @param yScale New value of property yScale.
     */
    public void setYScale(int yScale) {
        this.yScale = yScale;
    }

    /** 
     * Getter for property yFormat.
     * @return Value of property yFormat.
     */
    public NumberFormat getYFormat() {
        return(yFormat);
    }

    /** 
     * Setter for property yFormat.
     * @param yFormat New value of property yFormat.
     */
    public void setYFormat(NumberFormat yFormat) {
        this.yFormat = yFormat;
    }

    /** 
     * Getter for property xLegend.
     * @return Value of property xLegend.
     */
    public String getXLegend() {
        return(xLegend);
    }

    /** 
     * Setter for property xLegend.
     * @param xLegend New value of property xLegend.
     */
    public void setXLegend(String xLegend) {
        this.xLegend = xLegend;
    }

    /** 
     * Getter for property xLegendFont.
     * @return Value of property xLegendFont.
     */
    public FontDescriptor getXLegendFont() {
        return(xLegendFont);
    }

    /** 
     * Setter for property xLegendFont.
     * @param xLegendFont New value of property xLegendFont.
     */
    public void setXLegendFont(FontDescriptor xLegendFont) {
        this.xLegendFont = xLegendFont;
    }

    /** 
     * Getter for property xScale.
     * @return Value of property xScale.
     */
    public int getXScale() {
        return(xScale);
    }

    /** 
     * Setter for property xScale.
     * @param xScale New value of property xScale.
     */
    public void setXScale(int xScale) {
        this.xScale = xScale;
    }

    /** 
     * Getter for property xFormat.
     * @return Value of property xFormat.
     */
    public NumberFormat getXFormat() {
        return(xFormat);
    }

    /** 
     * Setter for property xFormat.
     * @param xFormat New value of property xFormat.
     */
    public void setXFormat(NumberFormat xFormat) {
        this.xFormat = xFormat;
    }

    /** 
     * Getter for property title.
     * @return Value of property title.
     */
    public String getTitle() {
        return(title);
    }

    /** 
     * Setter for property title.
     * @param title New value of property title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /** 
     * Getter for property titleFont.
     * @return Value of property titleFont.
     */
    public FontDescriptor getTitleFont() {
        return(titleFont);
    }

    /** 
     * Setter for property titleFont.
     * @param titleFont New value of property titleFont.
     */
    public void setTitleFont(FontDescriptor titleFont) {
        this.titleFont = titleFont;
    }

    public String toString() {
        return("Title = " + title + "\n" +
              "Title Font = " + titleFont + "\n" +
              "Y Legend = " + yLegend + "\n" +
              "Y Legend Font = " + yLegendFont + "\n" +
              "Y Scale = " + yScale + "\n" +
              "Y Format = " + yFormat + "\n" +
              "X Legend = " + xLegend + "\n" +
              "X Legend Font = " + xLegendFont + "\n" +
              "X Scale = " + xScale + "\n" +
              "X Format = " + xFormat + "\n" +
              "CalculableClass = " + calculableClass.getName() + "\n");
    }

    /** 
     * Getter for property calculableClass.
     * @return Value of property calculableClass.
     */
    public Class getCalculableClass() {
        return(calculableClass);
    }

    /** 
     * Setter for property calculableClass.
     * @param calculableClass New value of property calculableClass.
     */
    public void setCalculableClass(Class calculableClass) {
        this.calculableClass = calculableClass;
    }

    public BeanInfo getCalculableBeanInfo() throws IntrospectionException {
        return(calculableClass!=null?
              Introspector.getBeanInfo(calculableClass):
              null);
    }
}
