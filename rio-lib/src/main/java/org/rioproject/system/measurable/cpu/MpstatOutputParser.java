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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * The MpstatOutputParser parses the output of the mpstat command on Solaris
 *
 * @author Dennis Reedy
 */
public class MpstatOutputParser extends CPUExecHandler {
    InputStream in;
    double utilization;
    private final Object updateLock = new Object();

    public void parse(InputStream in) {
        this.in = in;
        synchronized(updateLock) {
            try {
                getExecutorService().execute(new Runner());
                updateLock.wait();
            } catch (InterruptedException e) {
                logger.warn( "Waiting on updateLock", e);
            }
        }
    }

    public double getUtilization() {
        return (utilization);
    }
            
    /* (non-Javadoc)
     * @see org.rioproject.system.measurable.cpu.CPUExecHandler#getCommand()
     */
    public String getCommand() {
        return("mpstat");
    }
    
    class Runner implements Runnable {
        public void run() {
            BufferedReader br = null;
            try {
                InputStreamReader isr = new InputStreamReader(in);
                br = new BufferedReader(isr);
                String line;
                int i = 0;
                double currentUtilization = 0.0;
                java.util.List<String> elements = new java.util.ArrayList<String>();
                while((line = br.readLine()) != null) {
                    if(logger.isTraceEnabled())
                        logger.trace("Parsing output from mpstat\n[{}]", line);
                    if(i > 0) {
                        elements.clear();
                        StringTokenizer st = new StringTokenizer(line, " ");
                        while(st.hasMoreTokens())
                            elements.add(st.nextToken());
                        double cpuIdle = Double.parseDouble(elements.get(15));
                        if(logger.isTraceEnabled())
                            logger.trace("CPU idle value={}", cpuIdle);
                        double utilPercent = 1.0 - (cpuIdle / 100);
                        if(utilPercent < 0) {
                            if(logger.isTraceEnabled())
                                logger.trace("CPU utilization={}, adjust to 0", utilPercent);
                            utilPercent=0;
                        }
                        if(logger.isTraceEnabled())
                            logger.trace("CPU utilization percent={}", utilPercent);
                        currentUtilization = 
                            (currentUtilization + utilPercent) / i;
                    }
                    i++;
                }
                utilization = currentUtilization;
            } catch(IOException e) {
                logger.warn("Parsing stream from mpstat", e);
            } finally {
                try {
                    if(br != null)
                        br.close();
                } catch(IOException e) {
                    logger.warn("Closing BufferedReader", e);
                }
                synchronized(updateLock) {
                    updateLock.notifyAll();
                }
            }
        }
    }    
}
