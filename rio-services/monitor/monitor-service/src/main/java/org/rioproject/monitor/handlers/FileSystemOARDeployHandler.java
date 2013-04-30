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
package org.rioproject.monitor.handlers;

import org.rioproject.monitor.DeploymentVerifier;
import org.rioproject.opstring.OAR;
import org.rioproject.opstring.OARException;
import org.rioproject.opstring.OARUtil;
import org.rioproject.opstring.OperationalString;
import org.rioproject.resources.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A {@link DeployHandler} that handles OAR files. This <tt>DeployHandler</tt>
 * uses two directories; a <tt>dropDirectory</tt>, and a
 * <tt>installDirectory</tt>.
 *
 * <p>The <tt>dropDirectory</tt> is polled for files ending in ".oar". If found,
 * the OAR files are installed to the <tt>installDirectory</tt>, where
 * {@link org.rioproject.opstring.OperationalString}s are created and returned.
 *
 */
public class FileSystemOARDeployHandler extends AbstractOARDeployHandler {
    private File dropDirectory;
    private File installDirectory;
    private final Map<String, Date> badOARs = new HashMap<String, Date>();

    /**
     * Create a FileSystemOARDeployHandler with the same drop and install
     * directory
     *
     * @param dir The drop and installation directory for OAR files
     * @param deploymentVerifier The {@link DeploymentVerifier} to use
     */
    public FileSystemOARDeployHandler(File dir, DeploymentVerifier deploymentVerifier) {
        this(dir, dir, deploymentVerifier);
    }

    /**
     * Create a FileSystemOARDeployHandler with drop and install
     * directories
     *
     * @param dropDirectory The directory where OAR files will be dropped
     * @param installDirectory The directory to install OARs into
     * @param deploymentVerifier The {@link DeploymentVerifier} to use
     */
    public FileSystemOARDeployHandler(File dropDirectory,
                                      File installDirectory,
                                      DeploymentVerifier deploymentVerifier) {
        super();
        this.deploymentVerifier = deploymentVerifier;
        this.dropDirectory = dropDirectory;
        this.installDirectory = installDirectory;
        if(!dropDirectory.exists()) {
            if(dropDirectory.mkdirs()) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Created dropDeployDir " +FileUtils.getFilePath(dropDirectory));
                }
            }
        }                
        if(!installDirectory.exists()) {
            if(installDirectory.mkdirs()) {
                if(logger.isDebugEnabled()) {
                logger.debug("Created installDir " +FileUtils.getFilePath(installDirectory));
                }
            }
        }
        
        if(logger.isDebugEnabled()) {
            logger.debug("OARs will be scanned for in this directory: "+FileUtils.getFilePath(dropDirectory));
            logger.debug("OARs will be installed into this directory: "+FileUtils.getFilePath(installDirectory));
        }
    }

    protected List<OperationalString> look(Date from) {
        List<OperationalString> list = new ArrayList<OperationalString>();
        File[] files = dropDirectory.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith("oar") &&!isBad(file)) {
                    try {
                        install(file, installDirectory);
                    } catch (IOException e) {
                        logger.warn("Could not install ["+file.getName()+"] " +
                                    "to ["+installDirectory.getName()+"]",
                                    e);
                        badOARs.put(file.getName(), new Date(file.lastModified()));
                    } catch (Exception e) {
                        logger.warn("The ["+file.getName()+"] is an " +
                                    "invalid OAR and cannot be installed, "+
                                    e.getClass().getName()+": "+e.getMessage());
                        badOARs.put(file.getName(), new Date(file.lastModified()));
                    }
                }
            }
        }

        files = installDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    OAR oar = OARUtil.getOAR(file);
                    if (oar!=null && oar.getActivationType().equals(OAR.AUTOMATIC)) {
                        list.addAll(parseOAR(oar, from));
                    }
                } catch (IOException e) {
                    logger.warn("Loading [" + file.getName() + "]", e);
                } catch (OARException e) {
                    logger.warn("Unable to install [" + file.getName() + "]", e);
                }
            }
        }

        return list;
    }

    private boolean isBad(File oar) {
        Date badOar = null;
        for(Map.Entry<String, Date> entry : badOARs.entrySet()) {
            if(entry.getKey().equals(oar.getName())) {
                badOar = entry.getValue();
                break;
            }
        }
        boolean isBad = false;
        if(badOar!=null) {
            Date oarDate = new Date(oar.lastModified());
            if (oarDate.after(badOar)) {
                badOARs.remove(oar.getName());
            } else {
                isBad = true;
            }
        }
        return isBad;
    }
}
