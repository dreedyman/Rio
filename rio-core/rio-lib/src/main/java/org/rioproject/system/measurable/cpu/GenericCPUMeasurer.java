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
package org.rioproject.system.measurable.cpu;

import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.watch.ThresholdValues;

/**
 * The GenericCPUMeasurer performs small benchmark tests, abstractly measuring CPU 
 * utilization, capacity, and load.
 *
 * @author Dennis Reedy
 */
public class GenericCPUMeasurer implements MeasurableMonitor<CpuUtilization> {
    /** Calculation thread */
    private Thread runner;
    /** Thread control variable */
    private boolean active=false;
    /** Local mutex variable */
    private final Object lock = new Object();
    /** Max number of iterations this object has performed */
    private double max;
    /** Stores number of current iterations */
    private double current;
    /** Thread control variable */
    boolean measuring = true;
    private String id;
    private ThresholdValues tVals;

    /**
     * Create a Generic CPUMeasurer
     */
    public GenericCPUMeasurer() {        
        runner = new Thread() {
            public void run() {
                while(measuring) {
                    synchronized(lock) {
                        double iteration=0;
                        while(active) {
                            iteration++;
                            try {
                                Thread.sleep(1);
                            } catch(InterruptedException e) {
                                if(!active)
                                    break;
                            }
                        }
                        current = iteration;
                        try {
                            lock.notifyAll();
                            lock.wait();
                        } catch(InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        };
        runner.setDaemon(true);
        runner.setPriority(Thread.MIN_PRIORITY);
        runner.start();
    }    
    
    /* (non-Javadoc)
     * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
     */
    public void terminate() {
        measuring = false;
        active = false;
        runner.interrupt();
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public CpuUtilization getMeasuredResource() {
        return (new CpuUtilization(id, getValue(), tVals));
    }

    /*
     * Get the utilization value
     */
    private double getValue() {
        double utilization = 0;
        try {
            synchronized(lock) {
                active = true;
                lock.notifyAll();
            }
            // Measure for a half of a second 
            Thread.sleep(500);
            active = false;
            synchronized(lock) {                
                max = (current>max?current:max);                
                utilization = (max==0.0?0.0:(1.0-(current/max)));
            }                    
            
        } catch(InterruptedException e) {
            measuring = false;
            runner.interrupt();            
        } catch(Exception e) {
            e.printStackTrace();            
        }
        return(utilization);
    }

}
