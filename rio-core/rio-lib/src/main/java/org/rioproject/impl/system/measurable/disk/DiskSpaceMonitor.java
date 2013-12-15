/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.impl.system.measurable.disk;

import org.rioproject.impl.exec.Util;
import org.rioproject.impl.system.OperatingSystemType;
import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.impl.system.measurable.SigarHelper;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The <code>DiskSpaceMonitor</code> monitors disk space usage. This class
 * uses either Hyperic SIGAR, or operations system specific utilities (like df)
 * to obtain this information. The use of SIGAR is preferred, and if not
 * available will use external <tt>df</t> exec by forking a process and parsing
 * it's results.
 *
 * @author Dennis Reedy
 */
public class DiskSpaceMonitor implements MeasurableMonitor<DiskSpaceUtilization> {
    static Logger logger = LoggerFactory.getLogger("org.rioproject.system.measurable.disk");
    private String id;
    private ThresholdValues tVals;
    private SigarHelper sigar;
    private String fileSystem = File.separator;
    private final Object updateLock = new Object();

    public DiskSpaceMonitor() {
        sigar = SigarHelper.getInstance();
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public void setFileSystemToMonitor(String fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    public DiskSpaceUtilization getMeasuredResource() {
        DiskSpaceUtilization dsu;
        if(sigar==null) {
            dsu = getDiskSpaceUtilization();
        } else {
             dsu = getDiskSpaceUtilizationUsingSigar();
        }
        return dsu;
    }

    /* (non-Javadoc)
    * @see org.rioproject.system.measurable.MeasurableMonitor#terminate()
    */
    public void terminate() {
        /* implemented for interface compliance */
    }

    private DiskSpaceUtilization getDiskSpaceUtilizationUsingSigar() {
        DiskSpaceUtilization dsu;
        try {
            /*
            FileSystemUsage fUse = sigar.getFileSystemUsage(File.separator);
            double available = fUse.getFree()*1024;
            double used = fUse.getUsed()*1024;
            double total = fUse.getTotal()*1024;
            */
            double available = sigar.getFileSystemFree(fileSystem)*1024;
            double used = sigar.getFileSystemUsed(fileSystem)*1024;
            double total = sigar.getFileSystemTotal(fileSystem)*1024;
            dsu = new DiskSpaceUtilization(id,
                                           used,
                                           available,
                                           total,
                                           sigar.getFileSystemUsedPercent(fileSystem),
                                           tVals);
        } catch (Exception e) {
            logger.warn("SIGAR exception getting FileSystemUsage", e);
            dsu = new DiskSpaceUtilization(id, -1, tVals);
        }
        return dsu;
    }

    private DiskSpaceUtilization getDiskSpaceUtilization() {
        double used = 0;
        double available = 0;
        if (!OperatingSystemType.isWindows()) {
            Process process = null;
            DFOutputParser outputParser = null;
            try {
                synchronized (updateLock) {
                    try {
                        process = Runtime.getRuntime().exec("df -k");
                        outputParser = new DFOutputParser(process.getInputStream());
                        outputParser.start();
                        updateLock.wait();
                    } catch (InterruptedException e) {
                        logger.warn("Waiting on updateLock", e);
                    }
                }
                used = outputParser.getUsed() * 1024;
                available = outputParser.getAvailable() * 1024;
            } catch (IOException e) {
                logger.warn("Executing or spawning [df-k]", e);
            } finally {
                if (outputParser != null)
                    outputParser.interrupt();
                if (process != null) {
                    Util.close(process.getOutputStream());
                    Util.close(process.getInputStream());
                    Util.close(process.getErrorStream());
                    process.destroy();
                }
            }
        }
        double capacity = used + available;
        return (new DiskSpaceUtilization(id,
                                         used,
                                         available,
                                         capacity,
                                         used / capacity,
                                         tVals));
    }

    /**
     * Class to parse output from the df -k command
     */
    class DFOutputParser extends Thread {
        InputStream in;
        double used;
        double available;

        public DFOutputParser(InputStream in) {
            this.in = in;
        }

        double getAvailable() {
            return (available);
        }

        double getUsed() {
            return (used);
        }

        public void run() {
            BufferedReader br = null;
            try {
                String fileSep = System.getProperty("file.separator");
                List<String> list = new ArrayList<String>();
                InputStreamReader isr = new InputStreamReader(in);
                br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null && !isInterrupted()) {
                    if (line.startsWith("Filesystem"))
                        continue;
                    StringTokenizer st = new StringTokenizer(line, " ");
                    while (st.hasMoreTokens())
                        list.add(st.nextToken());
                    /*
                     * The following 2 lines address the problem when the
                     * DiskSpaceMonitor parses the result of a 'df -k' and the
                     * local system has NFS mounted filesystems. On Linux (and
                     * possibly other OS's), if the name of filesystem (the
                     * first column) would run into the second column, then the
                     * df command prints the filesystem on the first line (by
                     * itself) and the remaining fields are printed on the
                     * second line. After tokenizing the df output, the run()
                     * method does a 'list.get(5)'. Unfortunately, list
                     * sometimes only contains 1 element.
                     */
                    if (list.size() == 1)
                        continue;
                    /*
                    * Get the mount point
                    */
                    String mountPoint = list.get(list.size()-1);
                    if (mountPoint.equals(fileSep)) {
                        used = Double.parseDouble(list.get(2));
                        available = Double.parseDouble(list.get(3));
                    }
                    list.clear();
                }
            } catch (IOException e) {
                logger.info("Grabbing output of df -k", e);
            } finally {
                try {
                    if (br != null)
                        br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (updateLock) {
                    updateLock.notifyAll();
                }
            }
        }
    }
}
