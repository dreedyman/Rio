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
package org.rioproject.resources.persistence;

/**
 * Exception thrown by <code>PersistentStore</code> when it discovers
 * the store has become corrupted.
 */
public class CorruptedStoreException extends StoreException {
    private static final long serialVersionUID = 2L;

    /**
     * Constructs an <code>CorruptedStoreException</code> with a detail
     * message.
     * @param s the detailed message
     */
    public CorruptedStoreException(String s) {
	super(s);
    }

    /**
     * Constructs an <code>CorruptedStoreException</code> with a detail
     * message and a nested exception.    
     * @param s the detailed message
     * @param t root cause for exception, may be <code>null</code>
     */
    public CorruptedStoreException(String s, Throwable t) {
	super(s, t);
    }
}
