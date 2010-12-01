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
package org.rioproject.monitor;

import org.rioproject.config.Constants;
import org.rioproject.core.OperationalString;
import org.rioproject.core.provision.DownloadRecord;
import org.rioproject.opstring.*;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.resources.util.FileUtils;
import org.rioproject.resolver.Resolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract {@link DeployHandler} providing support for ease of code.
 * {@link org.rioproject.core.OperationalString}s are created and returned.
 *
 */
public abstract class AbstractOARDeployHandler implements DeployHandler {
    protected OpStringLoader opStringLoader;
    protected Logger logger = Logger.getLogger(getClass().getName());
    protected abstract List<OperationalString> look(Date from);

    protected AbstractOARDeployHandler() {
        try {
            opStringLoader =
                new OpStringLoader(this.getClass().getClassLoader());
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                       "Could not create OpStringLoader, unable to deploy " +
                       "OperationalString Archive (OAR) files",
                       e);
        }
    }

    public List<OperationalString> listofOperationalStrings() {
        List<OperationalString> list = new ArrayList<OperationalString>();
        if (opStringLoader != null) {
            list.addAll(look(new Date(0)));
        } else {
            logger.warning("No OpString loader found. Won't be able to list " +
                           "OperationalStrings.");
        }
        return Collections.unmodifiableList(list);
    }

    public List<OperationalString> listofOperationalStrings(Date fromDate) {
        if (fromDate == null)
            throw new IllegalArgumentException("the fromDate must not be null");
        List<OperationalString> list = new ArrayList<OperationalString>();
        if (opStringLoader != null) {
            list.addAll(look(fromDate));
        } else {
            logger.warning("No OpString loader found. Won't be able to list " +
                           "OperationalStrings.");
        }
        return Collections.unmodifiableList(list);
    }

    protected List<OperationalString> parseOAR(OAR oar, Date from) {
        List<OperationalString> list = new ArrayList<OperationalString>();
        File dir = new File(oar.getDeployDir());
        File opstringFile = OARUtil.find(oar.getOpStringName(), dir);
        if (opstringFile != null) {
            Date opstringDate = new Date(opstringFile.lastModified());
            if (opstringDate.after(from)) {

                File pom = OARUtil.find("*.pom", dir);

                try {
                    OperationalString[] opstrings =
                        opStringLoader.parseOperationalString(opstringFile);
                    for(OperationalString opstring : opstrings) {
                        ((OpString) opstring).setLoadedFrom(oar.getURL());
                        try {
                            Resolver resolver = ResolverHelper.getInstance();
                            OpStringUtil.checkCodebase(opstring,
                                                       System.getProperty(
                                                           Constants.CODESERVER),
                                                       resolver,
                                                       pom);
                            list.add(opstring);
                        } catch(IOException e) {
                            logger.log(Level.WARNING,
                                       "Unable to resolve codebase for services " +
                                       "in ["+opstring.getName()+"] using codebase "+
                                       System.getProperty(Constants.CODESERVER)+
                                       ". Make sure that the codeserver is set " +
                                       "up correctly to serve the service's " +
                                       "jars, also make sure you have published " +
                                       "the service jars to the correct " +
                                       "location.",
                                       e);
                        }
                    }
                    //list.addAll(Arrays.asList(opstrings));
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                               "Parsing ["+ FileUtils.getFilePath(opstringFile)+"]",
                               e);
                }
            } else {
                if(logger.isLoggable(Level.FINEST))
                    logger.finest("OperationalString file " +
                                  "["+FileUtils.getFilePath(opstringFile)+"] " +
                                  "has a last modified date of: "
                                  +opstringDate+", which is before " +
                                  "requested date: "+from);
            }
        } else {
            logger.warning("No OperationalString found in OAR: "+
                           oar.getOpStringName()+", " +
                           "deploy directory: "+FileUtils.getFilePath(dir)+". " +
                           "The OAR may not be built correctly.");
        }
        if(logger.isLoggable(Level.FINER)) {
            StringBuffer buffer = new StringBuffer();
            for(OperationalString opstring : list) {
                if(buffer.length()>0)
                    buffer.append(", ");
                buffer.append(opstring.getName());
            }

            if(list.size()==0)
                logger.finer("Returning ["+list.size()+"] OperationalStrings");
            else
                logger.finer("Returning ["+list.size()+"], " +
                            "OperationalStrings: "+buffer.toString());
        }
        return list;
    }

    protected DownloadRecord install(URL archive, File installDir)
        throws IOException, OARException {

        DownloadRecord record = OARUtil.install(archive, installDir);
        /* Persist the download record */
        ObjectOutputStream oos = null;
        try {
            String path = record.getPath();
            if (!path.endsWith("/"))
                path = path + "/";
            oos = new ObjectOutputStream(new FileOutputStream(path + "record.ser"));
            oos.writeObject(record);
            oos.flush();
        } catch (IOException e) {
            if(oos!=null)
                oos.close();
            logger.log(Level.WARNING, "Writing DownloadRecord", e);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Installing "+archive.toExternalForm(), t);
        }
        return (record);
    }

    protected void install(File oar, File installDir) throws IOException, OARException {
        OARUtil.install(oar, installDir);
    }

}
