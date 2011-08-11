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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Handles getting the CPU states for Linux by using the /proc filesystem
 *
 * @author Dennis Reedy
 */
public class LinuxHandler implements MeasurableMonitor<CpuUtilization> {
    private static double[] knownStats = new double[]{0.0, 0.0, 0.0, 0.0};
    private static final String PROC_STAT = "/proc/stat";
    private String id;
    private ThresholdValues tVals;

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public CpuUtilization getMeasuredResource() {
        return (new CpuUtilization(id, getSystemUtilization(), tVals));
    }

    /* (non-Javadoc)
    * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
    */
    public void terminate() {
        /* implemented for interface compliance */
    }

    /*
    * Get the system utlization value
    */
    private double getSystemUtilization() {
        File statFile = new File(PROC_STAT);
        FileReader fr = null;
        BufferedReader bf = null;
        double utilization = 0.0;
        try {
            fr = new FileReader(statFile);
            bf = new BufferedReader(fr, 8192);
            String line;
            while ((line = bf.readLine()) != null) {
                //if(line.startsWith("cpu" + processorID)) {
                if (line.startsWith("cpu ")) {
                    String[] elements = line.split("\\s");
                    double user = Double.parseDouble(elements[2]);
                    double nice = Double.parseDouble(elements[3]);
                    double kernel = Double.parseDouble(elements[4]);
                    double idle = Double.parseDouble(elements[5]);
                    double totalJiffies =
                        (user - knownStats[0]) +
                        (nice - knownStats[1]) +
                        (kernel - knownStats[2]) +
                        (idle - knownStats[3]);
                    if (totalJiffies > 0)
                        utilization +=
                            1.0 - ((idle - knownStats[3]) / totalJiffies);
                    knownStats[0] = user;
                    knownStats[1] = nice;
                    knownStats[2] = kernel;
                    knownStats[3] = idle;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bf != null) {
                try {
                    bf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return (utilization);
    }


}
