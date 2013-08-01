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

import org.rioproject.exec.Util;
import org.rioproject.system.measurable.MeasurableMonitor;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The CPUExecHandler provides the basis for concrete implementations to execute
 * and parse a command which produces CPU utilization values.
 *
 * @author Dennis Reedy
 */
public abstract class CPUExecHandler implements MeasurableMonitor<CpuUtilization> {
    static final String COMPONENT = "org.rioproject.system.measurable.cpu";
    static Logger logger = LoggerFactory.getLogger(COMPONENT);
    /**
     * Utilization property
     */
    protected double utilization;
    /**
     * ThreadPool of one, used to execute the command
     */
    ExecutorService execService = Executors.newSingleThreadExecutor();
    private String id;
    private ThresholdValues tVals;

    /**
     * Get the command to execute
     *
     * @return command the name of the command
     */
    public abstract String getCommand();

    public abstract void parse(InputStream in);

    public abstract double getUtilization();

    /**
     * Get the ExecutorService
     *
     * @return The ExecutorService
     */
    protected ExecutorService getExecutorService() {
        return (execService);
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public CpuUtilization getMeasuredResource() {
        return (new CpuUtilization(id, getCpuUtilization(), tVals));
    }

   /* (non-Javadoc)
    * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
    */
    public void terminate() {
        execService.shutdownNow();
    }

    private synchronized double getCpuUtilization() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(getCommand());
            parse(process.getInputStream());
            utilization = getUtilization();
        } catch (IOException e) {
            logger.warn("ExecHandler thread", e);
        } finally {
            if (process != null) {
                Util.close(process.getOutputStream());
                Util.close(process.getInputStream());
                Util.close(process.getErrorStream());
                process.destroy();
            }
        }
        return (utilization);
    }


}
