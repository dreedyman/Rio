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

/**
 * Interface defining semantics for viewing a Calculable record
 */
public interface CalculableViewable {
    /**
     * Sets the watch data source for this view
     *
     * @param watchDataSource the watch data source.
     */
    void setWatchDataSource(WatchDataSource watchDataSource);
    
    /**
     * Paints the view to the graphics context
     *
     * @param g the graphics context
     * @param size The Dimension to paint
     */
    void paint(Graphics g, Dimension size);

    /**
     * Get the Calculable value for a corresponding point
     *
     * @param p The point
     *
     * @return If the plotted Calculable point's X value is equal to the
     * input point's X value return the PlottedCalculable. Otherwise return null
     */
    PlottedCalculable getCalcForPoint(Point p);

    /**
     * Data structure that holds as properties the the Calculable and the Point
     * it is plotted at
     */
    public static class PlottedCalculable {
        private Point point;
        private Calculable calculable;

        public PlottedCalculable(Point point, Calculable calculable) {
            this.point = point;
            this.calculable = calculable;
        }

        public Point getPoint() {
            return point;
        }

        public Calculable getCalculable() {
            return calculable;
        }
    }
}
