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
package org.rioproject.impl.opstring;

import org.rioproject.config.Constants;
import org.rioproject.opstring.OperationalString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * The {@code OpStringLoader} utility is a helper class used to parse and return an {@link org.rioproject.opstring.OperationalString}.<br/>
 * This class searches in the given classloader for a resource named
 * <tt>/org/rioproject/opstring/OpStringParserSelectionStrategy</tt> in the classpath.
 * If found the content of that resource is used to indicate the name of the class
 * to use as an {@link OpStringParserSelectionStrategy}.
 *
 * @author Dennis Reedy
 * @author Jerome Bernard
 */
@SuppressWarnings("unused")
public class OpStringLoader {
    private ClassLoader loader;
    private String[] groups;
    /** Path location of an OperationalString loaded from the file system */
    private String loadPath;
    /** Default FaultDetectionHandler */
    public static final String DEFAULT_FDH = "org.rioproject.impl.fdh.AdminFaultDetectionHandler";
    private static final String OPSTRING_PARSER_SELECTION_STRATEGY_LOCATION =
        "META-INF/org/rioproject/opstring/OpStringParserSelectionStrategy";
    /**
     * Simple constructor that creates an {@code OpStringLoader}
     */
    public OpStringLoader() {
        this(null);
    }

    /**
     * Create a new OpStringLoader, validating documents as they are parsed.
     * 
     * @param loader The parent ClassLoader to use for delegation
     */
    public OpStringLoader(ClassLoader loader) {
        this.loader = loader;
        String group = System.getProperty(Constants.GROUPS_PROPERTY_NAME);
        if (group != null)
            this.groups = new String[]{group};
    }

    /**
     * Set the default groups to add into the parsed OperationalString.
     *
     * @param groups The groups to set, must not be null
     *
     * @throws IllegalArgumentException if the groups parameter is null or
     * if the groups parameter is a zero-length array
     */
    public void setDefaultGroups(String... groups) {
        if(groups==null)
            throw new IllegalArgumentException("groups is null");
        if(groups.length == 0)
            throw new IllegalArgumentException("groups is empty");
        this.groups = new String[groups.length];
        System.arraycopy(groups, 0, this.groups, 0, groups.length);
    }

    /**
     * Parse on OperationalString from a File
     * 
     * @param file A File object for a groovy file
     * @return An array of OperationalString objects parsed from the file.
     *
     * @throws Exception if any errors occur parsing the document
     */
    public OperationalString[] parseOperationalString(File file) throws Exception {
        if (loadPath == null) {
            String path = file.getCanonicalPath();
            int index = path.lastIndexOf(System.getProperty("file.separator"));
            loadPath = path.substring(0, index + 1);
        }
        return parse(file);
    }

    /**
     * Parse on OperationalString from a URL location.
     * 
     * @param url URL location of the OperationalString
     * @return An array of OperationalString objects
     * parsed from the document loaded from the URL.
     *
     * @throws Exception if any errors occur parsing the document
     */
    public OperationalString[] parseOperationalString(URL url) throws Exception {
        if(url==null)
            throw new IllegalArgumentException("url is null");
        if (loadPath == null) {
            String path = url.toExternalForm();
            int index = path.lastIndexOf('/');
            loadPath = path.substring(0, index + 1);
        }
        return parse(url);
    }

    @SuppressWarnings("unchecked")
    private OperationalString[] parse(Object source) throws Exception {

        /* Search for a resource named
         * "/org/rioproject/opstring/OpStringParserSelectionStrategy" in the
         * classpath.
         *
         * If found read that resource and use the line of the resource as the
         * name of the class to use for selecting the {@link OpStringParser}
         * based on the source object.
         */
        OpStringParserSelectionStrategy selectionStrategy = new DefaultOpStringParserSelectionStrategy();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (loader != null)
            cl = loader;
        URL propFile = cl.getResource(OPSTRING_PARSER_SELECTION_STRATEGY_LOCATION);
        if (propFile != null) {
            String strategyClassName;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(propFile.openStream()));
                strategyClassName = reader.readLine();
            } finally {
                if (reader != null)
                    reader.close();
            }
            Class<OpStringParserSelectionStrategy> strategyClass =
                    (Class<OpStringParserSelectionStrategy>) cl.loadClass(strategyClassName);
            selectionStrategy = strategyClass.newInstance();
        }

        OpStringParser parser = selectionStrategy.findParser(source);
        /* If the parser has an init method, invoke it. This will
         * allow any previously cached results to be cleared */
        try {
            Method init = parser.getClass().getMethod("init");
            init.invoke(parser);
        } catch (Exception e) {
            //ignore
        }
        // parse the source
        List<OpString> opstrings = parser.parse(source, loader, groups, loadPath);
        return opstrings.toArray(new OperationalString[opstrings.size()]);
    }

    /**
     * Parse on OperationalString from a String location.
     * 
     * @param location String location of the file. The parameter
     * passed in can either point to an URL (prefixed by http) or a file found
     * in the classpath.
     * @return An array of OperationalString objects
     * parsed from an XML document loaded from the location.
     *
     * @throws Exception if any errors occur parsing the document 
     */
    private OperationalString[] parseOperationalString(String location) throws Exception {
        if(location==null)
            throw new IllegalArgumentException("location is null");
        URL url = getURL(location);
        if (url == null)
            throw new FileNotFoundException("OperationalString Location ["+location+"] not found");
        return parseOperationalString(url);
    }

    /**
     * Get the URL from a file or http connection
     *
     * @param location The location string
     *
     * @return A URL
     *
     * @throws MalformedURLException If the location string is bogus
     */
    private URL getURL(String location) throws MalformedURLException {
        URL url;
        if(location.startsWith("http") || location.startsWith("file:")) {
            url = new URL(location);
        } else {
            url = new File(location).toURI().toURL();
        }
        return(url);
    }

}
