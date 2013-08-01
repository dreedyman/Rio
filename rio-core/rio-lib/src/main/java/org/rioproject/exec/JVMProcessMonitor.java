/*
 * Copyright to the original author or authors.
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
package org.rioproject.exec;

import org.rioproject.fdh.FaultDetectionListener;
import org.rioproject.system.measurable.SigarHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Utility that that monitors a JVM on the same machine.</p>
 *
 * <p>The preference is to use <a href="http://www.hyperic.com/products/sigar.html">SIGAR</a>. A process list
 * is obtained, containing a collection of all running processes. For each JVM that is bing monitored, th JVM's
 * process id is looked for in the returned list of process ids. If not found, the JVM is determined to no longer
 * be running.
 * </p>
 * <p>If for some reason SIGAR cannot be loaded (typically this would point to {@code sigar.jar} not being in the
 * classpath), the <a href="http://docs.oracle.com/javase/6/docs/jdk/api/attach/spec/index.html">Attach API</a> is
 * used.</p>
 *
 * <p>The repaing period (how often to check for JVM existence) is controlled by the
 * {@code org.rioproject.exec.monitor.reap.interval} system property. This property is the amount of seconds, and
 * defaults to 5.
 * </p>
 * <br/>
 * <p>Note: For the Attach API to be loaded the {@code tool.jar} must be loaded.</p>
 *
 * @author Dennis Reedy
 */
public class JVMProcessMonitor {
    public static final String REAP_INTERVAL = "org.rioproject.exec.monitor.reap.interval";
    private MonitorReaper monitorReaper;
    private final Map<String, FaultDetectionListener<String>> monitoringMap;
    private static final JVMProcessMonitor instance = new JVMProcessMonitor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Logger logger = LoggerFactory.getLogger(JVMProcessMonitor.class);

    private JVMProcessMonitor() {
        monitoringMap = new ConcurrentHashMap<String, FaultDetectionListener<String>>();
    }

    public static JVMProcessMonitor getInstance() {
        return instance;
    }

    /**
     * Monitor a JVM using the provided pid
     *
     * @param pid The pid for the service, must not be {@code null}
     * @param listener The {@code FaultDetectionHandler}, must not be {@code null}
     *
     * @throws IllegalArgumentException if either of the arguments are {@code null}
     */
    public synchronized void monitor(final String pid, final FaultDetectionListener<String> listener) {
        if(pid==null)
            throw new IllegalArgumentException("pid is null");
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        monitoringMap.put(pid, listener);
        if(monitorReaper ==null) {
            monitorReaper = new MonitorReaper();
            executor.submit(monitorReaper);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        executor.shutdownNow();
        super.finalize();
    }

    /*
     * Make visible for testing
     */
    MonitorReaper getMonitorReaper() {
        return monitorReaper;
    }

    /*
     * Make visible for testing
     */
    void clear() {
        monitoringMap.clear();
    }

    /**
     * The reaper
     */
    class MonitorReaper implements Runnable  {
        private final List<String> removals = new ArrayList<String>();
        private final AtomicBoolean keepAlive = new AtomicBoolean(true);
        private int reapInterval;
        private final SigarHelper sigar;

        MonitorReaper() {
            this.sigar = SigarHelper.getInstance();
            logger.info("Using {} to produce list of processes", sigar==null?"Attach API": "SIGAR");
            String sPeriod = System.getProperty(REAP_INTERVAL, "5");
            try {
                reapInterval = Integer.parseInt(sPeriod);
            } catch(NumberFormatException e) {
                reapInterval = 5;
            }
            logger.debug("Reaping interval set to {} seconds", reapInterval);
        }

        int getReapInterval() {
            return reapInterval;
        }

        List<String> getIDs() throws Exception {
            List<String> list;
            if(sigar==null) {
                String[] ids = VirtualMachineHelper.listIDs();
                list = new ArrayList<String>();
                Collections.addAll(list, ids);
            } else {
                list = sigar.getProcessList();
            }
            return list;
        }

        @Override
        public void run() {
            while(keepAlive.get()) {
                if(!monitoringMap.isEmpty()) {
                    List<String> ids;
                    try {
                        ids = getIDs();
                    } catch (Exception e) {
                        logger.error("Cannot get process list", e);
                        break;
                    }
                    for(Map.Entry<String, FaultDetectionListener<String>> entry : monitoringMap.entrySet()) {
                        boolean found = false;
                        for(String id : ids) {
                            if(entry.getKey().equals(id)) {
                                found = true;
                                break;
                            }
                        }
                        if(!found) {
                            removals.add(entry.getKey());
                        }
                    }
                    for(String key : removals) {
                        logger.warn("Service's PID [{}] no longer found, assume it is no longer present. " +
                                    "Notify listener of failure", key);
                        FaultDetectionListener<String> listener = monitoringMap.remove(key);
                        listener.serviceFailure(null, key);
                    }
                    removals.clear();
                }

                if(monitoringMap.isEmpty()) {
                    keepAlive.set(false);
                } else {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(reapInterval));
                    } catch (InterruptedException e) {
                        logger.info("Monitor Reaper has been interrupted");
                        break;
                    }
                }
            }
            logger.debug("Monitor Reaper terminating");
            monitorReaper = null;
        }
    }
}
