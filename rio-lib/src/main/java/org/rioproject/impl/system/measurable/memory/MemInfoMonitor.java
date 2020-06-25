/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.system.measurable.memory;

import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.system.measurable.memory.SystemMemoryUtilization;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringTokenizer;

/**
 * Monitors {@code /proc/meminfo}
 *
 * @author Dennis Reedy
 */
public class MemInfoMonitor implements MeasurableMonitor<SystemMemoryUtilization> {
    private String id;
    private ThresholdValues thresholdValues;
    private String memInfo = "/proc/meminfo";
    private static final Logger logger = LoggerFactory.getLogger(MemInfoMonitor.class);

    @Override
    public void terminate() {
    }

    @Override
    public void setID(String id) {
        this.id = id;
    }

    @Override
    public void setThresholdValues(ThresholdValues thresholdValues) {
        this.thresholdValues = thresholdValues;
    }

    /* For testing */
    void setMemInfoFile(String memInfo) {
        this.memInfo = memInfo;
    }

    @Override
    public SystemMemoryUtilization getMeasuredResource() {
        SystemMemoryUtilization memoryUtilization;
        BufferedReader br = null;
        try {
            long total = 0;
            long active = 0;

            File file = new File(memInfo);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str;
            StringTokenizer token;
            while ((str = br.readLine()) != null) {
                token = new StringTokenizer(str);
                if (!token.hasMoreTokens())
                    continue;

                str = token.nextToken();
                if (!token.hasMoreTokens())
                    continue;

                if (str.equalsIgnoreCase("MemTotal:"))
                    total = Long.parseLong(token.nextToken());
                else if (str.equalsIgnoreCase("Active:"))
                    active = Long.parseLong(token.nextToken());
            }

            long free = total - active;
            double u = (double)active/(double)total;
            BigDecimal bd = new BigDecimal(u).setScale(2, RoundingMode.FLOOR);
            double usedPercent = bd.doubleValue();

            double f = (double)free/(double)total;
            bd = new BigDecimal(f).setScale(2, RoundingMode.CEILING);
            double freePercent = bd.doubleValue();

            double KB = 1024;
            memoryUtilization = new SystemMemoryUtilization(id,
                                                            usedPercent,
                                                            ((double)total)/ KB,
                                                            ((double)free)/ KB,
                                                            ((double)active)/ KB,
                                                            freePercent,
                                                            usedPercent,
                                                            total,
                                                            thresholdValues);
        } catch (IOException e) {
            memoryUtilization = new SystemMemoryUtilization(id, thresholdValues);
        } finally {
            if(br!=null)
                try {
                    br.close();
                } catch (IOException e) {
                    logger.warn("Trying to close the BufferedReader for /proc/meminfo", e);
                }
        }
        return memoryUtilization;
    }

}
