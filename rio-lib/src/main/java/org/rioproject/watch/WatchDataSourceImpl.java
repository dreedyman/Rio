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
package org.rioproject.watch;

import com.sun.jini.config.Config;
import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.config.ExporterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 The WatchDataSourceImpl provides support for the WatchDataSource
 interface. The WatchDataSourceImpl supports the following configuration
 entries; where
 each configuration entry name is associated with the component name <span
 style="font-family: monospace;">org.rioproject.watch </span> <br>
 </p>
 <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace;">watchDataSourceExporter
 </span> <br style="font-family: courier new,courier,monospace;">
 <table cellpadding="2" cellspacing="2" border="0"
 style="text-align: left; width: 100%;">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:
 <br>
 </td>
 <td style="vertical-align: top;">Exporter</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:
 <br>
 </td>
 <td style="vertical-align: top;">A new <code>BasicJeriExporter</code>
 with
 <ul>
 <li>a <code>TcpServerEndpoint</code> created on a random
 port,</li>
 <li>a <code>BasicILFactory</code>,</li>
 <li>distributed garbage collection turned off,</li>
 <li>keep alive on.</li>
 </ul>
 <code></code></td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:
 <br>
 </td>
 <td style="vertical-align: top;">The Exporter used to export
 the
 WatchDataSourceImpl server. A new exporter is obtained every time a
 WatchDataSourceImpl needs to export itself.</td>
 </tr>
 </tbody>
 </table>
 </li>
 </ul>
 <ul>
 <li><span
 style="font-weight: bold; font-family: courier new,courier,monospace;">collectionSize</span>
 <br style="font-family: courier new,courier,monospace;">
 <table cellpadding="2" cellspacing="2" border="0"
 style="text-align: left; width: 100%;">
 <tbody>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Type:
 <br>
 </td>
 <td style="vertical-align: top;">int</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Default:
 <br>
 </td>
 <td style="vertical-align: top;">1000</td>
 </tr>
 <tr>
 <td
 style="vertical-align: top; text-align: right; font-weight: bold;">Description:
 <br>
 </td>
 <td style="vertical-align: top;">The size of the
 WatchDataSource history
 collection.</td>
 </tr>
 </tbody>
 </table>
 </li>
 </ul>
 <p>
 */
public class WatchDataSourceImpl implements WatchDataSource, ServerProxyTrust {
    /** Defines the default history size */
    public final static int DEFAULT_COLLECTION_SIZE = 50;
    /** Defines the default history max size */
    public final static int MAX_COLLECTION_SIZE = 1000;
    /** The current history maximum size */
    private int max = DEFAULT_COLLECTION_SIZE;
    /** The history */
    private final ArrayList<Calculable> history = new ArrayList<Calculable>();
    /** Holds value of property id. */
    private String id = null;
    /** The class name used to view the WatchDataSource */
    private String viewClass;
    /** Holds value of property ThresholdVales. */
    private ThresholdValues thresholdValues = new ThresholdValues();
    /** Configuration for the WatchDataSource */
    private Configuration config;
    /** The Exporter for the WatchDataSource */
    private Exporter exporter;
    /** Object supporting remote semantics required for an WatchDataSource */
    private WatchDataSource proxy;
    /** Flag to indicate whether the WatchDataSource is initialized */
    private boolean initialized = false;
    /** Flag to indicate whether the WatchDataSource is exported */
    private boolean exported = false;
    /** Flag to indicate whether the WatchDataSource is closed */
    private boolean closed = false;
    /** Component for accessing configuration and getting a Logger */
    protected static final String COMPONENT = "org.rioproject.watch";
    /** A suitable Logger */
    protected static Logger logger = LoggerFactory.getLogger(WatchDataSourceImpl.class);
    private final List<WatchDataReplicator> replicators = new ArrayList<WatchDataReplicator>();

    /**
     * Create a WatchDataSourceImpl 
     */
    public WatchDataSourceImpl() {
    }

    /**
     * Constructs WatchDataSourceImpl object.
     *
     * @param id The ID of the WatchDataSource
     * @param config Configuration object for use
     */
    public WatchDataSourceImpl(String id, Configuration config) {
        if(id==null)
            throw new IllegalArgumentException("id is null");
        if(config==null)
            throw new IllegalArgumentException("config is null");
        this.id = id;
        this.config = config;
        doInit();
    }

