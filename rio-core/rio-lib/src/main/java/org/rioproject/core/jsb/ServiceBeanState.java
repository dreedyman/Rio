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
package org.rioproject.core.jsb;

/**
 * The ServiceBeanState provides information on the lifecycle of a ServiceBean
 *
 * @author Dennis Reedy
 */
public class ServiceBeanState {
    /**
     * ServiceBean is inactive
     */
    public static final int INACTIVE = 0;
    /**
     * ServiceBean is starting
     */
    public static final int STARTING = 1;
    /**
     * ServiceBean has been initialized
     */
    public static final int INITIALIZED = 2;
    /**
     * ServiceBean has been stopped
     */
    public static final int STOPPED = 3;
    /**
     * ServiceBean has been started
     */
    public static final int STARTED = 4;
    /**
     * ServiceBean has been unadvertised
     */
    public static final int UNADVERTISED = 5;
    /**
     * ServiceBean has been advertised
     */
    public static final int ADVERTISED = 6;
    /**
     * ServiceBean has been advertised
     */
    public static final int ABORTED = 7;
    /**
     * The current state
     */
    private int state;

    /**
     * Create a ServiceBeanState object
     */
    public ServiceBeanState() {
        state = INACTIVE;
    }

    /**
     * Get the current state of the ServiceBean
     * 
     * @return The state of the ServiceBean
     */
    public int getState() {
        int theState;
        synchronized(this) {
            theState = state;
        }
        return (theState);
    }

    /**
     * Set the state of the ServiceBean
     * 
     * @param newState The new state
     * 
     * @throws IllegalStateException if the <code>newState</code> is not a valid 
     * state
     */
    public void setState(int newState) {
        if(newState == getState())
            return;
        if(newState < 0 || newState > ABORTED)
            throw new IllegalStateException("newState out of bounds ["
                                            + newState
                                            + "]");
        synchronized(this) {
            verifyTransition(newState);
            state = newState;
        }
    }

    /**
     * Verify the state transition, checking if the proposed new state is
     * allowed from the current state. If the proposed state is not allowed an
     * IllegalStateException will be thrown
     * 
     * @param newState The new state to check
     * @throws IllegalStateException If the proposed newState is not allowed
     */
    public void verifyTransition(int newState) {
        boolean validTransition = false;
        if(isAborted() && getState()!=ABORTED)
            throw new IllegalStateException("Current state is ABORTED, "
                                            + "transition not allowed from "+
                                            getStateDesc(getState()));
        switch (newState) {
            case STARTING :
                if(getState() < STARTING)
                    validTransition = true;
                break;
            case INITIALIZED :
                if(getState() < INITIALIZED)
                    validTransition = true;
                break;
            case STARTED :
                if(getState() < STARTED)
                    validTransition = true;
                break;
            case STOPPED :
                if(getState() == STARTING ||
                   getState() == STARTED || getState() == UNADVERTISED)
                    validTransition = true;
                break;
            case UNADVERTISED :
                if(getState() == ADVERTISED || getState() == STOPPED)
                    validTransition = true;
                break;
            case ADVERTISED :
                if(getState() == STARTED || getState() == UNADVERTISED)
                    validTransition = true;
                break;
            case INACTIVE :
                if(getState() == STOPPED)
                    validTransition = true;
                break;
            case ABORTED:
                validTransition = true;
                break;
        }
        if(!validTransition)
            throw new IllegalStateException("New state ["
                                            +getStateDesc(newState)
                                            + "] not allowed "
                                            + "from ["
                                            +getStateDesc(getState())
                                            + "]");
    }

    /**
     * Check if we're aborted
     * 
     * @return If the state is ABORTED, return <code>true</code>
     */
    public boolean isAborted() {
        boolean aborted = false;
        synchronized(this) {
            if(getState() == ABORTED)
                aborted = true;
        }
        return (aborted);
    }

    private String getStateDesc(int state) {
        String desc = null;
        switch(state) {
            case STARTING :
                desc = "STARTING";
                break;
            case INITIALIZED :
                desc = "INITIALIZED";
                break;
            case STARTED :
                desc = "STARTED";
                break;
            case STOPPED :
                desc = "STOPPED";
                break;
            case UNADVERTISED :
                desc = "UNADVERTISED";
                break;
            case ADVERTISED :
                desc = "ADVERTISED";
                break;
            case INACTIVE :
                desc = "INACTIVE";
                break;
            case ABORTED :
                desc = "ABORTED";
                break;
        }
        return(desc);
    }
}
