/*
 * Copyright 2008 to the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to help in loading and using
 * <a href="http://www.hyperic.com/products/sigar.html">Hyperic SIGAR</a>.
 *
 * <p>This class does not explicitly include and Hyperic SIGAR classes, it uses
 * reflection to access and use SIGAR. In this way if there is an issue
 * including SIGAR, the distribution has no hard-coded dependencies for the
 * technology.
 *
 * @author Dennis Reedy
 */
public final class SigarHelper {
    public static final String COMPONENT = SigarHelper.class.getPackage().getName();
    static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private static final double NOT_AVAILABLE = -1;
    private Object sigarInstance;
    private final Object sigarLock = new Object();
    
    /* System cpu info method accessors */
    private Method getCpuPercMethod;
    private Method getSysMethod;
    private Method getUserMethod;
    private Method getLoadAverageMethod;

    /* Process statistics */
    private Method getProcList;

    /* Process cpu method accessors */
    private Method getPercentMethod;
    private Method getProcCpuMethod;
    private Method getProcCpuSysMethod;
    private Method getProcCpuUserMethod;

    /* Process memory usage method accessors */
    private Method getSizeMethod;
    private Method getProcMemMethod;
    private Method getResidentMethod;
    private Method getShareMethod;

    /* System file system usage method accessors */
    private Method getFileSystemUsageMethod;
    private Method fileSystemFreeMethod;
    private Method fileSystemUsedMethod;
    private Method fileSystemTotalMethod;
    private Method fileSystemUsedPercentMethod;

    /* System memory usage method accessors */
    private Method getMemMethod;
    private Method getUsedMemoryMethod;
    private Method getFreeMemoryMethod;
    private Method getFreeMemoryPercentMethod;
    private Method getUsedMemoryPercentMethod;
    private Method getSystemRAMMethod;
    private Method getSystemMemoryMethod;

    private SigarHelper() {
        try {
            Class<?> sigarClass = Class.forName("org.hyperic.sigar.Sigar");
            Object sigar = sigarClass.newInstance();
            Class<?> sigarProxyClass = Class.forName("org.hyperic.sigar.SigarProxyCache");
            Method newInstance = sigarProxyClass.getMethod("newInstance", sigarClass, int.class);
            synchronized(sigarLock) {
                sigarInstance = newInstance.invoke(null, sigar, 5000);
                /* Try to get the pid, if this passes SIGAR is available */
                try {
                    Method getPid = getMethod("getPid");
                    getPid.invoke(sigarInstance);
                } catch(Exception e) {
                    sigarInstance = null;
                }
            }
        } catch (Throwable e) {
            logger.debug("Could not load SIGAR {}: {}", e.getClass(), e.getMessage());
        }
    }
    
    /**
     * Get an instance of the SigarHelper.
     *
     * @return An instance of the SigarHelper.
     */
    public static synchronized SigarHelper getInstance() {
        SigarHelper helper = new SigarHelper();
        return helper.haveSigar() ? helper : null;
    }

    /**
     * Check if SIGAR is available.
     *
     * @return True if SIGAR is available, false otherwise.
     */
    public static boolean sigarAvailable() {
        return getInstance()!=null;
    }

    /**
     * Get the pid for the current process
     * 
     * @return The process id (pid) for the executing process. If the pid
     * cannot be obtained return -1.
     */
    public long getPid() {
        Long pid = (long)-1;
        try {
            Method getPid = getMethod("getPid");
            synchronized(sigarLock) {
                pid = (Long)getPid.invoke(sigarInstance);
            }
        } catch (Exception e) {
            log("Could not get PID from SIGAR", e);
        }
        return pid;
    }

