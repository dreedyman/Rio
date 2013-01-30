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
package org.rioproject.admin;

/**
 * Thrown when a Service Bean cannot be controlled
 *
 * @author Dennis Reedy
 */
public class ServiceBeanControlException extends Exception {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2L;

    /**
     * Constructs a <code>ServiceBeanControlException</code> with the specified detail
     * message
     * 
     * @param s the detail message.
     */
    public ServiceBeanControlException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ServiceBeanControlException</code> with the specified detail
     * message and optional exception that was raised while controlling the service
     * 
     * @param s The detail message
     * @param cause The exception that was raised while controlling the JSB
     */
    public ServiceBeanControlException(String s, Throwable cause) {
        super(s, cause);
    }
}