    public void initialize() throws RemoteException {
        doInit();
        export();
    }

    /*
     * Do initialization
     */
    private void doInit() {
        if(initialized)
            return;
        if(config==null)
            throw new IllegalStateException("config is null");
        int collectionSize;
        try {
            collectionSize = Config.getIntEntry(config,
                                                COMPONENT,
                                                "collectionSize",
                                                DEFAULT_COLLECTION_SIZE,
                                                1,
                                                MAX_COLLECTION_SIZE);
        } catch(ConfigurationException e) {
            logger.trace("Getting WatchDataSource collection size", e);
            collectionSize = DEFAULT_COLLECTION_SIZE;
        }
        logger.trace("Watch [{}] history collection size={}", id, collectionSize);
        max = collectionSize;
        initialized = true;
    }

    /**
     * Export the WatchDataSourceImpl using a configured Exporter, defaulting to
     * BasicJeriExporter
     *
     * @return A proxy to use for the WatchDataSource
     *
     * @throws RemoteException if there are errors exporting the WatchDataSource
     */
    public WatchDataSource export() throws RemoteException {
        if(exported && proxy!=null)
            return(proxy);

        if(config != null) {
            try {
                exporter = ExporterConfig.getExporter(config, COMPONENT, "watchDataSourceExporter");
            } catch(Exception e) {
                logger.error("Getting watchDataSourceExporter", e);
            }
        }
        proxy = (WatchDataSource)exporter.export(this);
        exported = true;        
        return (proxy);
    }

