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
package org.rioproject.resources.servicecore;

import com.sun.jini.lookup.entry.LookupAttributes;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.JoinManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A utility that contains a <code>JoinManager</code> and provides general
 * utility to set up a service's attribute collection
 * <p>
 * This class does not provide methods to act on a service's
 * <code>ServiceRegistration</code> or duplicate capabilities found in the
 * <code>JoinManager</code>. Actions on a service's registration need to be
 * performed through the <code>JoinManager</code> contained in this class.
 *
 * @author Dennis Reedy
 */
public class Joiner {
    protected JoinManager joinMgr;
    protected final ArrayList<Entry> attrList  = new ArrayList<Entry>();

    /**
     * Get the <code>JoinManager</code> for this <code>Joiner</code><br>
     * 
     * @return The JoinManager created by this utility. If a JoinManager has not 
     * been created, this method will return null.
     */
    public JoinManager getJoinManager() {
        JoinManager jMgr;
        synchronized(this) {
            jMgr = joinMgr;
        }
        return (jMgr);
    }

    /**
     * Delegates call to the JoinManager if a JoinManager exists
     * 
     * @param attrSetTemplates array of Entry used to identify which elements
     * to modify from the service's current set of attributes
     * @param attrSets array of Entry containing the actual modifications to
     * make in the matching sets found using the attrSetTemplates parameter
     * @see net.jini.lookup.JoinManager#modifyAttributes
     */
    public void modifyAttributes(Entry[] attrSetTemplates, Entry[] attrSets) {
        JoinManager jMgr = getJoinManager();
        if(jMgr == null)
            return;
        jMgr.modifyAttributes(attrSetTemplates, attrSets);
    }

    /**
     * Get the <code>DiscoveryManager</code> for this <code>Joiner</code>
     * If the JoinManager is null, this method will rturn null
     * 
     * @return DiscoveryManagement
     */
    public DiscoveryManagement getDiscoveryManager() {
        JoinManager jMgr = getJoinManager();
        if(jMgr == null)
            return(null);
        return (jMgr.getDiscoveryManager());
    }

    /**
     * Add an attribute to the collection of attributes managed by this
     * <code>Joiner</code>. This method does not perform attribute additions
     * on the JoinManager, rather it adds attributes to a collection contained
     * in the <code>Joiner</code>
     *
     * @param entry The attribute to add
     */
    public void addAttribute(Entry entry) {
        Entry[] attrs = getAttributeCollectionEntries();
        for (Entry attr : attrs) {
            if (LookupAttributes.equal(entry, attr)) {
                removeAttribute(attr);
                break;
            }
        }
        attrList.add(entry);
    }

    /**
     * Add an array of Entry attributes to the collection of attributes managed
     * by this <code>Joiner</code>. This method does not perform attribute
     * additions on the JoinManager, rather it adds attributes to a collection
     * contained in the <code>Joiner</code>
     *
     * @param entries An array of attributes to add
     */
    public void addAttributes(Entry[] entries) {
        for (Entry entry : entries)
            addAttribute(entry);
    }

    /**
     * Remove an attribute to the collection of attributes managed by this
     * <code>Joiner</code>. This method does not perform attribute removals
     * on the JoinManager, rather it adds attributes to a collection contained
     * in the <code>Joiner</code>
     *
     * @param entry attribute to remove
     */
    public void removeAttribute(Entry entry) {
        synchronized(attrList) {
            attrList.remove(entry);
        }
    }

    /**
     * Get the current attribute collection. This may not represent the
     * attribute list the JoinManager control so use with caution!
     * 
     * @return the current attribute collection set for the service
     */
    public Entry[] getAttributeCollectionEntries() {
        Entry[] attrs;
        synchronized(attrList) {
            attrs = attrList.toArray(new Entry[attrList.size()]);
        }
        return(attrs);
    }

    /**
     * Clears the collection of <code>Entry</code> elements from the attribute
     * list
     */
    public void clearAttributes() {
        synchronized(attrList) {
            attrList.clear();
        }
    }

    /**
     * Delegates a termination to the <code>JoinManager</code> contained by the
     * <code>Joiner</code>
     */
    public void terminate() {
        synchronized(this) {
            if(joinMgr != null) {
                attrList.clear();
                joinMgr.terminate();
            }
            joinMgr=null;
        }
    }

    /**
     * Provides a fire and forget join mechanism passing in an instance of
     * <code>DiscoveryManagement</code> and <code>LeaseRenewalManagement</code>
     *
     * @param proxy The service proxy
     * @param sid The ServiceID
     * @param additionalAttrs Additional attributes to add
     * @param dMgr A DiscoveryManagement instance
     * @param lrm A LeaseRenewalManager
     *
     * @throws IOException if an exception occurs creating the 
     * <code>JoinManager</code>
     *
     * @see net.jini.lookup.JoinManager
     */
    public void asyncJoin(Object proxy, 
                          ServiceID sid,
                          Entry[] additionalAttrs,
                          DiscoveryManagement dMgr, 
                          LeaseRenewalManager lrm)
    throws IOException {
        ArrayList<Entry> attrs = new ArrayList<Entry>();
        if(additionalAttrs!=null) {
            attrs.addAll(Arrays.asList(additionalAttrs));
        }
        attrs.addAll(attrList);
        synchronized(this) {
            joinMgr = new JoinManager(proxy,
                                      attrs.toArray(new Entry[attrs.size()]),
                                      sid,
                                      dMgr,
                                      lrm);
        }
    }
}
