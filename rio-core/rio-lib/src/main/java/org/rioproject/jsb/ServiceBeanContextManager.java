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
package org.rioproject.jsb;

import com.sun.jini.reliableLog.LogHandler;
import com.sun.jini.reliableLog.ReliableLog;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resources.persistence.PersistentStore;
import org.rioproject.resources.persistence.SnapshotHandler;
import org.rioproject.resources.persistence.StoreException;
import org.rioproject.resources.persistence.SubStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Manages a persistent {@link org.rioproject.core.jsb.ServiceBeanContext}
 *
 * @author Dennis Reedy
 */
public class ServiceBeanContextManager {
    /**
     * The ServiceBeanContext for the service. This value may be
     * <code>null</code>
     */
    private ServiceBeanContext context;
    /**
     * The LogHandler that will manage the persistent attributes for the
     * ServiceBeanContext and service attribute list
     */
    private ContextAttributeLogHandler attributeLogHandler = 
        new ContextAttributeLogHandler();
    /** the Logger for this component */
    private static final Logger logger = LoggerFactory.getLogger("org.rioproject.jsb");

    /**
     * Create a ServiceBeanContextManager
     * 
     * @param context ServiceBeanContext
     */
    public ServiceBeanContextManager(ServiceBeanContext context) {
        this.context = context;
    }

    /**
     * Get the <code>ContextAttributeLogHandler</code> object for this
     * <code>ServiceBeanContextManager</code>
     *
     * @return The ContextAttributeHandler
     */
    public ContextAttributeLogHandler getContextAttributeLogHandler() {
        return (attributeLogHandler);
    }

    /**
     * Delegates the snapshot to the <code>ContextAttributeLogHandler</code>
     * 
     * @param dm the DiscoveryManagement for the service
     * @exception Exception can raise any exception
     */
    public void takeSnapshot(DiscoveryManagement dm) throws Exception {
        attributeLogHandler.discoMgmt = dm;
        attributeLogHandler.takeSnapshot();
    }

    /**
     * Set the <code>PersistentStore</code> to use for persisting the state of
     * this service. If there is a log directory that already exists at this
     * location the persisted <code>ServiceBeanContext</code> state will be
     * retrieved and returned. <br>
     *
     * @param store The PersistentStore
     *
     * @return ServiceBeanContext the persisted ServiceBeanContext to use for
     * service initialization. This value will be <code>null</code> if there
     * is no context found
     *
     * @throws StoreException If the context cannot be restored
     */
    public ServiceBeanContext restoreContext(PersistentStore store)
        throws StoreException {
        try {
            store.acquireMutatorLock();
            store.addSubStore(attributeLogHandler);
        } finally {
            store.releaseMutatorLock();
        }
        ServiceBeanContext ctxt = attributeLogHandler.getRestoredContext();
        if(ctxt != null)
            context = ctxt;
        return (ctxt);
    }
    /**
     * Class which implements methods needed to meet the LogHandler interface
     */
    public class ContextAttributeLogHandler extends LogHandler
        implements
            SubStore,
            SnapshotHandler {
        /** Our persistent store */
        ReliableLog log;
        /** The recovered context. May be null */
        ServiceBeanContext restoredContext = null;
        /** The DiscoveryManagement to use */
        DiscoveryManagement discoMgmt = null;
        static final String logDirName = "ContextAttributes";

        public ContextAttributeLogHandler() {
            super();
        }

        void setDiscoveryManagement(DiscoveryManagement discoMgmt) {
            this.discoMgmt = discoMgmt;
        }

        // Inherit JavaDoc from super-type
        public void setDirectory(File dir) throws IOException {
            try {
                log = new ReliableLog(dir.getCanonicalPath(), this);
                log.recover();
            } catch(IOException e) {
                logger.error("Problem recovering/creating log", e);
                throw new IOException("ServiceBeanContextManager: log ["
                                      + dir.getCanonicalPath()
                                      + "] is corrupted:"
                                      + e.getLocalizedMessage());
            }
        }

        // Inherit JavaDoc from super-type
        public String subDirectory() {
            return (logDirName);
        }

        // Inherit doc comment from super interface
        public void snapshot(OutputStream out) throws Exception {
            if(discoMgmt == null)
                throw new IllegalArgumentException("DiscoveryManagement cannot be null");
            ObjectOutputStream oostream = new ObjectOutputStream(out);
            oostream.writeUTF(logDirName);
            oostream.writeObject(context);
            if(discoMgmt instanceof DiscoveryGroupManagement) {
                DiscoveryGroupManagement dgm = (DiscoveryGroupManagement)discoMgmt;
                String[] groups = dgm.getGroups();
                if(groups == null)
                    groups = new String[]{"all"};
                oostream.writeObject(groups);
            } else {
                oostream.writeObject(new String[0]);
            }
            if(discoMgmt instanceof DiscoveryLocatorManagement) {
                DiscoveryLocatorManagement dlm = (DiscoveryLocatorManagement)discoMgmt;
                oostream.writeObject(dlm.getLocators());
            } else {
                oostream.writeObject(new LookupLocator[0]);
            }
            oostream.flush();
        }

        // Inherit doc comment from super interface
        public void recover(InputStream in) throws Exception {
            ObjectInputStream oistream = new ObjectInputStream(in);
            if(!logDirName.equals(oistream.readUTF()))
                throw new IOException("log from wrong implementation");
            JSBContext jsbContext = (JSBContext)oistream.readObject();
            ServiceElement sElem = jsbContext.getServiceElement();
            ServiceBeanConfig sAttr = sElem.getServiceBeanConfig();
            try {
                String[] groups = (String[])oistream.readObject();
                LookupLocator[] locators = (LookupLocator[])oistream.readObject();
                sAttr.setGroups(groups);
                sAttr.setLocators(locators);
            } catch(Throwable t) {
                logger.error("Couldnt get groups or locators", t);
            }
            sElem.setServiceBeanConfig(sAttr);
            jsbContext.setServiceElement(sElem);
            restoredContext = jsbContext;
        }

        /**
         * @return the ServiceBeanContext that has been restored. If there is no
         * state to be restored this method will return <code>null</code>
         */
        public ServiceBeanContext getRestoredContext() {
            return (restoredContext);
        }

        /**
         * This method always throws <code>UnsupportedOperationException</code>
         * since <code>ContextAttributeLogHandler</code> should never update a
         * log.
         */
        public void applyUpdate(Object update) throws Exception {
            throw new UnsupportedOperationException("ContextAttributeLogHandler : "+
                                                    "Recovering log update this "+
                                                    "should not happen");
        }

        /**
         * Used by all the methods that change persistent state to commit the
         * change to disk
         */
        public void takeSnapshot() throws IOException {
            log.snapshot();
        }

        public void updatePerformed(int updateCount) {
        }

        public void prepareDestroy() {
            try {
                if(log != null)
                    log.close();
            } catch(IOException e) {
                logger.warn("Problem closing log during destroy, Ignoring and going on", e);
            }
        }
    }
}
