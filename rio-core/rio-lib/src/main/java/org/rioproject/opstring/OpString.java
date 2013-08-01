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
package org.rioproject.opstring;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of an {@link OperationalString}
 *
 * @author Dennis Reedy
 */
public class OpString implements OperationalString, Serializable {
    static final long serialVersionUID = 1L;
    /** Name of the OperationalString */
    private String name;
    /** Collection of services within the OperationalString */
    private final List<ServiceElement> services = new ArrayList<ServiceElement>();
    /** Collection of nested OperationalString */
    private final List<OperationalString> nestedOpStrings =
        new ArrayList<OperationalString>();
    /** The deployed state of the OperationalString */
    private int deployedStatus = OperationalString.UNDEPLOYED;
    /** The URL OperationalString was loaded from */
    private URL loadedFrom;

    /** 
     * Create an OpString 
     * 
     * @param name The name of the OperationalString, must not be null
     * @param loadedFrom The URL where the OperationalString was loaded
     * from. May be null
     */
    public OpString(String name, URL loadedFrom) {
        if (name == null)
            throw new IllegalArgumentException("OpString name is null");
        this.name = name;
        this.loadedFrom = loadedFrom;
    }    
    
    /**
     * @see OperationalString#setDeployed
     */
    public void setDeployed(int deployed) {
        if(deployed < OperationalString.UNDEPLOYED && 
            deployed > OperationalString.DEPLOYED)
            throw new IllegalArgumentException("bad deployment status "+
                                               "["+deployed+"]");
        synchronized(this) {
            deployedStatus = deployed;
        }                
    }
    
    /**
     * @see OperationalString#getStatus()
     */
    public int getStatus() {
        int myStatus;
        synchronized(this) {
            if(deployedStatus<OperationalString.DEPLOYED) {
                myStatus = deployedStatus;
            } else {
                myStatus = OperationalString.INTACT;
                ServiceElement[] sElems = getServices();
                for (ServiceElement sElem : sElems) {
                    int actual = sElem.getActual();
                    int planned = sElem.getPlanned();
                    if (actual == 0) {
                        myStatus = OperationalString.BROKEN;
                        break;
                    }
                    if (actual < planned) {
                        myStatus = OperationalString.COMPROMISED;
                        break;
                    }
                }
                OperationalString[] nested = getNestedOperationalStrings();
                for (OperationalString aNested : nested) {
                    int nestedStatus = aNested.getStatus();
                    if (nestedStatus < myStatus)
                        myStatus = nestedStatus;
                }
            }
        }
        return(myStatus);
    }
     
    /**
     * Set the name of the OperationalString
     * 
     * @param name The name of the OperationalString, must not be null. This
     * will additionally set all ServiceElement instance OperationalString
     * names as well
     */
    public void setName(String name) {
        if(name==null)
            throw new IllegalArgumentException("name is null");
        this.name = name;
        ServiceElement[] services = getServices();
        for (ServiceElement service : services)
            service.setOperationalStringName(name);
    }
    
    /**
     * @see OperationalString#getName
     */
    public String getName() {
        return(name);
    }

    /**
     * @see OperationalString#getServices
     */
    public ServiceElement[] getServices() {
        ServiceElement[] sams;
        synchronized(services) {
            sams = new ServiceElement[services.size()];
            for(int i=0; i<sams.length; i++) {
                sams[i] = services.get(i);
            }
        }
        return(sams);
    }

    /**
     * @see OperationalString#addService
     */
    public void addService(ServiceElement sElem) {
        if(sElem==null)
            throw new IllegalArgumentException("ServiceElement is null");
        synchronized(services) {
            if(!services.contains(sElem))
                services.add(sElem);
        }
    }    

    /**
     * @see OperationalString#removeService
     */    
    public void removeService(ServiceElement sElem) {
        if(sElem==null)
            throw new IllegalArgumentException("ServiceElement is null");
        synchronized(services) {
            services.remove(sElem);
        }
    }    

    /**
     * Add included OperationalString objects to the OperationalString
     * 
     * @param opStrings Array of OperationalString objects to add
     */
    public void addOperationalString(OperationalString[] opStrings) {
        if(opStrings==null)
            throw new IllegalArgumentException("OperationalString is null");
        for (OperationalString opString : opStrings) {
            addOperationalString(opString);
        }
    }

