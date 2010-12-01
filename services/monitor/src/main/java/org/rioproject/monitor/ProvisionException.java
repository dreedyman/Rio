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
package org.rioproject.monitor;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown when an unrecoverable exception happens trying to provision a service
 *
 * @author Dennis Reedy
 */
public class ProvisionException extends Exception {
    /**
     * serialVersionUID
     */
    static final long serialVersionUID = 1L;
    /**
     * This field indicates that the raised exception is in reference to a
     * service that is not provisionable
     */
    private boolean unInstantiable = false;

    /**
     * Constructs a <code>ProvisionException</code> with no detail
     * message.
     */
    public ProvisionException() {
        super();
    }

    /**
     * Constructs a <code>ProvisionException</code> with the specified
     * detail message
     * 
     * @param s the detail message.
     */
    public ProvisionException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>ProvisionException</code> with the specified
     * detail message and optional exception that was raised
     *
     * @param s the detail message
     * @param cause the exception that was raised
     */
    public ProvisionException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>ProvisionException</code> with the specified
     * detail message, optional exception that was raised
     *
     * @param s The detail message
     * @param cause The exception that was raised while provisioning
     * @param unInstantiable - Whether the raised exception is in reference to a
     * service that is not provisionable
     */
    public ProvisionException(String s, Throwable cause, boolean unInstantiable) {
        super(s, cause);
        this.unInstantiable = unInstantiable;
    }

    /**
     * Returns whether the raised exception is in reference to a service
     * that is not provisionable, due to such reasons as missing classes or
     * resources
     *
     * @return True if the service is unprovisionable
     */
    public boolean isUninstantiable() {
        return (unInstantiable);
    }

    /**
     * Prints the stack backtrace.
     *
     * @see java.lang.System#err
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack backtrace to the specified print stream.
     */
    public void printStackTrace(PrintStream ps) {
        Throwable cause = getCause();
        if(cause != null) {
            ps.print(ProvisionException.class.getName()+", cause: ");
            cause.printStackTrace(ps);
        } else {
            super.printStackTrace(ps);
        }
    }

    /**
     * Prints the stack backtrace to the specified print writer.
     */
    public void printStackTrace(PrintWriter pw) {
        Throwable cause = getCause();
        if(cause != null) {
            pw.print(ProvisionException.class.getName()+", cause: ");
            cause.printStackTrace(pw);
        } else {
            super.printStackTrace(pw);
        }
    }
}
