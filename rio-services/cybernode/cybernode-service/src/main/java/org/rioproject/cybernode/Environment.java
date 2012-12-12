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
package org.rioproject.cybernode;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.deploy.ServiceStatementManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The Environment class provides methods to query and setup the operational
 * environment required by the Cybernode
 *
 * @author Dennis Reedy
 */
public class Environment {
    /** Logger */
    static Logger logger = LoggerFactory.getLogger(Environment.class.getName());

    /*
     * Setup the default environment
     */
    static void setupDefaultEnvironment() throws IOException {
        String rioHomeDirectory = getRioHomeDirectory();
        File logDir = new File(rioHomeDirectory+"logs");
        checkAccess(logDir, false);
    }

    /**
     * Get the ServiceStatementManager
     * 
     * @param config The Configuration object 
     * 
     * @return A ServiceStatementManager based on the environment
     */
    static ServiceStatementManager getServiceStatementManager(Configuration config) {
        ServiceStatementManager defaultServiceStatementManager = new TransientServiceStatementManager(config);
        ServiceStatementManager serviceStatementManager;
        try {
            serviceStatementManager = (ServiceStatementManager)config.getEntry(CybernodeImpl.getConfigComponent(),
                                                                               "serviceStatementManager",
                                                                               ServiceStatementManager.class,
                                                                               defaultServiceStatementManager,
                                                                               config);
        } catch(ConfigurationException e) {
            logger.warn("Exception getting ServiceStatementManager", e);
            serviceStatementManager = defaultServiceStatementManager;
        }

        logger.debug("Using ServiceStatementManager: {}", serviceStatementManager.getClass().getName());
        return(serviceStatementManager);
    }

    /*
     * Setup the provisionRoot directory
     * 
     * @param provisionEnabled Whether provisioning has been enabled
     * @param config The Configuration object 
     * 
     * @return The root directory to provision software to
     */
    public static String setupProvisionRoot(boolean provisionEnabled,  Configuration config) throws IOException {
        String provisionRoot = getRioHomeDirectory()+"system"+File.separator+"external";
        try {
            provisionRoot = (String)config.getEntry(CybernodeImpl.getConfigComponent(), 
                                                    "provisionRoot", 
                                                    String.class, 
                                                    provisionRoot);
        } catch(ConfigurationException e) {
            logger.warn("Exception getting provisionRoot", e);
        }
        if(provisionEnabled) {
            File provisionDir = new File(provisionRoot);
            checkAccess(provisionDir);
            provisionRoot = provisionDir.getCanonicalPath();
        }
        return(provisionRoot);
    }
    
    /*
     * Setup the native directories
     * 
     * @param config The Configuration object 
     * 
     * @return A space delimited String of directory names to load native 
     * libraries from
     */
    static String setupNativeLibraryDirectories(Configuration config) throws IOException {
        List<String> nativeDirs = new ArrayList<String>();
        try {
            String configuredNativeDirs = (String)config.getEntry(CybernodeImpl.getConfigComponent(),
                                                                  "nativeLibDirectory", 
                                                                  String.class,
                                                                  null);
            if(configuredNativeDirs!=null) {
                String[] dirs = toStringArray(configuredNativeDirs);
                for(String dir : dirs) {
                    if(!nativeDirs.contains(dir))
                        nativeDirs.add(dir);
                }
            }
        } catch(ConfigurationException e) {
            logger.warn("Exception getting configured nativeLibDirectories", e);
        }
        String nativeLibDirs = null;
        if(!nativeDirs.isEmpty()) {
            StringBuilder buffer = new StringBuilder();
            String[] dirs = nativeDirs.toArray(new String[nativeDirs.size()]);
            for(int i=0; i<dirs.length; i++) {
                File nativeDirectory = new File(dirs[i]);
                if(i>0)
                    buffer.append(" ");
                buffer.append(nativeDirectory.getCanonicalPath());
            }
            nativeLibDirs = buffer.toString();
        }
        return nativeLibDirs;
    }
    

    /**
     * Setup the recordRoot directory
     *
     * @param config The Configuration to use
     *
     * @return File object for the record root directory
     *
     * @throws IOException if there are errors accessing the file system
     */
    public static File setupRecordRoot(Configuration config) throws IOException {
        String recordDir = getRioHomeDirectory()+"logs"+File.separator+"records";
        try {
            recordDir = (String)config.getEntry(CybernodeImpl.getConfigComponent(), 
                                                "recordDirectory", 
                                                String.class, 
                                                recordDir);
        } catch(ConfigurationException e) {
            logger.warn("Exception getting recordDirectory", e);
        }
        File recordRoot = new File(recordDir);        
        checkAccess(recordRoot);
        return(recordRoot);
    }

    /**
     * Get (and if needed create) the Rio home directory. If the environment variable
     * <pre>org.rioproject.home</pre> is not set, the Rio home directory will default 
     * to the <pre>.rio</pre> directory in the user's home directory 
     * 
     * @return The path to the Rio home directory
     */
    static String getRioHomeDirectory() {
        String rioHome;
        if(System.getProperty("org.rioproject.home")!=null) {
            rioHome = System.getProperty("org.rioproject.home");
        } else {
            if(System.getProperty("RIO_HOME")!=null) {
                rioHome = System.getProperty("RIO_HOME");
            } else {
                rioHome = System.getenv("RIO_HOME");
            }
            if(rioHome==null)
                rioHome = System.getProperty("user.home")+File.separator+".rio";
        }
        System.setProperty("org.rioproject.home", rioHome);
        File rioPath = new File(rioHome);
        if(!rioPath.exists()) {
            if(rioPath.mkdir()) {
                logger.debug("Created home directory [{}]", rioHome);
            }
        }
        if(!rioHome.endsWith(File.separator))
            rioHome = rioHome+File.separator;
        return(rioHome);
    }

    /**
     * Verify read and write access to a directory
     * 
     * @param directory Directory File object
     * 
     * @throws IOException if read or write access is not permitted
     */
    static void checkAccess(File directory) throws IOException {
        checkAccess(directory, true);        
    }
    
    /**
     * Verify read and write access to a directory
     * 
     * @param directory Directory File object
     * @param isWriteable Check if directory is writeable
     * 
     * @throws IOException if read or write access is not permitted
     */
    static void checkAccess(File directory, boolean isWriteable) throws IOException {
        if(!directory.exists()) {
            if(directory.mkdirs()) {
                logger.debug("Created directory [{}]", directory.getCanonicalPath());
            } else {
                throw new IOException("Could not create directory " +
                                      "["+directory.getCanonicalPath()+"], " +
                                      "make sure you have the proper " +
                                      "permissions to create, read & write " +
                                      "to the file system");
            }
        }
        // Find if we can write to the record root directory
        if(!directory.canRead())
            throw new IOException("Cant read from : "+directory.getCanonicalPath());
        if(isWriteable) {
            if(!directory.canWrite())
                throw new IOException("Cant write to : "+directory.getCanonicalPath());
        }
    }

    static String[] toStringArray(String s) {
        StringTokenizer tok = new StringTokenizer(s, File.pathSeparator+" ");
        List<String> sList = new ArrayList<String>();
        while(tok.hasMoreTokens())
            sList.add(tok.nextToken());
        return sList.toArray(new String[sList.size()]);
    }
}
