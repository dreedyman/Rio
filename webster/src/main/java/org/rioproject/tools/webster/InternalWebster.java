/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.tools.webster;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.CodeSource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for starting an Internal Webster
 *
 * @author Dennis Reedy
 */
public class InternalWebster {
    private static Logger logger = Logger.getLogger("org.rioproject.tools.webster");

    /**
     * Start an internal webster, setting the webster root to the location
     * this class was loaded from, and appending exportJar as the export
     * codebase for the JVM.
     *
     * <p>This method should only be called by Jini client entities that
     * require their export codebase to be set and arent clever enough to have
     * the location of where Webster should serve code from
     *
     * @param exportJar The jar to set for the codebase
     *
     * @return The port Webster has been started on
     *
     * @throws IOException If there are errors creating Webster
     */
    public static int startWebster(String exportJar) 
    throws IOException {
        String parentPath;
        InetAddress ip = InetAddress.getLocalHost();
        String localIPAddress = ip.getHostAddress();

        CodeSource cs =
            InternalWebster.class.getProtectionDomain().getCodeSource();
        if(cs != null && cs.getLocation() != null) {
            String codebase = cs.getLocation().toExternalForm();
            File jar = new File(codebase);
            URL parentURL = new URL(jar.getParent());
            File parent = new File(parentURL.getPath());
            parentPath = parent.getPath();
        } else {
            throw new RuntimeException("Cannot determine CodeSource");
        }

        String sMaxThreads = 
            System.getProperty("org.rioproject.tools.webster.maxThreads", 
                               "10");
        int maxThreads = 10;
        try {
            maxThreads = Integer.parseInt(sMaxThreads);
        } catch(NumberFormatException e) {
            logger.log(Level.WARNING, 
                       "Bad Max Threads Number ["+sMaxThreads+"], "+
                       "default to "+maxThreads, 
                       e);
        }
        String sPort =
            System.getProperty("org.rioproject.tools.webster.port",
                               "0");
        int port = 0;
        try {
            port = Integer.parseInt(sPort);
        } catch(NumberFormatException e) {
            logger.log(Level.WARNING,
                       "Bad port Number ["+sPort+"], "+
                       "default to "+port,
                       e);
        }

        port = new Webster(port,
                           parentPath,
                           null,
                           maxThreads).getPort();
        if(logger.isLoggable(Level.FINEST))
            logger.finest("Webster MaxThreads="+maxThreads);

        if(logger.isLoggable(Level.FINE))
            logger.fine("Webster serving on port="+port);

        String codebase = System.getProperty("java.rmi.server.codebase");
        
        if(codebase==null) {
            codebase = "http://"+localIPAddress+":"+port+"/"+exportJar;
            System.setProperty("java.rmi.server.codebase", codebase);
            if(logger.isLoggable(Level.FINE))
                logger.fine("Setting java.rmi.server.codebase : " + codebase);
        }
        return(port);
    }

    public static void main(String[] args) {
        try {            
            startWebster("rio-dl.jar");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}
