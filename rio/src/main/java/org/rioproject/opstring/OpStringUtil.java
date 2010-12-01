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
package org.rioproject.opstring;

import org.rioproject.core.ClassBundle;
import org.rioproject.core.OperationalString;
import org.rioproject.core.ServiceElement;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.resources.util.PropertyHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for working with OpStrings.
 *
 * @author Dennis Reedy
 */
public class OpStringUtil {
    static Logger logger = Logger.getLogger(OpStringUtil.class.getPackage().getName());

    /**
     * Check if the codebase is null or the codebase needs to be resolved
     *
     * @param opstring The OperationalString to check
     * @param codebase If the codebase is not set, set it to this value
     * @param resolver The Resolver to use if artifacts need to be downloaded
     *
     * @throws java.io.IOException If the jars cannot be served
     */
    public static void checkCodebase(OperationalString opstring,                                     
                                     String codebase,
                                     Resolver resolver) throws IOException {

        for (ServiceElement elem : opstring.getServices()) {
            checkCodebase(elem, codebase, resolver, null);
        }
        OperationalString[] nesteds = opstring.getNestedOperationalStrings();
        for (OperationalString nested : nesteds) {
            checkCodebase(nested, codebase, resolver);
        }
    }

    /**
     * Check if the codebase is null or the codebase needs to be resolved
     *
     * @param opstring The OperationalString to check
     * @param codebase If the codebase is not set, set it to this value
     * @param resolver The Resolver to use if artifacts need to be downloaded
     * @param resolveFile The file to pass to the Resolver
     *
     * @throws java.io.IOException If the jars cannot be served
     */
    public static void checkCodebase(OperationalString opstring,
                                     String codebase,
                                     Resolver resolver,
                                     File resolveFile) throws IOException {

        for (ServiceElement elem : opstring.getServices()) {
            checkCodebase(elem, codebase, resolver, resolveFile);
        }
        OperationalString[] nesteds = opstring.getNestedOperationalStrings();
        for (OperationalString nested : nesteds) {
            checkCodebase(nested, codebase, resolver, resolveFile);
        }
    }

