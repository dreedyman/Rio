/*
 * Copyright 2008 the original author or authors.
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

import prefuse.action.assignment.ColorAction;
import prefuse.data.expression.Predicate;
import prefuse.util.ColorLib;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages colors for elements in the UI
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class ColorManager {
    public static final int BORDER_COLOR_RGB = ColorLib.rgb(255,255,255);
    public static final int HOVER_COLOR_RGB = ColorLib.rgb(102,0,102);
    public static final int ROOT_COLOR_RGB = ColorLib.rgb(255, 153, 0);
    public static final int EMPTY_COLOR_RGB = ColorLib.rgb(131, 139, 139);
    public static final int WARNING_COLOR_RGB = ColorLib.rgb(255, 170, 15);
    public static final int OKAY_COLOR_RGB = ColorLib.rgb(35, 142, 35);
    public static final int FAILURE_COLOR_RGB = ColorLib.rgb(205,38,38);
    public static final int EDGE_COLOR_RGB = ColorLib.rgb(153, 153, 153);

    private Color okayColor;
    private Color warningColor;
    private Color failureColor;
    private ColorAction colorAction;
    private Predicate okayFilter;
    private Predicate ambiguousFilter;
    private Predicate noServiceItemFilter;
    private Predicate failureFilter;

    public ColorManager(Properties props) {
        if(props==null)
            throw new IllegalArgumentException("properties is null");
        int rgb = Integer.parseInt(props.getProperty(Constants.FAILURE_COLOR, Integer.toString(FAILURE_COLOR_RGB)));
        setFailureColor(new Color(rgb));

        rgb = Integer.parseInt(props.getProperty(Constants.OKAY_COLOR, Integer.toString(OKAY_COLOR_RGB)));
        setOkayColor(new Color(rgb));

        rgb = Integer.parseInt(props.getProperty(Constants.WARNING_COLOR, Integer.toString(WARNING_COLOR_RGB)));
        setWarningColor(new Color(rgb));
    }
    
    public void setOkayFilter(Predicate okayFilter) {
        this.okayFilter = okayFilter;
    }

    public void setAmbiguousFilter(Predicate ambiguousFilter) {
        this.ambiguousFilter = ambiguousFilter;
    }

    public void setFailureFilter(Predicate failureFilter) {
        this.failureFilter = failureFilter;
    }

    public void setNoServiceItemFilter(Predicate noServiceItemFilter) {
        this.noServiceItemFilter = noServiceItemFilter;
    }

    public void setColorAction(ColorAction colorAction) {
        this.colorAction = colorAction;
    }

    public Color getOkayColor() {
        return okayColor;
    }

    public void setOkayColor(Color okayColor) {
        if(okayColor==null)
            throw new IllegalArgumentException("color is null");
        this.okayColor = okayColor;
        if(okayFilter != null && colorAction!=null) {
            swapPredicateColor(okayFilter, okayColor);
        }
    }

    public Color getWarningColor() {
        return warningColor;
    }

    public void setWarningColor(Color warningColor) {
        if(warningColor==null)
            throw new IllegalArgumentException("color is null");
        this.warningColor = warningColor;
        if(ambiguousFilter != null && colorAction!=null) {
            swapPredicateColor(ambiguousFilter, warningColor);
        }
        if(noServiceItemFilter != null && colorAction!=null) {
            swapPredicateColor(noServiceItemFilter, warningColor);
        }
    }

    public Color getFailureColor() {
        return failureColor;
    }

    public void setFailureColor(Color failureColor) {
        if(failureColor==null)
            throw new IllegalArgumentException("color is null");
        this.failureColor = failureColor;
        if(failureFilter != null && colorAction!=null) {
            swapPredicateColor(failureFilter, failureColor);
        }
    }

    public Map<String, Color> getDefaultColorMap() {
        Map<String, Color> map = new HashMap<String, Color>();
        map.put(Constants.FAILURE_COLOR, new Color(FAILURE_COLOR_RGB));
        map.put(Constants.OKAY_COLOR, new Color(OKAY_COLOR_RGB));
        map.put(Constants.WARNING_COLOR, new Color(WARNING_COLOR_RGB));
        return map;
    }

    private void swapPredicateColor(Predicate predicate, Color color) {
        if(colorAction.remove(predicate)) {
            colorAction.add(predicate,ColorLib.color(color));
        }
    }
}
