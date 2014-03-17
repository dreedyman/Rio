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
package org.rioproject.impl.system;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.config.PlatformCapabilityConfig;
import org.rioproject.config.PlatformLoader;
import org.rioproject.costmodel.ResourceCostModel;
import org.rioproject.system.SystemWatchID;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.impl.system.capability.PlatformCapabilityLoader;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.system.capability.platform.OperatingSystem;
import org.rioproject.system.capability.platform.ProcessorArchitecture;
import org.rioproject.system.capability.platform.StorageCapability;
import org.rioproject.system.capability.software.J2SESupport;
import org.rioproject.system.capability.software.NativeLibrarySupport;
import org.rioproject.system.capability.software.RioSupport;
import org.rioproject.impl.system.measurable.MeasurableCapability;
import org.rioproject.impl.system.measurable.SigarHelper;
import org.rioproject.impl.system.measurable.cpu.CPU;
import org.rioproject.impl.system.measurable.disk.DiskSpace;
import org.rioproject.impl.system.measurable.memory.Memory;
import org.rioproject.impl.system.measurable.memory.SystemMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * The SystemCapabilities represents the capabilities of the ComputeResource
 * determined from a capabilities configuration file and default qualitative
 * and quantitative mechanisms.
 *
 * @author Dennis Reedy
 */
public class SystemCapabilities implements SystemCapabilitiesLoader {
    public static final String COMPONENT = "org.rioproject.system";
    public static final String CAPABILITY = COMPONENT+".capability";
    public static final String NATIVE_LIBS = COMPONENT+".native";
    public static final String PROCESSOR = ProcessorArchitecture.class.getName();
    public static final String OPSYS = OperatingSystem.class.getName();
    public static final String TCPIP = TCPConnectivity.class.getName();
    public static final String J2SE = J2SESupport.class.getName();
    public static final String MEMORY = org.rioproject.system.capability.platform.Memory.class.getName();
    public static final String SYSTEM_MEMORY = org.rioproject.system.capability.platform.SystemMemory.class.getName();
    public static final String STORAGE = StorageCapability.class.getName();
    public static final String NATIVE_LIB_CLASS = NativeLibrarySupport.class.getName();
    public static final String RIO = RioSupport.class.getName();
    static Logger logger = LoggerFactory.getLogger(COMPONENT);
    private String platformConfigDir;
    
    /**
     * Get the MeasurableCapability objects based on a passed in Configuration
     * 
     * @param config A Configuration to use to assist in creating 
     * MeasurableCapability objects
     *
     * @return Return an array of <code>MeasurableCapability</code> objects. This 
     * method will create a new array of <code>MeasurableCapability</code> objects 
     * each time it is invoked. At a minimum the following MeasurableCapability 
     * objects will be returned:
     * 
     * <ul>
     * <li>org.rioproject.system.measurable.cpu.CPU
     * <li>org.rioproject.system.measurable.memory.Memory
     * </ul>
     *
     * If the operating system is not a member of the "Windows" family of
     * operating systems, an additional <code>MeasurableCapability</code> is
     * returned :
     *
     * <ul>
     * <li>org.rioproject.system.measurable.disk.DiskSpace
     * </ul>
     */
    public MeasurableCapability[] getMeasurableCapabilities(Configuration config) {
        if(config==null)
            throw new IllegalArgumentException("config is null");
        
        List<MeasurableCapability> measurables = new ArrayList<MeasurableCapability>();
        /* Create the Memory MeasurableCapability. This will measure memory
         * for the JVM */
        MeasurableCapability memory =  new Memory(config);
        if(memory.isEnabled())
            measurables.add(memory);

        /* If SIGAR is available, create a Memory MeasurableCapability for the
         * physical machine as well */
        boolean haveSigar = SigarHelper.sigarAvailable();
        if(haveSigar) {
            MeasurableCapability systemMemory = new SystemMemory(config);
            if(systemMemory.isEnabled())
                measurables.add(systemMemory);

        }

        /* Load memory pool management */
        try {
            MeasurableCapability[] pools =
                (MeasurableCapability[])config.getEntry(COMPONENT+".memory.pool",
                                                        "memoryPools",
                                                        MeasurableCapability[].class,
                                                        null,
                                                        config);
            if(pools!=null)
                measurables.addAll(Arrays.asList(pools));
        } catch(ConfigurationException e) {
            logger.warn("Loading CPU MeasurableCapability", e);
        }

        /* Create the CPU MeasurableCapability */
        MeasurableCapability cpu = new CPU(config);
        if(cpu.isEnabled())
            measurables.add(cpu);

        MeasurableCapability jvmCpu = new CPU(config, SystemWatchID.PROC_CPU, true);
        if(jvmCpu.isEnabled())
            measurables.add(jvmCpu);


        /*
         * Load the DiskSpace capability only if we have SIGAR or if SIGAR is
         * not available and not running on Windows
         */
        MeasurableCapability diskSpace = null;
        if(haveSigar) {
            diskSpace = getDiskSpace(config);
        } else if(!OperatingSystemType.isWindows()) {
            diskSpace = getDiskSpace(config);
        }
        if(diskSpace!=null && diskSpace.isEnabled())
            measurables.add(diskSpace);

        /*
         * Load any additional MeasurableCapability instances that have been
         * configured
         */
        try {
            MeasurableCapability[] mCaps =
                (MeasurableCapability[])config.getEntry(COMPONENT,
                                                        "measurableCapabilities",
                                                        MeasurableCapability[].class,
                                                        null);
            if(mCaps!=null) {
                measurables.addAll(Arrays.asList(mCaps));
            }
        } catch (ConfigurationException e) {
            logger.warn("Loading MeasurableCapability array", e);
        }
        return(measurables.toArray(new MeasurableCapability[measurables.size()]));
    }        

