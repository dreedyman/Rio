/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.monitor.service.handlers;

import org.rioproject.config.Constants;
import org.rioproject.impl.opstring.*;
import org.rioproject.monitor.service.DeploymentVerifier;
import org.rioproject.opstring.*;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.impl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An abstract {@link DeployHandler} providing support for ease of code.
 * {@link org.rioproject.opstring.OperationalString}s are created and returned.
 *
 */
public abstract class AbstractOARDeployHandler implements DeployHandler {
    protected OpStringLoader opStringLoader;
    protected Logger logger = LoggerFactory.getLogger(getClass().getName());
    protected DeploymentVerifier deploymentVerifier;

    protected abstract List<OperationalString> look(Date from);

    protected AbstractOARDeployHandler() {
        try {
            opStringLoader = new OpStringLoader(this.getClass().getClassLoader());
        } catch (Exception e) {
            logger.error("Could not create OpStringLoader, unable to deploy " +
                         "OperationalString Archive (OAR) files",
                       e);
        }
    }

    public List<OperationalString> listOfOperationalStrings() {
        List<OperationalString> list = new ArrayList<>();
        if (opStringLoader != null) {
            list.addAll(look(new Date(0)));
        } else {
            logger.warn("No OpString loader found. Won't be able to list OperationalStrings.");
        }
        return Collections.unmodifiableList(list);
    }

    public List<OperationalString> listOfOperationalStrings(Date fromDate) {
        if (fromDate == null)
            throw new IllegalArgumentException("the fromDate must not be null");
        List<OperationalString> list = new ArrayList<>();
        if (opStringLoader != null) {
            list.addAll(look(fromDate));
        } else {
            logger.warn("No OpString loader found. Won't be able to list OperationalStrings.");
        }
        return Collections.unmodifiableList(list);
    }

    protected List<OperationalString> parseOAR(OAR oar, Date from) {
        List<OperationalString> list = new ArrayList<>();
        File dir = new File(oar.getDeployDir());
        File opstringFile = OARUtil.find(oar.getOpStringName(), dir);
        if (opstringFile != null) {
            Date opstringDate = new Date(opstringFile.lastModified());
            if (opstringDate.after(from)) {
                try {
                    OperationalString[] opStrings = opStringLoader.parseOperationalString(opstringFile);
                    for(OperationalString opString : opStrings) {
                        ((OpString) opString).setLoadedFrom(oar.getURL());
                        try {
                            Collection<RemoteRepository> r = oar.getRepositories();
                            RemoteRepository[] repositories = r.toArray(new RemoteRepository[0]);
                            deploymentVerifier.verifyOperationalString(opString, repositories);
                            list.add(opString);
                        } catch(IOException e) {
                            logger.warn("Unable to resolve codebase for services " +
                                        "in ["+opString.getName()+"] using codebase "+
                                        System.getProperty(Constants.WEBSTER)+
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
                    logger.warn("Parsing ["+ FileUtils.getFilePath(opstringFile)+"]", e);
                }
            } else {
                if(logger.isTraceEnabled())
                    logger.trace("OperationalString file " +
                                 "["+FileUtils.getFilePath(opstringFile)+"] " +
                                 "has a last modified date of: "
                                 +opstringDate+", which is before " +
                                 "requested date: "+from);
            }
        } else {
            logger.warn("No OperationalString found in OAR: "+
                        oar.getOpStringName()+", " +
                        "deploy directory: "+FileUtils.getFilePath(dir)+". " +
                        "The OAR may not be built correctly.");
        }
        if(logger.isDebugEnabled()) {
            StringBuilder buffer = new StringBuilder();
            for(OperationalString opstring : list) {
                if(buffer.length()>0)
                    buffer.append(", ");
                buffer.append(opstring.getName());
            }

            if(list.isEmpty())
                logger.debug("Returning ["+list.size()+"] OperationalStrings");
            else
                logger.debug("Returning ["+list.size()+"], OperationalStrings: "+buffer.toString());
        }
        return list;
    }

    protected void install(File oar, File installDir) throws IOException, OARException {
        OARUtil.install(oar, installDir);
    }

}