    /**
     * Add included OperationalString to the OperationalString
     * 
     * @param opString OperationalString to add
     */
    public void addOperationalString(OperationalString opString) {
        if(opString==null)
            throw new IllegalArgumentException("OperationalString is null");
        synchronized(nestedOpStrings) {
            nestedOpStrings.add(opString);
        }
    }

    /**
     * @see OperationalString#getNestedOperationalStrings()
     */
    public OperationalString[] getNestedOperationalStrings() {
        OperationalString[] ops;
        synchronized(nestedOpStrings) {
            ops = new OperationalString[nestedOpStrings.size()];
            for(int i=0; i<ops.length; i++) {
                ops[i] = nestedOpStrings.get(i);
            }
        }
        return(ops);
    }

    /**
     * Override hashCode to return the hashCode of the name attribute
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+name.hashCode();
        hc = 37*hc+services.hashCode();
        return(hc); 
    }

    /**
     * An OperationalString is equal to another OperationalString if their
     * names are the same and they have they same ServiceElements
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof OpString)) {            
            return(false);
        }        
        OpString that = (OpString)obj;
        if(this.name.equals(that.name)) {
            ServiceElement[] thisServices = this.getServices();
            ServiceElement[] thatServices = that.getServices();
            if(thisServices.length == thatServices.length) {
                /* Go through the services in both opstrings. They may contain
                 * the same services, but be in different order */
                for (ServiceElement thisService : thisServices) {
                    boolean matched = false;
                    for (ServiceElement thatService : thatServices) {
                        if (thisService.equals(thatService)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched)
                        return (false);
                }
                return(true);
            } 
        }
        return(false);
    }

    /**
     * Check if a service with the provided name is included in the
     * OperationalString
     *
     * @param serviceName The name of the service to check
     *
     * @return Return true if the serviceName is part of this 
     * OperationalString, false otherwise
     */
    public boolean containsNamedService(String serviceName) {
        boolean found=false;
        synchronized(services) {
            for (ServiceElement sElem : services) {
                if (sElem.getName().equals(serviceName)) {
                    found = true;
                    break;
                }
            }
        }
        return(found);
    }

    /**
     * Get the service with the provided name 
     *
     * @param serviceName The name of the service to check
     *
     * @return Return the ServiceElement for the serviceName or null if not
     * found
     */
    public ServiceElement getNamedService(String serviceName) {
        ServiceElement foundService=null;
        synchronized(services) {
            for (ServiceElement sElem : services) {
                if (sElem.getName().equals(serviceName)) {
                    foundService = sElem;
                    break;
                }
            }
        }
        return(foundService);
    }

    /**
     * Check if an OperationalString with the provided name is included in the 
     * OperationalString
     *
     * @param opStringName The name of the OperationalString to check
     *
     * @return Return true if the OperationalString is part of this 
     * OperationalString, false otherwise
     */
    public boolean containsOperationalString(String opStringName) {
        boolean found=false;
        synchronized(nestedOpStrings) {
            for (OperationalString nestedOpString : nestedOpStrings) {
                if (nestedOpString.getName().equals(opStringName)) {
                    found = true;
                    break;
                }
            }
        }
        return(found);
    }

    /**
     * Get the nested OperationalString with the provided name
     *
     * @param opStringName The name of the OperationalString to check
     *
     * @return  Return the OperationalString if found, otherwise return null
     */
    public OperationalString getNestedOperationalString(String opStringName) {
        OperationalString nested=null;
        synchronized(nestedOpStrings) {
            for (OperationalString nestedOpString : nestedOpStrings) {
                if (nestedOpString.getName().equals(opStringName)) {
                    nested = nestedOpString;
                    break;
                }
            }
        }
        return(nested);
    }
    
    /**
     * @see OperationalString#loadedFrom
     */
    public URL loadedFrom() {
        return(loadedFrom);
    }

    /**
     * Sets the Schedule for this OperationalString
     *
     * @param schedule the Schedule for this OperationalString
     */
    /*public void setSchedule(Schedule schedule) {
        if(schedule == null)
            throw new IllegalArgumentException("schedule is null");
        this.schedule = schedule;
    }*/
    
    /**
     * @deprecated
     */
    @Deprecated
    public Schedule getSchedule() {
        return(null);
    }

    /**
     * Set the loaded from property
     *
     * @param loadedFrom The URL the OperationalString was loaded from
     */
    public void setLoadedFrom(URL loadedFrom) {
        this.loadedFrom = loadedFrom;
    }
}

