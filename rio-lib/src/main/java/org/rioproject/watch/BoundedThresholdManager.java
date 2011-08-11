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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The BoundedThresholdManager provides support for threshold handling as
 * follows: A ThresholdNotify.notify method is invoked when the high threshold
 * is breached, and again invoked when the Calculable drops below the high
 * threshold. Low threshold processing occurs in the same manner
 */
public class BoundedThresholdManager extends ThresholdManager {
    private boolean thresholdCrossed = false;
    private static final int CLEARED = 0;
    private static final int BREACHED_LOWER = 1;
    private static final int BREACHED_UPPER = 2;
    private int direction = 0;
    static Logger logger = Logger.getLogger("org.rioproject.watch");

    public boolean getThresholdCrossed() {
        return (thresholdCrossed);
    }
    
    public void checkThreshold(Calculable calculable) {
        ThresholdValues thresholdValues = getThresholdValues();
        double value = calculable.getValue();
        if(thresholdCrossed) {
            if(direction == BREACHED_UPPER) {
                if(value < thresholdValues.getCurrentHighThreshold()) {
                    thresholdCrossed = false;
                    direction = CLEARED;
                    /* the next 2 lines produce a cleared event*/
                    thresholdValues.incThresholdClearedCount();
                    notifyListeners(calculable, ThresholdEvent.CLEARED);
                    checkLowThresholdBreach(calculable);
                } else {
                    checkHighThresholdBreach(calculable);
                }
            } else if(direction == BREACHED_LOWER) {
                if(value > thresholdValues.getCurrentLowThreshold()) {
                    thresholdCrossed = false;
                    direction = CLEARED;
                    thresholdValues.incThresholdClearedCount();
                    notifyListeners(calculable, ThresholdEvent.CLEARED);
                    checkHighThresholdBreach(calculable);
                } else {                    
                    checkLowThresholdBreach(calculable);
                }
            }
        } else {
            checkHighThresholdBreach(calculable);
            checkLowThresholdBreach(calculable);
        }
    }

    /**
     * Check if the Calculable has crossed the high threshold
     * 
     * @param calculable The Calculable value to check
     */
    private void checkHighThresholdBreach(Calculable calculable) {
        double value = calculable.getValue();
        if(value > thresholdValues.getCurrentHighThreshold()) {
            thresholdCrossed = true;
            direction = BREACHED_UPPER;
            thresholdValues.incThresholdBreachedCount();
            notifyListeners(calculable, ThresholdEvent.BREACHED);
        }
        if(logger.isLoggable(Level.FINEST) && calculable.getId().equals("load"))
            logger.log(Level.FINEST,
                       "["+calculable.getId()+"] Check High Threshold breach, "+ 
                       "value="+ value+ ", "+ 
                       "high threshold="+ thresholdValues.getCurrentHighThreshold());
    }

    /**
     * Check if the Calculable has crossed the low threshold
     * 
     * @param calculable The Calculable value to check
     */
    private void checkLowThresholdBreach(Calculable calculable) {
        double value = calculable.getValue();
        if(value < thresholdValues.getCurrentLowThreshold()) {
            thresholdCrossed = true;
            direction = BREACHED_LOWER;
            thresholdValues.incThresholdBreachedCount();
            notifyListeners(calculable, ThresholdEvent.BREACHED);
        }
        if(logger.isLoggable(Level.FINEST) && calculable.getId().equals("load"))
            logger.log(Level.FINEST, "["
                                     + calculable.getId()
                                     + "] Check Low Threshold breach, "
                                     + "value="
                                     + value
                                     + ", "
                                     + "low threshold="
                                     + thresholdValues.getCurrentLowThreshold());
    }
}