    /**
     * Get the PlatformCapability objects
     * 
     * @return An array of <code>PlatformCapability</code> objects. This 
     * method will create a new array of <code>PlatformCapability</code> objects 
     * each time it is invoked. If there are no <code>PlatformCapability</code> 
     * objects contained within the <code>platforms</code> Collection, a 
     * zero-length array will be returned.
     */
    public PlatformCapability[] getPlatformCapabilities(Configuration config) {
        List<PlatformCapability> platforms = new ArrayList<PlatformCapability>();
        try {
            /* 
             * Load default platform (qualitative) capabilities
             */
            ProcessorArchitecture processor = new ProcessorArchitecture();
            platforms.add(processor);

            PlatformCapability operatingSystem = new OperatingSystem();
            platforms.add(operatingSystem);

            PlatformCapability tcpIP = new TCPConnectivity();
            platforms.add(tcpIP);

            PlatformCapability j2se = new J2SESupport();
            platforms.add(j2se);

            List<PlatformCapability> platformCapabilityList = new ArrayList<PlatformCapability>();
            PlatformCapability[] pCaps = (PlatformCapability[])config.getEntry(COMPONENT,
                                                                               "platformCapabilities",
                                                                               PlatformCapability[].class,
                                                                               new PlatformCapability[0]);
            platformCapabilityList.addAll(Arrays.asList(pCaps));

            PlatformCapability[] addCaps = (PlatformCapability[])config.getEntry(COMPONENT,
                                                                                 "addPlatformCapabilities",
                                                                                 PlatformCapability[].class,
                                                                                 new PlatformCapability[0]);
            platformCapabilityList.addAll(Arrays.asList(addCaps));

            /*
             * Load the default capabilities
             */
            PlatformLoader loader = new PlatformLoader();
            pCaps = createPlatformCapabilities(loader.getDefaultPlatform(getRioHome()));
            platformCapabilityList.addAll(Arrays.asList(pCaps));
            /*
             * Get additional platform configurations
             */
            String platformDir = getPlatformConfigurationDirectory(config);
            if(platformDir!=null) {
                PlatformCapability[] caps = parsePlatformConfig(loader, platformDir);
                platformCapabilityList.addAll(Arrays.asList(caps));
            } else {
                logger.warn("Unable to establish the platform configuration directory, most likely RIO_HOME is not set.");
            }

            /*
             * Get the final array of PlatformCapability instances
             */
            pCaps = platformCapabilityList.toArray(new PlatformCapability[platformCapabilityList.size()]);
            for (PlatformCapability pCap : pCaps) {
                if (pCap.getClass().getName().equals(RIO)) {
                    if (getRioHome() != null) {
                        pCap.setPath(getRioHome());
                    }
                }                
                platforms.add(pCap);
            }

            /* Find out if we have loaded a StorageCapability class */                
            PlatformCapability storage = findCapability(platforms, STORAGE);
            if(storage==null) {
                storage = getPlatformCapability(STORAGE);
                platforms.add(storage);
            }

            /* Find out if we have loaded a Memory class */
            PlatformCapability memory = findCapability(platforms, MEMORY);
            if(memory == null) {
                memory = getPlatformCapability(MEMORY);
                platforms.add(memory);
            }

            /* Find out if we have loaded a SystemMemory class */
            PlatformCapability systemMemory = findCapability(platforms, SYSTEM_MEMORY);
            if(systemMemory == null) {
                systemMemory = getPlatformCapability(SYSTEM_MEMORY);
                platforms.add(systemMemory);
            }

            /* Create NativeLibrarySupport objects */ 
            String nativeLibDirs = System.getProperty(NATIVE_LIBS);
            List<File> dirList = new ArrayList<File>();
            if(nativeLibDirs!=null && nativeLibDirs.length()>0) {
                StringTokenizer st = new StringTokenizer(nativeLibDirs, File.pathSeparator+" ");
                while(st.hasMoreTokens()) {
                    String dirName = st.nextToken();
                    File dir = new File(dirName);
                    if(dir.isDirectory() && dir.canRead()) {
                        dirList.add(dir);
                        if(logger.isDebugEnabled())
                            logger.debug("Adding directory [{}] to check for native libraries", dirName);
                    } else {
                        logger.warn("Invalid directory name or access permissions to check for native libraries [{}]. Continuing ...",
                                    dirName);
                    }                       
                }
                final String[] libExtensions = getLibExtensions();
                File[] dirs = dirList.toArray(new File[dirList.size()]);
                for (File dir : dirs) {
                    File[] files = dir.listFiles(new FileFilter() {
                        public boolean accept(File pathName) {
                            try {
                                if(FileUtils.isSymbolicLink(pathName)) {
                                    return false;
                                }
                            } catch (IOException e) {
                                logger.warn( "Trying to determine whether the file is a symbolic link", e);
                            }
                            boolean matches = false;
                            for(String libExtension : libExtensions) {
                                if(pathName.getName().endsWith(libExtension)) {
                                    matches = true;
                                    break;
                                }
                            }
                            return matches;
                        }
                    });

                    for (File file : files) {
                        String fileName = file.getName();
                        int index = fileName.lastIndexOf(".");
                        if (index != -1) {
                            if (logger.isDebugEnabled())
                                logger.debug("Create NativeLibrarySupport object for [{}]", fileName);
                            PlatformCapability nLib = getPlatformCapability(NATIVE_LIB_CLASS);
                            String name;
                            /*if (!OperatingSystemType.isWindows()) {
                                name = fileName.substring(3, index);
                            } else {*/
                            name = fileName.substring(0, index);
                            //}
                            nLib.define(NativeLibrarySupport.NAME, name);
                            nLib.define(NativeLibrarySupport.FILENAME, fileName);
                            nLib.setPath(dir.getCanonicalPath());
                            platforms.add(nLib);
                        } else {
                            logger.warn("Illegal Shared Library name="+fileName);
                        }
                    }
                }
            } /* End creating NativeLibrarySupport objects */

        } catch(Throwable t) {
            logger.error("Getting PlatformCapability objects", t);
        }

        return(platforms.toArray(new PlatformCapability[platforms.size()]));
    }

