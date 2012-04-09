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
package org.rioproject.cybernode;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import org.rioproject.deploy.ServiceStatement;
import org.rioproject.deploy.ServiceStatementManager;
import org.rioproject.logging.WrappedLogger;
import org.rioproject.opstring.ServiceElement;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * The PersistentServiceStatementManager provides an implementation of the
 * ServiceStatementManager, reading and writing ServiceStatement instances to
 * the file system
 *
 * @author Dennis Reedy
 */
public class PersistentServiceStatementManager implements ServiceStatementManager {
    /** The directory to write and read ServiceRecord instances */
    File recordRoot;
    /** Filename extension */
    final static String STATEMENT_EXT = ".stmt";
    static final long SECOND = 1000;
    static final long MINUTE = SECOND * 60;
    static final long HOUR = MINUTE * 60;
    static final long DAY = HOUR * 24;
    static final long ETERNITY = 0;
    /**
     * If a ServiceStatement is older then this value, remove it from the
     * filesystem
     */
    long clean = DAY * 30;
    /** Timer to shedule FileSweeper tasks */
    Timer taskTimer;
    /** A semaphore for reading/writing */
    final Object rwSemaphore = new Object();
    /** Logger */
    static WrappedLogger logger = WrappedLogger.getLogger("org.rioproject.cybernode");

    /**
     * Create a PersistentServiceStatementManager
     *
     * @param config The configuration to use
     */
    public PersistentServiceStatementManager(Configuration config) {
        try {
            recordRoot = Environment.setupRecordRoot(config);
                logger.fine("Storing service statements in [%s]", recordRoot.getCanonicalPath());
            long age = clean;
            try {
                age = Config.getLongEntry(config,
                                          CybernodeImpl.getConfigComponent(),
                                          "recordAge",
                                          30,
                                          0,
                                          Long.MAX_VALUE);
                age = age * DAY;
            } catch(Throwable t) {
                logger.log(Level.WARNING, "PersistentServiceStatementManager : exception getting recordAge", t);
            }
            if(age != ETERNITY) {
                logger.fine( "ServiceStatement maximum age [%d] days", (age / DAY));
                taskTimer = new Timer(true);
                long now = System.currentTimeMillis();
                taskTimer.scheduleAtFixedRate(new FileSweeper(age), new Date(now + SECOND), HOUR);
            } else {
                logger.fine("ServiceStatements live forever");
            }
        } catch(IOException e) {
            logger.log(Level.WARNING, "Accessing storage for ServiceRecords", e);
        }
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#terminate
     */
    public void terminate() {
        if(taskTimer != null)
            taskTimer.cancel();
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#get
     */
    public ServiceStatement[] get() {
        List<ServiceStatement> list = new ArrayList<ServiceStatement>();
        File[] statements = recordRoot.listFiles();
        for (File statement : statements) {
            try {
                ServiceStatement stmnt = read(statement);
                if (stmnt != null)
                    list.add(stmnt);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Getting ServiceStatement instances", e);
            }
        }
        return (list.toArray(new ServiceStatement[list.size()]));
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#get
     */
    public ServiceStatement get(ServiceElement sElem) {
        ServiceStatement statement = null;
        try {            
            statement = read(makeName(sElem) + STATEMENT_EXT);
        } catch(IOException e) {
            logger.log(Level.SEVERE, "Getting ServiceStatement", e);
        } catch(ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Getting ServiceStatement", e);
        }
        return (statement);
    }

    /**
     * @see org.rioproject.deploy.ServiceStatementManager#record
     */
    public void record(ServiceStatement statement) {
        try {
            write(statement);
        } catch(IOException e) {
            logger.log(Level.SEVERE, "Writing ServiceStatement", e);
        }
    }

    /*
     * Write a ServiceStatement to the file system
     * 
     * @param statement The ServiceStatement to write
     */
    void write(ServiceStatement statement) throws IOException {
        synchronized(rwSemaphore) {
            File record = new File(recordRoot, 
                                   makeName(statement.getServiceElement())+
                                            STATEMENT_EXT);
            ObjectOutputStream oos = 
                new ObjectOutputStream(new FileOutputStream(record));
            oos.writeObject(statement);
            oos.flush();
        }
    }

    /*
     * Read a ServiceStatement from the file system
     * 
     * @param fileName The ServiceStatement file name
     * @return ServiceStatement
     */
    ServiceStatement read(String fileName) throws ClassNotFoundException,
        IOException {
        File file = new File(recordRoot, fileName);
        if(file.exists())
            return (read(file));
        return (null);
    }

    /*
     * Read a ServiceStatement from the file system
     * 
     * @param input The input File
     * @return ServiceStatement
     */
    ServiceStatement read(File input) throws ClassNotFoundException,
        IOException {
        synchronized(rwSemaphore) {
            ObjectInputStream ois = 
                new ObjectInputStream(new FileInputStream(input));
            return ((ServiceStatement)ois.readObject());
        }
    }
    
    /*
     * Make a name for the ServiceElement
     */
    String makeName(ServiceElement sElem) {
        return(sElem.getOperationalStringName()+"."+sElem.getName());
    }

    /**
     * The FileSweeper class is scheduled every hour to sweep the directory an
     * determine if any ServiceStatement files are older then the allotted age
     * (default is a month). If the files are older, and all the ServiceRecord
     * instances within the ServiceStatement are INACTIVE, then remove the
     * ServiceStatement
     */
    class FileSweeper extends TimerTask {
        long oldAge;

        FileSweeper(long oldAge) {
            this.oldAge = oldAge;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            long now = System.currentTimeMillis();
            File[] files = recordRoot.listFiles();
            if(files!=null) {
                for (File file : files) {
                    if (file.getName().endsWith(STATEMENT_EXT)) {
                        long lastModified = file.lastModified();
                        long age = now - lastModified;
                        if (age > oldAge) {
                            file.delete();
                            if (logger.isLoggable(Level.FINE))
                                logger.fine("ServiceStatement [%s] has aged past [%d] days, file has been removed",
                                            file.getName(), (oldAge/ DAY));
                        }
                    }
                }
            }
        }
    }
}