    /**
     * Check if the codebase is null or the codebase needs to be resolved
     *
     * @param elem The ServiceElement to check
     * @param codebase If the codebase is not set, set it to this value
     * @param resolver The Resolver to use if artifacts need to be downloaded
     * @param resolveFile The file to pass to the Resolver
     *
     * @throws java.io.IOException If the jars cannot be served
     */
    public static void checkCodebase(ServiceElement elem,
                                     String codebase,
                                     Resolver resolver,
                                     File resolveFile) throws IOException {

        if (codebase != null) {
            if (!codebase.endsWith("/"))
                codebase = codebase + "/";
        }
        ClassBundle bundle = elem.getComponentBundle();
        boolean deployedAsArtifact = bundle.getArtifact()!=null;
        if(!deployedAsArtifact) {
            if (bundle.getCodebase() == null) {
                if (codebase == null) {
                    if (logger.isLoggable(Level.WARNING))
                        logger.warning("Cannot fix null codebase for " +
                                       "[" + elem.getName() + "], " +
                                       "unknown codebase");
                    return;
                }
                for(String jar : bundle.getJARNames())
                    canServe(jar, codebase);
                bundle.setCodebase(codebase);
                logger.fine("Fixed ClassBundle "+bundle);

            } else if (bundle.getRawCodebase().startsWith("$[")) {
                String resolved =
                    PropertyHelper.expandProperties(bundle.getRawCodebase(),
                                                    PropertyHelper.RUNTIME);
                if (resolved == null) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Cannot fix " +
                                    "[" + bundle.getRawCodebase() +
                                    "] codebase " +
                                    "for [" + elem.getName() + "], " +
                                    "unknown property");
                    return;
                }
                canServe(bundle.getClassName(), codebase);
                bundle.setCodebase(codebase);
            }
        }

        ClassBundle[] exports = elem.getExportBundles();
        StringBuffer sb = new StringBuffer();

        StringBuffer sb1 = new StringBuffer();
        boolean didResolve = false;
        for (ClassBundle export : exports) {
            if(export.getArtifact()!=null) {
                sb.append(" (").append(export.getArtifact()).append("): ");
                ResolverHelper.resolve(export, resolver, resolveFile, true);
                didResolve = true;
                //ResolverHelper.resolve(export, resolver, resolveFile, codebase, true);
            } else {
                if(deployedAsArtifact) {
                    return;
                    /*System.out.println("===> service interface: "+export.getClassName());
                    sb.append(" (").append("default-exports").append("): ");
                    export.setCodebase("file://");
                    for(String jar : export.getJARNames()) {
                        if(getPath(jar)!=null)
                            export.addJAR(getPath(jar));
                    }*/
                }
            }
            if (export.getCodebase() == null) {
                for(String jar : export.getJARNames()) {
                    canServe(jar, codebase);
                }
                export.setCodebase(codebase);
                logger.fine("Fixed export ClassBundle "+export);
            } else if (export.getRawCodebase().startsWith("$[")) {
                String resolved =
                    PropertyHelper.expandProperties(export.getRawCodebase(),
                                                    PropertyHelper.RUNTIME);
                for(String jar : export.getJARNames())
                    canServe(jar, resolved);
                export.setCodebase(resolved);
                logger.fine("Fixed export ClassBundle "+export);
            }
            for(String jar : export.getJARNames()) {
                if(sb1.length()>0)
                    sb1.append(", ");
                else
                    sb1.append("\n");
                sb1.append(export.getCodebase()).append(jar);
            }
        }
        if(didResolve)
            elem.setRemoteRepositories(resolver.getRemoteRepositories());
        sb.append(sb1.toString());
        if (logger.isLoggable(Level.INFO)) {
            if(deployedAsArtifact)
                logger.info(elem.getName()+" "+
                            "derived classpath for loading artifact "+sb.toString());
        }
        /*
        if(elem.getFaultDetectionHandlerBundle()!=null) {
            ClassBundle fdhBundle = elem.getFaultDetectionHandlerBundle();
            if(fdhBundle.getCodebase()==null) {
                canServe(fdhBundle.getClassName(), codebase);
                fdhBundle.setCodebase(codebase);
            } else if(fdhBundle.getRawCodebase().startsWith("$[")) {
                String resolved =
                    PropertyHelper.expandProperties(fdhBundle.getRawCodebase(),
                                                    PropertyHelper.RUNTIME);
                canServe(fdhBundle.getClassName(), resolved);
                fdhBundle.setCodebase(codebase);
            }
        }
        */

    }

    private static void canServe(String name, String codebase) throws IOException {
        if(name.equals("rio-dl.jar") || name.equals("jsk-dl.jar"))
            return;
        InputStream is = null;
        URLConnection conn = null;
        try {
            URL url = new URL(codebase + name);
            if(codebase.startsWith("file:")) {
                try {
                    File f = new File(url.toURI());
                    if(!f.exists())
                        throw new IOException("Could not create URI from "+codebase);
                } catch (URISyntaxException e) {
                    throw new IOException("Could not create URI from "+codebase,
                                          e);
                }

            } else {
                conn = url.openConnection();
                is = conn.getInputStream();
                if(logger.isLoggable(Level.FINER))
                    logger.finer("Opened connection for "+url.toExternalForm());
            }

        } finally {
            if(is!=null)
                is.close();
            if(conn!=null && conn instanceof HttpURLConnection)
                ((HttpURLConnection)conn).disconnect();
        }
    }

    private static String getPath(String jarName) {
        File rioDLDir = new File(System.getProperty("RIO_HOME"), "lib-dl");
        File jar = new File(rioDLDir, jarName);
        String path = null;
        if(jar.exists())
            path = jar.getAbsolutePath();
        return path;

    }
}
