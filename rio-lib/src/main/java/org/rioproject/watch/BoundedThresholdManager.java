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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final String id;
    static Logger logger = LoggerFactory.getLogger("org.rioproject.watch");

    public BoundedThresholdManager(String id) {
        this.id = id;
    }

    public boolean getThresholdCrossed() {
        return (thresholdCrossed);
    }

    @Override
    protected String getID() {
        return id;
    }

    public void checkThreshold(Calculable calculable) {
        ThresholdValues thresholdValues = getThresholdValues();
        double value = calculable.getValue();
        if(thresholdCrossed) {
            if(direction == BREACHED_UPPER) {
                if(value < thresholdValues.getHighThreshold()) {
                    thresholdCrossed = false;
                    direction = CLEARED;
                    /* the next 2 lines produce a cleared event*/
                    thresholdValues.incThresholdClearedCount();
                    notifyListeners(calculable, ThresholdType.CLEARED);
                    checkLowThresholdBreach(calculable);
                } else {
                    checkHighThresholdBreach(calculable);
                }
            } else if(direction == BREACHED_LOWER) {
                if(value > thresholdValues.getLowThreshold()) {
                    thresholdCrossed = false;
                    direction = CLEARED;
                    thresholdValues.incThresholdClearedCount();
                    notifyListeners(calculable, ThresholdType.CLEARED);
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
        if(value > thresholdValues.getHighThreshold()) {
            boolean notify;
            if(thresholdValues.getStep()>0 && thresholdCrossed) {
                double diff = value - thresholdValues.getCurrentHighThreshold();
                notify = diff >= thresholdValues.getStep();
            } else {
                notify = true;
            }
            if(notify) {
                thresholdCrossed = true;
                direction = BREACHED_UPPER;
                thresholdValues.incThresholdBreachedCount();
                thresholdValues.setCurrentHighThreshold(value);
                notifyListeners(calculable, ThresholdType.BREACHED);
            }
        }
    }

    /**
     * Check if the Calculable has crossed the low threshold
     *
     * @param calculable The Calculable value to check
     */
    private void checkLowThresholdBreach(Calculable calculable) {
        double value = calculable.getValue();
        if(value < thresholdValues.getLowThreshold()) {
            boolean notify;
            if(thresholdValues.getStep()>0 && thresholdCrossed) {
                double diff = Math.abs(thresholdValues.getCurrentLowThreshold()-value);
                notify = diff >= thresholdValues.getStep();
            } else {
                notify = true;
            }
            if(notify) {
                thresholdCrossed = true;
                direction = BREACHED_LOWER;
                thresholdValues.incThresholdBreachedCount();
                thresholdValues.setCurrentLowThreshold(value);
                notifyListeners(calculable, ThresholdType.BREACHED);
            }
        }
    }
}