    /**
     * Get the WatchDataSource proxy
     *
     * @return The WatchDataSource proxy
     */
    public WatchDataSource getProxy() {
        return (proxy);
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Unexport the WatchDataSourceImpl
     *
     * @param force If true, unexports the WatchDataSourceImpl even if there
     * are pending or in-progress calls; if false, only unexports the
     * WatchDataSourceImpl if there are no pending or in-progress calls
     */
    public void unexport(boolean force) {
        if(!exported)
            return;
        try {
            exporter.unexport(force);
            exported = false;
            proxy = null;
        } catch(IllegalStateException e) {
            /* Ignore */
        }
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getID
     */
    public String getID() {
        return (id);
    }

    /**
     * Setter for property id.
     *
     * @param id New value of property id.
     */
    public void setID(String id) {
        if(id==null)
            throw new IllegalArgumentException("id is null");
        this.id = id;
    }

    /**
     * @see WatchDataSource#setMaxSize
     */
    public void setMaxSize(int size) {
        synchronized(history) {
            if(size < this.max) {
                // If the size is less then the current maximum, reset the maximum
                max = size;
                if(history.size() > size) {
                    trimHistory((history.size() - size) - 1);
                    history.trimToSize();
                }
            } else
                history.ensureCapacity(size);
            this.max = size;
            logger.trace("Watch [{}] history collection size={}", id, size);
        }
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#clear
     */
    public void clear() {
        synchronized(history) {
            history.clear();
        }
    }

    /*
     * Trims the history. Always starting at the beginning, with an index of 0
     * 
     * @param range The number or records to trim
     */
    private void trimHistory(int range) {
        if(range == 1) {
            history.remove(0);
            logger.trace("Removed first entry to make room in {} history, size now {}", id, history.size());
        } else {
            List subList = history.subList(0, range);
            history.removeAll(subList);
            history.trimToSize();
            logger.trace("Removed {} entries to make room in {} history, size now {}", id, range, history.size());
        }
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getMaxSize
     */
    public int getMaxSize() {
        return (max);
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getCurrentSize
     */
    public int getCurrentSize() {
        int size;
        synchronized(history) {
            size = history.size();
        }
        return (size);
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#addCalculable
     */
    @SuppressWarnings("unchecked")
    public void addCalculable(Calculable calculable) {
        if(calculable==null)
            throw new IllegalArgumentException("calculable is null");
        if(!closed) {
            addToHistory(calculable);
            for(WatchDataReplicator replicator : getWatchDataReplicators()) {
                if(logger.isTraceEnabled())
                    logger.trace("Replicating [{}] to {} {}", calculable.toString(), replicator, replicator.getClass().getName());
                replicator.addCalculable(calculable);
            }
        }
    }

    private void addToHistory(Calculable calculable) {
        synchronized(history) {
            if(history.size() == max)
                trimHistory(1);
            if(history.size() > max)
                trimHistory((history.size() - max) - 1);
            history.add(calculable);
            if(logger.isTraceEnabled())
                logger.trace("[{}] Adding [{}] to history", id, calculable.toString());
        }
    }
    
    /**
     * @see org.rioproject.watch.WatchDataSource#getCalculable
     */
    public Calculable[] getCalculable() {
        Calculable[] calcs;
        synchronized(history) {
            calcs = history.toArray(new Calculable[history.size()]);
        }
        return calcs;
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getCalculable
     */
    public Calculable[] getCalculable(String id) {
        if(id==null)
            throw new IllegalArgumentException("id is null");

        Calculable[] calcs;
        synchronized(history) {
            calcs = history.toArray(new Calculable[history.size()]);
        }
        List<Calculable> list = new ArrayList<Calculable>();
        for(Calculable c : calcs) {
            if(c.getId().equals(id))
                list.add(c);
        }
        return list.toArray(new Calculable[list.size()]);
    }    

    /**
     * @see org.rioproject.watch.WatchDataSource#getCalculable(long, long)
     */
    public Calculable[] getCalculable(long from, long to) {
        List<Calculable> list = new ArrayList<Calculable>();
        if(to>from) {
            synchronized(history) {
                for(Calculable calc : history) {
                    if(calc.getWhen()>=from && calc.getWhen()<=to)
                        list.add(calc);
                }
            }
        }
        return (list.toArray(new Calculable[list.size()]));
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getLastCalculable
     */
    public Calculable getLastCalculable() {
        Calculable c = null;
        try {
            synchronized(history) {
                c =  history.get(history.size() - 1);
            }
        } catch(IndexOutOfBoundsException ex) {
            /* ignore */
        }
        return c;
    }

    /**
     * Make sure the archival file is closed before garbage collection
     */
    protected void finalize() throws Throwable {        
        for(WatchDataReplicator replicator : getWatchDataReplicators())
            replicator.close();
        super.finalize();
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getThresholdValues
     */
    public ThresholdValues getThresholdValues() {
        return(thresholdValues);
    }

    /**
     *  @see org.rioproject.watch.WatchDataSource#setThresholdValues
     */
    public void setThresholdValues(ThresholdValues tValues) {
        if(tValues!=null)
            thresholdValues = tValues;
    }     

    /**
     * @see org.rioproject.watch.WatchDataSource#close
     */
    public void close() {
        for(WatchDataReplicator replicator : getWatchDataReplicators())
            replicator.close();
        synchronized(replicators) {
            replicators.clear();
        }
        closed = true;
        unexport(true);
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#setView
     */
    public void setView(String viewClass) {
        this.viewClass = viewClass;
    }

    /**
     * @see org.rioproject.watch.WatchDataSource#getView
     */
    public String getView() {
        return (viewClass);
    }    

    /**
     * Returns a <code>TrustVerifier</code> which can be used to verify that a
     * given proxy to this WatchDataSource can be trusted
     */
    public TrustVerifier getProxyVerifier() {
        return (new BasicProxyTrustVerifier(proxy));
    }

    public boolean addWatchDataReplicator(WatchDataReplicator replicator) {
        boolean added = false;
        if(replicator==null)
            return added;
        synchronized(replicators) {
            if(!replicators.contains(replicator))
                added = replicators.add(replicator);
        }
        return added;
    }

    public boolean removeWatchDataReplicator(WatchDataReplicator replicator) {
        boolean removed = false;
        if(replicator==null)
            return removed;
        synchronized(replicators) {
            removed = replicators.remove(replicator);
        }
        return removed;
    }

    public WatchDataReplicator[] getWatchDataReplicators() {
        WatchDataReplicator[] wdrs;
        synchronized(replicators) {
            wdrs = replicators.toArray(new WatchDataReplicator[replicators.size()]);
        }
        return wdrs;
    }
}
