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
package org.rioproject.system.measurable;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import org.rioproject.watch.ThresholdValues;
import org.rioproject.system.MeasuredResource;

/**
 * The <code>FlatlineMonitor</code> provides feedback based on a constant value
 *
 * @author Dennis Reedy
 */
public class FlatlineMonitor implements MeasurableMonitor {
    /** The constant */
    private double flatlineValue = 0.0;
    private ThresholdValues tVals;
    private String id;
    /** For getting configuration entry */
    static final String COMPONENT = "org.rioproject.system.measurable.FlatlineMonitor";
    
    /**
     * Create a FlatlineMonitor
     */
    public FlatlineMonitor() {        
    }
    
    /**
     * Create a FlatlineMonitor
     * 
     * @param value the value the FlatlineMonitor always returns
     */
    public FlatlineMonitor(long value) {
        this.flatlineValue = value;
    }
    
    /**
     * Create a FlatlineMonitor
     * 
     * @param config Configuration object to obtain the flatline value from
     */
    public FlatlineMonitor(Configuration config) {
        try {
            flatlineValue = Config.getLongEntry(config, 
                                                COMPONENT, 
                                                "flatlineValue", 
                                                0, 
                                                Long.MAX_VALUE, 
                                                0);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
     */
    public void terminate() {        
        /* implemented for interface compliance */
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public MeasuredResource getMeasuredResource() {
        return new MeasuredResource(id, flatlineValue, tVals);
    }

    public MeasuredResource getLastMeasuredResource() {
        return new MeasuredResource(id, flatlineValue, tVals);
    }
}