    /**
     * Get the PlatformCapability name table
     *
     * @return A Map of PlatformCapability names to PlatformCapability classnames
     */
    public Map<String, String> getPlatformCapabilityNameTable() {
        Map<String, String> nameTable = new HashMap<String, String>();
        nameTable.put("ConnectivityCapability", CAPABILITY+".connectivity.ConnectivityCapability");
        nameTable.put("TCPConnectivity", TCPIP);
        nameTable.put("J2SESupport", J2SE);
        nameTable.put("NativeLibrarySupport", NATIVE_LIB_CLASS);
        nameTable.put("RioSupport", RIO);
        nameTable.put("SoftwareSupport", CAPABILITY+".software.SoftwareSupport");
        nameTable.put("ByteOrientedDevice", CAPABILITY+".system.ByteOrientedDevice");
        nameTable.put("Memory", MEMORY);
        nameTable.put("OperatingSystem", OPSYS);
        nameTable.put("ProcessorArchitecture", PROCESSOR);
        nameTable.put("StorageCapability", STORAGE);
        return nameTable;
    }

    public String getPlatformConfigurationDirectory(Configuration config) {
        if(platformConfigDir==null) {
            String rioHome = getRioHome();
            if(rioHome==null) {
                return null;
            }

            String defaultDir = rioHome+File.separator+"config"+File.separator+"platform";
            if(config!=null) {
                try {
                    platformConfigDir = (String)config.getEntry(COMPONENT,
                                                                "platformDirs",
                                                                String.class,
                                                                defaultDir);
                } catch (ConfigurationException e) {
                    logger.warn("An exception occurred tying to read the "+
                                "{}.platformDirs property, continue on " +
                                "and use default value of {}",
                                COMPONENT, defaultDir,
                                e);
                    platformConfigDir = defaultDir;
                }
            } else {
                platformConfigDir = defaultDir;
            }
        }
        return platformConfigDir;
    }

