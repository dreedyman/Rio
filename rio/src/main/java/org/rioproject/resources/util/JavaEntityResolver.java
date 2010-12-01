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
package org.rioproject.resources.util;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class resolves an XML public or system entity in the
 * for of java://com/package/foo.dtd. It uses the java class path
 * to locate the dtd file.
 *
 * @author Jim Clarke
 * @author Dennis Reedy
 */
public class JavaEntityResolver implements EntityResolver {
    /** Holds value of property classLoader. */
    private ClassLoader classLoader;
    /** Logger instance  */
    private final static Logger logger =
        Logger.getLogger("org.rioproject.resources.util");
    static final String OLD_DTD = "org/jini/rio/dtd/rio_opstring.dtd";
    static final String DTD = "org/rioproject/dtd/rio_opstring.dtd";
    static final String PUBLIC_DTD = "http://www.rio-project.org/dtd/rio_opstring.dtd";

    /** Creates new JavaEntityResolver */
    public JavaEntityResolver() {
        Class c = this.getClass();
        classLoader = c.getClassLoader();
    }

    public JavaEntityResolver(ClassLoader classLoader) {
        this();
        setClassLoader(classLoader);
    }

    @SuppressWarnings("unchecked")
    public InputSource resolveEntity(String publicId, String systemId) 
    throws SAXException, IOException {
        if(publicId != null) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                           "Looking for public {0}, "+
                           "java:// for public is not yet implemented",
                           new Object[] {publicId});
                
            }
            System.out.flush();
            // not yet implemented.
        }
        boolean isThrowing = false;
        if(systemId != null) {
            if(systemId.equals(PUBLIC_DTD))
                systemId = "java://"+DTD;
            if(systemId.startsWith("java://")) {
                String target = systemId.substring(7);
                if (OLD_DTD.equals(target))
                    target = DTD;
                int paren;
                if((paren = target.indexOf('(')) > 2) { 
                    /* we have a java class to execute
                     * the form foo.bar() is executed and the result is passed back 
                     * as a string */
                    int demarc = target.lastIndexOf('.', paren);
                    String classpart = target.substring(0, demarc);
                    Hashtable methodTable = 
                        getMethodParts(target.substring(demarc+1));
                    List list = new LinkedList();
                    Class[] paramTypes;
                    Object[] paramArgs=null;
                    try {
                        Class clazz = Class.forName(classpart, true, classLoader);
                        Method meth = null;
                        Object instance = null;
                        list.add(clazz);
                        int last = methodTable.size()-1;
                        int i=0;
                        for(Enumeration en=methodTable.keys(); 
                                                            en.hasMoreElements();) {
                            String key = (String)en.nextElement();
                            String[] paramparts = 
                                getParamParts((String)methodTable.get(key));
                            if(paramparts.length ==0) {
                                paramTypes = new Class[0];
                                paramArgs = new Object[0];
                            } else {
                                paramTypes = new Class[paramparts.length];
                                for(int j=0; j<paramparts.length; j++)
                                    paramTypes[j] = String.class;
                                paramArgs = new Object[paramparts.length];
                                System.arraycopy(paramparts,
                                                 0,
                                                 paramArgs,
                                                 0,
                                                 paramArgs.length);
                            }
                            meth = clazz.getMethod(key, paramTypes);
                            list.add(meth);
                            if(i < last) {
                                clazz = meth.getReturnType();
                                list.add(clazz);
                            }
                            i++;
                        }
                        Iterator iter = list.iterator();
                        while(iter.hasNext()) {
                            try {
                                clazz = (Class)iter.next();
                                meth = (Method)iter.next();
                                instance = clazz.newInstance();
                            } catch(Exception ignore) {
                                // if no ctor, then it is assumed the method is static
                            }
                            instance = meth.invoke(instance, paramArgs);
                        }
                        if(instance==null) {
                            isThrowing = true;
                            System.out.println("Target ["+target+"] unresolvable");
                            //return(null);
                            throw new SAXException("Target ["+target+"] unresolvable");
                        }
                        return(new InputSource(
                                      new StringReader(instance.toString().trim())));
                    } catch(Throwable th) {
                        if(isThrowing)
                            throw (SAXException)th;
                        th.printStackTrace();
                        return(null);
                    }
                } else { // we have a java resource to load
                    InputStream in = getClassLoader().getResourceAsStream(target);
                    if(in != null)
                        return(new InputSource(in));
                    else {                        
                        logger.log(Level.WARNING, 
                                   "Failed to find in class path " + systemId);
                        System.out.flush();

                        return(null);
                    }
                }
            } else {
                // use default resolver
                return(null);
            }
        } else
            return(null);
    }
   
    /** 
     * Getter for property classLoader.
     * @return Value of property classLoader.
     */
    public ClassLoader getClassLoader() {
        return(classLoader);
    }

    /** 
     * Setter for property classLoader.
     * @param classLoader New value of property classLoader.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private Hashtable getMethodParts(String target) {
        Hashtable<String, String> table = new Hashtable<String, String>();
        try {
            int index=0;
            int ndx1, ndx2;
            String method, part;

            while(index < target.length()) {
                part = target.substring(index, target.length());
                ndx1 = part.indexOf('(');
                if(ndx1==-1)
                    return(table);
                method = part.substring(0, ndx1);
                ndx2 = part.indexOf(')');
                if((ndx2-ndx1)>1) {
                    String args = part.substring(ndx1+1, ndx2);
                    table.put(method, args);
                } else {
                    table.put(method, "");
                }
                index+=ndx2+1;
                if(target.substring(index, target.length()).startsWith(".")) {
                    index++;
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return(table);
    }

    private String[] getParamParts(String target) {
        List<String> list = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(target, ",)");

        while(st.hasMoreTokens()) {
            String param = st.nextToken();
            list.add(param);
        }
        return(list.toArray(new String[list.size()]));
    }
}
