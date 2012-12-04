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
 * Handle the parsing of the top command on Mac OS
 *
 * @author Dennis Reedy
 */
public class MacTopOutputParser extends CPUExecHandler {
    double utilization;
    private final Object updateLock = new Object();
    
    public void parse(InputStream in) {
        synchronized(updateLock) {
            try {
                getExecutorService().execute(new Runner(in));
                updateLock.wait();
            } catch (InterruptedException e) {
                logger.warn("Waiting on updateLock", e);
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
        return("top -l2 -n0 -R -F");
    }
    
    class Runner implements Runnable {
        InputStream in;

        Runner(InputStream in) {
            this.in = in;
        }
        
        public void run() {
            BufferedReader br = null;
            String line="";
            try {
                InputStreamReader isr = new InputStreamReader(in);
                br = new BufferedReader(isr);
                //String line;
                /* Parse the output and grab the second occurrence */
                int i = 0;
                while((line = br.readLine()) != null &&
                      !Thread.currentThread().isInterrupted()) {
                    int ndx = line.indexOf("CPU");
                    if(ndx != -1) {
                        if(i > 0) {
                            String newLine = line.substring(ndx);
                            ndx = newLine.indexOf(":");
                            if(ndx == -1)
                                continue;
                            newLine = newLine.substring(ndx + 1);
                            StringTokenizer st = new StringTokenizer(newLine,
                                                                     " ");
                            while(st.hasMoreTokens()) {
                                String value = st.nextToken();
                                String category = st.nextToken();
                                if(category.equalsIgnoreCase("idle")) {
                                    utilization = 1.0 - getDouble(value);
                                    break;
                                }
                            }
                        }
                        i++;
                    }
                }
            } catch(IOException e) {
                logger.info("Grabbing output of top, last line=["+line+"]", e);
            } finally {
                if(br != null) {
                    try {
                        br.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                synchronized(updateLock) {
                    updateLock.notifyAll();
                }
            }
        }
    }

    double getDouble(final String sVal) {
        String s  =sVal;
        double dVal = 0.0;
        try {
            int ndx = s.indexOf("%");
            if(ndx != -1)
                s = s.substring(0, ndx);
            dVal = Double.parseDouble(s) / 100;
        } catch(NumberFormatException e) {
            System.out.println("Bad value [" + s + "]");
        }
        return (dVal);
    }
    
}