    /**
     * Using the process identifier (pid) of a parent, find the matching child
     * process using a bottom up approach
     *
     * @param ppid The parent pid
     * @param sPids Array of child pids
     *
     * @return The matching child pid, or -1 if not found.
     */
    public long matchChild(final int ppid, final String[] sPids) {
        long found = -1;
        long[] pids = new long[sPids.length];
        for(int i=0; i<sPids.length; i++)
            pids[i] = new Long(sPids[i]);

        /* First check to see if the passed in ppid is in the list of child
         * pids. If we have a match there is no reason to traverse the hierarchy */
        for(long pid : pids) {
            if(pid==ppid) {
                found = pid;
                break;
            }
        }
        if(found>0)
            return found;

        Method getProcState = null;
        StringBuilder s = new StringBuilder();
        for(int i=0; i<pids.length; i++) {
            if(i>0)
                s.append(", ");
            s.append(pids[i]);
        }
        System.out.println("Parent PID: ["+ppid+"], JMX pids: ["+s.toString()+"]");
        try {
            for(String pid: sPids) {
                long lPid = new Long(pid);
                if(lPid<=0)
                    continue;

                if(getProcState==null)
                    getProcState = getMethod("getProcState", String.class);

                Object procState;
                synchronized(sigarLock) {
                    procState = getProcState.invoke(sigarInstance, pid);
                }
                Method getPpid = procState.getClass().getMethod("getPpid");
                long parentPID = (Long)getPpid.invoke(procState);

                System.out.println("PID=["+pid+"], PPID=["+parentPID+"]");
                if(parentPID==ppid) {
                    System.out.println("MATCHED!!! PID DETERMINED AS: "+pid);
                    found = new Long(pid);
                    break;
                } else {
                    long parent = matchChild(ppid, new String[]{Long.toString(parentPID)});
                    if(parent==ppid) {
                        System.out.println("Matched parent, PID is: "+parent);
                        found = parent;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log("Could not get PID from SIGAR", e);
        }

        return found;
    }

    public List<String> getProcessList() {
        checkGetProcListMethod();
        if(getProcList==null)
            return null;
        List<String> processList = new ArrayList<String>();
        try {
            long[] array = (long[])getProcList.invoke(sigarInstance);
            for(long l : array) {
                processList.add(Long.toString(l));
            }
        } catch (Exception e) {
            log("Failed invoking getProcList method", e);
        }
        return processList;
    }

    /**
     * Get the cpu kernel usage
     *
     * @return The cpu kernel use as a percentage; or -1 if not available.
     */
    public double getSystemCpuPercentage() {
        checkCpuPercMethod();
        if(getCpuPercMethod==null)
            return NOT_AVAILABLE;

        if(getSysMethod==null) {
            try {
                getSysMethod = getCpuPercMethod.getReturnType().getMethod("getSys");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getSys method from SIGAR", e);
                return NOT_AVAILABLE;
            }
        }
        double systemPercentage = NOT_AVAILABLE;
        try {
            systemPercentage = (Double)getSysMethod.invoke(getCpuPerc());
        } catch (Exception e) {
            log("Failed invoking getSys method", e);
        }

        return systemPercentage;
    }

    /**
     * Get the cpu user usage
     *
     * @return The cpu user use as a percentage; or -1 if not available.
     */
    public double getUserCpuPercentage() {
        checkCpuPercMethod();
        if(getCpuPercMethod==null)
            return NOT_AVAILABLE;

        if(getUserMethod==null) {
            try {
                getUserMethod = getCpuPercMethod.getReturnType().getMethod("getUser");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getUser method from SIGAR", e);
                return NOT_AVAILABLE;
            }
        }
        double userPercentage = NOT_AVAILABLE;
        try {
            userPercentage = (Double)getUserMethod.invoke(getCpuPerc());
        } catch (Exception e) {
            log("Failed invoking getUser method", e);
        }

        return userPercentage;
    }

    /**
     * Get the load average for the machine
     *
     * @return The system load averages for the past 1, 5, and 15 minutes. If
     * the load average could not be obtained, return an array with a single
     * element whose value is set to -1
     */
    public double[] getLoadAverage() {
        if(getLoadAverageMethod==null) {
            try {
                getLoadAverageMethod = getMethod("getLoadAverage");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getLoadAverage method from SIGAR", e);
                return new double[]{NOT_AVAILABLE};
            }
        }
        double[] loadAverage;
        try {
            synchronized(sigarLock) {
                loadAverage = (double[])getLoadAverageMethod.invoke(sigarInstance);
            }
        } catch (Exception e) {
            if(logger.isDebugEnabled())
                logger.debug("Failed invoking getLoadAverage method, load average is not available");
            loadAverage = new double[]{NOT_AVAILABLE};
        }
        return loadAverage;
    }

    /**
     * Get the CPU utilization (percentage) for a process
     *
     * @param pid The process id (pid) to obtain the CPU utilization
     * (percentage) for
     *
     * @return The CPU utilization (percentage) for a process, or -1 if not
     * available
     */
    public double getProcessCpuPercentage(final long pid) {
        checkProcCPU();
        if(getProcCpuMethod==null) {
            return NOT_AVAILABLE;
        }
        if(getPercentMethod==null) {
            Object procCpu;
            try {
                synchronized(sigarLock) {
                    procCpu = getProcCpuMethod.invoke(sigarInstance, pid);
                }
            } catch (Exception e) {
                log("Failed invoking getProcCpu method", e);
                return NOT_AVAILABLE;
            }
            try {
                getPercentMethod = procCpu.getClass().getMethod("getPercent");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getPercent method from SIGAR ProcCPU class", e);
                return NOT_AVAILABLE;
            }
        }

        double percent = NOT_AVAILABLE;
        try {
            synchronized(sigarLock) {
                percent = (Double)getPercentMethod.invoke(getProcCpuMethod.invoke(sigarInstance, pid));
            }
        } catch (Exception e) {
            log("Failed invoking getPercent method", e);
        }

        return percent;
    }

    /**
     * Get the CPU user usage for a process
     *
     * @param pid The process id (pid) to obtain the CPU user usage
     *
     * @return The cpu user usage for the process; or -1 if not available.
     */
    public long getProcessCpuUser(final long pid) {
        checkProcCPU();
        if(getProcCpuMethod==null) {
            return (long)NOT_AVAILABLE;
        }

        if(getProcCpuUserMethod==null) {
            Object procCpu;
            try {
                synchronized(sigarLock) {
                    procCpu = getProcCpuMethod.invoke(sigarInstance, pid);
                }
            } catch (Exception e) {
                log("Failed invoking getProcCpu method", e);
                return (long)NOT_AVAILABLE;
            }
            try {
                getProcCpuUserMethod = procCpu.getClass().getMethod("getUser");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getUser method from SIGAR ProcCPU class", e);
                return (long)NOT_AVAILABLE;
            }
        }
        long user = (long)NOT_AVAILABLE;
        try {
            synchronized(sigarLock) {
                user = (Long)getProcCpuUserMethod.invoke(getProcCpuMethod.invoke(sigarInstance, pid));
            }
        } catch (Exception e) {
            log("Failed invoking getUser method", e);
        }
        return user;
    }

    /**
     * Get the CPU system (kernel) usage for a process
     *
     * @param pid The process id (pid) to obtain the CPU system (kernel) usage
     *
     * @return The cpu system (kernel) usage for the process; or -1 if not available.
     */
    public long getProcessCpuSys(final long pid) {
        checkProcCPU();
        if(getProcCpuMethod==null) {
            return (long)NOT_AVAILABLE;
        }
        
        if(getProcCpuSysMethod==null) {
            Object procCpu;
            try {
                synchronized(sigarLock) {
                    procCpu = getProcCpuMethod.invoke(sigarInstance, pid);
                }
            } catch (Exception e) {
                log("Failed invoking getProcCpu method", e);
                return (long)NOT_AVAILABLE;
            }
            try {
                getProcCpuSysMethod = procCpu.getClass().getMethod("getSys");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getSys method from SIGAR ProcCPU class", e);
                return (long)NOT_AVAILABLE;
            }
        }
        long sys = (long)NOT_AVAILABLE;
        try {
            synchronized(sigarLock) {
                sys = (Long)getProcCpuSysMethod.invoke(getProcCpuMethod.invoke(sigarInstance, pid));
            }
        } catch (Exception e) {
            log("Failed invoking getSys method", e);
        }
        return sys;
    }

    private void checkProcCPU() {
        if(getProcCpuMethod==null) {
            try {
                getProcCpuMethod = getMethod("getProcCpu", long.class);
            } catch (NoSuchMethodException e) {
                log("Could not obtain getProcCpu method from SIGAR", e);
            }
        }
    }

    /**
     * Get the amount of virtual memory the process has available to it
     *
     * @param pid The process id (pid)
     *
     * @return The amount of virtual memory (in bytes) or -1 if not available
     */
    public long getProcessVirtualMemorySize(final long pid) {
        checkProcMemMethod();
        if(getProcMemMethod==null)
            return (long)NOT_AVAILABLE;

        if(getSizeMethod==null) {
            Object procMem = getProcMem(pid);
            if(procMem==null)
                return (long)NOT_AVAILABLE;
            try {
                getSizeMethod = procMem.getClass().getMethod("getSize");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getSize method from SIGAR ProcMem class", e);
                return (long)NOT_AVAILABLE;
            }
        }
        long size = (long)NOT_AVAILABLE;
        try {
            size = (Long)getSizeMethod.invoke(getProcMem(pid));
        } catch (Exception e) {
            log("Failed invoking getSize method", e);
        }

        return size;
    }

    /**
     * Get the amount of real memory the process has available to it
     *
     * @param pid The process id (pid)
     *
     * @return The amount of real memory (in bytes) or -1 if not available
     */
    public long getProcessResidentMemory(final long pid) {
        checkProcMemMethod();
        if(getProcMemMethod==null)
            return (long)NOT_AVAILABLE;

        if(getResidentMethod==null) {
            Object procMem = getProcMem(pid);
            if(procMem==null)
                return (long)NOT_AVAILABLE;
            try {
                getResidentMethod = procMem.getClass().getMethod("getResident");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getResident method from SIGAR ProcMem class", e);
                return (long)NOT_AVAILABLE;
            }
        }
        long resident = (long)NOT_AVAILABLE;
        try {
            resident = (Long)getResidentMethod.invoke(getProcMem(pid));
        } catch (Exception e) {
            log("Failed invoking getSize method", e);
        }
        return resident;
    }

    /**
     * Get the amount of shared memory the process has available to it
     *
     * @param pid The process id (pid)
     *
     * @return The amount of shared memory (in bytes) or -1 if not available
     */
    public long getProcessSharedMemory(final long pid) {
        checkProcMemMethod();
        if(getProcMemMethod==null)
            return (long)NOT_AVAILABLE;        

        if(getShareMethod==null) {
            Object procMem = getProcMem(pid);
            if(procMem==null)
                return (long)NOT_AVAILABLE;
            try {
                getShareMethod = procMem.getClass().getMethod("getShare");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getShare method from SIGAR ProcMem class", e);
                return (long)NOT_AVAILABLE;
            }
        }
        long shared = (long)NOT_AVAILABLE;
        try {
            shared = (Long)getShareMethod.invoke(getProcMem(pid));
        } catch (Exception e) {
            log("Failed invoking getShare method", e);
        }
        return shared;

    }

    /**
     * Get the amount of available K-bytes for the file system
     *
     * @param fileSystem The fileSystem name
     *
     * @return The amount of available K-bytes for the file system, or -1 if not
     * available
     */
    public long getFileSystemFree(final String fileSystem) {
        /*
         FileSystemUsage fUse = sigar.getFileSystemUsage(File.separator);
         double available = fUse.getFree()*1024;
         double used = fUse.getUsed()*1024;
         double total = fUse.getTotal()*1024;
         */
        checkFileSysemUsageMethods(fileSystem);
        if(fileSystemFreeMethod==null)
            return (long)NOT_AVAILABLE;
        long free = (long)NOT_AVAILABLE;
        try {
            free = (Long)fileSystemFreeMethod.invoke(getFileSystemUsage(fileSystem));
        } catch (Exception e) {
            log("Failed invoking getFree method", e);
        }
        return free;
    }

    /**
     * Get the amount of used K-bytes for the file system
     *
     * @param fileSystem The fileSystem name
     *
     * @return The amount of used K-bytes for the file system, or -1 if not
     * available
     */
    public long getFileSystemUsed(final String fileSystem) {
        checkFileSysemUsageMethods(fileSystem);
        if(fileSystemUsedMethod==null)
            return (long)NOT_AVAILABLE;

        long used = (long)NOT_AVAILABLE;
        try {
            used = (Long)fileSystemUsedMethod.invoke(getFileSystemUsage(fileSystem));
        } catch (Exception e) {
            log("Failed invoking getUsed method", e);
        }
        return used;
    }

    /**
     * Get the number of total K-bytes for the file system
     *
     * @param fileSystem The fileSystem name
     *
     * @return The number of K-bytes  for the file system, or -1 if not
     * available
     */
    public long getFileSystemTotal(final String fileSystem) {
        checkFileSysemUsageMethods(fileSystem);
        if(fileSystemTotalMethod==null)
            return (long)NOT_AVAILABLE;
        long total = (long)NOT_AVAILABLE;
        try {
            total = (Long)fileSystemTotalMethod.invoke(getFileSystemUsage(fileSystem));
        } catch (Exception e) {
            log("Failed invoking getUsed method", e);
        }
        return total;

    }

    /**
     * Get the percentage of disk used
     *
     * @param fileSystem The fileSystem name
     *
     * @return The percentage of disk used, or -1 if not available
     */
    public double getFileSystemUsedPercent(final String fileSystem) {
        checkFileSysemUsageMethods(fileSystem);
        if(fileSystemUsedPercentMethod==null)
            return NOT_AVAILABLE;
        double percent = NOT_AVAILABLE;
        try {
            percent = (Double)fileSystemUsedPercentMethod.invoke(getFileSystemUsage(fileSystem));
        } catch (Exception e) {
            log("Failed invoking getUsePercent method", e);
        }
        return percent;

    }

    /**
     * Get the amount of system RAM.
     *
     * @return Get the amount of system Random Access Memory (in MB),
     * or -1 if not available
     */
    public long getRam() {
        checkSystemMemoryMethods();
        if(getSystemRAMMethod==null)
            return (long)NOT_AVAILABLE;

        long ram = (long)NOT_AVAILABLE;
        try {
            ram = (Long)getSystemRAMMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getRam method", e);
        }
        return ram;
    }

    /**
     * Get the amount of system memory
     *
     * @return The total amount of system memory (in MB), or -1 if not available
     */
    public long getTotalSystemMemory() {
        checkSystemMemoryMethods();
        if(getSystemMemoryMethod==null)
            return (long)NOT_AVAILABLE;

        long totalMem = (long)NOT_AVAILABLE;
        try {
            totalMem = (Long)getSystemMemoryMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getTotal method", e);
        }
        return totalMem;
    }

    /**
     * Get the percent of free system memory
     *
     * @return The percent of free system memory, or -1 if not available
     */
    public double getFreeSystemMemoryPercent() {
        checkSystemMemoryMethods();
        if(getFreeMemoryPercentMethod==null)
            return NOT_AVAILABLE;

        double free = NOT_AVAILABLE;
        try {
            free = (Double)getFreeMemoryPercentMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getFreePercent method", e);
        }
        return free;
    }

    /**
     * Get the amount of free system memory
     *
     * @return The amount of free system memory, or -1 if not available
     */
    public long getFreeSystemMemory() {
        checkSystemMemoryMethods();
        if(getFreeMemoryMethod==null)
            return (long)NOT_AVAILABLE;

        long free = (long)NOT_AVAILABLE;
        try {
            free = (Long)getFreeMemoryMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getFreePercent method", e);
        }
        return free;
    }

    /**
     * Get the percent of used system memory
     *
     * @return The percent of used system memory, or -1 if not available
     */
    public double getUsedSystemMemoryPercent() {
        checkSystemMemoryMethods();
        if(getUsedMemoryPercentMethod==null)
            return NOT_AVAILABLE;

        double used = NOT_AVAILABLE;
        try {
            used = (Double)getUsedMemoryPercentMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getUsedPercent method", e);
        }
        return used;
    }

    /**
     * Get the amount of used system memory
     *
     * @return The amount of used system memory (in MB), or -1 if not available
     */
    public long getUsedSystemMemory() {
        checkSystemMemoryMethods();
        if(getUsedMemoryMethod==null)
            return (long)NOT_AVAILABLE;

        long used = (long)NOT_AVAILABLE;
        try {
            used = (Long)getUsedMemoryMethod.invoke(getMem());
        } catch (Exception e) {
            log("Failed invoking getUsed method", e);
        }
        return used;
    }

    private void checkGetProcListMethod() {
        if(getProcList==null) {
            try {
                getProcList = getMethod("getProcList");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getProcList method from SIGAR", e);
            }
        }
    }

    private void checkCpuPercMethod() {
        if(getCpuPercMethod==null) {
            try {
                getCpuPercMethod = getMethod("getCpuPerc");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getCpuPerc method from SIGAR", e);
            }
        }
    }

    private void checkProcMemMethod() {
        if(getProcMemMethod==null) {
            try {
                getProcMemMethod = getMethod("getProcMem", long.class);
            } catch (NoSuchMethodException e) {
                log("Could not obtain getProcMem method from SIGAR", e);
            }
        }
    }

    private Object getCpuPerc() {
        Object cpuPerc;
        try {
            synchronized(sigarLock) {
                cpuPerc =  getCpuPercMethod.invoke(sigarInstance);
            }
        } catch (Exception e) {
            log("Failed invoking getCpuPerc method", e);
            return null;
        }
        return cpuPerc;
    }

    private Object getProcMem(final long pid) {
        Object procMem;
        try {
            synchronized(sigarLock) {
                procMem = getProcMemMethod.invoke(sigarInstance, pid);
            }
        } catch (Exception e) {
            log("Failed invoking getProcMem method", e);
            return null;
        }

        return procMem;
    }

    private Object getFileSystemUsage(final String fileSystem) {
        Object fsu;
        try {
            synchronized(sigarLock) {
                fsu = getFileSystemUsageMethod.invoke(sigarInstance, fileSystem==null?File.separator:fileSystem);
            }
        } catch (Exception e) {
            log("Failed invoking getFileSystemUsage method", e);
            return null;
        }

        return fsu;
    }

    private void checkFileSysemUsageMethods(final String fileSystem) {
        checkFileSystemUsageMethod();
        if(getFileSystemUsageMethod==null)
            return;

        if(fileSystemFreeMethod==null) {
            try {
                Object fsu = getFileSystemUsage(fileSystem);
                if(fsu!=null)
                    fileSystemFreeMethod = fsu.getClass().getMethod("getFree");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getFree method", e);
                return;
            }
        }
        if(fileSystemUsedMethod==null) {
            try {
                Object fsu = getFileSystemUsage(fileSystem);
                if(fsu!=null)
                    fileSystemUsedMethod = fsu.getClass().getMethod("getUsed");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getUsed method", e);
                return;
            }
        }
        if(fileSystemTotalMethod==null) {
            try {
                Object fsu = getFileSystemUsage(fileSystem);
                if(fsu!=null)
                    fileSystemTotalMethod = fsu.getClass().getMethod("getTotal");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getTotal method", e);
                return;
            }
        }
        if(fileSystemUsedPercentMethod==null) {
            try {
                Object fsu = getFileSystemUsage(fileSystem);
                if(fsu!=null)
                    fileSystemUsedPercentMethod = fsu.getClass().getMethod("getUsePercent");
            } catch (NoSuchMethodException e) {
                log("Could not obtain getUsePercent method", e);
            }            
        }
    }

    private void checkFileSystemUsageMethod() {
        if(getFileSystemUsageMethod==null) {
            try {
                getFileSystemUsageMethod = getMethod("getFileSystemUsage", String.class);
            } catch (NoSuchMethodException e) {
                log("Could not obtain getFileSystemUsage method from SIGAR", e);
            }
        }
    }

    private void checkSystemMemoryMethods() {
        checkGetMemMethod();

        if(getFreeMemoryPercentMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getFreeMemoryPercentMethod = mem.getClass().getMethod("getFreePercent");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getFreePercent method from "+
                     getMem().getClass().getName(), e);
            }
        }
        if(getUsedMemoryPercentMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getUsedMemoryPercentMethod = mem.getClass().getMethod("getUsedPercent");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getUsePercent method from "+
                     getMem().getClass().getName(),
                     e);
            }
        }
        if(getSystemRAMMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getSystemRAMMethod =
                        mem.getClass().getMethod("getRam");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getRam method from "+getMem().getClass().getName(), e);
            }
        }

        if(getSystemMemoryMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getSystemMemoryMethod = mem.getClass().getMethod("getTotal");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getTotal method from "+getMem().getClass().getName(), e);
            }
        }

        if(getUsedMemoryMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getUsedMemoryMethod = mem.getClass().getMethod("getUsed");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getTotal method from "+getMem().getClass().getName(), e);
            }
        }

        if(getFreeMemoryMethod==null) {
            try {
                Object mem = getMem();
                if (mem != null)
                    getFreeMemoryMethod = mem.getClass().getMethod("getFree");
            } catch (NoSuchMethodException e) {
                 log("Could not obtain getFree method from "+getMem().getClass().getName(), e);
            }
        }
    }

    private void checkGetMemMethod() {
        try {
            if(getMemMethod==null) {
                getMemMethod = getMethod("getMem");
            }
        } catch (NoSuchMethodException e) {
            log("Could not obtain getMem method from SIGAR", e);
        }
    }

    private Object getMem() {
        Object mem;
        checkGetMemMethod();
        try {
            synchronized(sigarLock) {
                mem = getMemMethod.invoke(sigarInstance);
            }
        } catch (Exception e) {
            log("Failed invoking getMem method", e);
            return null;
        }
        return mem;
    }

    private boolean haveSigar() {
        boolean have;
        synchronized(sigarLock) {
            have = sigarInstance!=null;
        }
        return have;
    }

    private Method getMethod(final String methodName, final Class... parameterTypes)
        throws NoSuchMethodException {
        Method m;
        synchronized(sigarLock) {
            m = sigarInstance.getClass().getMethod(methodName, parameterTypes);
        }
        return m;
    }

    private void log(final String s, final Throwable t) {
        if(logger.isDebugEnabled())
            logger.debug(s, t);
        else
            logger.warn("{}. Caused by: {}: {}", s, t.getClass().getName(), t.getMessage());
    }
}