    protected PlatformCapability getPlatformCapability(String className) throws Exception {
        Class pCapClass = Class.forName(className);
        return((PlatformCapability)pCapClass.newInstance());
    }

    /*
     * Load the DiskSpace measurable capability
     */
    private MeasurableCapability getDiskSpace(Configuration config) {
        MeasurableCapability diskSpace = null;
        try {
            diskSpace = (MeasurableCapability)config.getEntry(COMPONENT,
                                                              "disk",
                                                              MeasurableCapability.class,
                                                              new DiskSpace(config),
                                                              config);
        } catch(ConfigurationException e) {
            logger.warn("Loading DiskSpace MeasurableCapability", e);
        }
        return diskSpace;
    }

    /*
     * Determine if a class has been loaded
     */
    private PlatformCapability findCapability(List<PlatformCapability> pCaps, String name) {
        PlatformCapability o = null;
        for(PlatformCapability pCap : pCaps) {
            if(pCap.getClass().getName().equals(name)) {
                o = pCap;
                break;
            }
        }
        return(o);
    }

    /*
     * Get the library extension to search for. Determined by the operating 
     * system name
     */
    private String[] getLibExtensions() {
        String opSys = System.getProperty("os.name");
        if(opSys.startsWith("Windows"))
            return(new String[]{"dll"});
        if(opSys.startsWith("Mac"))
            return(new String[]{"jnilib", "dylib"});
        return(new String[]{"so"});
    }

    /*
     * Load the platform configuration file
     */
    private PlatformCapability[] parsePlatformConfig(PlatformLoader loader, String configDir) throws Exception {
        return(createPlatformCapabilities(loader.parsePlatform(configDir)));
    }

    /*
     * Create an array of PlatformCapability classes from parsed
     * PlatformLoader.PlatformCapabilityConfig classes
     */
    private PlatformCapability[] createPlatformCapabilities(PlatformCapabilityConfig[] caps) throws Exception {
        PlatformCapability[] pCaps = new PlatformCapability[caps.length];
        for(int i=0; i<caps.length; i++) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(PlatformCapability.NAME, caps[i].getName());
            if(caps[i].getDescription()!=null)
                attrs.put(PlatformCapability.DESCRIPTION, caps[i].getDescription());
            if(caps[i].getManufacturer()!=null)
                attrs.put(PlatformCapability.MANUFACTURER, caps[i].getManufacturer());
            if(caps[i].getVersion()!=null)
                attrs.put(PlatformCapability.VERSION, caps[i].getVersion());
            PlatformCapability pCap = (PlatformCapability)Class.forName(caps[i].getPlatformClass()).newInstance();
            pCap.defineAll(attrs);
            if(caps[i].getClasspath()!=null)
                pCap.setClassPath(caps[i].getClasspath());
            if(caps[i].getPath()!=null)
                pCap.setPath(caps[i].getPath());
            if(caps[i].geCostModelClass()!=null) {
                ResourceCostModel costModel = (ResourceCostModel)Class.forName(caps[i].geCostModelClass()).newInstance();
                pCap.setResourceCostModel(costModel);
            }
            PlatformCapabilityLoader.getLoadableClassPath(pCap);
            pCaps[i] = pCap;
        }
        return(pCaps);
    }

    private String getRioHome() {
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome==null) {
            rioHome = System.getenv("RIO_HOME");
            if(rioHome!=null) {
                System.setProperty("RIO_HOME", rioHome);
                logger.info("Set RIO_HOME to "+System.getProperty("RIO_HOME"));
            }
        }
        return rioHome;
    }

}
