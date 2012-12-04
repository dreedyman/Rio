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
package org.rioproject.opstring;

import org.rioproject.deploy.DownloadRecord;
import org.rioproject.deploy.StagedData;
import org.rioproject.resources.util.DownloadManager;
import org.rioproject.resources.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * Utilities for working with OperationalString Archives (OARs)
 *
 * @author Dennis Reedy
 */
public class OARUtil {
    private static final Logger logger = LoggerFactory.getLogger(OARUtil.class.getName());

    /**
     * Install an OAR
     *
     * @param archive The URL of the archive
     * @param installDir The parent directory to install the archive into
     * @return A {@link org.rioproject.deploy.DownloadRecord}
     * detailing the installation specifics
     *
     * @throws OARException If the manifest cannot be read
     * @throws IOException If there are errors accessing the file system or the
     * archive parameter
     */
    public static DownloadRecord install(URL archive, File installDir)
        throws IOException, OARException {

        if(archive.toExternalForm().endsWith("jar") ||
           archive.toExternalForm().endsWith("oar")) {
            OAR oar = new OAR(archive);
            String dirName = oar.getCompoundName();
            if(dirName==null)
                throw new IOException("Not an OAR");
            StagedData artifact = new StagedData();
            artifact.setLocation(archive.toExternalForm());
            artifact.setInstallRoot(dirName);
            artifact.setUnarchive(true);
            DownloadManager downloadMgr =
                new DownloadManager(FileUtils.getFilePath(installDir), artifact);
            DownloadRecord record = downloadMgr.download();
            return(record);
        } else {
            throw new IOException("Installation must be a .jar or an .oar");
        }
    }

    /**
     * Install an OAR
     *
     * @param oarFile The OAR File
     * @param installDir The parent directory to install the archive into
     *
     * @throws OARException If the manifest cannot be read
     * @throws IOException If there are errors accessing the file system or the
     * archive parameter
     */
    public static void install(File oarFile, File installDir)
        throws IOException, OARException {

        if(oarFile.getName().endsWith("jar") ||
           oarFile.getName().endsWith("oar")) {

            JarFile jarFile = new JarFile(oarFile);
            OAR oar;
            try {
                if(jarFile.getManifest()!=null)
                    oar = new OAR(jarFile.getManifest());
                else
                    throw new IOException("No manifest in "+oarFile.getName()+", unable to create OAR");
            } finally {
                jarFile.close();
            }
            
            String dirName = oar.getCompoundName();
            if(dirName==null)
                throw new IOException("Not an OAR");
            File oarInstallDir = new File(installDir, dirName);
            if(!oarInstallDir.exists()) {
                if(oarInstallDir.mkdirs()) {
                    logger.info("Created {}", oarInstallDir.getPath());
                }
            }
            File installed = new File(oarInstallDir, oarFile.getName());
            if(installed.exists()) {
                if(installed.delete())
                   logger.info("Removed older OAR {}", installed.getName());
            }
            if(oarFile.renameTo(installed)) {
                logger.info("Installed OAR to {}", installed.getPath());
                DownloadManager.extract(oarInstallDir, installed);                
            } else {
                throw new IOException("Could not install OAR to "+installed.getPath()+". This may be a permissions problem");
            }
        } else {
            throw new IOException("Installation must be a .jar or an .oar");
        }
    }

    /**
     * Find the OAR file and return an OAR object
     *
     * @param dir The directory to search
     *
     * @return If an OAR file is found, return an OAR object
     *
     * @throws OARException If the manifest cannot be read
     * @throws IOException if there are errors accessing the file system
     */
    public static OAR getOAR(File dir) throws IOException, OARException {
        OAR oar  = null;
        if(dir.isDirectory()) {
            File[] files = dir.listFiles();
            if(files!=null) {
                for (File file : files) {
                    if (file.getName().endsWith("oar")) {
                        oar = new OAR(file);
                        oar.setDeployDir(FileUtils.getFilePath(file.getParentFile()));
                        break;
                    }
                }
            }
        }
        return(oar);
    }

    /**
     * Find a file in an extracted OAR directory
     *
     * @param name The filename to find. If the first character of the
     * name is '*', then the match will be the first file that ends with the
     * remaining string
     * @param dir The parent directory to start the search from
     *
     * @return The file, or null if not found
     */
    public static File find(String name, File dir) {
        File found = null;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    found = find(name, file);
                    if (found != null)
                        break;
                }
                if (name.startsWith("*")) {
                    if (file.getName().endsWith(name)) {
                        found = file;
                        break;
                    }
                } else {
                    if (file.getName().equals(name)) {
                        found = file;
                        break;
                    }
                }
            }
        }

        return found;
    }
}
