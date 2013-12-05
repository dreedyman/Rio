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
package org.rioproject.impl.exec;

import org.rioproject.impl.util.StreamRedirector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a wrapper around a process, including the pid of the started process
 * as well as other helpful information
 *
 * @author Dennis Reedy
 */
public abstract class ProcessManager {
    private Process process;
    private int pid;
    private final List<Listener> listeners = new ArrayList<Listener>();
    protected StreamRedirector outputStream;
    protected StreamRedirector errorStream;

    /**
     * Create a ProcessManager
     *
     * @param process The {@link Process} the ProcessManager will manage
     * @param pid The process ID of the started Process
     */
    public ProcessManager(final Process process, final int pid) {
        if (process == null)
            throw new IllegalArgumentException("process is null");
        if (pid <= 0)
            throw new IllegalArgumentException("pid cannot be <= 0");
        this.process = process;
        this.pid = pid;
    }

    /**
     * Get the Process the ProcessManager is managing
     *
     * @return The Process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Get the pid
     *
     * @return The pid
     */
    public int getPid() {
        return pid;
    }

    /**
     * Register a {@link Listener}
     *
     * @param l The listener to register
     */
    public void registerListener(Listener l) {
        synchronized(listeners) {
            listeners.add(l);
        }
    }

    /**
     * Unregister a {@link Listener}
     *
     * @param l The listener to unregister
     */
    public void unregisterListener(Listener l) {
        synchronized(listeners) {
            listeners.remove(l);
        }
    }

    protected void handleRedirects(final String stdOutFileName,
                                   final String stdErrFileName) {
        if (stdOutFileName == null) {
            /*System.out*/
            outputStream = new StreamRedirector(getProcess().getInputStream(),
                                                System.out);
            outputStream.start();
        }
        if (stdErrFileName == null) {
            /*System.err*/
            errorStream = new StreamRedirector(getProcess().getErrorStream(),
                                               System.err);
            errorStream.start();
        }
    }

    /**
     * Notify all {@link Listener}s of the process termination
     */
    protected void notifyOnTermination() {
        Listener[] localListeners;
        synchronized(listeners) {
            localListeners =  listeners.toArray(new Listener[listeners.size()]);
        }
        for(Listener l : localListeners)
            l.processTerminated(pid);
    }

    /**
     * Manage the Process
     *
     * @throws java.io.IOException if the process management utility cannot be created
     */
    public abstract void manage() throws IOException;

    /**
     * Destroy the managed process
     *
     * @param includeChildren If true, destroy all child processes asl well.
     * This method will look for all child processes that have a prent process
     * ID of the managed process and forcibly terminate them.
     */
    public abstract void destroy(boolean includeChildren);

    /**
     * Notification for Process termination
     */
    public static interface Listener {
        void processTerminated( int pid);
    }
}
