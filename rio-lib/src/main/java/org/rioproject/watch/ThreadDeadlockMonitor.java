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
package org.rioproject.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Monitor thread deadlocks for a Java Virtual Machine.
 */
public class ThreadDeadlockMonitor {
    public static final String ID = "thread-deadlock-monitor";
    public static final String ACCESSOR = "threadDeadlockCalculable";
    private ThreadMXBean threadMXBean;
    private final Map<Long, ThreadInfo> deadlockedThreads = new HashMap<Long, ThreadInfo>();
    private static Logger logger = LoggerFactory.getLogger(ThreadDeadlockMonitor.class.getName());

    public void setThreadMXBean(ThreadMXBean threadMXBean) {
        this.threadMXBean = threadMXBean;
        if(logger.isInfoEnabled())
            logger.info("ThreadMXBean set, monitoring current JVM for thread deadlocks");
    }

    public Calculable getThreadDeadlockCalculable() {
        int deadlockCount = findDeadlockedThreads();
        Calculable metric = new Calculable(ID, deadlockCount, System.currentTimeMillis());
        if(deadlockCount>0) {
            String detail = formatDeadlockedThreadInfo();
            metric.setDetail(detail);
            if(logger.isTraceEnabled())
                logger.trace(detail);
        } else {
             if(logger.isTraceEnabled())
                logger.trace("No deadlocked threads");
        }
        return metric;
    }

    /**
     * Get the default {@link org.rioproject.watch.WatchDescriptor} for this
     * utility. This allows the <tt>ThreadDeadlockMonitor</tt> to be used
     * by the SLA framework.
     *
     * @return A <tt>WatchDescriptor</tt> set to poll every 5 seconds, checking
     * if any threads are deadlocked. 
     */
    public static WatchDescriptor getWatchDescriptor() {
        return new WatchDescriptor(ID, ACCESSOR, 5000);
    }

    private String formatDeadlockedThreadInfo() {
        StringBuilder buff = new StringBuilder();
        Set<Map.Entry<Long, ThreadInfo>> entrySet;
        synchronized(deadlockedThreads) {
            entrySet = deadlockedThreads.entrySet();
        }
        buff.append("Deadlocked thread count: ");
        buff.append(entrySet.size());
        buff.append("\n");
        int count=1;
        for(Map.Entry<Long, ThreadInfo> entry : entrySet ) {
            buff.append("\n");
            buff.append("Deadlocked Thread #");
            buff.append(count++);
            buff.append("\n");
            buff.append("------------------");
            buff.append("\n");
            buff.append("Name: ");
            ThreadInfo ti = entry.getValue();
            buff.append(ti.getThreadName());
            buff.append("\n");
            buff.append("State: ");
            buff.append(ti.getThreadState());
            buff.append(" on ");
            buff.append(ti.getLockName());
            buff.append("owned by: ");
            buff.append(ti.getLockOwnerName());
            buff.append("\n");
            buff.append("Total blocked: ");
            buff.append(ti.getBlockedCount());
            buff.append(" Total waited: ");
            buff.append(ti.getWaitedCount());
            buff.append("\n");
            buff.append("Stack trace:");
            buff.append("\n");
            for(StackTraceElement ste : ti.getStackTrace())
                buff.append("at ").append(ste).append("\n");
        }
        return buff.toString();
    }

    private int findDeadlockedThreads() {
        if(threadMXBean==null) {
            setThreadMXBean(ManagementFactory.getThreadMXBean());
        }
        long[] ids = threadMXBean.findMonitorDeadlockedThreads();
        if(ids != null && ids.length > 0) {
            for(Long l : ids) {
                if(!knowsAbout(l)) {
                    ThreadInfo ti = threadMXBean.getThreadInfo(l, Integer.MAX_VALUE);
                    synchronized(deadlockedThreads) {
                        deadlockedThreads.put(l, ti);
                    }
                }
            }
        }
        return ids==null?0:ids.length;
    }

    private boolean knowsAbout(long id) {
        boolean has;
        synchronized(deadlockedThreads) {
            has = deadlockedThreads.containsKey(id);
        }
        return has;
    }
}
