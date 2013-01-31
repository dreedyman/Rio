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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.ServiceItemFilter;
import org.rioproject.entry.OperationalStringEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Filter for matching opstring names
 *
 * @author Dennis Reedy
 */
public class OpStringFilter implements ServiceItemFilter {
    private String opStringName;
    private static final Logger logger = LoggerFactory.getLogger(OpStringFilter.class);
    /**
     * Create an OpStringFilter
     * 
     * @param opStringName The name of the OperationalString to filter
     */
    public OpStringFilter(String opStringName) {
        if(opStringName==null)
            throw new IllegalArgumentException("opStringName is null");
        this.opStringName = opStringName;
    }
    
    /**
     * If the input ServiceItem has an 
     * {@link org.rioproject.entry.OperationalStringEntry} and the name matches
     * the <code>opStringName</code> property of this class, then return 
     * <code>true</code>
     * 
     * Additionally, if the input ServiceItem does not have the
     * {@link org.rioproject.entry.OperationalStringEntry} in it's attribute set,
     * return <code>true</code>. This supports intended semantics to discover 
     * external services which may match the 
     * {@link net.jini.core.lookup.ServiceTemplate}
     *   
     * @see net.jini.lookup.ServiceItemFilter#check(net.jini.core.lookup.ServiceItem)
     */
    public boolean check(ServiceItem item) {
        if(getOperationalStringEntry(item.attributeSets)!=null)
            return(matches(item.attributeSets, opStringName));
        return(true);        
    }
    
    /**
     * Determine if the attribute collecton contains an OperatonalStringEntry which
     * matches the opStrngName
     * 
     * @param attrs Array of Entry objects
     * @param opStringName The name to match
     * 
     * @return If an OperationalStringEntry is found in the collection and it's
     * name matches the<code>opStringName</code> parameter, return <code>true</code>,
     * otherwise, return <code>false</code>
     */
    public static boolean matches(Entry[] attrs, String opStringName) {
        boolean matched = false;
        for (Entry attr : attrs) {
            if (attr.getClass().getName().equals(OperationalStringEntry.class.getName())) {
                if (attr instanceof OperationalStringEntry) {
                    OperationalStringEntry oe = (OperationalStringEntry) attr;
                    if (oe.name.equals(opStringName)) {
                        logger.trace("Matched required {} with value of {}", oe.name, opStringName);
                        matched = true;
                        break;
                    } else {
                        logger.trace("Did not match required {} with value of {}", oe.name, opStringName);
                    }
                } else {
                    /*
                     * This addresses the issue where the discovered service
                     * has an OperationalStringEntry but there is a class loading
                     * problem, which results in the classes being loaded by sibling
                     * class loaders, and assignment does not work.
                     */
                    OperationalStringEntry oe = new OperationalStringEntry();
                    try {
                        Field name = attr.getClass().getDeclaredField("name");
                        oe.name = (String) name.get(attr);
                        if (oe.name.equals(opStringName)) {
                            logger.trace("Reflective matching: Matched required {} with value of {}", oe.name, opStringName);
                            matched = true;
                            break;
                        } else {
                            logger.trace("Reflective matching: Did not match required {} with value of {}", oe.name, opStringName);
                        }
                    } catch (Exception e) {
                        logger.warn("Could not obtain {}", OperationalStringEntry.class.getName(), e);
                    }
                }
            }
        }
        return(matched);
    }    
    
    /**
     * Gets first OperationalStringEntry from attribute array
     * 
     * @param attrs Array of Entry objects
     * 
     * @return OperationalStringEntry
     */
    public static OperationalStringEntry getOperationalStringEntry(Entry[] attrs) {
        for(int x=0; x < attrs.length; x++) {
            if(attrs[x].getClass().getName().equals(
                                       OperationalStringEntry.class.getName())) {                                               
                if(attrs[x] instanceof OperationalStringEntry) {
                    return(OperationalStringEntry)attrs[x];
                } else {
                    /*
                     * This addresses the issue wjere the discovered service
                     * has an OperationalStrine entry but there is a class loading
                     * problem, which results in the classes bing loaded by sibling
                     * class loaders, and assignability doesnt work.
                     */
                    OperationalStringEntry oe = new OperationalStringEntry();
                    try {
                        Field name = attrs[x].getClass().getDeclaredField("name");                
                        oe.name = (String)name.get(attrs[x]);
                    } catch(Exception e) {
                        e.printStackTrace();
                        oe = null;                    
                    }
                    return(oe);
                }
            }
        }
        return(null);
    }
}
