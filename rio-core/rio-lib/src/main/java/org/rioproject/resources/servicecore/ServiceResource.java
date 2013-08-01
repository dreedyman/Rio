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

import com.sun.jini.landlord.LeasedResource;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;

/**
 * Leased service resources to be used with <code>LeaseDurationPolicy</code> and/or
 * <code>LeaseManager</code>
 *
 * @author Dennis Reedy
 */
public class ServiceResource implements LeasedResource {
    private Uuid cookie;
    private long expiration;
    private final Object resource;

    /**
     * Create a ServiceResource
     * 
     * @param resource The resource being leased
     */
    public ServiceResource(final Object resource) {
        this.resource = resource;
        synchronized(ServiceResource.class) {
            cookie = UuidFactory.generate();
        }
    }

    /**
     * Returns the expiration time of the leased resource.
     * 
     * @return The expiration time in milliseconds since the beginning of the epoch
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * Changes the expiration time of the leased resource.
     * 
     * @param expiration The new expiration time in milliseconds since the beginning of 
     * the epoch
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns a unique identifier that can be used by the grantor of
     * the resource to identify it
     */
    public Uuid getCookie() {
        return cookie;
    }

    /**
     * Returns the resource that is being leased
     *
     * @return The actual resource
     */
    public Object getResource() {
        return resource;
    }

    /**
     * Overrides <code>equals()</code> to be based on the value of the cookie attribute
     */
    public boolean equals(Object o) {
        if(o instanceof ServiceResource) {
            ServiceResource sr = (ServiceResource)o;
            return(cookie.equals(sr.cookie));
        }
        return false;
    }

    /**
     * Overrides <code>hashcode()</code> to be based on the hashcode of the 
     * cookie attribute
     */
    public int hashCode() {
        return cookie.hashCode();
    }
}
